# Market Data service (Phase 1)

Binance **public** REST + WS candle ingest + SQLite store + 1m gap-fill + health machine + **consumer APIs** (history / live SSE / bootstrap).

**Hard rules:** no trade execution, no API secrets, no venue merge, no Bybit adapter yet, no P2-P4.
**Deploy:** local/box only — **no VPS**.

Contract: [docs/market-data/MD-1.1-candle-contract.md](../../docs/market-data/MD-1.1-candle-contract.md)

## Layout

| Path | Purpose |
|------|--------|
| `src/binance/rest.ts` | Public klines REST + pagination |
| `src/binance/ws.ts` | Public kline stream + reconnect/backoff skeleton |
| `src/binance/map.ts` | Venue rows to MD-1.1 candles |
| `src/binance/errors.ts` | Typed 429 / 418 / 5xx / timeout |
| `src/server.ts` | HTTP: health, history, live SSE, bootstrap plan |
| `src/api/` | MD-1.7 history + MD-1.9 bootstrap helpers + depth caps |
| `src/live/` | MD-1.8 live hub (exactly-one final per bar key) |
| `src/store/candle-store.ts` | MD-1.4 SQLite candle store (upsert + range query) |
| `src/gap/` | MD-1.5 1m gap detect + REST fill into store |
| `src/health/` | MD-1.6 health state machine (`ok\|degraded\|stale\|disconnected`) |
| `fixtures/` | Offline closed candles + health samples for Alerts |
| `data/` | Local SQLite file (`candles.sqlite`) — gitignored |
| `Dockerfile` / `docker-compose.yml` | **Local/box compose only** |

## Run tests (offline)

```bash
cd services/market-data
npm install
npm test
```

Optional live WS integration (skipped by default):

```bash
npm run test:integration
```

## Local Docker only (NO VPS)

```bash
cd services/market-data
docker compose up --build
# or detached: docker compose up --build -d
curl http://127.0.0.1:8080/health
curl "http://127.0.0.1:8080/v1/candles?symbol=BTCUSDT&timeframe=1m&fromMs=0&toMs=9999999999999"
```

Optional live Binance public WS ingest (local only):

```bash
ENABLE_LIVE_INGEST=1 LIVE_TIMEFRAMES=1m,5m docker compose up --build
```

There is **no** VPS deploy path, host SSH, or remote compose target in this scaffold.


## Candle store (MD-1.4)

Persists MD-1.1 candles in SQLite under `data/candles.sqlite` (override with `CandleStore({ dbPath })`).

- **Primary key:** `(venue, symbol, timeframe, openTimeMs)`
- **Idempotent upsert:** re-writing the same key is OK; a forming bar (`isFinal=false`) may be replaced by its final
- **Range query:** `queryRange({ venue, symbol, timeframe, fromMs, toMs, closedOnly? })` ordered by `openTimeMs` ascending

```ts
import { CandleStore } from '@confluence/market-data';

const store = new CandleStore(); // → data/candles.sqlite
store.upsert(candle);
const closed = store.queryRange({
  venue: 'binance',
  symbol: 'BTCUSDT',
  timeframe: '1m',
  fromMs,
  toMs,
  closedOnly: true,
});
store.close();
```

## Gap detect + REST fill (MD-1.5)

Detect missing `openTimeMs` steps on **1m** for a venue/symbol, fill via Binance public REST (`fetchKlinesPaginated`), upsert into `CandleStore`, and expose `gapCount` for health.

```ts
import { CandleStore, detectOneMinuteGaps, fillOneMinuteGaps } from '@confluence/market-data';

const store = new CandleStore({ dbPath });
const gaps = detectOneMinuteGaps(store, { venue: 'binance', symbol: 'BTCUSDT' });
// gaps.gapCount / gaps.missingOpenTimeMs / gaps.ranges

const filled = await fillOneMinuteGaps({
  store,
  venue: 'binance',
  symbol: 'BTCUSDT',
  // fetchKlines: mock in tests; defaults to live REST
});
```

Offline unit tests mock REST + use a temp SQLite store.

## Health machine (MD-1.6)

In-process MD-1.1 health status with priority: **disconnected** > **stale** (>60s no update) > **degraded** (partial TF or `gapCount > 0`) > **ok**.

```ts
import { HealthMachine, STALE_THRESHOLD_MS } from '@confluence/market-data';

const hm = new HealthMachine({ venue: 'binance', symbol: 'BTCUSDT' });
hm.setFeedConnected(true);
hm.recordUpdate(Date.now(), '1m');
hm.setActiveTimeframes(['1m', '5m', '15m', '1h', '4h', '1d', '1w']);
hm.setGapCount(gaps.gapCount);
const health = hm.getHealth(); // { status, lastSourceTsMs, venue, symbol, ... }
```


## Consumer APIs (MD-1.7 / 1.8 / 1.9)

### MD-1.7 — History GET

```http
GET /v1/candles?symbol=BTCUSDT&timeframe=1m&fromMs=&toMs=&venue=binance
```

- Returns **closed only** (`isFinal=true`), ordered by `openTimeMs` ascending
- Reads from `CandleStore` (never raw exchange payloads)
- Respects MD-1.1 depth caps (`1m` 60d, `5m` 180d, `15m` 1y, higher TFs unbounded): older `fromMs` is clamped; response includes `truncated` + `effectiveFromMs`

### MD-1.8 — Live forming + final close (SSE)

```http
GET /v1/candles/stream?symbol=BTCUSDT&timeframe=1m
GET /v1/candles/stream?finalsOnly=1
```

- `event: health` on connect; `event: candle` for forming (`isFinal=false`) and **exactly one** final per bar key
- Wire: Binance WS mapper → `LiveCandleHub.ingest` → store + SSE subscribers
- **Alerts path:** finals only (`finalsOnly=1` or filter `isFinal===true`). Charts may paint forming.

### MD-1.9 — Bootstrap-then-subscribe

1. Subscribe live (`GET /v1/candles/stream`)
2. History GET closed bars with overlap into now (≥2 bar lengths)
3. Upsert by primary key — seam dups collapse; no hole

```http
GET /v1/bootstrap-plan?timeframe=1m&lookbackMs=3600000
```

Library helpers: `planBootstrapThenSubscribe`, `bootstrapThenSubscribe` (see tests).

### Health

```http
GET /health
GET /v1/health
```

Uses in-process `HealthMachine`.

## Alerts fixture paths

Offline golden-vector inputs for Alerts (relative to `services/market-data/`):

### fixtures/candles/

- `fixtures/candles/btcusdt-1m-closed.json`
- `fixtures/candles/btcusdt-5m-closed.json`
- `fixtures/candles/btcusdt-15m-closed.json`
- `fixtures/candles/btcusdt-1h-closed.json` (~5916 bars)
- `fixtures/candles/btcusdt-4h-closed.json`
- `fixtures/candles/btcusdt-1d-closed.json`
- `fixtures/candles/btcusdt-1w-closed.json`

### fixtures/health/

- `fixtures/health/ok.json`
- `fixtures/health/degraded.json`
- `fixtures/health/stale.json`
- `fixtures/health/disconnected.json`

See also [fixtures/README.md](./fixtures/README.md).
