# ALT-1.8 — MD close-events + health-gate interface (P1)

**Status:** Draft consuming **MD-1.1 v0.1** (Alerts ACK sent)  
**Owner:** Confluence Alerts Engineer  
**Phase:** 1 contract only — **no** eval/push implementation  
**Upstream:** Market Data MD-1.1

## 1. What Alerts consumes

### 1.1 Final closes (triggers later in P4)
- Subscribe/consume live stream; **Alerts confluence triggers = `isFinal=true` only**
- Exactly one final per MD primary key `(venue, symbol, timeframe, openTimeMs)`
- History GET for warm-up / VWAP lookback / VP: closed candles only, ascending `openTimeMs`

### 1.2 Health object
Fields per MD-1.1: `status`, `lastSourceTsMs`, `venue`, `symbol` (+ optional gapCount, activeTimeframes, note)

| Health status | Alerts gate (P1 policy → enforce in P4 runtime) |
|---------------|--------------------------------------------------|
| `ok` | Allow eval when runtime exists |
| `degraded` | **Suppress** new confluence fires (conservative AL policy until Cavin overrides) |
| `stale` | **MUST suppress** new confluence fires |
| `disconnected` | **MUST suppress** new confluence fires |

History may still return last-good closed candles (MD O1); suppression applies to **new fires**, not to reading history for charts/contracts.

## 2. Venue failover
If `venue` changes (e.g. binance→bybit), Alerts **resets confluence window** / score state (P4). Do not merge series across venues.

## 3. Interface notes (logical — not code)
```
onFinalCandle(candle: MdCandle)  // isFinal=true guaranteed
onHealth(health: MdHealth)
shouldEvaluate(health): boolean  // false if degraded|stale|disconnected
```
P1: document only. P4: implement on Arch-B backend.

## 4. Live price levels (future P4 — out of P1 scope)
Separate path may use forming quotes for level touch; **must not** use forming candles for indicator confluence. Not built in P1.

## 5. Golden vectors dependency
Need MD sample closed-candle fixtures per TF used in ALT golden-vector list (separate doc).

## 6. ACK
MD: confirm ALT-1.8 matches MD-1.1 intent. PM: note degraded=suppress policy.
