/** Typed errors for Binance public REST/WS. */

export type BinanceErrorCode =
  | 'RATE_LIMIT_429'
  | 'IP_BAN_418'
  | 'SERVER_5XX'
  | 'TIMEOUT'
  | 'NETWORK'
  | 'PARSE'
  | 'UNKNOWN';

export class BinanceApiError extends Error {
  readonly code: BinanceErrorCode;
  readonly status?: number;
  readonly retryAfterMs?: number;

  constructor(
    code: BinanceErrorCode,
    message: string,
    opts?: { status?: number; retryAfterMs?: number; cause?: unknown },
  ) {
    super(message, opts?.cause !== undefined ? { cause: opts.cause } : undefined);
    this.name = 'BinanceApiError';
    this.code = code;
    this.status = opts?.status;
    this.retryAfterMs = opts?.retryAfterMs;
  }
}

export function classifyHttpStatus(status: number, bodySnippet?: string): BinanceApiError {
  if (status === 429) {
    return new BinanceApiError('RATE_LIMIT_429', `Binance rate limit (429): ${bodySnippet ?? ''}`.trim(), {
      status,
    });
  }
  if (status === 418) {
    return new BinanceApiError('IP_BAN_418', `Binance IP ban (418): ${bodySnippet ?? ''}`.trim(), {
      status,
    });
  }
  if (status >= 500 && status <= 599) {
    return new BinanceApiError('SERVER_5XX', `Binance server error (${status}): ${bodySnippet ?? ''}`.trim(), {
      status,
    });
  }
  return new BinanceApiError('UNKNOWN', `Binance HTTP ${status}: ${bodySnippet ?? ''}`.trim(), { status });
}

export function isTimeoutError(err: unknown): boolean {
  if (!err || typeof err !== 'object') return false;
  const e = err as { name?: string; code?: string; cause?: { code?: string } };
  return (
    e.name === 'TimeoutError' ||
    e.name === 'AbortError' ||
    e.code === 'ABORT_ERR' ||
    e.code === 'ETIMEDOUT' ||
    e.cause?.code === 'ETIMEDOUT'
  );
}
