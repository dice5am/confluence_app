import fs from 'node:fs';
import http from 'node:http';
import os from 'node:os';
import path from 'node:path';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import {
  createServer,
  createDefaultDeps,
  type ServerDeps,
} from '../src/server.js';
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

async function listen(server: http.Server): Promise<{
  port: number;
  base: string;
  close: () => Promise<void>;
}> {
  await new Promise<void>((resolve) => server.listen(0, '127.0.0.1', resolve));
  const addr = server.address();
  if (!addr || typeof addr === 'string') throw new Error('no address');
  const port = addr.port;
  return {
    port,
    base: `http://127.0.0.1:${port}`,
    close: () =>
      new Promise((resolve, reject) =>
        server.close((err) => (err ? reject(err) : resolve())),
      ),
  };
}

describe('consumer HTTP APIs (MD-1.7/1.8/1.9 + health)', () => {
  let dbPath: string;
  let deps: ServerDeps;
  let server: http.Server;
  let base: string;
  let close: () => Promise<void>;
  const fixedNow = 1_700_000_500_000;

  beforeEach(async () => {
    const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'md-http-'));
    dbPath = path.join(dir, 'candles.sqlite');
    deps = createDefaultDeps({ dbPath, nowMs: () => fixedNow });
    deps.health.setFeedConnected(true);
    deps.health.recordUpdate(fixedNow - 1_000, '1m');
    deps.health.setActiveTimeframes([
      '1m',
      '5m',
      '15m',
      '1h',
      '4h',
      '1d',
      '1w',
    ]);
    deps.health.setNote(undefined);

    const t0 = 1_700_000_000_000;
    deps.store.upsertMany([
      makeCandle({ openTimeMs: t0, close: 1, isFinal: true }),
      makeCandle({ openTimeMs: t0 + 60_000, close: 2, isFinal: false }),
      makeCandle({ openTimeMs: t0 + 60_000, close: 2.5, isFinal: true }),
      makeCandle({ openTimeMs: t0 + 120_000, close: 3, isFinal: true }),
    ]);

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

  it('GET /health and /v1/health use HealthMachine', async () => {
    for (const path of ['/health', '/v1/health']) {
      const res = await fetch(`${base}${path}`);
      expect(res.status).toBe(200);
      const body = (await res.json()) as {
        status: string;
        venue: string;
        symbol: string;
        lastSourceTsMs: number;
      };
      expect(body.status).toBe('ok');
      expect(body.venue).toBe('binance');
      expect(body.symbol).toBe('BTCUSDT');
      expect(body.lastSourceTsMs).toBe(fixedNow - 1_000);
    }
  });

  it('GET /v1/candles returns closed-only ascending history', async () => {
    const t0 = 1_700_000_000_000;
    const url =
      `${base}/v1/candles?symbol=BTCUSDT&timeframe=1m` +
      `&fromMs=${t0}&toMs=${t0 + 200_000}&venue=binance`;
    const res = await fetch(url);
    expect(res.status).toBe(200);
    const body = (await res.json()) as {
      candles: Candle[];
      truncated: boolean;
    };
    expect(body.candles).toHaveLength(3);
    expect(body.candles.every((c) => c.isFinal)).toBe(true);
    expect(body.candles.map((c) => c.openTimeMs)).toEqual([
      t0,
      t0 + 60_000,
      t0 + 120_000,
    ]);
    expect(body.candles.map((c) => c.close)).toEqual([1, 2.5, 3]);
  });

  it('GET /v1/candles rejects bad params', async () => {
    const res = await fetch(`${base}/v1/candles?timeframe=1m&fromMs=x&toMs=1`);
    expect(res.status).toBe(400);
  });

  it('GET /v1/bootstrap-plan documents sequence + alerts finals-only', async () => {
    const res = await fetch(
      `${base}/v1/bootstrap-plan?timeframe=1m&lookbackMs=3600000`,
    );
    expect(res.status).toBe(200);
    const body = (await res.json()) as {
      sequence: string[];
      overlapMs: number;
      alertsNote: string;
      endpoints: Record<string, string>;
    };
    expect(body.sequence[0]).toBe('subscribe_live');
    expect(body.overlapMs).toBe(120_000);
    expect(body.alertsNote).toMatch(/isFinal=true/i);
    expect(body.endpoints.history).toContain('/v1/candles');
    expect(body.endpoints.live).toContain('/v1/candles/stream');
  });

  it('GET /v1/candles/stream SSE pushes forming then exactly one final', async () => {
    const open = 1_700_000_300_000;
    const ac = new AbortController();
    const res = await fetch(
      `${base}/v1/candles/stream?symbol=BTCUSDT&timeframe=1m`,
      { signal: ac.signal },
    );
    expect(res.status).toBe(200);
    expect(res.headers.get('content-type')).toMatch(/text\/event-stream/);

    const reader = res.body!.getReader();
    const decoder = new TextDecoder();
    let buf = '';
    const candles: Candle[] = [];

    const readUntil = async (n: number, timeoutMs = 2000) => {
      const deadline = Date.now() + timeoutMs;
      while (candles.length < n && Date.now() < deadline) {
        const { value, done } = await reader.read();
        if (done) break;
        buf += decoder.decode(value, { stream: true });
        const parts = buf.split('\n\n');
        buf = parts.pop() ?? '';
        for (const block of parts) {
          const lines = block.split('\n');
          let event = 'message';
          let data = '';
          for (const line of lines) {
            if (line.startsWith('event:')) event = line.slice(6).trim();
            if (line.startsWith('data:')) data = line.slice(5).trim();
          }
          if (event === 'candle' && data) {
            candles.push(JSON.parse(data) as Candle);
          }
        }
      }
    };

    // Inject after stream connected
    deps.hub.ingest(
      makeCandle({
        openTimeMs: open,
        isFinal: false,
        close: 10,
        sourceTsMs: fixedNow,
      }),
    );
    deps.hub.ingest(
      makeCandle({
        openTimeMs: open,
        isFinal: true,
        close: 11,
        sourceTsMs: fixedNow + 1,
      }),
    );
    deps.hub.ingest(
      makeCandle({
        openTimeMs: open,
        isFinal: true,
        close: 11.5,
        sourceTsMs: fixedNow + 2,
      }),
    );

    await readUntil(2);
    ac.abort();
    try {
      await reader.cancel();
    } catch {
      /* ignore */
    }

    expect(candles.length).toBeGreaterThanOrEqual(2);
    expect(candles[0]!.isFinal).toBe(false);
    expect(candles[0]!.close).toBe(10);
    const finals = candles.filter((c) => c.isFinal && c.openTimeMs === open);
    expect(finals).toHaveLength(1);
    expect(finals[0]!.close).toBe(11);
  });

  it('SSE finalsOnly=1 skips forming (Alerts path)', async () => {
    const open = 1_700_000_400_000;
    const ac = new AbortController();
    const res = await fetch(
      `${base}/v1/candles/stream?finalsOnly=1&timeframe=1m`,
      { signal: ac.signal },
    );
    const reader = res.body!.getReader();
    const decoder = new TextDecoder();
    let buf = '';
    const candles: Candle[] = [];

    const pump = async () => {
      const deadline = Date.now() + 1500;
      while (candles.length < 1 && Date.now() < deadline) {
        const { value, done } = await reader.read();
        if (done) break;
        buf += decoder.decode(value, { stream: true });
        for (const block of buf.split('\n\n').slice(0, -1)) {
          if (block.includes('event: candle')) {
            const dataLine = block.split('\n').find((l) => l.startsWith('data:'));
            if (dataLine) {
              candles.push(JSON.parse(dataLine.slice(5).trim()) as Candle);
            }
          }
        }
        buf = buf.split('\n\n').pop() ?? '';
      }
    };

    deps.hub.ingest(
      makeCandle({ openTimeMs: open, isFinal: false, close: 1 }),
    );
    deps.hub.ingest(
      makeCandle({ openTimeMs: open, isFinal: true, close: 2 }),
    );
    await pump();
    ac.abort();
    try {
      await reader.cancel();
    } catch {
      /* ignore */
    }

    expect(candles).toHaveLength(1);
    expect(candles[0]!.isFinal).toBe(true);
    expect(candles[0]!.close).toBe(2);
  });
});
