import fs from 'node:fs';
import http from 'node:http';
import os from 'node:os';
import path from 'node:path';
import { EventEmitter } from 'node:events';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import {
  createServer,
  createDefaultDeps,
  type ServerDeps,
} from '../src/server.js';
import { startMultiTfLiveIngest } from '../src/live/multi-tf-ingest.js';

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
  on(event: string | symbol, listener: (...args: unknown[]) => void) {
    return super.on(event, listener);
  }
}

async function listen(server: http.Server) {
  await new Promise<void>((resolve) => server.listen(0, '127.0.0.1', resolve));
  const addr = server.address();
  if (!addr || typeof addr === 'string') throw new Error('no address');
  return {
    base: `http://127.0.0.1:${addr.port}`,
    close: () =>
      new Promise<void>((resolve, reject) =>
        server.close((err) => (err ? reject(err) : resolve())),
      ),
  };
}

describe('MD-2.9 health to consumers', () => {
  let dbPath: string;
  let deps: ServerDeps;
  let server: http.Server;
  let base: string;
  let close: () => Promise<void>;
  const fixedNow = 1_700_000_500_000;

  beforeEach(async () => {
    FakeWebSocket.instances = [];
    const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'md-health-cons-'));
    dbPath = path.join(dir, 'candles.sqlite');
    deps = createDefaultDeps({ dbPath, nowMs: () => fixedNow });
    deps.health.setFeedConnected(true);
    deps.health.recordUpdate(fixedNow - 1_000, '1m');
    deps.health.setExpectedTimeframes(['1m', '5m']);
    deps.health.setActiveTimeframes(['1m', '5m']);
    deps.health.setNote(undefined);
    server = createServer(deps);
    const listened = await listen(server);
    base = listened.base;
    close = listened.close;
  });

  afterEach(async () => {
    await close();
    deps.store.close();
    fs.rmSync(path.dirname(dbPath), { recursive: true, force: true });
  });

  it('GET /health and /v1/health expose MD-1.1 health + AL suppress note', async () => {
    for (const p of ['/health', '/v1/health']) {
      const res = await fetch(`${base}${p}`);
      expect(res.status).toBe(200);
      const body = (await res.json()) as {
        status: string;
        lastSourceTsMs: number;
        consumerNote: string;
      };
      expect(body.status).toBe('ok');
      expect(body.lastSourceTsMs).toBe(fixedNow - 1_000);
      expect(body.consumerNote).toMatch(/suppress/i);
      expect(body.consumerNote).toMatch(/stale/i);
    }
  });

  it('SSE stream sends health on connect with consumer note', async () => {
    const ac = new AbortController();
    const res = await fetch(`${base}/v1/candles/stream?timeframe=1m`, {
      signal: ac.signal,
    });
    expect(res.status).toBe(200);
    const reader = res.body!.getReader();
    const decoder = new TextDecoder();
    let buf = '';
    let healthEvent: { status?: string; consumerNote?: string } | undefined;
    const deadline = Date.now() + 2000;
    while (!healthEvent && Date.now() < deadline) {
      const { value, done } = await reader.read();
      if (done) break;
      buf += decoder.decode(value, { stream: true });
      for (const block of buf.split('\n\n')) {
        if (block.includes('event: health')) {
          const dataLine = block.split('\n').find((l) => l.startsWith('data:'));
          if (dataLine) {
            healthEvent = JSON.parse(dataLine.slice(5).trim()) as {
              status?: string;
              consumerNote?: string;
            };
          }
        }
      }
    }
    ac.abort();
    try {
      await reader.cancel();
    } catch {
      /* ignore */
    }
    expect(healthEvent?.status).toBe('ok');
    expect(healthEvent?.consumerNote).toMatch(/suppress/i);
  });

  it('multi-TF ingest sets expectedTimeframes to in-use set (not always degraded)', async () => {
    const handle = startMultiTfLiveIngest({
      hub: deps.hub,
      health: deps.health,
      timeframes: ['1m', '5m'],
      ws: {
        WebSocketImpl: FakeWebSocket as unknown as typeof import('ws').default,
        nowMs: () => fixedNow,
      },
    });
    await vi.waitFor(() => {
      expect(FakeWebSocket.instances).toHaveLength(2);
      expect(deps.health.isFeedConnected()).toBe(true);
    });
    expect(deps.health.getExpectedTimeframes()).toEqual(['1m', '5m']);
    deps.health.recordUpdate(fixedNow - 500, '1m');
    const health = deps.health.getHealth(fixedNow);
    expect(health.status).toBe('ok');
    expect(health.status).not.toBe('degraded');
    handle.stop();
  });
});
