# ALT-PHASE-A — Calc honesty (no look-ahead)

**Status:** Phase A EXIT card (`:domain-indicators` / `IndicatorCalc`)  
**Engine:** `alt-apply-calc` `0.1.0-five-autos` (PR #30 on `main`)  
**Snapshot:** `md_snapshot@2026-09-19`  
**Cutoff:** `cutoffMs = 1789815599999` · `2026-09-19 10:59 UTC` · `2026-09-19 06:59 EDT (America/Toronto, UTC-4)`  
**Scope:** five autos only — RSI · volume (+SMA) · MAs · Ichimoku · volume profile  
**Out of scope:** live WS · VWAP/Fib · P3 alerts · design panes · APPLY APK · Mobile Charts chrome

This card proves: **every overlay point at bar `i` equals `IndicatorCalc` run on the candle window ending at that bar’s `closeTimeMs`** (only bars with `closeTimeMs <= bar_i.closeTimeMs`, after the global cutoff fence). No look-ahead. No pre-baked series.

## 1. Fence (same as APPLY)

Drop forming (`isFinal == false`) and any bar with `closeTimeMs > cutoffMs`. Then, for an as-of read at bar `i`, keep `closeTimeMs <= bar_i.closeTimeMs`.

| TF | Packaged bars | Fenced kept | Last kept `closeTimeMs` |
|----|--------------:|------------:|------------------------:|
| 1h | 499 | 499 (no overshoot) | `1789815599999` (= cutoff) |
| 1d | 399 | 399 | `1789775999999` |
| 1m | 799 | <799 (overshoot dropped) | `<= cutoffMs` |

As-of API: `IndicatorCalc.evaluateAsOf(bars, cutoff, asOfCloseTimeMs)` (optional `params`).  
`evaluate(bars, cutoff)` is **as-of the last fenced bar only**. Parameterized overloads (`IndicatorParams`) do **not** change the fence: still `isFinal && closeTimeMs <= cutoffMs`, then `closeTimeMs <= asOfCloseTimeMs`. Default params are the day-one five autos (EMA 9/21, SMA 50/200, RSI 14, Ichimoku 9/26/52/26, vol SMA 20, VP last-24).

## 2. Formula summary (known at `closeTimeMs` of bar `i`)

| Family | Formula (ALT-1.2) | Value known at bar `i` | Warmup |
|--------|-------------------|------------------------|--------|
| RSI | Wilder on close, period 14 | `rsi14[i]` uses closes `0..i` only | first defined at index 14 (`period+1` closes) |
| Volume | MD volume passthrough | `volume[i] = bar_i.volume` | none |
| Volume SMA | SMA 20 of volume | `volumeSma20[i]` uses volumes `i-19..i` | first defined at index 19 |
| EMA 9 / 21 | SMA seed, then `k=2/(p+1)` on close | `ema*[i]` uses closes `0..i` | first defined at `period-1` |
| SMA 50 / 200 | SMA on close | `sma*[i]` uses closes `i-p+1..i` | first defined at `period-1`; SMA-200 undefined on 1w (199 bars) |
| Ichimoku Tenkan / Kijun | Donchian midpoint 9 / 26 | `tenkan[i]`, `kijun[i]` use highs/lows `i-p+1..i` | 9 / 26 bars |
| Ichimoku Senkou A/B **raw** | `(tenkan+kijun)/2` and Donchian-52 | `senkouARaw[i]`, `senkouBRaw[i]` known at `i`; **plotted at `i+26`** | Senkou B raw needs 52 |
| Ichimoku Senkou A/B **plot** | raw shifted +26 | `senkouA[i] = senkouARaw[i-26]` — known at `i` (computed 26 bars earlier) | plot Senkou B from index 77 |
| Ichimoku Chikou **plot** | `chikou[i] = close[i+26]` | **not known at `i`**; known when bar `i+26` closes. Known-at-`i` close is `bar_i.close`, drawn at plot index `i-26` | last 26 plot slots undefined as-of last bar |
| Volume profile | last 24 closed bars of the *provided* window; 50 rows; 70% VA | snapshot as-of the last bar in that window — **not** a per-bar series | `lookbackLimited` if &lt;24 bars |

## 3. Sample bar audit

### 3.1 Fenced 1h · index 250

| Field | Value |
|-------|-------|
| `openTimeMs` | `1788919200000` · 2026-09-09 02:00:00 UTC |
| `closeTimeMs` | `1788922799999` · 2026-09-09 02:59:59 UTC |
| Prefix | fenced bars `0..250` (251 bars), all `closeTimeMs <= 1788922799999` |
| Full series | fenced 1h `0..498` (499 bars), last close `1789815599999` |

**Causal overlay points (prefix vs full at index 250):** RSI, volume, volume SMA, EMA 9/21, SMA 50/200, Tenkan, Kijun, Senkou A/B **plot**, Senkou A/B **raw** — **match**. Pinned values from `evaluateAsOf` (identical on the full series at this index):

| Point | Prefix = full at i=250 |
|-------|------------------------|
| RSI-14 | 50.25641484462052 |
| Volume SMA-20 | 747.073902 |
| EMA 9 / 21 | 78649.150053 / 78664.647431 |
| SMA 50 / 200 | 78978.239400 / 79034.746850 |
| Tenkan / Kijun | 78600.07 / 78552.505 |
| Senkou A/B **plot** (known earlier, drawn here) | 79360.0575 / 79619.995 |
| Senkou A/B **raw** (known here, drawn at i+26) | 78576.2875 / 79090.0 |
| Chikou **plot** | `null` on as-of (would be `close[276]` on full evaluate — not known at this close) |

Evidence: `CalcHonestyLookAheadTest.sampleBarAudit1hPrefixMatchesFullAtIndex250`  
and `causalSeriesOnFenced1hMatchPrefixEvaluate`.

**Volume profile at this as-of:** `computeAsOf` / `evaluateAsOf` uses last 24 bars **ending at index 250** (`windowLastCloseTimeMs = 1788922799999`) — POC 78463.0239, VAH 78753.2418, VAL 78228.0856.  
Full `evaluate` VP uses last 24 of the **whole** 1h series (`windowLastCloseTimeMs = 1789815599999`, POC 81100.321). Those snapshots are **not equal** — proving that reusing the full-series VP as a historical overlay at index 250 would peek ahead.

### 3.2 Fenced 1d · index 250

| Field | Value |
|-------|-------|
| `openTimeMs` | `1776902400000` · 2026-04-23 00:00:00 UTC |
| `closeTimeMs` | `1776988799999` · 2026-04-23 23:59:59 UTC |
| Prefix | fenced 1d `0..250` |
| Full series | fenced 1d `0..398` (399 bars), last close `1789775999999` |

Same causal match. Evidence: `sampleBarAudit1dPrefixMatchesFullAtIndex250`.

### 3.3 Fence still wins over a later as-of

`evaluateAsOf(1m, cutoff, overshootCloseTimeMs)` still drops `closeTimeMs > cutoffMs`.  
Evidence: `asOfDoesNotLiftGlobalCutoffFence`.

## 4. Look-ahead risks (explicit)

### Ichimoku Senkou forward displacement — **not look-ahead of plot values at `i`**

Plot-aligned `senkouA[i]` / `senkouB[i]` are raw values from `i-26`. They do **not** use bars after `i`. Prefix vs full match at `i`.

The **raw** spans known at `i` are plotted 26 bars forward:

- Inside the series: `evaluateAsOf(..., bar_i.closeTimeMs).forwardCloud[k]` equals `evaluate(full).senkouA[i+k]` when that index exists.
- Past the last bar: `forwardCloud` uses projected timestamps (`lastOpen + k * barDuration`). Those are displaced plots, **not** invented future candles.

Evidence: `ichimokuForwardCloudAtAsOfMatchesLaterPlotAlignedSenkou`.

### Ichimoku Chikou — **plot look-ahead if treated as known at `i`**

`chikou[i] = close[i+26]`. On a completed chart that is the usual Ichimoku drawing (current close plotted 26 bars back). It is **not** a value known at `bar_i.closeTimeMs`.

Honest read:

- As-of bar `i`, `chikou[j]` is defined iff `j+26 <= i` (value = `close[j+26]`).
- `evaluateAsOf` therefore leaves `chikou[i]` **null** (no future close).
- `evaluate(full).chikou[i]` **does** equal `close[i+26]` when that bar exists — that is visualization look-ahead on historical plot slots.

Consumers that need a causal overlay point at `i` must use `evaluateAsOf` (or only read `chikou[i-26] = close[i]`). The engine does not hide this: `IchimokuResult.chikou` KDoc states it.

Evidence: `ichimokuChikouPlotSlotIsKnownOnlyAfterDisplacedClose`, `futureBarShockDoesNotMoveCausalPointsOrAsOfVolumeProfile`.

### Volume profile — **single snapshot; historical reuse was look-ahead**

`VolumeProfile.compute(series)` / `DayOneIndicators.volumeProfile` is last-24 of the **provided** series (as-of its last bar). Overlaying that POC/VAH/VAL on earlier bars would use future volume.

**Fix this pass:** `VolumeProfile.computeAsOf(bars, asOfCloseTimeMs)` and `IndicatorCalc.evaluateAsOf(...)`. VP at as-of bar `i` uses only `bars[i-23..i]` (or fewer if limited). A future-bar price/volume shock changes full-series VP and does **not** change as-of VP.

Evidence: `volumeProfileAsOfUsesOnlyLast24EndingAtBar`, `VolumeProfileTest.computeAsOfIgnoresBarsAfterAsOfClose`, `futureBarShockDoesNotMoveCausalPointsOrAsOfVolumeProfile`.

## 5. PASS / FAIL per family

| Family | Verdict | Evidence |
|--------|---------|----------|
| RSI | **PASS** | `causalSeriesOnFenced1hMatchPrefixEvaluate`, `causalSeriesOnFenced1dMatchPrefixEvaluate`, sample-bar audits, future-shock test |
| Volume + SMA | **PASS** | same prefix tests (`volume`, `volumeSma20`) |
| MAs (EMA 9/21, SMA 50/200) | **PASS** | same prefix tests |
| Ichimoku (Tenkan, Kijun, Senkou plot + raw, forward cloud as-of) | **PASS** | prefix tests + `ichimokuForwardCloudAtAsOfMatchesLaterPlotAlignedSenkou` |
| Ichimoku Chikou (known-at-`i`) | **PASS** via as-of (undefined at plot index `i`); plot-aligned historical `chikou[i]` is **documented visualization look-ahead**, not a causal overlay point | `ichimokuChikouPlotSlotIsKnownOnlyAfterDisplacedClose` |
| Volume profile | **PASS** as-of last bar of the provided window; **PASS** historical as-of via `computeAsOf` / `evaluateAsOf` (this was the API gap) | `volumeProfileAsOfUsesOnlyLast24EndingAtBar`, `evaluateAsOfLastBarEqualsFullEvaluate` |

Causal overlay points (RSI / vol / MAs / Ichimoku known-at-`i` / VP as-of) survive a future-bar shock. Full-series Chikou plot slots and full-series VP do **not** — and must not be used as historical as-of values.

## 6. Gaps remaining (honest)

- **Chikou completed-chart drawing** still plots later closes onto earlier slots. That is standard Ichimoku, not a causal signal at those slots. Alerts / trust-score must not read `chikou[i]` as known at `i`.
- **Chart overlay wiring (PR #31)** still draws `evaluate(full)` Chikou and a **single** full-series VP. Chart chrome is out of scope here; historical VP/Chikou on that canvas can still *display* look-ahead. Honest calc path is `evaluateAsOf`.
- **No per-bar VP series** is stored on `DayOneIndicators` (expensive; 50-bin histogram each bar). Call `computeAsOf` / `evaluateAsOf` per as-of time.
- **VWAP / Fib / pivots / discretionary levels** not in this engine.
- **SMA-200 on 1w** remains undefined (199 &lt; 200) — warmup honesty, not look-ahead.
- **Gaps in candles** are not invented; series stay aligned to provided closed bars.
- **Golden vectors** (GV-RSI-14, …) still listed in `ALT-golden-vectors-needed.md`; this pass proves the prefix property on the packaged snapshot, not a checked-in numeric golden file.

## 7. What changed in the engine for this card

- `IndicatorCalc.evaluateAsOf` — global fence, then `closeTimeMs <= asOfCloseTimeMs` (optional `IndicatorParams`; defaults keep day-one five autos).
- `VolumeProfile.computeAsOf` — last-N ending at as-of (default N=24; ignores later bars).
- `IchimokuResult.senkouARaw` / `senkouBRaw` — values known at `i` vs plot-shifted `senkouA` / `senkouB`.
- Formulas themselves (RSI Wilder, EMA/SMA, Donchian Ichimoku, last-N VP) are unchanged at default params.
