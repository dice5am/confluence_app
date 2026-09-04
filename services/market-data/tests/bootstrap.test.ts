import { describe, expect, it } from 'vitest';
import {
  planBootstrapThenSubscribe,
  bootstrapThenSubscribe,
  TIMEFRAME_MS,
} from '../src/api/bootstrap.js';
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

describe('bootstrap-then-subscribe (MD-1.9)', () => {
  it('plans subscribe → history → upsert sequence with overlap', () => {
    const now = 2_000_000_000_000;
    const plan = planBootstrapThenSubscribe({
      timeframe: '1m',
      lookbackMs: 60 * 60_000,
      nowMs: now,
    });
    expect(plan.sequence).toEqual([
      'subscribe_live',
      'history_get_with_overlap',
      'upsert_by_primary_key',
    ]);
    expect(plan.overlapMs).toBe(2 * TIMEFRAME_MS['1m']);
    expect(plan.historyFromMs).toBe(now - 60 * 60_000);
    expect(plan.historyToMs).toBe(now);
    expect(plan.note).toMatch(/Alerts consume isFinal=true only/i);
  });

  it('seam: overlapping final from live + history upserts once (no hole/dup)', async () => {
    const t0 = 1_700_000_000_000;
    const t1 = t0 + 60_000;
    const t2 = t0 + 120_000; // forming / live edge

    // History returns closed bars including seam bar t1
    const historyBars = [
      makeCandle({ openTimeMs: t0, close: 100, isFinal: true }),
      makeCandle({ openTimeMs: t1, close: 110, isFinal: true }),
    ];

    // Live emits final for t1 (overlap) then forming for t2
    const liveEvents = [
      makeCandle({ openTimeMs: t1, close: 111, isFinal: true, sourceTsMs: 999 }),
      makeCandle({
        openTimeMs: t2,
        close: 120,
        isFinal: false,
        sourceTsMs: 1000,
      }),
    ];

    let liveHandler: ((c: Candle) => void) | undefined;
    const upserts: Candle[] = [];

    const result = await bootstrapThenSubscribe({
      timeframe: '1m',
      lookbackMs: 5 * 60_000,
      nowMs: t2 + 1_000,
      fetchHistory: async () => {
        // Simulate live arriving during/before history (subscribe first)
        for (const ev of liveEvents) liveHandler?.(ev);
        return historyBars;
      },
      subscribe: (handler) => {
        liveHandler = handler;
        return () => {
          liveHandler = undefined;
        };
      },
      onUpsert: (c) => upserts.push(c),
    });

    // Merged by openTimeMs — exactly one entry per bar
    expect(result.mergedByOpenTimeMs.size).toBe(3);
    expect([...result.mergedByOpenTimeMs.keys()].sort((a, b) => a - b)).toEqual([
      t0,
      t1,
      t2,
    ]);

    // Seam bar t1: last write wins (history after live → close 110), still one key
    expect(result.mergedByOpenTimeMs.get(t1)?.close).toBe(110);
    expect(result.mergedByOpenTimeMs.get(t2)?.isFinal).toBe(false);

    // No hole between t0 and t2
    const opens = [...result.mergedByOpenTimeMs.keys()].sort((a, b) => a - b);
    expect(opens[1]! - opens[0]!).toBe(60_000);
    expect(opens[2]! - opens[1]!).toBe(60_000);

    result.unsubscribe();
  });

  it('documents alerts finals-only in plan note', () => {
    const plan = planBootstrapThenSubscribe({
      timeframe: '5m',
      lookbackMs: 300 * 5 * 60_000,
    });
    expect(plan.overlapMs).toBe(2 * TIMEFRAME_MS['5m']);
    expect(plan.note.toLowerCase()).toContain('isfinal=true');
  });
});
