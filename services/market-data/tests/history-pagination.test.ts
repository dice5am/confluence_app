import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { CandleStore } from '../src/store/candle-store.js';
import type { Candle } from '../src/types/candle.js';
import {
  queryClosedHistory,
  nextHistoryPageQuery,
  HISTORY_DEFAULT_LIMIT,
} from '../src/api/index.js';

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

describe('MD-2.7 history pagination + resume', () => {
  let dbPath: string;
  let store: CandleStore;
  const t0 = 1_700_000_000_000;
  const nowMs = t0 + 60_000 * 20;

  beforeEach(() => {
    const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'md-hist-page-'));
    dbPath = path.join(dir, 'candles.sqlite');
    store = new CandleStore({ dbPath });
    // 10 closed 1m bars
    for (let i = 0; i < 10; i++) {
      store.upsert(
        makeCandle({
          openTimeMs: t0 + i * 60_000,
          closeTimeMs: t0 + i * 60_000 + 59_999,
          close: 100 + i,
          isFinal: true,
        }),
      );
    }
  });

  afterEach(() => {
    store.close();
    fs.rmSync(path.dirname(dbPath), { recursive: true, force: true });
  });

  it('pages with limit and exposes nextFromMs / nextCursor', () => {
    const page1 = queryClosedHistory(
      store,
      { timeframe: '1m', fromMs: t0, toMs: t0 + 60_000 * 9, limit: 4 },
      nowMs,
    );
    expect(page1.candles).toHaveLength(4);
    expect(page1.hasMore).toBe(true);
    expect(page1.nextCursor).toBe(t0 + 3 * 60_000);
    expect(page1.nextFromMs).toBe(t0 + 3 * 60_000 + 1);
    expect(page1.limit).toBe(4);

    const page2 = queryClosedHistory(
      store,
      {
        timeframe: '1m',
        fromMs: page1.nextFromMs!,
        toMs: t0 + 60_000 * 9,
        limit: 4,
      },
      nowMs,
    );
    expect(page2.candles.map((c) => c.openTimeMs)).toEqual([
      t0 + 4 * 60_000,
      t0 + 5 * 60_000,
      t0 + 6 * 60_000,
      t0 + 7 * 60_000,
    ]);
    expect(page2.hasMore).toBe(true);

    const page3 = queryClosedHistory(
      store,
      nextHistoryPageQuery(page2)!,
      nowMs,
    );
    expect(page3.candles).toHaveLength(2);
    expect(page3.hasMore).toBe(false);
    expect(page3.nextFromMs).toBeUndefined();
    expect(nextHistoryPageQuery(page3)).toBeNull();
  });

  it('resumes after app kill via cursor (exclusive of last open)', () => {
    // Client had applied first 6 bars, then app killed
    const lastApplied = t0 + 5 * 60_000;
    const resumed = queryClosedHistory(
      store,
      {
        timeframe: '1m',
        fromMs: t0, // original lookback
        toMs: t0 + 60_000 * 9,
        cursor: lastApplied,
        limit: 100,
      },
      nowMs,
    );
    // Exclusive: must NOT re-include lastApplied
    expect(resumed.effectiveFromMs).toBe(lastApplied + 1);
    expect(resumed.candles[0]!.openTimeMs).toBe(t0 + 6 * 60_000);
    expect(resumed.candles).toHaveLength(4);
    expect(resumed.hasMore).toBe(false);
  });

  it('defaults limit to HISTORY_DEFAULT_LIMIT', () => {
    const result = queryClosedHistory(
      store,
      { timeframe: '1m', fromMs: t0, toMs: t0 + 60_000 * 9 },
      nowMs,
    );
    expect(result.limit).toBe(HISTORY_DEFAULT_LIMIT);
    expect(result.hasMore).toBe(false);
    expect(result.candles).toHaveLength(10);
  });

  it('does not skip or dup across exclusive resume boundary', () => {
    const all: number[] = [];
    let q = {
      timeframe: '1m' as const,
      fromMs: t0,
      toMs: t0 + 60_000 * 9,
      limit: 3,
    };
    let page = queryClosedHistory(store, q, nowMs);
    for (;;) {
      for (const c of page.candles) all.push(c.openTimeMs);
      const next = nextHistoryPageQuery(page);
      if (!next) break;
      page = queryClosedHistory(store, next, nowMs);
    }
    expect(all).toEqual([
      t0,
      t0 + 60_000,
      t0 + 120_000,
      t0 + 180_000,
      t0 + 240_000,
      t0 + 300_000,
      t0 + 360_000,
      t0 + 420_000,
      t0 + 480_000,
      t0 + 540_000,
    ]);
    expect(new Set(all).size).toBe(10);
  });
});
