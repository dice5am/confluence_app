# Golden vectors needed (P1 list only — do not implement scorer/push)

**Owner:** Confluence Alerts Engineer  
**Purpose:** Parity fixtures for shared formula contract (Mobile overlays ↔ future Alerts backend)  
**Status:** Requirements list for MD sample candles + later CI fixtures

## Rules
- Inputs: MD-1.1 closed candles only
- One vector set per formula below on `BTCUSDT`, venue `binance` (and optionally `bybit` later)
- Include warmup bars so steady-state values are defined
- **Do not** implement trust score / push in P1

## Required vectors (Phase-A alertables + VWAP modes)

| ID | Formula | Params | TF suggestion | Notes |
|----|---------|--------|---------------|-------|
| GV-RSI-14 | rsi | period=14 | 1h | ≥100 closed bars |
| GV-VOL-SMA-20 | volume_sma | period=20 | 1h | |
| GV-EMA-9 | ema | period=9 | 1h | |
| GV-EMA-21 | ema | period=21 | 1h | |
| GV-SMA-50 | sma | period=50 | 1h | |
| GV-SMA-200 | sma | period=200 | 1h | needs depth |
| GV-VWAP-MTD | vwap | mode=MTD, tz=America/Toronto | 1h | month partial OK |
| GV-VWAP-YTD | vwap | mode=YTD, tz=America/Toronto | 1h | |
| GV-VWAP-PIT | vwap | mode=PIT_CUSTOM, anchorOpenTimeMs=T | 1h | pick documented T |

## Optional / later (not P1 blocker)
| ID | Formula | Notes |
|----|---------|-------|
| GV-ICH-STD | ichimoku 9/26/52/26 | overlay parity P3 |
| GV-VP-1H | volume profile session/fixed | needs ALT-4.2 design; 1h+ hist |

## MD ask
Provide exportable closed-candle JSON fixtures covering the lookbacks above (esp. SMA-200 and YTD VWAP on 1h).

## Non-goals
- No scorer fixtures in P1
- No push payload fixtures in P1
