# MD-2.4 — Bootstrap depth proposal (chart P2)

**Status:** Proposal for chart Phase 2 — **not blocked on Alerts**. Alerts may raise minima later (Ichimoku etc.).  
**Owner:** Market Data Engineer  
**Repo:** https://github.com/dice5am/confluence_app  
**Consumers:** Mobile Chart & UX (proceed), Confluence Alerts (confirm / raise later)  
**Hard rules:** free public data only · no secrets · no trade execution

---

## Proposed minimum closed bars (per TF)

These are **consumer bootstrap** floors for first paint + indicator headroom on charts. They are independent of MD-1.1 **stored history depth** caps (60d 1m, etc.).

| TF | Min closed bars | Approx wall-clock (at TF) | Notes |
|----|-----------------|---------------------------|--------|
| `1m` | **≥500** | ~8.3h | Chart cold start / short lookback |
| `5m` | **≥300** | ~25h | |
| `15m` | **≥300** | ~3.1d | |
| `1h` | **≥300** | ~12.5d | |
| `4h` | **≥200** | ~33d | |
| `1d` | **≥200** | ~200d | |
| `1w` | **≥150** | ~2.9y | |

**Chart may proceed with these numbers.** Alerts may raise later (e.g. Ichimoku ~52 periods + headroom on higher TFs).

---

## How consumers use this

1. Prefer `GET /v1/bootstrap-plan?timeframe=&lookbackMs=` with `lookbackMs ≥ minBars * barMs`.
2. History GET closed bars (`isFinal=true` only) via `GET /v1/candles` — paginate/resume per MD-2.7 if the range is large.
3. Subscribe live (`GET /v1/candles/stream`) with overlap (MD-1.9).
4. Honor health (MD-2.9): badge / suppress on `stale` / `disconnected`.

MD backfill (`backfillAllTimeframes` / MD-2.2) should target at least these minima per TF when seeding a fresh store (`maxBarsPerTf`).

---

## Relationship to stored depth (MD-1.1 §2)

| Concern | Authority |
|---------|-----------|
| How far back the **store** keeps | MD-1.1 depth caps (`1m` 60d, …) |
| How many bars Mobile/Alerts need at **cold start** | **This doc (MD-2.4)** |

Bootstrap mins ≪ stored depth for `1m`/`5m`/`15m`. Higher TFs are unbounded in store; bootstrap mins still apply for first paint.

---

## Non-goals

- Final AL lock for Ichimoku / MA / RSI warmup (Alerts may raise later)
- MD-2.5 / 2.6 VP
- Changing MD-1.1 stored depth caps
- VPS / Android client work

---

## Changelog

| Date (America/Toronto) | Change |
|------------------------|--------|
| 2026-09-04 | Initial chart-P2 proposal: 1m≥500, 5m≥300, 15m≥300, 1h≥300, 4h≥200, 1d≥200, 1w≥150 |
