import type { Candle, Timeframe } from '../types/candle.js';

/** Binance REST kline row (array form). */
export type BinanceKlineRow = [
  number, // open time
  string, // open
  string, // high
  string, // low
  string, // close
  string, // volume
  number, // close time
  string, // quote asset volume
  number, // number of trades
  string, // taker buy base
  string, // taker buy quote
  string, // ignore
];

export interface BinanceWsKlinePayload {
  e: string;
  E: number;
  s: string;
  k: {
    t: number;
    T: number;
    s: string;
    i: string;
    o: string;
    c: string;
    h: string;
    l: string;
    v: string;
    x: boolean;
  };
}

const INTERVAL_MAP: Record<Timeframe, string> = {
  '1m': '1m',
  '5m': '5m',
  '15m': '15m',
  '1h': '1h',
  '4h': '4h',
  '1d': '1d',
  '1w': '1w',
};

const INTERVAL_TO_TF: Record<string, Timeframe> = {
  '1m': '1m',
  '5m': '5m',
  '15m': '15m',
  '1h': '1h',
  '4h': '4h',
  '1d': '1d',
  '1w': '1w',
};

export function timeframeToBinanceInterval(tf: Timeframe): string {
  return INTERVAL_MAP[tf];
}

export function binanceIntervalToTimeframe(interval: string): Timeframe {
  const tf = INTERVAL_TO_TF[interval];
  if (!tf) throw new Error(`Unsupported Binance interval: ${interval}`);
  return tf;
}

export function mapRestKlineToCandle(
  row: BinanceKlineRow,
  opts: {
    timeframe: Timeframe;
    symbol?: string;
    ingestTsMs?: number;
    sourceTsMs?: number;
    /** REST closed bars are final. */
    isFinal?: boolean;
  },
): Candle {
  const ingestTsMs = opts.ingestTsMs ?? Date.now();
  const openTimeMs = row[0];
  const closeTimeMs = row[6];
  return {
    venue: 'binance',
    symbol: opts.symbol ?? 'BTCUSDT',
    timeframe: opts.timeframe,
    openTimeMs,
    closeTimeMs,
    open: Number(row[1]),
    high: Number(row[2]),
    low: Number(row[3]),
    close: Number(row[4]),
    volume: Number(row[5]),
    isFinal: opts.isFinal ?? true,
    sourceTsMs: opts.sourceTsMs ?? closeTimeMs,
    ingestTsMs,
  };
}

export function mapWsKlineToCandle(
  payload: BinanceWsKlinePayload,
  opts?: { ingestTsMs?: number },
): Candle {
  const k = payload.k;
  const timeframe = binanceIntervalToTimeframe(k.i);
  const ingestTsMs = opts?.ingestTsMs ?? Date.now();
  return {
    venue: 'binance',
    symbol: k.s || payload.s || 'BTCUSDT',
    timeframe,
    openTimeMs: k.t,
    closeTimeMs: k.T,
    open: Number(k.o),
    high: Number(k.h),
    low: Number(k.l),
    close: Number(k.c),
    volume: Number(k.v),
    isFinal: Boolean(k.x),
    sourceTsMs: payload.E,
    ingestTsMs,
  };
}
