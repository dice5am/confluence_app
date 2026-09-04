import {
  fetchKlinesPage,
  type FetchKlinesOptions,
  type PaginateKlinesOptions,
} from '../binance/rest.js';
import {
  KLINES_REQUEST_WEIGHT,
  RestWeightBudget,
  getSharedRestWeightBudget,
} from '../binance/weight-budget.js';
import { resolveVenueNativeInterval } from '../policy/tf-policy.js';
import type { CandleStore } from '../store/candle-store.js';
import type { Candle, Timeframe, Venue } from '../types/candle.js';
import { TIMEFRAMES } from '../types/candle.js';

export type FetchKlinesFn = (
  opts: PaginateKlinesOptions,
) => Promise<Candle[]>;

export interface BackfillTimeframeSpec {
  timeframe: Timeframe;
  /** Inclusive start open time (ms). Optional — omit for "latest N bars". */
  startTimeMs?: number;
  endTimeMs?: number;
  /** Max closed bars to pull for this TF (across pages). Default 500. */
  maxBars?: number;
}

export interface MultiTfBackfillOptions {
  store: CandleStore;
  venue?: Venue;
  symbol?: string;
  /**
   * TFs to backfill. Default: all 7 product TFs.
   * Each TF is fetched via venue-native Binance interval (MD-2.1).
   */
  timeframes?: readonly Timeframe[] | readonly BackfillTimeframeSpec[];
  /** Default maxBars when a TF spec omits it. */
  maxBarsPerTf?: number;
  /**
   * Shared weight budget with gap-fill. Defaults to process shared instance.
   */
  weightBudget?: RestWeightBudget;
  /**
   * Injectable batch fetch (tests). When set, each TF costs one weight acquire
   * and the mock returns the full candle set for that TF.
   */
  fetchKlines?: FetchKlinesFn;
  /** Forwarded to REST adapter (baseUrl, timeout, fetchImpl, nowMs). */
  rest?: Omit<
    FetchKlinesOptions,
    'timeframe' | 'symbol' | 'startTimeMs' | 'endTimeMs' | 'limit'
  >;
  /**
   * Max concurrent TF backfills. Default 1 (serialize) — safest vs 429.
   * Weight gate still serializes acquires even if concurrency > 1.
   */
  concurrency?: number;
}

export interface TfBackfillResult {
  timeframe: Timeframe;
  binanceInterval: string;
  upserted: number;
  candles: Candle[];
  requestPages: number;
}

export interface MultiTfBackfillResult {
  venue: Venue;
  symbol: string;
  results: TfBackfillResult[];
  totalUpserted: number;
  /** Weight units acquired for klines during this run. */
  weightSpent: number;
}

const DEFAULT_MAX_BARS = 500;
const PAGE_LIMIT = 1000;

function normalizeSpecs(
  timeframes: MultiTfBackfillOptions['timeframes'],
  maxBarsPerTf: number,
): BackfillTimeframeSpec[] {
  if (!timeframes || timeframes.length === 0) {
    return TIMEFRAMES.map((tf) => ({ timeframe: tf, maxBars: maxBarsPerTf }));
  }
  return timeframes.map((t) => {
    if (typeof t === 'string') {
      return { timeframe: t, maxBars: maxBarsPerTf };
    }
    return { ...t, maxBars: t.maxBars ?? maxBarsPerTf };
  });
}

/**
 * MD-2.2 — backfill multiple TFs into CandleStore via venue-native REST klines.
 *
 * - Uses MD-2.1 policy (no 1m rollup).
 * - Acquires {@link KLINES_REQUEST_WEIGHT} per REST page from a shared
 *   {@link RestWeightBudget} so backfill + MD-1.5 gap-fill cannot 429.
 * - Upserts closed bars (`isFinal=true`) idempotently by primary key.
 */
export async function backfillTimeframes(
  opts: MultiTfBackfillOptions,
): Promise<MultiTfBackfillResult> {
  const venue = opts.venue ?? 'binance';
  if (venue !== 'binance') {
    throw new Error(
      `backfillTimeframes: venue "${venue}" not supported (Binance REST only)`,
    );
  }
  const symbol = opts.symbol ?? 'BTCUSDT';
  const maxBarsPerTf = opts.maxBarsPerTf ?? DEFAULT_MAX_BARS;
  const specs = normalizeSpecs(opts.timeframes, maxBarsPerTf);
  const budget = opts.weightBudget ?? getSharedRestWeightBudget();
  const concurrency = Math.max(1, opts.concurrency ?? 1);

  let weightSpent = 0;

  async function fetchTf(spec: BackfillTimeframeSpec): Promise<TfBackfillResult> {
    const interval = resolveVenueNativeInterval(spec.timeframe);
    const maxBars = spec.maxBars ?? maxBarsPerTf;
    const candles: Candle[] = [];
    let requestPages = 0;

    if (opts.fetchKlines) {
      await budget.acquire(KLINES_REQUEST_WEIGHT);
      weightSpent += KLINES_REQUEST_WEIGHT;
      requestPages = 1;
      const page = await opts.fetchKlines({
        ...opts.rest,
        symbol,
        timeframe: spec.timeframe,
        startTimeMs: spec.startTimeMs,
        endTimeMs: spec.endTimeMs,
        maxBars,
      });
      candles.push(...page);
    } else {
      let startTimeMs = spec.startTimeMs;
      while (candles.length < maxBars) {
        await budget.acquire(KLINES_REQUEST_WEIGHT);
        weightSpent += KLINES_REQUEST_WEIGHT;
        requestPages += 1;

        const remaining = maxBars - candles.length;
        const page = await fetchKlinesPage({
          ...opts.rest,
          symbol,
          timeframe: spec.timeframe,
          startTimeMs,
          endTimeMs: spec.endTimeMs,
          limit: Math.min(PAGE_LIMIT, remaining),
        });
        if (page.length === 0) break;
        candles.push(...page);
        if (page.length < Math.min(PAGE_LIMIT, remaining)) break;
        const lastOpen = page[page.length - 1]!.openTimeMs;
        const nextStart = lastOpen + 1;
        if (spec.endTimeMs !== undefined && nextStart > spec.endTimeMs) break;
        if (startTimeMs !== undefined && nextStart <= startTimeMs) break;
        startTimeMs = nextStart;
      }
    }

    const closed = candles.filter((c) => c.isFinal).slice(0, maxBars);
    opts.store.upsertMany(closed);

    return {
      timeframe: spec.timeframe,
      binanceInterval: interval,
      upserted: closed.length,
      candles: closed,
      requestPages,
    };
  }

  const results: TfBackfillResult[] = new Array(specs.length);
  let idx = 0;
  async function worker(): Promise<void> {
    while (idx < specs.length) {
      const my = idx;
      idx += 1;
      results[my] = await fetchTf(specs[my]!);
    }
  }
  await Promise.all(
    Array.from({ length: Math.min(concurrency, specs.length) }, () =>
      worker(),
    ),
  );

  const ordered = results as TfBackfillResult[];
  return {
    venue,
    symbol,
    results: ordered,
    totalUpserted: ordered.reduce((s, r) => s + r.upserted, 0),
    weightSpent,
  };
}

/** Convenience: backfill all 7 product TFs with shared defaults. */
export async function backfillAllTimeframes(
  opts: Omit<MultiTfBackfillOptions, 'timeframes'>,
): Promise<MultiTfBackfillResult> {
  return backfillTimeframes({ ...opts, timeframes: TIMEFRAMES });
}
