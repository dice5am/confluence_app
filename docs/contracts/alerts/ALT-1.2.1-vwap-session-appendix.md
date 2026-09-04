# ALT-1.2.1 — VWAP session / TZ appendix (P1)

**Status:** Locked modes (Cavin/PM); encoding for shared contract  
**Owner:** Confluence Alerts Engineer  
**TZ:** **America/Toronto** (locked)  
**Candle basis:** Prefer **1h+** for YTD/MTD/PIT lookback; **15m optional**; do **not** assume >90d of 1m (MD history lock)

## 1. Modes (day one)

| Mode id | Meaning | Anchor | Reset |
|---------|---------|--------|-------|
| `MTD` | Month-to-date / rolling current calendar month | 00:00 America/Toronto on 1st of current month | Next month boundary |
| `YTD` | Year-to-date | 00:00 America/Toronto on Jan 1 of current year | Next Jan 1 |
| `PIT_CUSTOM` | Point-in-time from custom anchor | `anchor_ref` → resolves to `anchorOpenTimeMs` (and venue/symbol/tf) | None automatic; changes when anchor changes |

## 2. Computation
- From first candle with `openTimeMs >= anchorOpenTimeMs` through latest **closed** candle on the VWAP timeframe.
- Typical price: HLC3 = `(high+low+close)/3`
- `vwap = sum(tp * volume) / sum(volume)` over included bars (skip bars with volume=0 in denominator contribution; if all zero, value undefined)
- Chart and Alerts **MUST** use the same `mode` (+ same `anchor_ref` for PIT)

## 3. PIT_CUSTOM
- **UX owner:** Mobile (pick last high, etc.)
- **Alerts:** consumes `anchor_ref` opaque id + resolved `anchorOpenTimeMs` from Mobile/shared state — does not invent anchors
- MD: History GET from `anchorMs` (see MD-1.1 §6)

## 4. Calendar notes
- DST: use IANA `America/Toronto` (EST/EDT) for month/year boundaries — not fixed UTC offsets
- Venue candle times remain UTC ms; convert boundaries Toronto → UTC ms for filtering

## 5. ACK
Mobile + MD: `ACK ALT-1.2.1` or `CHANGES:`.

## 6. Mode source of truth (Mobile ACK fit note)
- **SoT:** Mobile Chart VWAP mode picker enum `MTD|YTD|PIT_CUSTOM` (+ optional `anchor_ref` for PIT).
- Alerts (P4) scores / explains against the **same** selected mode only — no parallel Alerts-only VWAP mode.
