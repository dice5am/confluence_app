import { describe, expect, it } from 'vitest';
import { mapRestKlineToCandle, mapWsKlineToCandle } from '../src/binance/map.js';
import type { BinanceKlineRow } from '../src/binance/map.js';

describe('mapRestKlineToCandle', () => {
  it('maps Binance REST row to MD-1.1 candle with isFinal=true', () => {
    const row: BinanceKlineRow = [
      1_700_000_000_000,
      '42000.1',
      '42100.5',
      '41900.0',
      '42050.25',
      '12.5',
      1_700_000_059_999,
      '500000',
      100,
      '6',
      '250000',
      '0',
    ];
    const c = mapRestKlineToCandle(row, {
      timeframe: '1m',
      ingestTsMs: 1_700_000_100_000,
    });
    expect(c).toMatchObject({
      venue: 'binance',
      symbol: 'BTCUSDT',
      timeframe: '1m',
      openTimeMs: 1_700_000_000_000,
      closeTimeMs: 1_700_000_059_999,
      open: 42000.1,
      high: 42100.5,
      low: 41900.0,
      close: 42050.25,
      volume: 12.5,
      isFinal: true,
      ingestTsMs: 1_700_000_100_000,
    });
    expect(c.sourceTsMs).toBe(1_700_000_059_999);
  });
});

describe('mapWsKlineToCandle', () => {
  it('maps forming and final WS klines', () => {
    const base = {
      e: 'kline',
      E: 1_700_000_030_000,
      s: 'BTCUSDT',
      k: {
        t: 1_700_000_000_000,
        T: 1_700_000_059_999,
        s: 'BTCUSDT',
        i: '1m',
        o: '100',
        h: '110',
        l: '90',
        c: '105',
        v: '3.2',
        x: false,
      },
    };
    const forming = mapWsKlineToCandle(base, { ingestTsMs: 99 });
    expect(forming.isFinal).toBe(false);
    expect(forming.sourceTsMs).toBe(1_700_000_030_000);
    expect(forming.ingestTsMs).toBe(99);

    const closed = mapWsKlineToCandle({ ...base, k: { ...base.k, x: true } }, { ingestTsMs: 100 });
    expect(closed.isFinal).toBe(true);
  });
});
