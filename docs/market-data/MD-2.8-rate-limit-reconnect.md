# MD-2.8 — Rate-limit + reconnect policy

**Status:** Implemented in `services/market-data`  
**Owner:** Market Data Engineer  
**Hard rules:** never tight 429 loop · prefer WS over REST poll · public data only · no secrets

---

## REST weight budget

| Knob | Default | Code |
|------|---------|------|
| Klines request weight | `2` | `KLINES_REQUEST_WEIGHT` |
| Shared ceiling / rolling minute | `1000` | `DEFAULT_WEIGHT_LIMIT_PER_MIN` |
| Soft reserve for 1m gap-fill | `100` | `DEFAULT_GAP_FILL_RESERVE` |

`RestWeightBudget.acquire(weight)` blocks until the request fits. Multi-TF backfill (MD-2.2) and gap-fill (MD-1.5) share one process-local budget so bootstrap cannot burn the IP limit.

## REST retry (429 / 418 / 5xx / timeout)

| Knob | Default |
|------|---------|
| Initial backoff | `1000ms` |
| Max backoff | `60s` |
| Max retries | `5` (after first attempt) |
| Jitter | ~25% of base |
| Retry-After | Honored (max with computed delay) |

Code: `withRestRetry` + `computeRestBackoffMs` in `src/binance/reconnect-policy.ts`.  
`fetchKlinesPage` uses this by default (`retry: false` to disable).

**Never** tight-loop on 429 — each retry sleeps at least the backoff.

## WS reconnect

| Knob | Default |
|------|---------|
| Initial backoff | `500ms` |
| Max backoff | `30s` |
| Jitter | ≤20% / 250ms cap |
| On `open` | Attempt counter resets |

Code: `BinanceKlineWsClient` → `computeWsReconnectDelayMs`. Live ingest prefers WS; REST is for backfill / gap-fill / fallback — not a tight poll loop.

## Policy summary export

`RATE_LIMIT_RECONNECT_POLICY_SUMMARY` (library) mirrors this doc for runtime introspection.

---

## Changelog

| Date (America/Toronto) | Change |
|------------------------|--------|
| 2026-09-04 | Written + coded policy for REST weight/retry + WS backoff |
