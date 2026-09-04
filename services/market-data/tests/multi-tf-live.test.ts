import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { EventEmitter } from 'node:events';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { CandleStore } from '../src/store/candle-store.js';
import { HealthMachine } from '../src/health/machine.js';
import { LiveCandleHub } from '../src/live/hub.js';
import { startMultiTfLiveIngest } from '../src/live/multi-tf-ingest.js';
import type { Timeframe } from '../src/types/candle.js';

class FakeWebSocket extends EventEmitter {
  static instances: FakeWebSocket[] = [];
  url: string;
  readyState = 0;
  constructor(url: string) {
    super();
    this.url = url;
    FakeWebSocket.instances.push(this);
    queueMicrotask(() => {
      this.readyState = 1;
      this.emit('open');
    });
  }
  close() {
    this.readyState = 3;
    this.emit('close');
  }
  removeAllListeners() {
    return super.removeAllListeners();
  }
  // ws client may call these
  on(event: string | symbol, listener: (...args: unknown[]) => void) {
    return super.on(event, listener);
  }
}

function klineMsg(tf: Timeframe, openTimeMs: number, isFinal: boolean, close: number) {
  return JSON.stringify({
    e: 'kline',
    E: openTimeMs + 1_000,
    s: 'BTCUSDT',
    k: {
      t: openTimeMs,
      T: openTimeMs + 59_999,
      s: 'BTCUSDT',
      i: tf,
      o: '100',
      c: String(close),
      h: '101',
      l: '99',
      v: '1.5',
      x: isFinal,
    },
  });
}

describe('MD-2.3 multi-TF live ingest', () => {
  let dbPath: string;
  let store: CandleStore;
  let health: HealthMachine;
  let hub: LiveCandleHub;

  beforeEach(() => {
    FakeWebSocket.instances = [];
    const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'md-live-mtf-'));
    dbPath = path.join(dir, 'candles.sqlite');
    store = new CandleStore({ dbPath });
    health = new HealthMachine({ nowMs: () => 1_700_000_100_000 });
    hub = new LiveCandleHub({ store, health });
  });

  afterEach(() => {
    store.close();
    fs.rmSync(path.dirname(dbPath), { recursive: true, force: true });
  });

  it('subscribes only configured TFs, upserts forming+final, ticks health lastSource', async () => {
    const handle = startMultiTfLiveIngest({
      hub,
      health,
      timeframes: ['1m', '5m', '1h'],
      ws: {
        WebSocketImpl: FakeWebSocket as unknown as typeof import('ws').default,
        nowMs: () => 1_700_000_050_000,
      },
    });

    // wait for opens
    await vi.waitFor(() => {
      expect(FakeWebSocket.instances).toHaveLength(3);
      expect(health.isFeedConnected()).toBe(true);
    });

    expect(handle.subscribedTimeframes()).toEqual(['1m', '5m', '1h']);
    expect(FakeWebSocket.instances.map((ws) => ws.url).sort()).toEqual([
      'wss://stream.binance.com:9443/ws/btcusdt@kline_1h',
      'wss://stream.binance.com:9443/ws/btcusdt@kline_1m',
      'wss://stream.binance.com:9443/ws/btcusdt@kline_5m',
    ]);

    const open = 1_700_000_000_000;
    FakeWebSocket.instances
      .find((ws) => ws.url.includes('kline_1m'))!
      .emit('message', klineMsg('1m', open, false, 42010));
    FakeWebSocket.instances
      .find((ws) => ws.url.includes('kline_5m'))!
      .emit('message', klineMsg('5m', open, true, 42100));

    expect(store.get('binance', 'BTCUSDT', '1m', open)?.isFinal).toBe(false);
    expect(store.get('binance', 'BTCUSDT', '1m', open)?.close).toBe(42010);
    expect(store.get('binance', 'BTCUSDT', '5m', open)?.isFinal).toBe(true);
    expect(health.getLastSourceTsMs()).toBe(open + 1_000);
    expect(health.getHealth().activeTimeframes).toEqual(
      expect.arrayContaining(['1m', '5m', '1h']),
    );

    handle.stop();
    expect(handle.isRunning()).toBe(false);
    expect(health.isFeedConnected()).toBe(false);
  });

  it('rejects empty TF set', () => {
    expect(() =>
      startMultiTfLiveIngest({
        hub,
        timeframes: [],
      }),
    ).toThrow(/non-empty/);
  });
});
