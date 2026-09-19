# Frozen MD Snapshot Cutoff

- **Cutoff UTC:** `2026-09-19 10:59 UTC`
- **Cutoff America/Toronto:** `2026-09-19 06:59 EDT (America/Toronto, UTC-4)`
- **cutoffMs:** `1789815599999` (prefer 1h last close end)
- **Fetched at (UTC):** `2026-09-19 11:05:12 UTC`
- **Source:** public REST `data-api.binance.vision` (no secrets)
- **Symbol / venue:** `BTCUSDT` / `binance`

## Note

Live WebSocket market data remains **HOLD**. This refresh updates the **frozen snapshot only** for Android assets / offline bootstrap.

## Per-timeframe closed candles

| TF | Count | Last close | lastCloseTimeMs | lastClose UTC |
|----|------:|-----------:|----------------:|---------------|
| 1m | 799 | 81300.0 | 1789815899999 | 2026-09-19 11:04:59 UTC |
| 5m | 499 | 81300.0 | 1789815899999 | 2026-09-19 11:04:59 UTC |
| 15m | 499 | 81216.7 | 1789815599999 | 2026-09-19 10:59:59 UTC |
| 1h | 499 | 81216.7 | 1789815599999 | 2026-09-19 10:59:59 UTC |
| 4h | 399 | 81100.16 | 1789804799999 | 2026-09-19 07:59:59 UTC |
| 1d | 399 | 80883.87 | 1789775999999 | 2026-09-18 23:59:59 UTC |
| 1w | 199 | 76842.01 | 1789343999999 | 2026-09-13 23:59:59 UTC |

## Depth targets (MD-2.4)

- 1m ≥500 (used ~800)
- 5m, 15m, 1h ≥300 (used ~500)
- 4h, 1d ≥200 (used ~400)
- 1w ≥150 (used ~200)
