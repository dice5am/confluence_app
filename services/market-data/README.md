# Market Data service (Phase 1 scaffold)

Binance **public** REST + WS candle ingest scaffold + offline health server.

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
| `src/server.ts` | GET /health |
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

Not in scope here: HTTP history/live/bootstrap APIs (MD-1.7–1.9).

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

Does not add HTTP history/live routes (those are MD-1.7–1.9).

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
