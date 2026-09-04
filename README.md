# Confluence App (Android)

Personal Android app for **BTC/USDT charts** and **confluence insight alerts**.

> **Insight only — never executes trades.**  
> No exchange API keys on device. No Buy/Sell order CTAs. Arch-B: Market Data API stubs only.

**Package:** `com.cavin.confluence`  
**Stack:** Kotlin · Jetpack Compose · Material 3 · Navigation Compose  
**minSdk:** 26 · **compileSdk / targetSdk:** 35

## Architecture

- **Arch-B:** thin backend (VPS Docker) owns market-data ingest
- Device: **no exchange API keys**
- **No venue merge** (Binance primary, Bybit failover)
- Market data contract: [`docs/market-data/MD-1.1-candle-contract.md`](docs/market-data/MD-1.1-candle-contract.md)

## HOLD — do not push

GitHub access / BUILD gate: **do not `git push`** until CoS unlocks the repo.  
Scaffold lives locally under `/workspace/confluence_app`.

Intended remote (when unlocked): `https://github.com/dice5am/confluence_app`

## Module map

| Module | Role |
|--------|------|
| `:app` | `ConfluenceApp`, `MainActivity`, NavHost + deep-link stubs |
| `:core-ui` | Dark-first theme tokens (Color, Type, Spacing) + `ConfluenceTheme` |
| `:data` | MD-1.1 shaped models + `MarketDataApi` / fixtures (no exchange SDKs) |
| `:feature-home` | Home hub (price / %Δ / freshness / Open chart / Alerts badge) |
| `:feature-chart` | **Stub only** — “Chart — Phase 2”; no Canvas candle engine |
| `:feature-alerts` | Stub inbox / placeholder |

### Dependency direction

```
app → feature-* → core-ui
                ↘ data
app → core-ui, data
```

No circular deps. Features must not pull exchange SDKs.

## Delivered (Phase 1 slice)

- **MOB-1.1 / 1.1.1** — multi-module scaffold
- **MOB-1.2** — dark theme default, chart-safe contrast tokens, Material 3
- **MOB-1.3** — Home ↔ Chart ↔ Alerts (+ Settings placeholder); deep links `confluence://app/home|chart|alerts`
- **MOB-1.4** — Home hub with loading / empty / error / ready
- **MOB-1.5 (light)** — MD-1.1 interfaces + fake fixtures

**Out of scope here:** Canvas candles, overlays, trade execution, git push.

## MD-1.1 data notes

- Primary key: `(venue, symbol, timeframe, openTimeMs)`
- Venues: `binance` \| `bybit` · Symbol v1: `BTCUSDT`
- TFs: `1m` `5m` `15m` `1h` `4h` `1d` `1w`
- History GET: **closed-only** (`isFinal=true`)
- Live: may include forming (`isFinal=false`)
- Health: `ok` \| `degraded` \| `stale` \| `disconnected` + `lastSourceTsMs`
- Calendar anchors (consumer VWAP MTD/YTD): **America/Toronto**

## Build

```bash
./gradlew :app:assembleDebug
```

If the Android SDK / JDK / Gradle wrapper binary are missing on the machine, see [`STATUS.md`](STATUS.md).

## Safety copy

UI strings are insight / advisory only. There are **no** Buy, Sell, or order-placement actions in this scaffold.
