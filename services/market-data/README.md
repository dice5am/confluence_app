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
| `fixtures/` | Offline closed candles + health samples for Alerts |
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
