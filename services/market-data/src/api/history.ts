import type { CandleStore } from '../store/candle-store.js';
import type { Candle, Timeframe, Venue } from '../types/candle.js';
import { TIMEFRAMES } from '../types/candle.js';
import { clampHistoryFromMs } from './depth.js';

export interface HistoryQuery {
  venue?: Venue | string;
  symbol?: string;
  timeframe: Timeframe | string;
  fromMs: number;
  toMs: number;
}

export interface HistoryResult {
  venue: Venue;
  symbol: string;
  timeframe: Timeframe;
  fromMs: number;
  toMs: number;
  /** Effective lower bound after depth-cap clamp (may be > requested fromMs). */
  effectiveFromMs: number;
  truncated: boolean;
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
 * MD-1.7 history GET: closed-only candles from CandleStore.
 * Respects MD-1.1 depth caps (truncates fromMs when older than stored depth).
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

  const { effectiveFromMs, truncated } = clampHistoryFromMs(
    timeframe,
    query.fromMs,
    nowMs,
  );

  const candles = store.queryRange({
    venue,
    symbol,
    timeframe,
    fromMs: effectiveFromMs,
    toMs: query.toMs,
    closedOnly: true,
  });

  return {
    venue,
    symbol,
    timeframe,
    fromMs: query.fromMs,
    toMs: query.toMs,
    effectiveFromMs,
    truncated,
    candles,
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
