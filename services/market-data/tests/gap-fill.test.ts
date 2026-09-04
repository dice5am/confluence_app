import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import {
  detectOneMinuteGaps,
  fillOneMinuteGaps,
  missingToRanges,
  ONE_MINUTE_MS,
} from '../src/gap/index.js';
import { CandleStore } from '../src/store/candle-store.js';
import type { Candle } from '../src/types/candle.js';

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

function restCandle(openTimeMs: number, close: number): Candle {
  return makeCandle({
    openTimeMs,
    closeTimeMs: openTimeMs + ONE_MINUTE_MS - 1,
    close,
    sourceTsMs: openTimeMs + ONE_MINUTE_MS - 1,
    ingestTsMs: openTimeMs + ONE_MINUTE_MS,
  });
}

describe('detectOneMinuteGaps (MD-1.5)', () => {
  let dbPath: string;
  let store: CandleStore;

  beforeEach(() => {
    const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'md-gap-'));
    dbPath = path.join(dir, 'candles.sqlite');
    store = new CandleStore({ dbPath });
  });

  afterEach(() => {
    store.close();
    try {
      fs.rmSync(path.dirname(dbPath), { recursive: true, force: true });
    } catch {
      /* ignore */
    }
  });

  it('returns zero gaps for contiguous 1m sequence', () => {
    const t0 = 1_700_000_000_000;
    for (let i = 0; i < 5; i++) {
      store.upsert(makeCandle({ openTimeMs: t0 + i * ONE_MINUTE_MS, close: 100 + i }));
    }
    const result = detectOneMinuteGaps(store, {
      venue: 'binance',
      symbol: 'BTCUSDT',
    });
    expect(result.gapCount).toBe(0);
    expect(result.missingOpenTimeMs).toEqual([]);
    expect(result.ranges).toEqual([]);
  });

  it('detects interior missing openTimeMs on 1m', () => {
    const t0 = 1_700_000_000_000;
    // bars 0,1,3,4 — missing bar 2
    for (const i of [0, 1, 3, 4]) {
      store.upsert(makeCandle({ openTimeMs: t0 + i * ONE_MINUTE_MS }));
    }
    const result = detectOneMinuteGaps(store, {
      venue: 'binance',
      symbol: 'BTCUSDT',
    });
    expect(result.missingOpenTimeMs).toEqual([t0 + 2 * ONE_MINUTE_MS]);
    expect(result.gapCount).toBe(1);
    expect(result.ranges).toEqual([
      {
        fromOpenMs: t0 + 2 * ONE_MINUTE_MS,
        toOpenMs: t0 + 2 * ONE_MINUTE_MS,
        missingCount: 1,
      },
    ]);
  });

  it('detects multi-bar contiguous hole and edge gaps against explicit window', () => {
    const t0 = 1_700_000_000_000;
    // store bars 2,3 only; window 0..5 → missing 0,1 and 4,5
    store.upsert(makeCandle({ openTimeMs: t0 + 2 * ONE_MINUTE_MS }));
    store.upsert(makeCandle({ openTimeMs: t0 + 3 * ONE_MINUTE_MS }));

    const result = detectOneMinuteGaps(store, {
      venue: 'binance',
      symbol: 'BTCUSDT',
      fromMs: t0,
      toMs: t0 + 5 * ONE_MINUTE_MS,
    });
    expect(result.missingOpenTimeMs).toEqual([
      t0,
      t0 + ONE_MINUTE_MS,
      t0 + 4 * ONE_MINUTE_MS,
      t0 + 5 * ONE_MINUTE_MS,
    ]);
    expect(result.gapCount).toBe(4);
    expect(result.ranges).toEqual([
      { fromOpenMs: t0, toOpenMs: t0 + ONE_MINUTE_MS, missingCount: 2 },
      {
        fromOpenMs: t0 + 4 * ONE_MINUTE_MS,
        toOpenMs: t0 + 5 * ONE_MINUTE_MS,
        missingCount: 2,
      },
    ]);
  });

  it('scopes by venue/symbol and ignores other TFs', () => {
    const t0 = 1_700_000_000_000;
    store.upsert(makeCandle({ openTimeMs: t0 }));
    store.upsert(makeCandle({ openTimeMs: t0 + 2 * ONE_MINUTE_MS }));
    store.upsert(
      makeCandle({
        timeframe: '5m',
        openTimeMs: t0 + ONE_MINUTE_MS,
      }),
    );
    store.upsert(
      makeCandle({
        venue: 'bybit',
        openTimeMs: t0 + ONE_MINUTE_MS,
      }),
    );

    const result = detectOneMinuteGaps(store, {
      venue: 'binance',
      symbol: 'BTCUSDT',
    });
    expect(result.missingOpenTimeMs).toEqual([t0 + ONE_MINUTE_MS]);
  });

  it('missingToRanges collapses contiguous opens', () => {
    const t0 = 1_000;
    expect(
      missingToRanges([t0, t0 + 60_000, t0 + 120_000, t0 + 300_000]),
    ).toEqual([
      { fromOpenMs: t0, toOpenMs: t0 + 120_000, missingCount: 3 },
      { fromOpenMs: t0 + 300_000, toOpenMs: t0 + 300_000, missingCount: 1 },
    ]);
  });
});

describe('fillOneMinuteGaps (MD-1.5)', () => {
  let dbPath: string;
  let store: CandleStore;

  beforeEach(() => {
    const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'md-gap-fill-'));
    dbPath = path.join(dir, 'candles.sqlite');
    store = new CandleStore({ dbPath });
  });

  afterEach(() => {
    store.close();
    try {
      fs.rmSync(path.dirname(dbPath), { recursive: true, force: true });
    } catch {
      /* ignore */
    }
  });

  it('fills detected gaps via mocked REST into CandleStore', async () => {
    const t0 = 1_700_000_000_000;
    store.upsert(makeCandle({ openTimeMs: t0, close: 1 }));
    store.upsert(makeCandle({ openTimeMs: t0 + 3 * ONE_MINUTE_MS, close: 4 }));

    const fetchKlines = vi.fn(async (opts: { startTimeMs?: number; endTimeMs?: number }) => {
      const start = opts.startTimeMs ?? t0;
      const end = opts.endTimeMs ?? start;
      const out: Candle[] = [];
      for (let t = start; t <= end; t += ONE_MINUTE_MS) {
        out.push(restCandle(t, t === t0 + ONE_MINUTE_MS ? 2 : 3));
      }
      return out;
    });

    const result = await fillOneMinuteGaps({
      store,
      venue: 'binance',
      symbol: 'BTCUSDT',
      fetchKlines,
    });

    expect(result.gapCount).toBe(2);
    expect(result.filledCount).toBe(2);
    expect(fetchKlines).toHaveBeenCalledTimes(1);
    expect(store.get('binance', 'BTCUSDT', '1m', t0 + ONE_MINUTE_MS)?.close).toBe(2);
    expect(store.get('binance', 'BTCUSDT', '1m', t0 + 2 * ONE_MINUTE_MS)?.close).toBe(3);

    const after = detectOneMinuteGaps(store, {
      venue: 'binance',
      symbol: 'BTCUSDT',
    });
    expect(after.gapCount).toBe(0);
  });

  it('no-ops when store sequence is contiguous', async () => {
    const t0 = 1_700_000_000_000;
    for (let i = 0; i < 3; i++) {
      store.upsert(makeCandle({ openTimeMs: t0 + i * ONE_MINUTE_MS }));
    }
    const fetchKlines = vi.fn(async () => []);
    const result = await fillOneMinuteGaps({
      store,
      venue: 'binance',
      symbol: 'BTCUSDT',
      fetchKlines,
    });
    expect(result.filledCount).toBe(0);
    expect(fetchKlines).not.toHaveBeenCalled();
  });

  it('rejects non-binance venue', async () => {
    await expect(
      fillOneMinuteGaps({
        store,
        venue: 'bybit',
        symbol: 'BTCUSDT',
      }),
    ).rejects.toThrow(/Binance REST only/);
  });
});
