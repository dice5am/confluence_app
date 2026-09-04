# MOB-2.3 / 2.4 / 2.6

**Date:** 2026-09-04  
**Fixtures only** — MOB-2.5 live append + MOB-2.9 real MD client still waiting on MD multi-TF.

## MOB-2.3 last-used TF
- `ChartTfPreferences` SharedPreferences (`confluence_chart` / `last_used_tf`)
- First-open default **1h** when no pref and no deep-link TF
- Deep-link `tf=` overrides for that session; chip changes persist last-used

## MOB-2.4 LOD
- `CandleLod.maybeDecimate` buckets OHLC when `size > maxPoints` (default 2000)
- Respects depth hints: 1m ~60d, 5m 180d, 15m 1y, 1h+ full (documented; API still fixtures)
- Unit test: `CandleLodTest`

## MOB-2.6 perf harness
- `ChartPerf.measureMs` / `logSeriesStats` / `assertLodBudget` → logcat tag `ConfluenceChartPerf`
- TF switch path logs `tfSwitch:<tf> <ms>ms (DoD <100ms cached)`
- UI shows `drawn X/Y · TF switch Nms` when available
- Device/emulator smoke still required for 60fps pan/zoom DoD (`assembleDebug` deferred on agent box)

## Non-goals
No P3 indicators / no P4 alerts / no real live append yet.

## Also in this PR
- Completes MOB-2.7 volume pane on `CandleChart` (`showVolume` + `seriesKey`) — PR #11 UI already toggled it but Canvas lacked the params (compile gap).
