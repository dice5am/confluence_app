/**
 * MD-2.2 — shared REST request-weight budget for multi-TF backfill + gap-fill.
 *
 * Binance spot IP limit is REQUEST_WEIGHT (commonly 1200–6000 / min depending
 * on endpoint group). We use a conservative sliding-window budget so parallel
 * TF backfill and 1m gap-fill cannot tight-loop into 429.
 *
 * Public data only — no API keys.
 */

/** Weight of GET /api/v3/klines (Binance spot). */
export const KLINES_REQUEST_WEIGHT = 2;

/** Conservative shared ceiling (weight units per rolling minute). */
export const DEFAULT_WEIGHT_LIMIT_PER_MIN = 1000;

/**
 * Soft reserve held back from backfill for 1m gap-fill / emergency REST.
 * Backfill may still use reserved weight only when `allowReserve` is true.
 */
export const DEFAULT_GAP_FILL_RESERVE = 100;

export interface RestWeightBudgetOptions {
  /** Max weight per rolling window. Default 1000. */
  limitPerMinute?: number;
  /** Weight reserved for gap-fill. Default 100. */
  gapFillReserve?: number;
  /** Window length ms. Default 60_000. */
  windowMs?: number;
  /** Clock injection for tests. */
  nowMs?: () => number;
  /**
   * Sleep helper (tests inject a no-op / fake timer).
   * Defaults to real setTimeout.
   */
  sleep?: (ms: number) => Promise<void>;
}

interface WeightEvent {
  atMs: number;
  weight: number;
}

function defaultSleep(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

/**
 * Sliding-window weight gate. Call `acquire(weight)` before each REST call;
 * it waits until the request fits under the budget (leaving gap-fill reserve
 * for non-reserve callers).
 */
export class RestWeightBudget {
  private readonly limit: number;
  private readonly reserve: number;
  private readonly windowMs: number;
  private readonly nowMs: () => number;
  private readonly sleep: (ms: number) => Promise<void>;
  private events: WeightEvent[] = [];
  /** Serialize acquire waits so concurrent callers don't oversubscribe. */
  private chain: Promise<void> = Promise.resolve();

  constructor(options: RestWeightBudgetOptions = {}) {
    this.limit = options.limitPerMinute ?? DEFAULT_WEIGHT_LIMIT_PER_MIN;
    this.reserve = options.gapFillReserve ?? DEFAULT_GAP_FILL_RESERVE;
    this.windowMs = options.windowMs ?? 60_000;
    this.nowMs = options.nowMs ?? Date.now;
    this.sleep = options.sleep ?? defaultSleep;
    if (this.reserve >= this.limit) {
      throw new Error('gapFillReserve must be < limitPerMinute');
    }
  }

  /** Weight consumed in the current window (after prune). */
  used(): number {
    this.prune(this.nowMs());
    return this.events.reduce((s, e) => s + e.weight, 0);
  }

  /** Remaining weight for a caller. Non-reserve callers see limit − reserve − used. */
  remaining(opts?: { allowReserve?: boolean }): number {
    const used = this.used();
    const cap = opts?.allowReserve ? this.limit : this.limit - this.reserve;
    return Math.max(0, cap - used);
  }

  /**
   * Block until `weight` fits, then record it.
   * @param allowReserve when true (gap-fill), may spend into the reserve band.
   */
  async acquire(
    weight: number,
    opts?: { allowReserve?: boolean },
  ): Promise<void> {
    if (weight <= 0) return;
    const allowReserve = opts?.allowReserve === true;
    const run = async () => {
      for (;;) {
        const now = this.nowMs();
        this.prune(now);
        const cap = allowReserve ? this.limit : this.limit - this.reserve;
        const used = this.events.reduce((s, e) => s + e.weight, 0);
        if (used + weight <= cap) {
          this.events.push({ atMs: now, weight });
          return;
        }
        const oldest = this.events[0];
        const waitMs = oldest
          ? Math.max(1, oldest.atMs + this.windowMs - now)
          : this.windowMs;
        await this.sleep(waitMs);
      }
    };
    const next = this.chain.then(run, run);
    this.chain = next.then(
      () => undefined,
      () => undefined,
    );
    await next;
  }

  /** Test helper: record weight without waiting. */
  record(weight: number): void {
    this.events.push({ atMs: this.nowMs(), weight });
  }

  /** Test helper: clear history. */
  reset(): void {
    this.events = [];
  }

  private prune(nowMs: number): void {
    const cutoff = nowMs - this.windowMs;
    while (this.events.length > 0 && this.events[0]!.atMs < cutoff) {
      this.events.shift();
    }
  }
}

/** Shared process-local budget (backfill + gap-fill). Overridable in tests. */
let sharedBudget: RestWeightBudget | undefined;

export function getSharedRestWeightBudget(): RestWeightBudget {
  if (!sharedBudget) {
    sharedBudget = new RestWeightBudget();
  }
  return sharedBudget;
}

/** Replace shared budget (tests). Pass undefined to clear. */
export function setSharedRestWeightBudget(
  budget: RestWeightBudget | undefined,
): void {
  sharedBudget = budget;
}
