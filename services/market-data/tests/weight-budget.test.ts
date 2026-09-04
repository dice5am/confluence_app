import { describe, expect, it, vi } from 'vitest';
import {
  KLINES_REQUEST_WEIGHT,
  RestWeightBudget,
} from '../src/binance/weight-budget.js';

describe('RestWeightBudget (MD-2.2)', () => {
  it('allows acquires under the non-reserve cap', async () => {
    const budget = new RestWeightBudget({
      limitPerMinute: 20,
      gapFillReserve: 4,
      nowMs: () => 1_000,
      sleep: async () => {
        throw new Error('should not sleep');
      },
    });
    await budget.acquire(KLINES_REQUEST_WEIGHT);
    await budget.acquire(KLINES_REQUEST_WEIGHT);
    expect(budget.used()).toBe(4);
    expect(budget.remaining()).toBe(12); // 20 - 4 reserve - 4 used
  });

  it('waits when non-reserve callers would exceed cap', async () => {
    let now = 0;
    const sleeps: number[] = [];
    const budget = new RestWeightBudget({
      limitPerMinute: 10,
      gapFillReserve: 4,
      windowMs: 60_000,
      nowMs: () => now,
      sleep: async (ms) => {
        sleeps.push(ms);
        now += ms;
      },
    });
    // Non-reserve cap = 6. Fill it.
    await budget.acquire(6);
    expect(budget.remaining()).toBe(0);
    // Next acquire must wait for window to slide
    const p = budget.acquire(2);
    await p;
    expect(sleeps.length).toBeGreaterThanOrEqual(1);
    expect(budget.used()).toBe(2);
  });

  it('gap-fill allowReserve can spend into the reserve band', async () => {
    const budget = new RestWeightBudget({
      limitPerMinute: 10,
      gapFillReserve: 4,
      nowMs: () => 0,
      sleep: async () => {
        throw new Error('should not sleep');
      },
    });
    await budget.acquire(6); // fills non-reserve
    await budget.acquire(KLINES_REQUEST_WEIGHT, { allowReserve: true });
    expect(budget.used()).toBe(8);
    expect(budget.remaining({ allowReserve: true })).toBe(2);
  });
});
