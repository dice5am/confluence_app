# MOB-1.7 — Non-goals / safety copy pass

**DoD:** UI strings are insight-only; no Buy/Sell/order/execute CTAs.

## Files audited (2026-09-04)

| Path | Result |
|------|--------|
| `app/src/main/res/values/strings.xml` | Pass — `insight_only_disclaimer` only |
| `feature-home/.../HomeScreen.kt` | Pass — “Insight only — never executes trades”; Open chart / Alerts only |
| `feature-home/.../HomeUiState.kt` | Pass — KDoc insight-only |
| `feature-chart/.../ChartScreen.kt` | Pass — Phase 2 stub; no trade CTAs |
| `feature-alerts/.../AlertsScreen.kt` | Pass — advisory copy; “No Buy / Sell actions. Advisory only.” |
| `app/.../ConfluenceApp.kt` | Pass — insight-only KDoc |
| `README.md` | Pass — insight-only banner |

## Findings
- No order placement, brokerage, or execution CTAs found.
- Mentions of “Buy / Sell” appear only in **negation** (prohibited actions), which is intentional safety copy.
- No changes required to string resources for DoD.

## Non-goals restated
- No trade execution
- No exchange API keys on device
- No venue merge UI

**Owner:** Mobile Chart & UX  
**Date:** 2026-09-04
