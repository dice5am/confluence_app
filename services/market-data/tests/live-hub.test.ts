import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { CandleStore } from '../src/store/candle-store.js';
import { HealthMachine } from '../src/health/machine.js';
import { LiveCandleHub } from '../src/live/hub.js';
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
    isFinal: false,
    sourceTsMs: 1_700_000_010_000,
    ingestTsMs: 1_700_000_010_100,
    ...overrides,
  };
}

describe('LiveCandleHub (MD-1.8)', () => {
  let dbPath: string;
  let store: CandleStore;
  let health: HealthMachine;
  let hub: LiveCandleHub;

  beforeEach(() => {
    const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'md-live-hub-'));
    dbPath = path.join(dir, 'candles.sqlite');
    store = new CandleStore({ dbPath });
    health = new HealthMachine({ nowMs: () => 1_700_000_100_000 });
    health.setFeedConnected(true);
    hub = new LiveCandleHub({ store, health });
  });

  afterEach(() => {
    store.close();
    fs.rmSync(path.dirname(dbPath), { recursive: true, force: true });
  });

  it('pushes forming updates and upserts store', () => {
    const received: Candle[] = [];
    hub.subscribe((c) => received.push(c));

    const forming1 = makeCandle({ close: 42010, sourceTsMs: 100 });
    const forming2 = makeCandle({ close: 42020, high: 42030, sourceTsMs: 200 });
    expect(hub.ingest(forming1).broadcast).toBe(true);
    expect(hub.ingest(forming2).broadcast).toBe(true);
    expect(received).toHaveLength(2);
    expect(received.every((c) => c.isFinal === false)).toBe(true);
    expect(store.get('binance', 'BTCUSDT', '1m', forming1.openTimeMs)?.close).toBe(
      42020,
    );
    expect(health.getLastSourceTsMs()).toBe(200);
  });

  it('broadcasts exactly one final per bar key', () => {
    const received: Candle[] = [];
    hub.subscribe((c) => received.push(c));

    const open = 1_700_000_000_000;
    hub.ingest(makeCandle({ openTimeMs: open, isFinal: false, close: 1 }));
    const final1 = makeCandle({
      openTimeMs: open,
      isFinal: true,
      close: 2,
      sourceTsMs: 300,
    });
    const final2 = makeCandle({
      openTimeMs: open,
      isFinal: true,
      close: 2.1,
      sourceTsMs: 400,
    });

    expect(hub.ingest(final1).broadcast).toBe(true);
    expect(hub.ingest(final2).broadcast).toBe(false); // no re-broadcast

    const finals = received.filter((c) => c.isFinal);
    expect(finals).toHaveLength(1);
    expect(finals[0]!.close).toBe(2);

    // Store still upserts the duplicate final (idempotent)
    expect(store.get('binance', 'BTCUSDT', '1m', open)?.close).toBe(2.1);
    expect(hub.hasBroadcastFinal('binance', 'BTCUSDT', '1m', open)).toBe(true);
  });

  it('allows finals for distinct openTimeMs', () => {
    const received: Candle[] = [];
    hub.subscribe((c) => received.push(c));
    hub.ingest(
      makeCandle({ openTimeMs: 1000, isFinal: true, close: 1 }),
    );
    hub.ingest(
      makeCandle({ openTimeMs: 2000, isFinal: true, close: 2 }),
    );
    expect(received.filter((c) => c.isFinal)).toHaveLength(2);
  });

  it('subscriber errors do not break ingest', () => {
    hub.subscribe(() => {
      throw new Error('boom');
    });
    const ok: Candle[] = [];
    hub.subscribe((c) => ok.push(c));
    expect(() => hub.ingest(makeCandle({ isFinal: true }))).not.toThrow();
    expect(ok).toHaveLength(1);
  });
});
