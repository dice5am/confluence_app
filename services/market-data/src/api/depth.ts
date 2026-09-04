import type { Timeframe } from '../types/candle.js';

const DAY_MS = 24 * 60 * 60 * 1000;

/**
 * MD-1.1 §2 history depth caps (stored depth).
 * Out-of-range history requests are truncated to these windows.
 */
export const HISTORY_DEPTH_MS: Readonly<Record<Timeframe, number>> = {
  '1m': 60 * DAY_MS,
  '5m': 180 * DAY_MS,
  '15m': 365 * DAY_MS,
  '1h': Number.POSITIVE_INFINITY,
  '4h': Number.POSITIVE_INFINITY,
  '1d': Number.POSITIVE_INFINITY,
  '1w': Number.POSITIVE_INFINITY,
};

/** Earliest openTimeMs allowed for a TF given `nowMs` (depth cap). */
export function earliestAllowedOpenTimeMs(
  timeframe: Timeframe,
  nowMs: number = Date.now(),
): number {
  const depth = HISTORY_DEPTH_MS[timeframe];
  if (!Number.isFinite(depth)) return Number.NEGATIVE_INFINITY;
  return nowMs - depth;
}

/**
 * Clamp requested fromMs to the depth window.
 * Returns the effective fromMs and whether truncation occurred.
 */
export function clampHistoryFromMs(
  timeframe: Timeframe,
  fromMs: number,
  nowMs: number = Date.now(),
): { effectiveFromMs: number; truncated: boolean } {
  const earliest = earliestAllowedOpenTimeMs(timeframe, nowMs);
  if (fromMs < earliest) {
    return { effectiveFromMs: earliest, truncated: true };
  }
  return { effectiveFromMs: fromMs, truncated: false };
}
