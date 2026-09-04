# MOB-2.1.1 — Canvas vs MPAndroidChart go/no-go

**Date:** 2026-09-04  
**Recommendation:** **GO — continue Compose Canvas** (contingency MPAndroidChart not triggered).

## Evidence
- Spike code landed: Canvas OHLC + pan/pinch + crosshair on MD-1.1 fixtures
- Aligns with locked stack (Kotlin+Compose) and custom overlay path for Phase 3 later
- Device assemble/smoke **not yet run** on agent box (no JDK/SDK) — residual risk noted

## Decision
| Option | Verdict |
|--------|---------|
| L1 Compose Canvas | **Selected** — proceed to MOB-2.2 / 2.7 / 2.8 |
| L2 MPAndroidChart interop | Hold as contingency only if device smoke fails gestures/perf |

## Impact if Canvas selected
Unlocks TF chips, volume pane, banners without View interop tax; keeps Phase 3 overlays on same draw path.

## Impact if fallback later
+~10h View interop; may constrain custom overlay drawing — only if Canvas smoke fails.
