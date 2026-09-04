# Market Data service (Phase 1 scaffold)

Binance **public** REST + WS candle ingest scaffold + offline Docker health server.

**Hard rules:** no trade execution, no API secrets, no venue merge, no Bybyt adapter yet, no P2–P4.

Contract: [docs/market-data/MD-1.1-candle-contract.md](../../docs/market-data/MD-1.1-candle-contract.md)

## Layout

| Path | Purpose |
|------|--------|
| `src/binance/rest.ts` | Public klines REST + pagination |
| `src/binance/ws.ts` | Public kline stream + reconnect/backoff skeleton |
| `src/binance/map.ts` | Venue rows – MD-1.1 candles |
| `src/binance/errors.ts` | Typed 429 / 418 / 5xx / timeout |
| `src/server.ts` | GET /health |
| `fixtures/` | Offline closed candles + health samples for Alerts |
| `Dockerfile` / `docker-compose.yml` | **Local compose only** |

## Run tests (offline)

```bash
cd services/market-data
pnp install
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
curl http://127.0.0.1:8080/health
```

There is **no** VPS deploy path, host SSH, or remote compose target in this scaffold.

## Alerts fixtures

See [fixtures/README.md](./fixtures/README.md) for golden-vector candle + health paths.
