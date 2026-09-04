import type { CandleStore } from '../store/candle-store.js';
import type { Timeframe, Venue } from '../types/candle.js';

/** 1m openTimeMs step (ms). */
export const ONE_MINUTE_MS = 60_000;

export interface GapRange {
  /** Inclusive first missing openTimeMs */
  fromOpenMs: number;
  /** Inclusive last missing openTimeMs */
  toOpenMs: number;
  /** Number of missing 1m bars in this contiguous hole */
  missingCount: number;
}

export interface DetectGapsOptions {
  venue: Venue;
  symbol: string;
  /**
   * Inclusive lower bound on openTimeMs. When omitted, uses the earliest
   * stored closed 1m candle for this venue/symbol.
   */
  fromMs?: number;
  /**
   * Inclusive upper bound on openTimeMs. When omitted, uses the latest
   * stored closed 1m candle for this venue/symbol.
   */
  toMs?: number;
  /** Only 1m is supported for MD-1.5 gap detect. */
  timeframe?: '1m';
}

export interface DetectGapsResult {
  venue: Venue;
  symbol: string;
  timeframe: Timeframe;
  fromMs: number;
  toMs: number;
  /** Contiguous missing openTime ranges */
  ranges: GapRange[];
  /** Flat list of every missing openTimeMs (ascending) */
  missingOpenTimeMs: number[];
  /** Alias for missingOpenTimeMs.length — for health.gapCount */
  gapCount: number;
}

/**
 * Detect missing openTimeMs values on the 1m sequence for a venue/symbol.
 *
 * Walks closed candles in [fromMs, toMs] (default: min..max stored) and
 * reports any holes where consecutive opens differ by more than 60_000ms.
 * Edge gaps against an explicit fromMs/toMs window are also reported when
 * the first/last stored bar does not sit on the window boundary.
 */
export function detectOneMinuteGaps(
  store: CandleStore,
  opts: DetectGapsOptions,
): DetectGapsResult {
  const timeframe: Timeframe = '1m';
  const venue = opts.venue;
  const symbol = opts.symbol;

  // Probe full series bounds when window not fully specified.
  const probe = store.queryRange({
    venue,
    symbol,
    timeframe,
    fromMs: opts.fromMs ?? 0,
    toMs: opts.toMs ?? Number.MAX_SAFE_INTEGER,
    closedOnly: true,
  });

  if (probe.length === 0) {
    const fromMs = opts.fromMs ?? 0;
    const toMs = opts.toMs ?? fromMs;
    const missingOpenTimeMs: number[] = [];
    if (opts.fromMs !== undefined && opts.toMs !== undefined && opts.toMs >= opts.fromMs) {
      for (let t = opts.fromMs; t <= opts.toMs; t += ONE_MINUTE_MS) {
        missingOpenTimeMs.push(t);
      }
    }
    return {
      venue,
      symbol,
      timeframe,
      fromMs,
      toMs,
      ranges: missingToRanges(missingOpenTimeMs),
      missingOpenTimeMs,
      gapCount: missingOpenTimeMs.length,
    };
  }

  const fromMs = opts.fromMs ?? probe[0]!.openTimeMs;
  const toMs = opts.toMs ?? probe[probe.length - 1]!.openTimeMs;

  // Re-query within the resolved window (probe may have used defaults).
  const candles =
    opts.fromMs !== undefined || opts.toMs !== undefined
      ? store.queryRange({
          venue,
          symbol,
          timeframe,
          fromMs,
          toMs,
          closedOnly: true,
        })
      : probe;

  const present = new Set(candles.map((c) => c.openTimeMs));
  const missingOpenTimeMs: number[] = [];

  // Align walk to 1m grid starting at fromMs (caller should pass aligned times).
  for (let t = fromMs; t <= toMs; t += ONE_MINUTE_MS) {
    if (!present.has(t)) missingOpenTimeMs.push(t);
  }

  return {
    venue,
    symbol,
    timeframe,
    fromMs,
    toMs,
    ranges: missingToRanges(missingOpenTimeMs),
    missingOpenTimeMs,
    gapCount: missingOpenTimeMs.length,
  };
}

/** Collapse ascending missing open times into contiguous GapRange segments. */
export function missingToRanges(missingOpenTimeMs: readonly number[]): GapRange[] {
  if (missingOpenTimeMs.length === 0) return [];
  const ranges: GapRange[] = [];
  let start = missingOpenTimeMs[0]!;
  let prev = start;
  let count = 1;

  for (let i = 1; i < missingOpenTimeMs.length; i++) {
    const t = missingOpenTimeMs[i]!;
    if (t === prev + ONE_MINUTE_MS) {
      prev = t;
      count += 1;
      continue;
    }
    ranges.push({ fromOpenMs: start, toOpenMs: prev, missingCount: count });
    start = t;
    prev = t;
    count = 1;
  }
  ranges.push({ fromOpenMs: start, toOpenMs: prev, missingCount: count });
  return ranges;
}
