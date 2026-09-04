# MD-2.1 — Timeframe policy (venue-native)

**Status:** Phase 2 GREENLIGHT (MD-2.1 / 2.2 / 2.3)  
**Owner:** Market Data Engineer  
**Repo:** https://github.com/dice5am/confluence_app  
**Consumers:** Mobile Chart & UX, Confluence Alerts  
**Hard rules:** free public data only · no secrets · no trade execution · no silent 1m rollup

---

## Decision (locked)

All **7** product timeframes use **venue-native Binance klines**. MD does **not** synthesize higher TFs by rolling up `1m` (or any lower TF) for consumer series.

| TF | Binance interval | Source mode | Notes |
|----|------------------|-------------|--------|
| `1m` | `1m` | venue-native | Gap-fill (MD-1.5) also uses native `1m` REST |
| `5m` | `5m` | venue-native | No `1m→5m` rollup |
| `15m` | `15m` | venue-native | No rollup |
| `1h` | `1h` | venue-native | No rollup |
| `4h` | `4h` | venue-native | No rollup — matches exchange/TradingView venue bars |
| `1d` | `1d` | venue-native | UTC day open per Binance |
| `1w` | `1w` | venue-native | Monday open per Binance |

Code authority: `services/market-data/src/policy/tf-policy.ts` + `src/binance/map.ts` (`timeframeToBinanceInterval`).

---

## Why not roll up from 1m?

1. **Chart parity** — rolled OHLC can diverge from Binance / TradingView venue charts (session boundaries, partial bars, volume aggregation).
2. **Contract identity** — primary key is `(venue, symbol, timeframe, openTimeMs)`; each TF is its own series from the venue.
3. **Alerts fidelity** — indicators on rolled bars can fire differently than on venue-native bars.

**Rollup is out of scope** for consumer candle series. If a future failover venue lacks an interval, that is an explicit fallback decision (document + flag) — never a silent path.

---

## Ingest implications

| Path | Behavior |
|------|----------|
| REST backfill (MD-2.2) | One native-interval request stream per TF; shared weight budget with 1m gap-fill |
| Live WS (MD-2.3) | Subscribe `kline_<interval>` only for **configured in-use** TFs; forming + final |
| Gap-fill (MD-1.5) | Still **1m-only**; does not invent higher-TF bars from fills |

---

## Non-goals (this doc / this PR)

- MD-2.5 / 2.6 aggTrades / volume profile  
- MD-2.4 bootstrap depth freeze (AL/MO numbers)  
- Bybit failover / rollup fallback  
- Live VPS deploy · Android client work  

---

## Changelog

| Date (America/Toronto) | Change |
|------------------------|--------|
| 2026-09-04 | Initial lock: all 7 TFs venue-native; no silent 1m rollup |
