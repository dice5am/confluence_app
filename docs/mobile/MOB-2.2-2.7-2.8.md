# MOB-2.2 / 2.7 / 2.8

**Date:** 2026-09-04

## MOB-2.2 TF chips
Day-one chips: 1m, 5m, 15m, 1h, 4h, 1d, 1w. Selecting a chip reloads fixture history for that TF (`ChartViewModel.setTimeframe`). Last-used persistence = MOB-2.3 (not yet).

## MOB-2.7 Volume pane
Collapsible via top-bar “Vol on/off”. When on, Canvas draws a bottom histogram strip (~22% height) aligned to visible candles.

## MOB-2.8 Banners
Empty / loading / error / health-based stale|disconnected|degraded banners above the chart. O1 cache behavior still MOB-4.6.

## Cleanup
Removed duplicate `feature.chart.canvas` package left from dual spike paths; single `CandleChart` in `feature.chart`.
