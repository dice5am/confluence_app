# MD-2.7 — Range pagination + resume

**Status:** Implemented on `GET /v1/candles`  
**Owner:** Market Data Engineer  
**Goal:** App kill must **not** require full history re-bootstrap.

---

## Query

```
GET /v1/candles?symbol=BTCUSDT&timeframe=1m&fromMs=&toMs=&venue=binance&limit=500&cursor=
```

| Param | Meaning |
|-------|---------|
| `fromMs` / `toMs` | Inclusive `openTimeMs` range (unchanged from MD-1.7) |
| `limit` | Page size (default **500**, max **1000**) |
| `cursor` | Optional resume: last page's final `openTimeMs` → next page is **exclusive** (`openTimeMs > cursor`) |

## Response extras

| Field | Meaning |
|-------|---------|
| `hasMore` | More closed candles remain in range after this page |
| `nextCursor` | Last candle `openTimeMs` in this page |
| `nextFromMs` | `nextCursor + 1` when `hasMore` — pass as next `fromMs` |
| `resume` | Human note + default/max limit |

Closed only (`isFinal=true`), ascending `openTimeMs`. Depth caps still apply (`truncated` / `effectiveFromMs`).

## Resume after app kill

1. Persist last applied `openTimeMs` (or last `nextCursor`).
2. On restart: `GET` with same `toMs` and either:
   - `cursor=<lastOpenTimeMs>`, or
   - `fromMs=<lastOpenTimeMs + 1>`
3. Upsert by `(venue,symbol,timeframe,openTimeMs)` — overlap is harmless (MD-1.9).

Helper: `nextHistoryPageQuery(previousResult)` in the library.

## Off-by-one note

`closeTimeMs` is venue-native end; range filters use **`openTimeMs`**. Resume uses **exclusive** next open (`lastOpen + 1`), never exclusive closeTime, to avoid skipping/duping bars.

---

## Changelog

| Date (America/Toronto) | Change |
|------------------------|--------|
| 2026-09-04 | limit/hasMore/nextCursor/nextFromMs + documented from/to resume |
