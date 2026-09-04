# Market Data Service (Phase 1 scaffold)

Implements:

| ID | Scope |
|----|--------|
| **MD-1.1** | Candle field contract (types in `candle.py`) |
| **MD-1.2** | Binance public REST klines → MD-1.1 candles |
| **MD-1.3** | Binance public WS klines + reconnect/backoff skeleton |
| **MD-1.10** | Offline Docker + `GET /health` HTTP scaffold |

**Hard rules:** no trade execution, no signed exchange endpoints, no API secrets, no venue merge (venue=`binance` only for now).

## Layout

```
services/market-data/
  src/market_data/
    candle.py          # MD-1.1 types
    errors.py          # 429 / 418 / 5xx / timeout
    binance_rest.py    # REST klines + pagination
    binance_ws.py      # WS subscribe + backoff
    server.py          # GET /health (+ stub routes)
  tests/               # unit tests (fixture JSON; no live net)
  Dockerfile
  docker-compose.yml
```

## Run tests (no network)

From this directory:

```bash
cd services/market-data
PYTHONPATH=src python3 -m unittest discover -s tests -v
```

Optional live WS integration (skipped by default):

```bash
pip install -r requirements.txt
RUN_BINANCE_WS_INTEGRATION=1 PYTHONPATH=src python3 -m unittest tests.test_binance_ws.LiveWsIntegrationTests -v
```

## Run HTTP server locally (without Docker)

```bash
cd services/market-data
PYTHONPATH=src python3 -m market_data
# curl http://127.0.0.1:8080/health
```

## Docker Compose (local only)

Requires Docker Desktop / Engine on your machine. **No VPS deploy.**

```bash
cd services/market-data
docker compose up --build
```

Then:

```bash
curl http://localhost:8080/health
```

Stub routes (501): `GET /v1/history`, `GET /v1/live` — reserved for later phases.

## Candle mapping notes

- REST closed rows → `isFinal=true`
- WS `k.x=false` → forming (`isFinal=false`); `k.x=true` → closed (`isFinal=true`)
- Timeframes: `1m,5m,15m,1h,4h,1d,1w` for `BTCUSDT` / venue `binance`

## Out of scope (P2+)

Bybit failover runtime, history store depth enforcement, live stream fan-out to Mobile/Alerts — stubs/comments only.
