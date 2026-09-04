# MOB-2.1 — Compose Canvas OHLC spike

**Date:** 2026-09-04  
**Status:** Implemented on fixtures (not device-verified — no JDK/SDK on box).

## Approach
- Pure Compose `Canvas` in `:feature-chart` (`CandleChart.kt`)
- Data: `FakeMarketDataApi.getHistory` closed candles (default TF 1h)
- Gestures: `detectTransformGestures` (pan + pinch → candle width / start index); `detectDragGesturesAfterLongPress` for crosshair
- Visible-window draw only; Y scale from visible high/low + padding
- Colors: `ConfluenceColors.Bull/Bear/Grid/Crosshair`

## Known limits (for MOB-2.1.1 go/no-go)
- Not run on emulator/device yet (`assembleDebug` deferred — no Android SDK on agent box)
- No volume pane (MOB-2.7), TF chips (MOB-2.2), stale banners (MOB-2.8)
- No live append (MOB-2.5) / real MD client (MOB-2.9)
- Pinch+pan combined gesture can feel coarse; may need velocity fling later
- Crosshair clears on transform; no magnet-to-wick polish yet

## Go / no-go lean (pending device smoke)
**Lean GO on Canvas** if device smoke shows stable pan/zoom + readable candles.  
**Fallback MOB-2.1F (MPAndroidChart)** only if gestures jank or Canvas path blocks P2 schedule.

## Non-goals respected
No indicators/alerts/execution.
