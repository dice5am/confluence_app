# MOB-2.5 / MOB-2.9

**Date:** 2026-09-04

## MOB-2.9 real MD client
- `HttpMarketDataApi` → `GET /v1/candles`, `GET /v1/health`, SSE `GET /v1/candles/stream`
- `ResilientMarketDataApi` falls back to fixtures if MD unreachable
- Default base URL `http://10.0.2.2:8080` (emulator→host); override via `MdConfigPrefs`
- Cleartext allowed only for localhost / 10.0.2.2 (network security config)
- Arch-B: **no** exchange keys on device

## MOB-2.5 live append
- After history load, subscribe `observeLive`
- `CandleSeries.applyLive` replaces tip or appends — **no** full history reload
- Perf log: `liveAppend:<tf> <ms>ms` under `ConfluenceChartPerf`

## Non-goals
No P3 overlays / no P4 alerts.
