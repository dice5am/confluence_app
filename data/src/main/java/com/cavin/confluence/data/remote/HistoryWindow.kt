package com.cavin.confluence.data.remote

import com.cavin.confluence.data.model.Timeframe

/**
 * Client-side history window aligned with MD depth caps (MD-1.1 / MOB-2.4).
 * Server still truncates; we send explicit fromMs/toMs (required by GET /v1/candles).
 */
object HistoryWindow {
    private const val DAY_MS = 24L * 60L * 60L * 1000L

    fun lookbackMs(tf: Timeframe): Long = when (tf) {
        Timeframe.M1 -> 60 * DAY_MS
        Timeframe.M5 -> 180 * DAY_MS
        Timeframe.M15 -> 365 * DAY_MS
        // 1h+: practical first paint (full available on server; cap request size)
        Timeframe.H1 -> 365 * DAY_MS
        Timeframe.H4 -> 365 * DAY_MS * 2
        Timeframe.D1, Timeframe.W1 -> 365 * DAY_MS * 5
    }

    fun fromToMs(tf: Timeframe, nowMs: Long = System.currentTimeMillis()): Pair<Long, Long> {
        val from = (nowMs - lookbackMs(tf)).coerceAtLeast(0L)
        return from to nowMs
    }
}
