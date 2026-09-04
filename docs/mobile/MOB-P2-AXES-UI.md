# Phase 2 revision — dynamic axes + UI uplift

**Date:** 2026-09-04

## Axes (MUST)
- Y = price, X = time (America/Toronto labels)
- Labels recompute from the **visible** candle window → update on pan/zoom and TF (`seriesKey`)
- Nice price ticks; time density adapts to candle width

## Data
- Keep `SnapshotMarketDataApi` as default (`MarketDataFactory.create`) — no FakeFixtures regress

## UI
- Axis chrome labels on chart screen (blue Y / orange X)
- Continues glass/neon design system from PR #17
