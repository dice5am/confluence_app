# MD-1.11 — No exchange keys on device

**Status:** Phase 1 checklist  
**Owner:** Market Data Engineer  
**Contract:** [MD-1.1](./MD-1.1-candle-contract.md)  
**Architecture:** Arch-B thin backend (VPS Docker)

Device clients consume only the market-data service (history / live / bootstrap / health). They never talk to exchanges and never hold venue credentials.

## Checklist

- [x] **Arch-B thin backend owns public market data** — REST/WS ingest, store, gap-fill, and consumer APIs live in `services/market-data` (server-side only).
- [x] **Device never holds exchange API keys** — no Binance/Bybit secrets in Android/app config, local.properties, or client code paths.
- [x] **Device never calls the exchange** — Mobile/Alerts call MD history, SSE live, bootstrap, and health only; no direct venue REST/WS from the device.
- [x] **No signed endpoints** — MD uses public market-data endpoints only (no trade/signed account APIs; no execution).

## References

- MD-1.1 § Architecture: device holds **no** exchange API keys; no trade execution.
- MD-1.1 §7 Non-goals: trade execution / signed exchange APIs; exchange keys on device; raw venue payloads to app.
