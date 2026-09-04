# ALT-1.2 — Shared indicator formula contract (v0.1)

**Status:** Kickoff / freeze candidate for Mobile ACK  
**Owner:** Confluence Alerts Engineer (co-own with Mobile)  
**Phase:** 1 contract; overlay impl P3; alert eval P4  
**Consumes:** MD-1.1 candles `(venue, symbol, timeframe, openTimeMs, OHLCV, isFinal)`  
**Timezone default:** America/Toronto (locked) unless Cavin overrides

## 1. Versioning
- Document id: `formula-contract`
- Semver in frontmatter / header: **0.1.0**
- Breaking formula changes bump MAJOR; param default tweaks MINOR; docs-only PATCH
- Both Mobile and Alerts MUST pin the same version string in builds that claim parity

## 2. Common series conventions
- Inputs: closed candles only (`isFinal=true`) unless a formula explicitly documents forming behavior (none in v0.1 alertables).
- Prices: USDT; volume: BTC base (per MD-1.1).
- Missing bars: do not invent; gap handling = leave series undefined across gap (document per indicator if special).
- Precision: store/compute in float64; display rounding is Mobile UX (not part of parity vectors).

## 3. Day-one formulas (kickoff)

### 3.1 RSI
- `rsi(period=14)` Wilder smoothing on close
- Outputs: `rsi ∈ [0,100]` per bar after warmup

### 3.2 Volume + volume SMA
- `volume` = MD volume
- `volume_sma(period=20)` SMA of volume
- Spike helper (spec only): `volume / volume_sma` ratio — threshold later in P4 taxonomy

### 3.3 Moving averages
- `sma(source=close, period)`
- `ema(source=close, period)` standard EMA
- Day-one pairs (defaults): EMA 9/21, SMA 50, SMA 200 — Mobile may plot more; Alerts alertable set stays this list until catalog says otherwise

### 3.4 VWAP — see ALT-1.2.1
- Modes: `MTD` | `YTD` | `PIT_CUSTOM`
- Typical price per bar: `(high+low+close)/3` (document if HLC3 vs close-only — **HLC3 locked here**)
- Cumulative PV / cumulative V from mode anchor; reset per mode rules in appendix

### 3.5 Price levels
- User/manual levels: absolute price; interactions defined in P4 taxonomy (not runtime here)
- Presets (names only): prior day high/low (session calendar America/Toronto)

### 3.6 Ichimoku (formula present for overlay parity; alertable later)
- Standard defaults: Tenkan 9, Kijun 26, Senkou B 52, displacement 26
- Cloud / spans forward-shifted per usual Ichimoku

### 3.7 Volume profile (overlay/alertable later; compute assumptions)
- Prefer **1h+** history (15m optional); do not assume >90d of 1m
- POC / VAH / VAL definitions deferred to detailed design pass; P1 only notes history constraint

### 3.8 Fibonacci
- **Manual draw only** day one (Mobile). Auto not day one.
- Alert rules **deferred** (ALT-4.3 backlog). Contract stores drawing params only: `swingHigh`, `swingLow`, standard ratios 0/0.236/0.382/0.5/0.618/0.786/1 (+ optional extensions)

## 4. Identity of a computed point
`(formulaId, paramsHash, venue, symbol, timeframe, openTimeMs) → value(s)`

## 5. Non-goals (P1)
- No trust score, no push, no interaction detector implementation
- No venue blending

## 6. ACK
Mobile: `ACK ALT-1.2` or `CHANGES:`.
