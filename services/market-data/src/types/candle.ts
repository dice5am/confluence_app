/** MD-1.1 candle contract types (v0.1). */

export type Venue = 'binance' | 'bybit';

export type Timeframe = '1m' | '5m' | '15m' | '1h' | '4h' | '1d' | '1w';

export const TIMEFRAMES: readonly Timeframe[] = [
  '1m',
  '5m',
  '15m',
  '1h',
  '4h',
  '1d',
  '1w',
] as const;

/** Primary key: (venue, symbol, timeframe, openTimeMs) */
export interface Candle {
  venue: Venue;
  symbol: string;
  timeframe: Timeframe;
  openTimeMs: number;
  closeTimeMs: number;
  open: number;
  high: number;
  low: number;
  close: number;
  volume: number;
  isFinal: boolean;
  sourceTsMs: number;
  ingestTsMs: number;
}

export type HealthStatus = 'ok' | 'degraded' | 'stale' | 'disconnected';

export interface Health {
  status: HealthStatus;
  lastSourceTsMs: number;
  venue: string;
  symbol: string;
  gapCount?: number;
  activeTimeframes?: Timeframe[];
  note?: string;
}
