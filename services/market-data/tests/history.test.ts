import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { CandleStore } from '../src/store/candle-store.js';
import type { Candle } from '../src/types/candle.js';
import {
  HISTORY_DEPTH_MS,
  clampHistoryFromMs,
  queryClosedHistory,
  HistoryQueryError,
} from '../src/api/index.js';

const DAY_MS = 24 * 60 * 60 * 1000;

function makeCandle(overrides: Partial<Candle> = {}): Candle {
  return {
    venue: 'binance',
    symbol: 'BTCUSDT',
    timeframe: '1m',
    openTimeMs: 1_700_000_000_000,
    closeTimeMs: 1_700_000_059_999,
    open: 42000,
    high: 42100,
    low: 41900,
    close: 42050,
    volume: 12.5,
    isFinal: true,
    sourceTsMs: 1_700_000_059_999,
    ingestTsMs: 1_700_000_100_000,
    ...overrides,
  };
}

describe('depth caps (MD-1.7)', () => {
  it('1m depth is 60d', () => {
    expect(HISTORY_DEPTH_MS['1m']).toBe(60 * DAY_MS);
  });

  it('clamps fromMs older than depth', () => {
    const now = 2_000_000_000_000;
    const tooOld = now - 90 * DAY_MS;
    const { effectiveFromMs, truncated } = clampHistoryFromMs('1m', tooOld, now);
    expect(truncated).toBe(true);
    expect(effectiveFromMs).toBe(now - 60 * DAY_MS);
  });

  it('does not clamp within depth', () => {
    const now = 2_000_000_000_000;
    const from = now - 10 * DAY_MS;
    const { effectiveFromMs, truncated } = clampHistoryFromMs('1m', from, now);
    expect(truncated).toBe(false);
    expect(effectiveFromMs).toBe(from);
  });

  it('1h has unbounded depth', () => {
    const now = 2_000_000_000_000;
    const from = 0;
    const { truncated, effectiveFromMs } = clampHistoryFromMs('1h', from, now);
    expect(truncated).toBe(false);
    expect(effectiveFromMs).toBe(0);
  });
});

describe('queryClosedHistory (MD-1.7)', () => {
  let dbPath: string;
  let store: CandleStore;

  beforeEach(() => {
    const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'md-history-'));
    dbPath = path.join(dir, 'candles.sqlite');
    store = new CandleStore({ dbPath });
  });

  afterEach(() => {
    store.close();
    fs.rmSync(path.dirname(dbPath), { recursive: true, force: true });
  });

  it('returns closed only, ascending openTimeMs', () => {
    const t0 = 1_700_000_000_000;
    store.upsertMany([
      makeCandle({ openTimeMs: t0 + 120_000, isFinal: true, close: 3 }),
      makeCandle({ openTimeMs: t0, isFinal: true, close: 1 }),
      makeCandle({ openTimeMs: t0 + 60_000, isFinal: false, close: 2 }),
      makeCandle({ openTimeMs: t0 + 60_000, isFinal: true, close: 2.5 }),
    ]);

    const result = queryClosedHistory(
      store,
      {
        timeframe: '1m',
        fromMs: t0,
        toMs: t0 + 200_000,
      },
      t0 + 200_000, // nowMs near fixtures so depth clamp does not wipe range
    );

    expect(result.candles).toHaveLength(3);
    expect(result.candles.every((c) => c.isFinal)).toBe(true);
    expect(result.candles.map((c) => c.openTimeMs)).toEqual([
      t0,
      t0 + 60_000,
      t0 + 120_000,
    ]);
    // No raw exchange fields
    for (const c of result.candles) {
      expect(c).not.toHaveProperty('e');
      expect(c).not.toHaveProperty('k');
      expect(Object.keys(c).sort()).toEqual(
        [
          'venue',
          'symbol',
          'timeframe',
          'openTimeMs',
          'closeTimeMs',
          'open',
          'high',
          'low',
          'close',
          'volume',
          'isFinal',
          'sourceTsMs',
          'ingestTsMs',
        ].sort(),
      );
    }
  });

  it('excludes forming-only bars', () => {
    const t0 = 1_700_000_000_000;
    store.upsert(makeCandle({ openTimeMs: t0, isFinal: false }));
    const result = queryClosedHistory(
      store,
      {
        timeframe: '1m',
        fromMs: t0,
        toMs: t0 + 1,
      },
      t0 + 1,
    );
    expect(result.candles).toHaveLength(0);
  });

  it('truncates by depth cap', () => {
    const now = 2_000_000_000_000;
    const inside = now - 10 * DAY_MS;
    const outside = now - 90 * DAY_MS;
    store.upsert(makeCandle({ openTimeMs: outside, isFinal: true }));
    store.upsert(makeCandle({ openTimeMs: inside, isFinal: true }));

    const result = queryClosedHistory(
      store,
      { timeframe: '1m', fromMs: outside, toMs: now },
      now,
    );
    expect(result.truncated).toBe(true);
    expect(result.effectiveFromMs).toBe(now - 60 * DAY_MS);
    expect(result.candles).toHaveLength(1);
    expect(result.candles[0]!.openTimeMs).toBe(inside);
  });

  it('rejects invalid timeframe', () => {
    expect(() =>
      queryClosedHistory(store, {
        timeframe: '2m',
        fromMs: 0,
        toMs: 1,
      }),
    ).toThrow(HistoryQueryError);
  });

  it('rejects fromMs > toMs', () => {
    expect(() =>
      queryClosedHistory(store, {
        timeframe: '1m',
        fromMs: 100,
        toMs: 50,
      }),
    ).toThrow(/fromMs must be <= toMs/);
  });
});
