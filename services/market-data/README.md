# Market Data service (Phase 1 + Phase 2 chart support)

Binance **public** REST + WS candle ingest + SQLite store + 1m gap-fill + health machine + **consumer APIs** (history / live SSE / bootstrap) + **multi-TF venue-native backfill/live** (MD-2.1–2.3) + **pagination/resume** (MD-2.7) + **rate-limit/reconnect** (MD-2.8) + **health to consumers** (MD-2.9).

**Hard rules:** no trade execution, no API secrets, no venue merge, no Bybit adapter yet, no MD-2.5/2.6 VP, no P3/P4.
**Deploy:** local/box only — **no VPS**.

Contract: [docs/market-data/MD-1.1-candle-contract.md](../../docs/market-data/MD-1.1-candle-contract.md)  
TF policy: [docs/market-data/MD-2.1-tf-policy.md](../../docs/market-data/MD-2.1-tf-policy.md)  
Bootstrap depth: [docs/market-data/MD-2.4-bootstrap-depth.md](../../docs/market-data/MD-2.4-bootstrap-depth.md)  
Pagination: [docs/market-data/MD-2.7-pagination-resume.md](../../docs/market-data/MD-2.7-pagination-resume.md)  
Rate-limit/reconnect: [docs/market-data/MD-2.8-rate-limit-reconnect.md](../../docs/market-data/MD-2.8-rate-limit-reconnect.md)  
Health consumers: [docs/market-data/MD-2.9-health-consumers.md](../../docs/market-data/MD-2.9-health-consumers.md)

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
| `src/policy/` | MD-2.1 venue-native TF policy |
| `src/binance/weight-budget.ts` | MD-2.2 shared REST weight gate (backfill + gap-fill) |
| `src/backfill/` | MD-2.2 multi-TF REST → CandleStore |
| `src/live/multi-tf-ingest.ts` | MD-2.3 configurable multi-TF live WS |
| `src/binance/reconnect-policy.ts` | MD-2.8 REST retry + WS backoff (no tight 429 loop) |
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
ENABLE_LIVE_INGEST=1 docker compose up --build
# subset: LIVE_TIMEFRAMES=1m,5m,15m ENABLE_LIVE_INGEST=1 docker compose up --build
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

### MD-1.7 / MD-2.7 — History GET + pagination/resume

```http
GET /v1/candles?symbol=BTCUSDT&timeframe=1m&fromMs=&toMs=&venue=binance&limit=500&cursor=
```

- Returns **closed only** (`isFinal=true`), ordered by `openTimeMs` ascending
- Reads from `CandleStore` (never raw exchange payloads)
- Respects MD-1.1 depth caps (`1m` 60d, `5m` 180d, `15m` 1y, higher TFs unbounded): older `fromMs` is clamped; response includes `truncated` + `effectiveFromMs`
- **Pagination:** `limit` (default 500, max 1000); response `hasMore`, `nextCursor`, `nextFromMs`
- **Resume after app kill:** pass `cursor=<last openTimeMs>` or `fromMs=<nextFromMs>` — no full re-bootstrap. Upsert by primary key.

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

### Health (MD-1.6 / MD-2.9)

```http
GET /health
GET /v1/health
```

Uses in-process `HealthMachine` (MD-1.1 payload). Also `event: health` on SSE connect + ~15s keepalive.

**Alerts MUST suppress** new confluence fires when `status` is `stale` or `disconnected`. Mobile badges non-ok. `degraded` = Alerts policy.



## Phase 2 — Multi-TF (MD-2.1 / 2.2 / 2.3)

### MD-2.1 Venue-native TF policy

All 7 TFs (`1m`…`1w`) map 1:1 to Binance kline intervals. Consumer series are **never** rolled up from `1m`. See the policy doc linked above; code: `src/policy/tf-policy.ts`.

### MD-2.2 Multi-TF REST backfill

```ts
import {
  CandleStore,
  backfillAllTimeframes,
  RestWeightBudget,
} from '@confluence/market-data';

const store = new CandleStore({ dbPath });
const budget = new RestWeightBudget(); // shared with gap-fill
await backfillAllTimeframes({
  store,
  weightBudget: budget,
  maxBarsPerTf: 500,
  // fetchKlines: mock in tests
});
```

Weight gate: each klines page costs weight `2`; soft reserve is held for MD-1.5 gap-fill (`allowReserve`). Default concurrency is `1`.

### MD-2.3 Multi-TF live klines

Subscribe **only** TFs in use (configurable). Forming + final upsert via `LiveCandleHub` and update `HealthMachine` lastSource / activeTimeframes.

```bash
ENABLE_LIVE_INGEST=1 LIVE_TIMEFRAMES=1m,5m,15m,1h docker compose up --build
```

```ts
import { startMultiTfLiveIngest } from '@confluence/market-data';
const handle = startMultiTfLiveIngest({
  hub, health, timeframes: ['1m', '5m', '1h'],
});
// handle.stop()
```

## Phase 2 remaining (MD-2.4 / 2.7 / 2.8 / 2.9)

### MD-2.4 Bootstrap depth (proposal)

Chart P2 mins (Alerts may raise later): **1m ≥500**, **5m ≥300**, **15m ≥300**, **1h ≥300**, **4h ≥200**, **1d ≥200**, **1w ≥150**.  
See [MD-2.4-bootstrap-depth.md](../../docs/market-data/MD-2.4-bootstrap-depth.md).

### MD-2.7 Pagination + resume

Documented above on `GET /v1/candles`. Library: `nextHistoryPageQuery`, `HISTORY_DEFAULT_LIMIT`.

### MD-2.8 Rate-limit + reconnect

- **REST weight:** `RestWeightBudget` (shared backfill + gap-fill)
- **REST retry:** `withRestRetry` / `fetchKlinesPage` — exponential backoff on 429/418/5xx/timeout; never tight-loop
- **WS:** `computeWsReconnectDelayMs` in `BinanceKlineWsClient` (500ms → 30s + jitter)

See [MD-2.8-rate-limit-reconnect.md](../../docs/market-data/MD-2.8-rate-limit-reconnect.md).

### MD-2.9 Health to consumers

`/health`, `/v1/health`, and SSE `event: health` expose MD-1.1 health. Multi-TF ingest sets `expectedTimeframes` to the in-use set.  
See [MD-2.9-health-consumers.md](../../docs/market-data/MD-2.9-health-consumers.md).

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


## Multi-TF (MD-2.1 / 2.2 / 2.3)

Policy doc: [docs/market-data/MD-2.1-tf-policy.md](../../docs/market-data/MD-2.1-tf-policy.md)

- **MD-2.1** — all 7 TFs (`1m`…`1w`) are **venue-native** Binance klines. Code: `policy/tf-policy.ts`. No silent 1m→higher rollup.
- **MD-2.2** — `backfillTimeframes` / `backfillAllTimeframes` pull native REST klines into `CandleStore`, sequential by default, gated by `RestWeightBudget` (shared with 1m gap-fill).
- **MD-2.3** — `startMultiTfLiveIngest` (also used by `startLiveIngest`) subscribes forming+final for a configurable TF set (default **all 7** via `LIVE_TIMEFRAMES` / `TIMEFRAMES`), upserts via `LiveCandleHub`, ticks health `lastSourceTsMs` / `activeTimeframes`.

```ts
import {
  CandleStore,
  backfillAllTimeframes,
  startMultiTfLiveIngest,
  LiveCandleHub,
  HealthMachine,
  RestWeightBudget,
} from '@confluence/market-data';

const store = new CandleStore({ dbPath });
const health = new HealthMachine({ venue: 'binance', symbol: 'BTCUSDT' });
const hub = new LiveCandleHub({ store, health });

await backfillAllTimeframes({
  store,
  maxBarsPerTf: 500,
  // fetchKlines: mock in tests
});

const live = startMultiTfLiveIngest({
  hub,
  health,
  timeframes: ['1m', '5m', '15m', '1h', '4h', '1d', '1w'],
});
// live.stop()
```
