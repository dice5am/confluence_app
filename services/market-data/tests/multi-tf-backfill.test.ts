import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { CandleStore } from '../src/store/candle-store.js';
import {
  backfillAllTimeframes,
  backfillTimeframes,
} from '../src/backfill/multi-tf.js';
import {
  KLINES_REQUEST_WEIGHT,
  RestWeightBudget,
} from '../src/binance/weight-budget.js';
import { TIMEFRAMES, type Candle, type Timeframe } from '../src/types/candle.js';
import { resolveVenueNativeInterval } from '../src/policy/tf-policy.js';

function makeCandle(tf: Timeframe, openTimeMs: number, close = 100): Candle {
  return {
    venue: 'binance',
    symbol: 'BTCUSDT',
    timeframe: tf,
    openTimeMs,
    closeTimeMs: openTimeMs + 59_999,
    open: close,
    high: close + 1,
    low: close - 1,
    close,
    volume: 1,
    isFinal: true,
    sourceTsMs: openTimeMs + 59_999,
    ingestTsMs: openTimeMs + 60_000,
  };
}

describe('MD-2.2 multi-TF REST backfill', () => {
  let dbPath: string;
  let store: CandleStore;

  beforeEach(() => {
    const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'md-backfill-'));
    dbPath = path.join(dir, 'candles.sqlite');
    store = new CandleStore({ dbPath });
  });

  afterEach(() => {
    store.close();
    fs.rmSync(path.dirname(dbPath), { recursive: true, force: true });
  });

  it('backfills all 7 TFs via venue-native intervals into the store', async () => {
    const seen: Timeframe[] = [];
    const fetchKlines = vi.fn(async (opts: { timeframe: Timeframe }) => {
      seen.push(opts.timeframe);
      expect(resolveVenueNativeInterval(opts.timeframe)).toBe(opts.timeframe);
      return [makeCandle(opts.timeframe, 1_700_000_000_000 + TIMEFRAMES.indexOf(opts.timeframe))];
    });

    const budget = new RestWeightBudget({
      limitPerMinute: 1000,
      gapFillReserve: 100,
      nowMs: () => 0,
      sleep: async () => undefined,
    });

    const result = await backfillAllTimeframes({
      store,
      fetchKlines,
      weightBudget: budget,
      maxBarsPerTf: 1,
      concurrency: 1,
    });

    expect(seen).toEqual([...TIMEFRAMES]);
    expect(result.results).toHaveLength(7);
    expect(result.totalUpserted).toBe(7);
    expect(result.weightSpent).toBe(7 * KLINES_REQUEST_WEIGHT);
    for (const tf of TIMEFRAMES) {
      expect(store.get('binance', 'BTCUSDT', tf, 1_700_000_000_000 + TIMEFRAMES.indexOf(tf))).toBeTruthy();
      expect(result.results.find((r) => r.timeframe === tf)?.binanceInterval).toBe(tf);
    }
  });

  it('serializes acquires so a tight budget does not oversubscribe', async () => {
    let now = 0;
    const sleeps: number[] = [];
    const budget = new RestWeightBudget({
      limitPerMinute: 6, // non-reserve cap = 4 → only two klines weight=2
      gapFillReserve: 2,
      windowMs: 1_000,
      nowMs: () => now,
      sleep: async (ms) => {
        sleeps.push(ms);
        now += ms;
      },
    });

    const fetchKlines = vi.fn(async (opts: { timeframe: Timeframe }) => [
      makeCandle(opts.timeframe, 1_000),
    ]);

    await backfillTimeframes({
      store,
      timeframes: ['1m', '5m', '15m'],
      fetchKlines,
      weightBudget: budget,
      concurrency: 1,
      maxBarsPerTf: 1,
    });

    expect(fetchKlines).toHaveBeenCalledTimes(3);
    expect(sleeps.length).toBeGreaterThanOrEqual(1);
    expect(store.count()).toBe(3);
  });

  it('rejects non-binance venues', async () => {
    await expect(
      backfillTimeframes({
        store,
        venue: 'bybit',
        timeframes: ['1m'],
        fetchKlines: async () => [],
      }),
    ).rejects.toThrow(/Binance REST only/);
  });
});
