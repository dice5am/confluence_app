import type { CandleStore } from '../store/candle-store.js';
import type { Candle, Timeframe, Venue } from '../types/candle.js';
import { TIMEFRAMES } from '../types/candle.js';
import { clampHistoryFromMs } from './depth.js';

/** Default page size for history GET (MD-2.7). */
export const HISTORY_DEFAULT_LIMIT = 500;
/** Hard ceiling per response (MD-2.7). */
export const HISTORY_MAX_LIMIT = 1000;

export interface HistoryQuery {
  venue?: Venue | string;
  symbol?: string;
  timeframe: Timeframe | string;
  fromMs: number;
  toMs: number;
  /**
   * Max closed candles in this page (default 500, max 1000).
   * MD-2.7 pagination.
   */
  limit?: number;
  /**
   * Opaque resume cursor = last page's final `openTimeMs`.
   * Next page uses exclusive lower bound: openTimeMs > cursor
   * (i.e. effectiveFrom = max(depthClamp(fromMs), cursor + 1)).
   * Prefer this after app kill so clients do not re-bootstrap the full range.
   */
  cursor?: number;
}

export interface HistoryResult {
  venue: Venue;
  symbol: string;
  timeframe: Timeframe;
  fromMs: number;
  toMs: number;
  /** Effective inclusive lower bound after depth-cap + cursor resume. */
  effectiveFromMs: number;
  truncated: boolean;
  /** Page size applied. */
  limit: number;
  /** True when more closed candles exist after this page within [effectiveFrom, toMs]. */
  hasMore: boolean;
  /**
   * Inclusive openTimeMs of the last candle in this page (omit when empty).
   * Pass back as `cursor` on the next request for exclusive resume.
   */
  nextCursor?: number;
  /**
   * Convenience resume lower bound: lastOpenTimeMs + 1 (omit when empty / !hasMore).
   * Documented from/to resume: next GET with fromMs=nextFromMs (same toMs).
   */
  nextFromMs?: number;
  /** Closed candles only (isFinal=true), ascending openTimeMs. */
  candles: Candle[];
}

const VENUES: readonly Venue[] = ['binance', 'bybit'];

export function parseTimeframe(raw: string): Timeframe | null {
  return (TIMEFRAMES as readonly string[]).includes(raw)
    ? (raw as Timeframe)
    : null;
}

export function parseVenue(raw: string | undefined): Venue | null {
  if (raw === undefined || raw === '') return 'binance';
  return (VENUES as readonly string[]).includes(raw) ? (raw as Venue) : null;
}

/**
 * Clamp / normalize a requested page limit.
 */
export function normalizeHistoryLimit(raw: number | undefined): number {
  if (raw === undefined || !Number.isFinite(raw)) {
    return HISTORY_DEFAULT_LIMIT;
  }
  const n = Math.floor(raw);
  if (n < 1) {
    throw new HistoryQueryError(
      'invalid_limit',
      `limit must be between 1 and ${HISTORY_MAX_LIMIT}`,
    );
  }
  return Math.min(n, HISTORY_MAX_LIMIT);
}

/**
 * MD-1.7 + MD-2.7 history GET: closed-only candles from CandleStore with
 * range pagination + resume cursor / nextFromMs.
 *
 * Resume after app kill (no full re-bootstrap):
 * 1. Client keeps last applied `openTimeMs` (or `nextCursor` from prior page)
 * 2. Re-request with same `toMs` and either `cursor=<lastOpen>` or
 *    `fromMs=<lastOpen+1>` (exclusive of the already-applied bar)
 * 3. Upsert by primary key — overlap is harmless
 *
 * Never returns raw exchange payloads.
 */
export function queryClosedHistory(
  store: CandleStore,
  query: HistoryQuery,
  nowMs: number = Date.now(),
): HistoryResult {
  const venue = parseVenue(
    query.venue === undefined ? undefined : String(query.venue),
  );
  if (!venue) {
    throw new HistoryQueryError('invalid_venue', 'venue must be binance|bybit');
  }

  const timeframe = parseTimeframe(String(query.timeframe));
  if (!timeframe) {
    throw new HistoryQueryError(
      'invalid_timeframe',
      `timeframe must be one of ${TIMEFRAMES.join('|')}`,
    );
  }

  const symbol =
    query.symbol && query.symbol.length > 0 ? query.symbol : 'BTCUSDT';

  if (!Number.isFinite(query.fromMs) || !Number.isFinite(query.toMs)) {
    throw new HistoryQueryError(
      'invalid_range',
      'fromMs and toMs must be finite numbers',
    );
  }
  if (query.fromMs > query.toMs) {
    throw new HistoryQueryError(
      'invalid_range',
      'fromMs must be <= toMs',
    );
  }

  if (query.cursor !== undefined && !Number.isFinite(query.cursor)) {
    throw new HistoryQueryError(
      'invalid_cursor',
      'cursor must be a finite number (last openTimeMs)',
    );
  }

  const limit = normalizeHistoryLimit(query.limit);

  const { effectiveFromMs: depthFrom, truncated } = clampHistoryFromMs(
    timeframe,
    query.fromMs,
    nowMs,
  );

  // Cursor resume: exclusive of the last-seen openTimeMs (MD-2.7).
  let effectiveFromMs = depthFrom;
  if (query.cursor !== undefined) {
    const resumeFrom = query.cursor + 1;
    if (resumeFrom > effectiveFromMs) {
      effectiveFromMs = resumeFrom;
    }
  }

  if (effectiveFromMs > query.toMs) {
    return {
      venue,
      symbol,
      timeframe,
      fromMs: query.fromMs,
      toMs: query.toMs,
      effectiveFromMs,
      truncated,
      limit,
      hasMore: false,
      candles: [],
    };
  }

  // Fetch one extra row to detect hasMore without a second query.
  const fetched = store.queryRange({
    venue,
    symbol,
    timeframe,
    fromMs: effectiveFromMs,
    toMs: query.toMs,
    closedOnly: true,
    limit: limit + 1,
  });

  const hasMore = fetched.length > limit;
  const candles = hasMore ? fetched.slice(0, limit) : fetched;

  const result: HistoryResult = {
    venue,
    symbol,
    timeframe,
    fromMs: query.fromMs,
    toMs: query.toMs,
    effectiveFromMs,
    truncated,
    limit,
    hasMore,
    candles,
  };

  if (candles.length > 0) {
    const lastOpen = candles[candles.length - 1]!.openTimeMs;
    result.nextCursor = lastOpen;
    if (hasMore) {
      result.nextFromMs = lastOpen + 1;
    }
  }

  return result;
}

/**
 * Build the next HistoryQuery for resume / pagination (MD-2.7).
 * Returns null when the previous page reported hasMore=false or was empty.
 */
export function nextHistoryPageQuery(
  previous: HistoryResult,
): HistoryQuery | null {
  if (!previous.hasMore || previous.nextFromMs === undefined) {
    return null;
  }
  return {
    venue: previous.venue,
    symbol: previous.symbol,
    timeframe: previous.timeframe,
    fromMs: previous.nextFromMs,
    toMs: previous.toMs,
    limit: previous.limit,
    cursor: previous.nextCursor,
  };
}

export class HistoryQueryError extends Error {
  readonly code: string;

  constructor(code: string, message: string) {
    super(message);
    this.name = 'HistoryQueryError';
    this.code = code;
  }
}
