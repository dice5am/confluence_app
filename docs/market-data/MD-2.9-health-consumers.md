# MD-2.9 — Health to consumers

**Status:** Wired (P1 `/health` + `/v1/health` + SSE) · extended for multi-TF ingest  
**Owner:** Market Data Engineer  
**Consumers:** Mobile Chart & UX (badge), Confluence Alerts (suppress)

---

## Endpoints

| Path | Payload |
|------|---------|
| `GET /health` | MD-1.1 `Health` + `consumerNote` |
| `GET /v1/health` | Same |
| `GET /v1/candles/stream` | `event: health` on connect **and** every ~15s keepalive |

Health object (MD-1.1):

```
status: "ok" | "degraded" | "stale" | "disconnected"
lastSourceTsMs, venue, symbol
optional: gapCount, activeTimeframes[], note
```

## Consumer rules (locked)

| Status | Mobile | Alerts |
|--------|--------|--------|
| `ok` | Normal | Normal |
| `degraded` | May badge | **Alerts policy** (partial TF / gaps) |
| `stale` (>60s no update) | **Badge** | **MUST suppress** new confluence fires |
| `disconnected` | **Badge** (+ O1 stale-cache OK) | **MUST suppress** |

Ignoring health → false confluence risk.

## Multi-TF ingest (extension)

`startMultiTfLiveIngest` sets:

- `feedConnected` from open WS set  
- `activeTimeframes` / `lastSourceTsMs` from live candles  
- **`expectedTimeframes` = subscribed in-use set** so a chart subset is not permanently `degraded` vs all 7 product TFs

## Changelog

| Date (America/Toronto) | Change |
|------------------------|--------|
| 2026-09-04 | Consumer note on health HTTP + SSE; multi-TF expected TF set; keepalive health |
