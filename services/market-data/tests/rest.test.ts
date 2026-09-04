import { describe, expect, it, vi } from 'vitest';
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';
import { fetchKlinesPage, fetchKlinesPaginated } from '../src/binance/rest.js';
import { BinanceApiError } from '../src/binance/errors.js';
import type { BinanceKlineRow } from '../src/binance/map.js';

const __dirname = dirname(fileURLToPath(import.meta.url));
const fixturePath = join(__dirname, '../fixtures/candles/btcusdt-1h-closed.json');

function sampleRows(n: number, startOpen = 1_700_000_000_000): BinanceKlineRow[] {
  const hour = 3_600_000;
  const rows: BinanceKlineRow[] = [];
  for (let i = 0; i < n; i++) {
    const open = startOpen + i * hour;
    rows.push([
      open,
      String(100 + i),
      String(101 + i),
      String(99 + i),
      String(100.5 + i),
      String(1 + i * 0.01),
      open + hour - 1,
      '0',
      1,
      '0',
      '0',
      '0',
    ]);
  }
  return rows;
}

describe('fetchKlinesPage (offline fixtures)', () => {
  it('maps fixture REST payload to MD-1.1 candles', async () => {
    const fixture = JSON.parse(readFileSync(fixturePath, 'utf8')) as {
      candles: Array<{ openTimeMs: number; close: number; isFinal: boolean }>;
    };
    expect(fixture.candles.length).toBeGreaterThanOrEqual(220);
    expect(fixture.candles.every((c) => c.isFinal === true)).toBe(true);

    const rawRows = sampleRows(3);
    const fetchImpl = vi.fn(async () =>
      new Response(JSON.stringify(rawRows), { status: 200, headers: { 'content-type': 'application/json' } }),
    );

    const candles = await fetchKlinesPage({
      timeframe: '1h',
      fetchImpl: fetchImpl as unknown as typeof fetch,
      nowMs: () => 42,
    });
    expect(candles).toHaveLength(3);
    expect(candles[0]).toMatchObject({
      venue: 'binance',
      symbol: 'BTCUSDT',
      timeframe: '1h',
      isFinal: true,
      ingestTsMs: 42,
    });
  });

  it('classifies 429 / 418 / 5xx', async () => {
    for (const [status, code] of [
      [429, 'RATE_LIMIT_429'],
      [418, 'IP_BAN_418'],
      [503, 'SERVER_5XX'],
    ] as const) {
      const fetchImpl = vi.fn(async () => new Response('nope', { status }));
      await expect(
        fetchKlinesPage({ timeframe: '1m', fetchImpl: fetchImpl as unknown as typeof fetch }),
      ).rejects.toMatchObject({ code, status } satisfies Partial<BinanceApiError>);
    }
  });

  it('classifies timeout', async () => {
    const fetchImpl = vi.fn(async () => {
      const err = new Error('aborted');
      err.name = 'AbortError';
      throw err;
    });
    await expect(
      fetchKlinesPage({
        timeframe: '1m',
        timeoutMs: 10,
        fetchImpl: fetchImpl as unknown as typeof fetch,
      }),
    ).rejects.toMatchObject({ code: 'TIMEOUT' });
  });
});

describe('fetchKlinesPaginated', () => {
  it('pages until maxBars', async () => {
    const page1 = sampleRows(2, 1_000);
    const page2 = sampleRows(2, 1_000 + 2 * 3_600_000);
    let calls = 0;
    const fetchImpl = vi.fn(async () => {
      calls += 1;
      const body = calls === 1 ? page1 : page2;
      return new Response(JSON.stringify(body), { status: 200 });
    });

    const candles = await fetchKlinesPaginated({
      timeframe: '1h',
      startTimeMs: 1_000,
      limit: 2,
      maxBars: 3,
      fetchImpl: fetchImpl as unknown as typeof fetch,
      nowMs: () => 1,
    });
    expect(candles).toHaveLength(3);
    expect(fetchImpl).toHaveBeenCalledTimes(2);
  });
});
