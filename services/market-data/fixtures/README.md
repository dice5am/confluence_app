# Market-data fixtures (Alerts ALT-golden-vectors-needed)

Offline MD-1.1 closed candles + health samples. Synthetic timestamps documented for **America/Toronto** calendar anchors.

## Closed candles (isFinal=true)

| Path | TF | Count (approx) | Notes |
|------|----|----------------|-------|
| `candles/btcusdt-1m-closed.json` | 1m | 120 | warm-up sample |
| `candles/btcusdt-5m-closed.json` | 5m | 120 | |
| `candles/btcusdt-15m-closed.json` | 15m | 120 | |
| `candles/btcusdt-1h-closed.json` | 1h | >=220 (YTD span) | SMA-200 + YTD/MTD VWAP lookback |
| `candles/btcusdt-4h-closed.json` | 4h | 120 | |
| `candles/btcusdt-1d-closed.json` | 1d | 90 | |
| `candles/btcusdt-1w-closed.json` | 1w | 60 | |

### 1h YTD / PIT anchors (documented in file)

- `ytdAnchorOpenTimeMs`: 2026-01-01T00:00:00 America/Toronto = 2026-01-01T05:00:00.000Z (EST UTC-5)
- `pitCustomAnchorOpenTimeMs`: bar index 100 openTime (for GV-VWAP-PIT)
- All series: venue=binance, symbol=BTCUSDT, every candle isFinal=true

Timestamps are synthetic (deterministic walk), not live Binance history.

## Health

| Path | Status |
|------|--------|
| `health/ok.json` | ok |
| `health/degraded.json` | degraded |
| `health/stale.json` | stale (>60s) |
| `health/disconnected.json` | disconnected |

Aligned with MD-1.1 section 3 and docs/contracts/alerts/ALT-golden-vectors-needed.md.
