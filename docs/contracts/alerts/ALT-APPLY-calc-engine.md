# ALT-APPLY — local indicator calc engine (five autos)

**Status:** APPLY ENTRY implementation (calc-only)  
**Owner:** Confluence Alerts Engineer  
**Consumes:** MD-1.1 closed candles (`SnapshotMarketDataApi` / `Candle` OHLCV+time)  
**Snapshot pin:** `md_snapshot@2026-09-19` (PR #29)  
**Module:** `:domain-indicators` (pure JVM; no Compose / no APK wiring)

This pass proves **five automatic families only**. It does not score, push, or draw chart chrome.

## 1. In scope (EXIT proof)

| Family | Contract | Defaults |
|--------|----------|----------|
| RSI | ALT-1.2 §3.1 Wilder on close | period 14 |
| Volume + volume SMA | ALT-1.2 §3.2 | SMA period 20 |
| MAs | ALT-1.2 §3.3 | EMA 9, EMA 21, SMA 50, SMA 200 on close |
| Ichimoku | ALT-1.2 §3.6 | 9 / 26 / 52, displacement 26 |
| Volume profile | ALT-1.2 §3.7 | last **24** closed bars; 50 rows; 70% value area |

Public facade: `IndicatorCalc.evaluate(bars, cutoff)` → `DayOneIndicators` (as-of the last fenced bar).  
Causal / historical overlay points: `IndicatorCalc.evaluateAsOf(bars, cutoff, asOfCloseTimeMs)` — global fence, then `closeTimeMs <= asOfCloseTimeMs`.  
Inputs are `List<IndicatorBar>`, a 1:1 OHLCV+time projection of `Candle` (same field names; no resampled/fake series). Charts/Alerts map `Candle` → `IndicatorBar` when they wire later.

Phase A look-ahead proof: [`ALT-PHASE-A-calc-honesty.md`](ALT-PHASE-A-calc-honesty.md).

Incremental path: `IndicatorCalc.onClosedBar(current, bar)` upserts one **final** bar and recomputes from the fenced window (same functions as batch; suitable for live closed bars later).

## 2. Cutoff fence (required)

Drop any bar with `isFinal == false` **or** `closeTimeMs > cutoffMs`.

Cutoff labels are passed through from snapshot `meta.json` / `docs/market-data/SNAPSHOT-CUTOFF.md` — the engine does not invent them.

Packaged pin:

- `cutoffMs = 1789815599999`
- UTC: `2026-09-19 10:59 UTC`
- America/Toronto: `2026-09-19 06:59 EDT (America/Toronto, UTC-4)`

`FenceReport` always exposes those labels plus kept/dropped counts. 1m and 5m packaged files currently overshoot the cutoff; 1h last close equals the cutoff and is kept.

## 3. Held this pass (deferred, not abandoned)

Do **not** treat these as APPLY EXIT items:

- VWAP (all modes: MTD / YTD / PIT_CUSTOM) — see ALT-1.2.1
- Fibonacci (manual swings or otherwise)
- Discretionary / manual price levels and prior-day H/L helpers
- Auto pivot / structure levels
- Confluence trust-score / push / interaction taxonomy runtime
- Live WS indicator streams, TradingView / exchange indicator APIs
- Mobile Charts APPLY chrome / AlertsScreen restyle / APK wiring

## 4. Volume profile session (honest)

Locked, candle-only rule: **last 24 closed bars of the provided series**. No manual range UI/params.

- Prefer callers pass **1h+** (ALT-1.2). The engine does not roll 1m up.
- On 1h this is ~24 hours of 1h candles. It is **not** an America/Toronto cash session and **not** tick/aggTrade VP.
- Snapshot 1h depth is ~499 bars (~20d). This engine never assumes >90d of 1m.
- Volume of each closed bar is spread uniformly across 50 price rows overlapping `[low, high]`.
- POC = midpoint of the highest-volume row (ties → lowest price). VAH/VAL = edges of the 70% value area expanded from POC.
- `evaluate(...).volumeProfile` is that snapshot **as-of the last fenced bar**. Historical as-of: `VolumeProfile.computeAsOf` / `IndicatorCalc.evaluateAsOf`.

If fewer than 24 bars are available, all fenced bars are used and `lookbackLimited = true`.

## 5. Honesty gaps (snapshot depth / formula)

| Topic | Reality on packaged snapshot |
|-------|------------------------------|
| SMA-200 | Defined on fenced **1h** (499) and **1d** (399). **Undefined on 1w** (199 < 200). |
| RSI / EMA / vol SMA | Warmup nulls until period is filled; snapshot 1h/1d have enough bars. |
| Ichimoku forward spans | Senkou A/B are plot-shifted by 26. `forwardCloud` uses projected timestamps (`lastOpen + k * barDuration`) — displaced plots, **not** invented future candles. Chikou is undefined on the last 26 plot slots (needs future closes). Senkou B needs 52 bars; 1h/1d ok. |
| Volume profile | Fixed 24-bar lookback of the *provided* window; closed-bar distribution, not ticks. Not a Toronto session. Full-series VP is as-of last bar — use `computeAsOf` for earlier bars. |
| Ichimoku Chikou | Plot-aligned `chikou[i] = close[i+26]` is **not** known at `i`. Causal read: `evaluateAsOf`. |
| 1m/5m fence | Must truncate overshoot before calc; unfenced last closes are after `cutoffMs`. |
| Gaps | Missing bars are not invented; series stay aligned to provided closed candles. |

## 6. Non-goals

- No trust score, no push, no interaction detector
- No venue blending, no secrets, no device keys
- Sandbox ≠ prod

## 7. Tests

`:domain-indicators` unit tests load packaged `data/src/main/assets/md_snapshot/` (and the services fixtures mirror), apply the fence, and assert finite outputs on fenced **1h** plus **1d** / **1w** honesty (SMA-200 warmup on 1w). An explicit test drops `closeTimeMs > cutoffMs` bars. Phase A prefix / as-of look-ahead tests live in `CalcHonestyLookAheadTest`.
