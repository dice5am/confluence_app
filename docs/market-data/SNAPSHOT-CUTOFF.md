# Frozen MD Snapshot Cutoff

- **Cutoff UTC:** `2026-09-11 14:59 UTC`
- **Cutoff America/Toronto:** `2026-09-11 10:59 EDT (America/Toronto, UTC-4)`
- **cutoffMs:** `1789138799999` (prefer 1h last close end)
- **Fetched at (UTC):** `2026-09-11 15:40:24 UTC`
- **Source:** public REST `data-api.binance.vision` (no secrets)
- **Symbol / venue:** `BTCUSDT` / `binance`

## Note

Live WebSocket market data remains **HOLD**. This refresh updates the **frozen snapshot only** for Android assets / offline bootstrap.

## Per-timeframe closed candles

| TF | Count | Last close | lastCloseTimeMs | lastClose UTC |
|----|------:|-----------:|----------------:|---------------|
| 1m | 799 | 78780.01 | 1789141199999 | 2026-09-11 15:39:59 UTC |
| 5m | 499 | 78780.01 | 1789141199999 | 2026-09-11 15:39:59 UTC |
| 15m | 499 | 78720.76 | 1789140599999 | 2026-09-11 15:29:59 UTC |
| 1h | 499 | 78798.51 | 1789138799999 | 2026-09-11 14:59:59 UTC |
| 4h | 399 | 77036.12 | 1789127999999 | 2026-09-11 11:59:59 UTC |
| 1d | 399 | 76568.72 | 1789084799999 | 2026-09-10 23:59:59 UTC |
| 1w | 199 | 80341.83 | 1788739199999 | 2026-09-06 23:59:59 UTC |

## Depth targets (MD-2.4)

- 1m ≥500 (used ~800)
- 5m, 15m, 1h ≥300 (used ~500)
- 4h, 1d ≥200 (used ~400)
- 1w ≥150 (used ~200)
