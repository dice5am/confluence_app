# Frozen MD Snapshot Cutoff

- **Cutoff UTC:** `2026-09-10 19:59 UTC`
- **Cutoff America/Toronto:** `2026-09-10 15:59 EDT (America/Toronto, UTC-4)`
- **cutoffMs:** `1789070399999` (prefer 1h last close end)
- **Fetched at (UTC):** `2026-09-10 20:09:16 UTC`
- **Source:** public REST `data-api.binance.vision` (no secrets)
- **Symbol / venue:** `BTCUSDT` / `binance`

## Note

Live WebSocket market data remains **HOLD**. This refresh updates the **frozen snapshot only** for Android assets / offline bootstrap.

## Per-timeframe closed candles

| TF | Count | Last close | lastCloseTimeMs | lastClose UTC |
|----|------:|-----------:|----------------:|---------------|
| 1m | 799 | 77248.01 | 1789070939999 | 2026-09-10 20:08:59 UTC |
| 5m | 499 | 77172.0 | 1789070699999 | 2026-09-10 20:04:59 UTC |
| 15m | 499 | 77160.83 | 1789070399999 | 2026-09-10 19:59:59 UTC |
| 1h | 499 | 77160.83 | 1789070399999 | 2026-09-10 19:59:59 UTC |
| 4h | 399 | 77160.83 | 1789070399999 | 2026-09-10 19:59:59 UTC |
| 1d | 399 | 78306.43 | 1788998399999 | 2026-09-09 23:59:59 UTC |
| 1w | 199 | 80341.83 | 1788739199999 | 2026-09-06 23:59:59 UTC |

## Depth targets (MD-2.4)

- 1m ≥500 (used ~800)
- 5m, 15m, 1h ≥300 (used ~500)
- 4h, 1d ≥200 (used ~400)
- 1w ≥150 (used ~200)
