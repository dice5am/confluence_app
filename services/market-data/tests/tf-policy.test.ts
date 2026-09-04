import { describe, expect, it } from 'vitest';
import { TIMEFRAMES } from '../src/types/candle.js';
import { timeframeToBinanceInterval } from '../src/binance/map.js';
import {
  assertAllVenueNative,
  getTfPolicy,
  listTfPolicies,
  resolveVenueNativeInterval,
} from '../src/policy/tf-policy.js';

describe('MD-2.1 TF policy (venue-native)', () => {
  it('covers all 7 product TFs as venue-native with rollup forbidden', () => {
    expect(TIMEFRAMES).toHaveLength(7);
    const policies = listTfPolicies();
    expect(policies).toHaveLength(7);
    for (const p of policies) {
      expect(p.sourceMode).toBe('venue-native');
      expect(p.allowSilentRollup).toBe(false);
      expect(p.binanceInterval).toBe(timeframeToBinanceInterval(p.timeframe));
      expect(resolveVenueNativeInterval(p.timeframe)).toBe(p.binanceInterval);
    }
    expect(assertAllVenueNative()).toBe(true);
  });

  it('maps each TF to its own Binance interval (no silent 1m rollup recipe)', () => {
    for (const tf of TIMEFRAMES) {
      const entry = getTfPolicy(tf);
      expect(entry.binanceInterval).toBe(tf);
      expect(entry.allowSilentRollup).toBe(false);
    }
  });
});
