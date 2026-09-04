import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
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

describe('CandleStore (MD-1.4)', () => {
  let dbPath: string;
  let store: CandleStore;

  beforeEach(() => {
    const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'md-candle-store-'));
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

  it('persists and retrieves by primary key', () => {
    const c = makeCandle();
    store.upsert(c);
    expect(store.count()).toBe(1);
    expect(store.get('binance', 'BTCUSDT', '1m', c.openTimeMs)).toEqual(c);
  });

  it('idempotent upsert: same key rewrite does not duplicate', () => {
    const c = makeCandle({ close: 42050, ingestTsMs: 100 });
    store.upsert(c);
    store.upsert({ ...c, close: 42060, ingestTsMs: 200 });
    expect(store.count()).toBe(1);
    const got = store.get('binance', 'BTCUSDT', '1m', c.openTimeMs)!;
    expect(got.close).toBe(42060);
    expect(got.ingestTsMs).toBe(200);
  });

  it('forming candle may be replaced by final', () => {
    const open = 1_700_000_000_000;
    store.upsert(
      makeCandle({
        openTimeMs: open,
        isFinal: false,
        close: 42010,
        high: 42020,
        ingestTsMs: 50,
      }),
    );
    expect(store.get('binance', 'BTCUSDT', '1m', open)?.isFinal).toBe(false);

    store.upsert(
      makeCandle({
        openTimeMs: open,
        isFinal: true,
        close: 42050,
        high: 42100,
        ingestTsMs: 100,
      }),
    );
    const got = store.get('binance', 'BTCUSDT', '1m', open)!;
    expect(got.isFinal).toBe(true);
    expect(got.close).toBe(42050);
    expect(got.high).toBe(42100);
    expect(store.count()).toBe(1);
  });

  it('range query by venue/symbol/tf + fromMs/toMs, ordered ascending', () => {
    const t0 = 1_700_000_000_000;
    const step = 60_000;
    for (let i = 0; i < 5; i++) {
      store.upsert(
        makeCandle({
          openTimeMs: t0 + i * step,
          closeTimeMs: t0 + i * step + 59_999,
          close: 42000 + i,
          isFinal: i < 4,
        }),
      );
    }
    // different tf / venue / symbol must not leak
    store.upsert(
      makeCandle({
        timeframe: '5m',
        openTimeMs: t0 + step,
        close: 999,
      }),
    );
    store.upsert(
      makeCandle({
        venue: 'bybit',
        openTimeMs: t0 + step,
        close: 888,
      }),
    );
    store.upsert(
      makeCandle({
        symbol: 'ETHUSDT',
        openTimeMs: t0 + step,
        close: 777,
      }),
    );

    const rows = store.queryRange({
      venue: 'binance',
      symbol: 'BTCUSDT',
      timeframe: '1m',
      fromMs: t0 + step,
      toMs: t0 + 3 * step,
    });
    expect(rows.map((r) => r.openTimeMs)).toEqual([
      t0 + step,
      t0 + 2 * step,
      t0 + 3 * step,
    ]);
    expect(rows.map((r) => r.close)).toEqual([42001, 42002, 42003]);
  });

  it('closedOnly filter excludes forming candles', () => {
    const t0 = 1_700_000_000_000;
    store.upsert(makeCandle({ openTimeMs: t0, isFinal: true, close: 1 }));
    store.upsert(
      makeCandle({
        openTimeMs: t0 + 60_000,
        isFinal: false,
        close: 2,
      }),
    );
    store.upsert(
      makeCandle({
        openTimeMs: t0 + 120_000,
        isFinal: true,
        close: 3,
      }),
    );

    const all = store.queryRange({
      venue: 'binance',
      symbol: 'BTCUSDT',
      timeframe: '1m',
      fromMs: t0,
      toMs: t0 + 120_000,
    });
    expect(all).toHaveLength(3);

    const closed = store.queryRange({
      venue: 'binance',
      symbol: 'BTCUSDT',
      timeframe: '1m',
      fromMs: t0,
      toMs: t0 + 120_000,
      closedOnly: true,
    });
    expect(closed.map((c) => c.close)).toEqual([1, 3]);
    expect(closed.every((c) => c.isFinal)).toBe(true);
  });

  it('upsertMany is transactional and idempotent', () => {
    const t0 = 1_700_000_000_000;
    const batch = [0, 1, 2].map((i) =>
      makeCandle({
        openTimeMs: t0 + i * 60_000,
        close: 100 + i,
        isFinal: true,
      }),
    );
    store.upsertMany(batch);
    store.upsertMany(
      batch.map((c) => ({ ...c, close: c.close + 10, ingestTsMs: 999 })),
    );
    expect(store.count()).toBe(3);
    expect(store.get('binance', 'BTCUSDT', '1m', t0)?.close).toBe(110);
  });

  it('survives reopen on same file', () => {
    const c = makeCandle({ close: 42111 });
    store.upsert(c);
    store.close();

    const again = new CandleStore({ dbPath });
    expect(again.get('binance', 'BTCUSDT', '1m', c.openTimeMs)?.close).toBe(
      42111,
    );
    again.close();
    // prevent afterEach double-close on closed handle — reopen for teardown
    store = new CandleStore({ dbPath });
  });
});
