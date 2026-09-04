/**
 * MD-2.8 — Rate-limit + reconnect policy (REST + WS).
 *
 * Hard rules:
 * - Never tight-loop on 429 / 418
 * - Prefer WS live over REST poll
 * - Exponential backoff with jitter; respect Retry-After when present
 * - Shared REST weight budget (see weight-budget.ts) prevents bootstrap storms
 *
 * Public market data only — no API keys.
 */

/** WS reconnect: initial delay (ms). */
export const WS_INITIAL_BACKOFF_MS = 500;
/** WS reconnect: cap (ms). */
export const WS_MAX_BACKOFF_MS = 30_000;
/** WS reconnect: jitter fraction of delay (0–1). */
export const WS_JITTER_FRACTION = 0.2;
/** WS reconnect: absolute jitter cap (ms). */
export const WS_JITTER_CAP_MS = 250;

/** REST 429/5xx: initial backoff (ms). */
export const REST_INITIAL_BACKOFF_MS = 1_000;
/** REST 429/5xx: cap (ms). */
export const REST_MAX_BACKOFF_MS = 60_000;
/** REST: max retries before surfacing the error (does not include the first attempt). */
export const REST_MAX_RETRIES = 5;
/** REST jitter fraction. */
export const REST_JITTER_FRACTION = 0.25;

export interface BackoffOptions {
  initialMs?: number;
  maxMs?: number;
  jitterFraction?: number;
  jitterCapMs?: number;
  /** Injected RNG for tests (0..1). */
  random?: () => number;
}

/**
 * Exponential backoff delay for WS reconnect attempt index (0-based after first close).
 * delay = min(initial * 2^attempt, max) + jitter
 */
export function computeWsReconnectDelayMs(
  attempt: number,
  opts: BackoffOptions = {},
): number {
  const initial = opts.initialMs ?? WS_INITIAL_BACKOFF_MS;
  const max = opts.maxMs ?? WS_MAX_BACKOFF_MS;
  const frac = opts.jitterFraction ?? WS_JITTER_FRACTION;
  const jitterCap = opts.jitterCapMs ?? WS_JITTER_CAP_MS;
  const random = opts.random ?? Math.random;
  const safeAttempt = Math.max(0, Math.floor(attempt));
  const base = Math.min(initial * Math.pow(2, safeAttempt), max);
  const jitter = Math.floor(random() * Math.min(jitterCap, base * frac));
  return base + jitter;
}

/**
 * Exponential backoff for REST retries after 429 / 418 / 5xx.
 * Honors `retryAfterMs` when provided (takes max of computed and Retry-After).
 */
export function computeRestBackoffMs(
  attempt: number,
  opts: BackoffOptions & { retryAfterMs?: number } = {},
): number {
  const initial = opts.initialMs ?? REST_INITIAL_BACKOFF_MS;
  const max = opts.maxMs ?? REST_MAX_BACKOFF_MS;
  const frac = opts.jitterFraction ?? REST_JITTER_FRACTION;
  const random = opts.random ?? Math.random;
  const safeAttempt = Math.max(0, Math.floor(attempt));
  const base = Math.min(initial * Math.pow(2, safeAttempt), max);
  const jitter = Math.floor(random() * base * frac);
  const computed = base + jitter;
  if (opts.retryAfterMs !== undefined && Number.isFinite(opts.retryAfterMs)) {
    return Math.max(computed, Math.max(0, opts.retryAfterMs));
  }
  return computed;
}

export interface RestRetryPolicyOptions {
  maxRetries?: number;
  initialMs?: number;
  maxMs?: number;
  sleep?: (ms: number) => Promise<void>;
  random?: () => number;
  /**
   * Predicate: should this error be retried?
   * Default: RATE_LIMIT_429, IP_BAN_418, SERVER_5XX, TIMEOUT.
   */
  shouldRetry?: (err: unknown, attempt: number) => boolean;
  /** Optional hook for tests / metrics. */
  onRetry?: (info: {
    attempt: number;
    delayMs: number;
    error: unknown;
  }) => void;
}

function defaultSleep(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

function defaultShouldRetry(err: unknown): boolean {
  if (!err || typeof err !== 'object') return false;
  const code = (err as { code?: string }).code;
  return (
    code === 'RATE_LIMIT_429' ||
    code === 'IP_BAN_418' ||
    code === 'SERVER_5XX' ||
    code === 'TIMEOUT'
  );
}

/**
 * Run an async REST call with exponential backoff on rate-limit / server errors.
 * Never tight-loops: each retry sleeps at least the computed backoff.
 */
export async function withRestRetry<T>(
  fn: () => Promise<T>,
  opts: RestRetryPolicyOptions = {},
): Promise<T> {
  const maxRetries = opts.maxRetries ?? REST_MAX_RETRIES;
  const sleep = opts.sleep ?? defaultSleep;
  const shouldRetry = opts.shouldRetry ?? ((e) => defaultShouldRetry(e));

  let attempt = 0;
  for (;;) {
    try {
      return await fn();
    } catch (err) {
      if (attempt >= maxRetries || !shouldRetry(err, attempt)) {
        throw err;
      }
      const retryAfterMs =
        err && typeof err === 'object' && 'retryAfterMs' in err
          ? Number((err as { retryAfterMs?: number }).retryAfterMs)
          : undefined;
      const delayMs = computeRestBackoffMs(attempt, {
        initialMs: opts.initialMs,
        maxMs: opts.maxMs,
        random: opts.random,
        retryAfterMs: Number.isFinite(retryAfterMs) ? retryAfterMs : undefined,
      });
      opts.onRetry?.({ attempt, delayMs, error: err });
      await sleep(delayMs);
      attempt += 1;
    }
  }
}

/** Human-readable policy summary for docs / health notes. */
export const RATE_LIMIT_RECONNECT_POLICY_SUMMARY = {
  restWeight: {
    klinesWeight: 2,
    defaultLimitPerMinute: 1000,
    gapFillReserve: 100,
    note: 'Shared RestWeightBudget gates multi-TF backfill + 1m gap-fill; never burn the full IP budget.',
  },
  restRetry: {
    initialBackoffMs: REST_INITIAL_BACKOFF_MS,
    maxBackoffMs: REST_MAX_BACKOFF_MS,
    maxRetries: REST_MAX_RETRIES,
    note: 'On 429/418/5xx/timeout: exponential backoff + jitter; honor Retry-After; never tight-loop.',
  },
  wsReconnect: {
    initialBackoffMs: WS_INITIAL_BACKOFF_MS,
    maxBackoffMs: WS_MAX_BACKOFF_MS,
    note: 'Prefer WS live over REST poll. Exponential backoff + jitter on disconnect; reset attempt on open.',
  },
} as const;
