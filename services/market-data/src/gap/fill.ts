import {
  fetchKlinesPaginated,
  type PaginateKlinesOptions,
} from '../binance/rest.js';
import {
  KLINES_REQUEST_WEIGHT,
  RestWeightBudget,
  getSharedRestWeightBudget,
} from '../binance/weight-budget.js';
import type { CandleStore } from '../store/candle-store.js';
import type { Candle, Venue } from '../types/candle.js';
import {
  detectOneMinuteGaps,
  type DetectGapsOptions,
  type DetectGapsResult,
  type GapRange,
} from './detect.js';

export type FetchKlinesFn = (
  opts: PaginateKlinesOptions,
) => Promise<Candle[]>;

export interface FillGapsOptions extends DetectGapsOptions {
  store: CandleStore;
  /**
   * Injectable REST fetch (defaults to Binance fetchKlinesPaginated).
   * Tests pass a mock that returns controlled candles.
   */
  fetchKlines?: FetchKlinesFn;
  /** Forwarded to REST adapter (baseUrl, timeout, fetchImpl, nowMs, …). */
  rest?: Omit<PaginateKlinesOptions, 'timeframe' | 'symbol' | 'startTimeMs' | 'endTimeMs' | 'maxBars'>;
  /** Only binance is supported (existing REST adapter). */
  venue: Venue;
  /**
   * Shared with MD-2.2 multi-TF backfill. Gap-fill uses allowReserve so
   * it can spend the soft reserve band and avoid racing backfill into 429.
   */
  weightBudget?: RestWeightBudget;
}

export interface FillGapsResult extends DetectGapsResult {
  /** How many missing bars were written into the store */
  filledCount: number;
  /** Candles upserted (for assertions) */
  filled: Candle[];
}

/**
 * MD-1.5: detect 1m openTimeMs gaps for a venue/symbol, fill via Binance REST
 * into CandleStore, and return gap/fill counts for health.
 *
 * Non-binance venues throw — only the Binance public REST adapter exists.
 */
export async function fillOneMinuteGaps(
  opts: FillGapsOptions,
): Promise<FillGapsResult> {
  if (opts.venue !== 'binance') {
    throw new Error(
      `fillOneMinuteGaps: venue "${opts.venue}" not supported (Binance REST only)`,
    );
  }

  const detected = detectOneMinuteGaps(opts.store, opts);
  if (detected.gapCount === 0) {
    return { ...detected, filledCount: 0, filled: [] };
  }

  const fetchFn = opts.fetchKlines ?? fetchKlinesPaginated;
  const budget = opts.weightBudget ?? getSharedRestWeightBudget();
  const filled: Candle[] = [];

  for (const range of detected.ranges) {
    await budget.acquire(KLINES_REQUEST_WEIGHT, { allowReserve: true });
    const page = await fetchFn({
      ...opts.rest,
      symbol: opts.symbol,
      timeframe: '1m',
      startTimeMs: range.fromOpenMs,
      endTimeMs: range.toOpenMs,
      maxBars: range.missingCount + 2, // small slack for venue edge
    });

    const wanted = new Set(expandRange(range));
    for (const candle of page) {
      if (!wanted.has(candle.openTimeMs)) continue;
      // Mark reconciled gap-fill on optional quality flag (in-memory; store ignores extras).
      const row: Candle = { ...candle, isFinal: true };
      opts.store.upsert(row);
      filled.push(row);
      wanted.delete(candle.openTimeMs);
    }
  }

  return {
    ...detected,
    filledCount: filled.length,
    filled,
  };
}

function expandRange(range: GapRange): number[] {
  const out: number[] = [];
  for (let t = range.fromOpenMs; t <= range.toOpenMs; t += 60_000) {
    out.push(t);
  }
  return out;
}
