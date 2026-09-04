import type { Timeframe } from '../types/candle.js';
import { TIMEFRAMES } from '../types/candle.js';
import { timeframeToBinanceInterval } from '../binance/map.js';

/** How a TF series is sourced for consumer candles. */
export type TfSourceMode = 'venue-native';

/**
 * MD-2.1 locked policy: every product TF is venue-native Binance klines.
 * There is intentionally no `rollup-from-1m` mode for consumer series.
 */
export interface TfPolicyEntry {
  timeframe: Timeframe;
  /** Binance kline interval string (identical to our TF ids for these 7). */
  binanceInterval: string;
  sourceMode: TfSourceMode;
  /** Explicit: MD must not synthesize this TF from lower bars. */
  allowSilentRollup: false;
}

const POLICY: Readonly<Record<Timeframe, TfPolicyEntry>> = Object.freeze(
  Object.fromEntries(
    TIMEFRAMES.map((tf) => [
      tf,
      Object.freeze({
        timeframe: tf,
        binanceInterval: timeframeToBinanceInterval(tf),
        sourceMode: 'venue-native' as const,
        allowSilentRollup: false as const,
      }),
    ]),
  ) as Record<Timeframe, TfPolicyEntry>,
);

/** All 7 Phase-1/2 timeframes under the venue-native policy. */
export function listTfPolicies(): readonly TfPolicyEntry[] {
  return TIMEFRAMES.map((tf) => POLICY[tf]);
}

/** Lookup policy for one TF. Throws on unknown TF. */
export function getTfPolicy(timeframe: Timeframe): TfPolicyEntry {
  const entry = POLICY[timeframe];
  if (!entry) {
    throw new Error(`Unknown timeframe: ${String(timeframe)}`);
  }
  return entry;
}

/**
 * Resolve the REST/WS interval for a TF under MD-2.1.
 * Always returns the venue-native Binance interval — never a rollup recipe.
 */
export function resolveVenueNativeInterval(timeframe: Timeframe): string {
  const entry = getTfPolicy(timeframe);
  if (entry.sourceMode !== 'venue-native' || entry.allowSilentRollup) {
    throw new Error(
      `MD-2.1 forbids non-native / rollup sourcing for ${timeframe}`,
    );
  }
  return entry.binanceInterval;
}

/** True iff every product TF is locked to venue-native with rollup forbidden. */
export function assertAllVenueNative(): boolean {
  for (const tf of TIMEFRAMES) {
    const e = getTfPolicy(tf);
    if (e.sourceMode !== 'venue-native' || e.allowSilentRollup !== false) {
      return false;
    }
    if (e.binanceInterval !== timeframeToBinanceInterval(tf)) {
      return false;
    }
  }
  return true;
}
