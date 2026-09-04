import type { Candle, Timeframe } from '../types/candle.js';
import { BinanceApiError, classifyHttpStatus, isTimeoutError } from './errors.js';
import {
  mapRestKlineToCandle,
  timeframeToBinanceInterval,
  type BinanceKlineRow,
} from './map.js';
import { withRestRetry, type RestRetryPolicyOptions } from './reconnect-policy.js';

export const BINANCE_REST_BASE = 'https://api.binance.com';

export interface FetchKlinesOptions {
  symbol?: string;
  timeframe: Timeframe;
  /** Inclusive start open time (ms). */
  startTimeMs?: number;
  /** Inclusive end open time (ms) — Binance endTime is inclusive of open times up to this. */
  endTimeMs?: number;
  /** Max bars per request (Binance max 1000). */
  limit?: number;
  baseUrl?: string;
  timeoutMs?: number;
  fetchImpl?: typeof fetch;
  nowMs?: () => number;
  /**
   * MD-2.8: retry/backoff policy for 429/418/5xx/timeout.
   * Pass `{ maxRetries: 0 }` to disable retries (tests).
   */
  retry?: RestRetryPolicyOptions | false;
}

const DEFAULT_LIMIT = 1000;
const DEFAULT_TIMEOUT_MS = 15_000;

async function fetchWithTimeout(
  url: string,
  opts: { timeoutMs: number; fetchImpl: typeof fetch },
): Promise<Response> {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), opts.timeoutMs);
  try {
    return await opts.fetchImpl(url, { signal: controller.signal });
  } catch (err) {
    if (isTimeoutError(err) || (err instanceof Error && err.name === 'AbortError')) {
      throw new BinanceApiError('TIMEOUT', `Binance REST timeout after ${opts.timeoutMs}ms`, {
        cause: err,
      });
    }
    throw new BinanceApiError('NETWORK', `Binance REST network error: ${(err as Error).message}`, {
      cause: err,
    });
  } finally {
    clearTimeout(timer);
  }
}

/** Single-page klines fetch (MD-2.8: retries 429/418/5xx/timeout with backoff). */
export async function fetchKlinesPage(opts: FetchKlinesOptions): Promise<Candle[]> {
  const symbol = opts.symbol ?? 'BTCUSDT';
  const interval = timeframeToBinanceInterval(opts.timeframe);
  const limit = Math.min(opts.limit ?? DEFAULT_LIMIT, 1000);
  const baseUrl = opts.baseUrl ?? BINANCE_REST_BASE;
  const timeoutMs = opts.timeoutMs ?? DEFAULT_TIMEOUT_MS;
  const fetchImpl = opts.fetchImpl ?? fetch;
  const nowMs = opts.nowMs ?? Date.now;

  const params = new URLSearchParams({
    symbol,
    interval,
    limit: String(limit),
  });
  if (opts.startTimeMs !== undefined) params.set('startTime', String(opts.startTimeMs));
  if (opts.endTimeMs !== undefined) params.set('endTime', String(opts.endTimeMs));

  const url = `${baseUrl}/api/v3/klines?${params.toString()}`;

  const once = async (): Promise<Candle[]> => {
    const res = await fetchWithTimeout(url, { timeoutMs, fetchImpl });

    if (!res.ok) {
      const snippet = (await res.text().catch(() => '')).slice(0, 200);
      const headerRetry = res.headers.get('retry-after');
      let retryAfterMs: number | undefined;
      if (headerRetry) {
        const asNum = Number(headerRetry);
        if (Number.isFinite(asNum)) {
          // Retry-After may be seconds (typical) or ms if huge
          retryAfterMs = asNum > 1_000_000 ? asNum : asNum * 1000;
        }
      }
      throw classifyHttpStatus(res.status, snippet, { retryAfterMs });
    }

    let rows: BinanceKlineRow[];
    try {
      rows = (await res.json()) as BinanceKlineRow[];
    } catch (err) {
      throw new BinanceApiError('PARSE', 'Failed to parse Binance klines JSON', {
        cause: err,
      });
    }

    const ingestTsMs = nowMs();
    return rows.map((row) =>
      mapRestKlineToCandle(row, {
        timeframe: opts.timeframe,
        symbol,
        ingestTsMs,
        isFinal: true,
      }),
    );
  };

  if (opts.retry === false) {
    return once();
  }
  return withRestRetry(once, opts.retry ?? {});
}

export interface PaginateKlinesOptions extends FetchKlinesOptions {
  /** Stop after this many bars (across pages). */
  maxBars?: number;
}

/**
 * Paginate Binance klines forward from startTimeMs.
 * Uses openTime of last bar + 1 as next startTime.
 */
export async function fetchKlinesPaginated(opts: PaginateKlinesOptions): Promise<Candle[]> {
  const pageLimit = Math.min(opts.limit ?? DEFAULT_LIMIT, 1000);
  const maxBars = opts.maxBars ?? pageLimit;
  const out: Candle[] = [];
  let startTimeMs = opts.startTimeMs;

  while (out.length < maxBars) {
    const remaining = maxBars - out.length;
    const page = await fetchKlinesPage({
      ...opts,
      startTimeMs,
      limit: Math.min(pageLimit, remaining),
    });
    if (page.length === 0) break;

    out.push(...page);
    if (page.length < Math.min(pageLimit, remaining)) break;

    const lastOpen = page[page.length - 1]!.openTimeMs;
    const nextStart = lastOpen + 1;
    if (opts.endTimeMs !== undefined && nextStart > opts.endTimeMs) break;
    if (startTimeMs !== undefined && nextStart <= startTimeMs) break;
    startTimeMs = nextStart;
  }

  return out.slice(0, maxBars);
}

/** Fetch closed klines for all Phase-1 timeframes (single page each by default). */
export async function fetchAllTimeframeKlines(
  opts: Omit<FetchKlinesOptions, 'timeframe'> & { timeframes: Timeframe[] },
): Promise<Record<Timeframe, Candle[]>> {
  const result = {} as Record<Timeframe, Candle[]>;
  for (const tf of opts.timeframes) {
    result[tf] = await fetchKlinesPage({ ...opts, timeframe: tf });
  }
  return result;
}
