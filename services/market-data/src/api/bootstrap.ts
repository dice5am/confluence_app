import type { Candle, Timeframe } from '../types/candle.js';

/** Bar duration hints for overlap sizing (ms). */
export const TIMEFRAME_MS: Readonly<Record<Timeframe, number>> = {
  '1m': 60_000,
  '5m': 5 * 60_000,
  '15m': 15 * 60_000,
  '1h': 60 * 60_000,
  '4h': 4 * 60 * 60_000,
  '1d': 24 * 60 * 60_000,
  '1w': 7 * 24 * 60 * 60_000,
};

export interface BootstrapSubscribePlan {
  /** Inclusive history fromMs (caller-chosen lookback). */
  historyFromMs: number;
  /** Inclusive history toMs — typically now, or last closed open. */
  historyToMs: number;
  /**
   * Overlap: re-fetch / re-apply this many ms of closed bars after subscribe
   * so the seam cannot hole. Default = 2 bar lengths.
   */
  overlapMs: number;
  /**
   * Recommended: subscribe live first (or concurrently), then GET history
   * covering [historyFromMs, historyToMs] with overlap into live window.
   * Upsert by (venue,symbol,tf,openTimeMs) — duplicates at the seam are OK.
   */
  sequence: readonly [
    'subscribe_live',
    'history_get_with_overlap',
    'upsert_by_primary_key',
  ];
  note: string;
}

/**
 * MD-1.9: plan a bootstrap-then-subscribe sequence for Mobile/Alerts.
 *
 * Consumers MUST:
 * 1. Subscribe to the live stream (forming + finals)
 * 2. GET closed history with an overlap window into "now"
 * 3. Upsert by primary key — live finals and history closes for the same
 *    bar key collapse to one row (no hole, no consumer-visible dup)
 *
 * Alerts: evaluate on `isFinal=true` only (forming is for charts).
 */
export function planBootstrapThenSubscribe(opts: {
  timeframe: Timeframe;
  /** How far back history should go (ms). */
  lookbackMs: number;
  nowMs?: number;
  /** Overlap bars at the seam (default 2). */
  overlapBars?: number;
}): BootstrapSubscribePlan {
  const nowMs = opts.nowMs ?? Date.now();
  const barMs = TIMEFRAME_MS[opts.timeframe];
  const overlapBars = opts.overlapBars ?? 2;
  const overlapMs = barMs * overlapBars;
  const historyFromMs = nowMs - opts.lookbackMs;
  return {
    historyFromMs,
    historyToMs: nowMs,
    overlapMs,
    sequence: [
      'subscribe_live',
      'history_get_with_overlap',
      'upsert_by_primary_key',
    ],
    note:
      'Subscribe live first, then history GET through now (overlap ≥2 bars). ' +
      'Idempotent upsert by (venue,symbol,timeframe,openTimeMs) removes seam dups. ' +
      'Alerts consume isFinal=true only.',
  };
}

export type CandleHandler = (candle: Candle) => void;
export type Unsubscribe = () => void;

export interface BootstrapFetchHistory {
  (args: {
    fromMs: number;
    toMs: number;
  }): Promise<readonly Candle[]> | readonly Candle[];
}

export interface BootstrapSubscribe {
  (handler: CandleHandler): Unsubscribe;
}

export interface BootstrapThenSubscribeResult {
  /** Closed history applied (and any overlapping live finals already seen). */
  history: Candle[];
  /** Live candles received during/after bootstrap (forming + finals). */
  liveReceived: Candle[];
  /** Merged set keyed by openTimeMs (last write wins) — no dups. */
  mergedByOpenTimeMs: Map<number, Candle>;
  unsubscribe: Unsubscribe;
  plan: BootstrapSubscribePlan;
}

/**
 * Executable MD-1.9 helper: subscribe → history with overlap → upsert merge.
 * Designed for offline tests with mock fetch/subscribe; Mobile/Alerts can
 * mirror the same sequence against HTTP/SSE.
 */
export async function bootstrapThenSubscribe(opts: {
  timeframe: Timeframe;
  lookbackMs: number;
  fetchHistory: BootstrapFetchHistory;
  subscribe: BootstrapSubscribe;
  nowMs?: number;
  overlapBars?: number;
  /**
   * Optional sink for merged candles (e.g. client store upsert).
   * Called for every history row and every live event.
   */
  onUpsert?: CandleHandler;
}): Promise<BootstrapThenSubscribeResult> {
  const plan = planBootstrapThenSubscribe({
    timeframe: opts.timeframe,
    lookbackMs: opts.lookbackMs,
    nowMs: opts.nowMs,
    overlapBars: opts.overlapBars,
  });

  const mergedByOpenTimeMs = new Map<number, Candle>();
  const liveReceived: Candle[] = [];

  const apply = (c: Candle) => {
    mergedByOpenTimeMs.set(c.openTimeMs, c);
    opts.onUpsert?.(c);
  };

  // 1. Subscribe live first (capture seam overlap)
  const unsubscribe = opts.subscribe((c) => {
    liveReceived.push(c);
    apply(c);
  });

  // 2. History GET covering lookback through now (overlap into live)
  const historyRaw = await opts.fetchHistory({
    fromMs: plan.historyFromMs,
    toMs: plan.historyToMs,
  });
  const history = [...historyRaw];
  for (const c of history) {
    apply(c);
  }

  return {
    history,
    liveReceived,
    mergedByOpenTimeMs,
    unsubscribe,
    plan,
  };
}
