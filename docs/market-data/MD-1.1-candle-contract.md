# MD-1.1 — Internal market data contract (v0.1 freeze candidate)

**Status:** Alerts ACK 2026-09-04. Mobile ACK pending. America/Toronto LOCKED. Repo push blocked (GitHub access) — escalated CoS. Phase 1 only.  
**Owner:** Market Data Engineer  
**Consumers:** Mobile Chart & UX, Confluence Alerts  
**Repo:** https://github.com/dice5am/confluence_app  
**Architecture:** Arch-B thin backend (VPS Docker). Device holds **no** exchange API keys. **No venue merge.** No trade execution.

---

## 1. Identity & candle

**Primary key:** `(venue, symbol, timeframe, openTimeMs)`

| Field | Type | Rules |
|-------|------|--------|
| `venue` | string enum | `binance` \| `bybit` (v1). Never blend series across venues. |
| `symbol` | string | v1: `BTCUSDT` only |
| `timeframe` | string enum | `1m` \| `5m` \| `15m` \| `1h` \| `4h` \| `1d` \| `1w` |
| `openTimeMs` | int64 | Candle start, UTC ms, inclusive |
| `closeTimeMs` | int64 | Candle end, UTC ms (venue-native end; document as exclusive of next open) |
| `open`, `high`, `low`, `close` | number | Quote currency (USDT) |
| `volume` | number | Base asset (BTC) |
| `isFinal` | boolean | `false` = forming; `true` = closed |
| `sourceTsMs` | int64 | When exchange event / REST row was observed |
| `ingestTsMs` | int64 | When we accepted it into store |

**Optional quality flags** (on candle or sidecar): `gap`, `stale`, `reconciled`, `sourceMismatch` (boolean or present/absent).

**TF policy (locked lean):** venue-native klines for all listed TFs. No silent 1m→higher rollup for consumer series.

---

## 2. History depth (LOCKED)

| TF | Stored depth |
|----|----------------|
| `1m` | 60d default (ops band 30–90d) |
| `5m` | 180d |
| `15m` | 1y |
| `1h` | full available from venue |
| `4h` | full available |
| `1d` | full available (priority) |
| `1w` | full available (priority) |

---

## 3. Health

```
status: "ok" | "degraded" | "stale" | "disconnected"
lastSourceTsMs: int64
venue: string
symbol: string
optional: gapCount, activeTimeframes[], note
```

| Status | Meaning | Consumer rule |
|--------|---------|----------------|
| `ok` | Live updates flowing | Normal |
| `degraded` | Partial TF / failover in progress | AL policy; MO may badge |
| `stale` | No update **>60s** | MO badge; **AL MUST suppress** new confluence fires |
| `disconnected` | No socket / no feed | MO badge + O1 stale-cache OK; **AL MUST suppress** |

**O1 stale-cache:** history GET may return last-good **closed** candles when status is `stale` or `disconnected`. Live forming may pause. Health still reported honestly.

---

## 4. Consumer ops (what Mobile/Alerts call)

### 4.1 History GET
- Input: `symbol`, `timeframe`, `fromMs`, `toMs` (and/or cursor)
- Output: array of candles with **`isFinal=true` only**, ordered by `openTimeMs` ascending
- Never raw exchange payloads
- Respect depth caps above; out-of-range → empty or truncated with clear signal

### 4.2 Live stream
- Forming bar: `isFinal=false` updates for subscribed TFs
- Closed bar: **exactly one** event with `isFinal=true` per `(venue,symbol,tf,openTimeMs)`
- **Alerts consume finals only** for triggers; charts may paint forming

### 4.3 Bootstrap-then-subscribe
1. History GET enough closed bars for the TF  
2. Subscribe live  
3. Overlap window so no hole/dup at the seam (idempotent upsert by primary key)

### 4.4 Health subscribe / poll
- Same health object as §3

---

## 5. Freshness SLA

- Prefer live WS; forming age **≤2s** on good network  
- Fallback REST: closed/forming freshness **≤1 minute**  
- Stale threshold: **>60s** without update → `stale`

---

## 6. VWAP note (for consumers — MD does not compute VWAP)

Locked modes: **MTD**, **YTD**, **custom PIT anchor** (e.g. last high). **Not** UTC-day session.

- Consumers request candle history from `anchorMs` → now via History GET  
- **YTD/MTD lookback:** use **`1h` (or `15m`)** — not year-long `1m`  
- Calendar anchors: **LOCKED `America/Toronto`** for MTD/YTD month/year start (PM/ALT-1.2.1)  
- Custom PIT: consumer supplies `anchorMs`  
- aggTrades window is for Volume Profile (later P3), **not** YTD VWAP

---

## 7. Non-goals (this contract)

- Trade execution / signed exchange APIs  
- Exchange keys on device  
- Merging Binance + Bybit into one OHLC series  
- Indicator math (Alerts), chart rendering (Mobile)  
- Raw venue payloads to app

---

## 8. Failover (v1 — data resilience)

Primary `binance`, failover `bybit`. On switch: new `venue` on all subsequent candles; **never blend**. Alerts must reset confluence window across venue change.

---

## 9. ACK

Please reply with: **ACK MD-1.1** or **CHANGES:** (list).  
Do not implement against a different private schema.

