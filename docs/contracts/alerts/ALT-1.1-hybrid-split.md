# ALT-1.1 / ALT-1.1.1 — Arch-B hybrid split (P1)

**Status:** Draft for ACK (Mobile + Market Data + PM)  
**Owner:** Confluence Alerts Engineer  
**Phase:** 1 (contracts only — no eval/push runtime)  
**Repo:** https://github.com/dice5am/confluence_app  
**Architecture:** Arch-B (locked)

## 1. Purpose
Define ownership boundaries so Mobile overlays and (later) Alerts eval/push do not diverge on indicator math or candle truth.

## 2. Locked split

| Concern | Owner | Phase when runtime lands |
|---------|-------|---------------------------|
| Candle ingest, history, live forming/finals, health | Market Data (MD-1.1) | P1+ |
| Chart UI, overlays, Fib **manual** draw, PIT VWAP anchor UX | Mobile Chart & UX | P2–P3 |
| Shared **versioned formula contract** | Alerts + Mobile co-own; Alerts drafts | P1 contract; implement P3+ |
| Backend indicator series for alertables, interaction detect, trust score, thresholds, push, history API | Alerts (Arch-B backend) | **P4 only** (not P1) |
| Deep-link IDs / highlight markers | Mobile owns IDs; Alerts emits refs in payloads | Draft P1/P2; runtime P4 |

## 3. Rules
1. **One formula contract** (`docs/contracts/formula-contract.md`, semver). Mobile overlays and Alerts backend MUST use the same definitions for any series both render and evaluate.
2. **MD is candle SoT.** Alerts and Mobile consume MD-1.1; neither invents alternate OHLCV.
3. **No trade execution.** Insights only (`BUY_INSIGHT` / `SELL_INSIGHT` / `WATCH` later).
4. **No exchange keys on device.** No venue merge.
5. **P1 delivers docs only** — no scorer, no push, no rule runtime.

## 4. ACK
Reply `ACK ALT-1.1` or `CHANGES:` (list). Do not silently fork ownership.
