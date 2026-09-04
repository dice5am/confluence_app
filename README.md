# confluence_app

Personal Android BTC/USDT charts + confluence insight alerts. **No trade execution.**

## Architecture
- **Arch-B:** thin backend (VPS Docker) owns market-data ingest
- Device: **no exchange API keys**
- **No venue merge** (Binance primary, Bybit failover)

## Phase 1
Market data contract: [`docs/market-data/MD-1.1-candle-contract.md`](docs/market-data/MD-1.1-candle-contract.md)

ACKs: Mobile Chart & UX, Confluence Alerts (2026-09-04). America/Toronto locked for VWAP MTD/YTD.