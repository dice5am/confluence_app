# confluence_app

Personal Android BTC/USDT charts + confluence insight alerts. **No trade execution.**

## Architecture
- **Arch-B:** thin backend (VPS Docker) owns market-data ingest
- Device: **no exchange API keys**
- **No venue merge** (Binance primary, Bybit failover)

## Phase 1
Market data contract: [`docs/market-data/MD-1.1-candle-contract.md`](docs/market-data/MD-1.1-candle-contract.md)

ACKs: Mobile Chart & UX, Confluence Alerts (2026-09-04). America/Toronto locked for VWAP MTD/YTD.

### Market-data scaffold (MD-1.2 / MD-1.3 / MD-1.10)

Service code lives under [`services/market-data/`](services/market-data/).

```bash
# Unit tests (no live network)
cd services/market-data
PYTHONPATH=src python3 -m unittest discover -s tests -v

# Local Docker (offline scaffold — no remote deploy)
cd services/market-data
docker compose up --build
curl http://localhost:8080/health
```

See [`services/market-data/README.md`](services/market-data/README.md) for details.
