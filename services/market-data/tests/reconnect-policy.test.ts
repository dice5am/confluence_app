import { describe, expect, it, vi } from 'vitest';
import {
  computeWsReconnectDelayMs,
  computeRestBackoffMs,
  withRestRetry,
  WS_INITIAL_BACKOFF_MS,
  WS_MAX_BACKOFF_MS,
  REST_INITIAL_BACKOFF_MS,
  REST_MAX_BACKOFF_MS,
  RATE_LIMIT_RECONNECT_POLICY_SUMMARY,
} from '../src/binance/reconnect-policy.js';
import { BinanceApiError } from '../src/binance/errors.js';
import { fetchKlinesPage } from '../src/binance/rest.js';

describe('MD-2.8 reconnect / rate-limit policy', () => {
  it('WS backoff grows exponentially and caps', () => {
    const d0 = computeWsReconnectDelayMs(0, { random: () => 0 });
    const d1 = computeWsReconnectDelayMs(1, { random: () => 0 });
    const d2 = computeWsReconnectDelayMs(2, { random: () => 0 });
    expect(d0).toBe(WS_INITIAL_BACKOFF_MS);
    expect(d1).toBe(WS_INITIAL_BACKOFF_MS * 2);
    expect(d2).toBe(WS_INITIAL_BACKOFF_MS * 4);
    const high = computeWsReconnectDelayMs(20, { random: () => 0 });
    expect(high).toBe(WS_MAX_BACKOFF_MS);
  });

  it('REST backoff grows, caps, and honors retryAfterMs', () => {
    const d0 = computeRestBackoffMs(0, { random: () => 0 });
    expect(d0).toBe(REST_INITIAL_BACKOFF_MS);
    const capped = computeRestBackoffMs(20, { random: () => 0 });
    expect(capped).toBe(REST_MAX_BACKOFF_MS);
    const honored = computeRestBackoffMs(0, {
      random: () => 0,
      retryAfterMs: 45_000,
    });
    expect(honored).toBe(45_000);
  });

  it('withRestRetry never tight-loops on 429 (sleeps between attempts)', async () => {
    const sleeps: number[] = [];
    let calls = 0;
    const result = await withRestRetry(
      async () => {
        calls += 1;
        if (calls < 3) {
          throw new BinanceApiError('RATE_LIMIT_429', 'slow down', {
            status: 429,
          });
        }
        return 'ok';
      },
      {
        maxRetries: 5,
        random: () => 0,
        sleep: async (ms) => {
          sleeps.push(ms);
        },
      },
    );
    expect(result).toBe('ok');
    expect(calls).toBe(3);
    expect(sleeps.length).toBe(2);
    expect(sleeps[0]).toBeGreaterThanOrEqual(REST_INITIAL_BACKOFF_MS);
    expect(sleeps[1]).toBeGreaterThan(sleeps[0]!);
  });

  it('withRestRetry gives up after maxRetries without tight looping', async () => {
    const sleeps: number[] = [];
    await expect(
      withRestRetry(
        async () => {
          throw new BinanceApiError('RATE_LIMIT_429', 'nope', { status: 429 });
        },
        {
          maxRetries: 2,
          random: () => 0,
          sleep: async (ms) => {
            sleeps.push(ms);
          },
        },
      ),
    ).rejects.toMatchObject({ code: 'RATE_LIMIT_429' });
    expect(sleeps).toHaveLength(2);
  });

  it('fetchKlinesPage retries 429 with mocked sleep (not tight loop)', async () => {
    let calls = 0;
    const sleeps: number[] = [];
    const rows = [
      [
        1_700_000_000_000,
        '1',
        '2',
        '0.5',
        '1.5',
        '10',
        1_700_000_000_000 + 3_600_000 - 1,
        '0',
        1,
        '0',
        '0',
        '0',
      ],
    ];
    const fetchImpl = vi.fn(async () => {
      calls += 1;
      if (calls === 1) {
        return new Response('rate limit', {
          status: 429,
          headers: { 'retry-after': '1' },
        });
      }
      return new Response(JSON.stringify(rows), { status: 200 });
    });

    const candles = await fetchKlinesPage({
      timeframe: '1h',
      fetchImpl: fetchImpl as unknown as typeof fetch,
      retry: {
        maxRetries: 3,
        random: () => 0,
        sleep: async (ms) => {
          sleeps.push(ms);
        },
      },
    });
    expect(candles).toHaveLength(1);
    expect(calls).toBe(2);
    expect(sleeps.length).toBe(1);
    expect(sleeps[0]).toBeGreaterThanOrEqual(1000);
  });

  it('exports a policy summary for docs/runtime', () => {
    expect(RATE_LIMIT_RECONNECT_POLICY_SUMMARY.restRetry.maxRetries).toBe(5);
    expect(
      RATE_LIMIT_RECONNECT_POLICY_SUMMARY.wsReconnect.initialBackoffMs,
    ).toBe(WS_INITIAL_BACKOFF_MS);
  });
});
