# Golden vectors needed (P1 list only — do not implement scorer/push)

**Owner:** Confluence Alerts Engineer  
**Purpose:** Parity fixtures for shared formula contract (Mobile overlays ↔ future Alerts backend)  
**Status:** Requirements list for MD sample candles + later CI fixtures

## APPLY calc engine pass (2026-09-19) — five autos only

This APPLY ENTRY proves local calc for **RSI, volume SMA, MAs, Ichimoku, volume profile**.

**Held (deferred, not abandoned):** VWAP (MTD/YTD/PIT), Fib, discretionary/manual price levels, auto pivots. Do not treat GV-VWAP-* as EXIT for this PR.

## Rules
- Inputs: MD-1.1 closed candles only
- One vector set per formula below on `BTCUSDT`, venue `binance` (and optionally `bybit` later)
- Include warmup bars so steady-state values are defined
- **Do not** implement trust score / push in P1

## Required vectors (Phase-A alertables + VWAP modes)

| ID | Formula | Params | TF suggestion | Notes |
|----|---------|--------|---------------|-------|
| GV-RSI-14 | rsi | period=14 | 1h | ≥100 closed bars — **APPLY this pass** |
| GV-VOL-SMA-20 | volume_sma | period=20 | 1h | **APPLY this pass** |
| GV-EMA-9 | ema | period=9 | 1h | **APPLY this pass** |
| GV-EMA-21 | ema | period=21 | 1h | **APPLY this pass** |
| GV-SMA-50 | sma | period=50 | 1h | **APPLY this pass** |
| GV-SMA-200 | sma | period=200 | 1h | needs depth — **APPLY this pass** |
| GV-VWAP-MTD | vwap | mode=MTD, tz=America/Toronto | 1h | **HOLD this APPLY pass** |
| GV-VWAP-YTD | vwap | mode=YTD, tz=America/Toronto | 1h | **HOLD this APPLY pass** |
| GV-VWAP-PIT | vwap | mode=PIT_CUSTOM, anchorOpenTimeMs=T | 1h | **HOLD this APPLY pass** |

## Optional / later (not P1 blocker)
| ID | Formula | Notes |
|----|---------|-------|
| GV-ICH-STD | ichimoku 9/26/52/26 | overlay parity P3 — **APPLY calc this pass** |
| GV-VP-1H | volume profile fixed 24-bar lookback | **APPLY calc this pass**; not a manual range |

Manual price levels / prior-day H/L / Fib / pivots: **not** APPLY EXIT.

## MD ask
Provide exportable closed-candle JSON fixtures covering the lookbacks above (esp. SMA-200 and YTD VWAP on 1h).

## Non-goals
- No scorer fixtures in P1
- No push payload fixtures in P1

