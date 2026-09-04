# Build / environment status

**Project root:** `/workspace/confluence_app`  
**Checked:** 2026-09-04 (America/Toronto)  
**Landed:** [PR #1](https://github.com/dice5am/confluence_app/pull/1) merged to `main` (`f060fab`)

## assembleDebug

**Not run** — tooling absent on this box:

| Tool | Status |
|------|--------|
| JDK / `java` | Not installed (`java: command not found`) |
| `ANDROID_HOME` | Unset; no Android SDK detected |
| Gradle Wrapper | `gradlew` + `gradle-wrapper.jar` committed (Gradle 8.11.1) |

Complete Kotlin / Gradle / Compose sources for **MOB-1.1 → 1.5** are on `main`.  
Once JDK 17+ and Android SDK (compileSdk 35) are available:

```bash
cd /workspace/confluence_app
./gradlew :app:assembleDebug
```

## Ambiguities / TODOs (not invented)

| Item | Note |
|------|------|
| Settings contents | Placeholder route only — **TODO(product)** |
| Offline O1/O2/O3 | Undecided per MOB-1.6 — not implemented |
| Real Market Data base URL | Arch-B API host not wired — fixtures only |
| Alert payload schema | Inbox is stub until Alerts lane (MOB-4.1) |
| Default TF persistence | Chart stub accepts `tf`; last-used persist is MOB-2.3 |

## Non-goals respected

- No exchange SDKs / no trade execution
- No Compose Canvas candle engine (MOB-2.1+)
- Alerts docs under `docs/contracts/alerts/` untouched by this scaffold
- MD `services/` docker scaffold is separate (not part of MOB P1)
