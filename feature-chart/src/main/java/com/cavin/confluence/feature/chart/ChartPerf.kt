package com.cavin.confluence.feature.chart

import android.os.SystemClock
import android.util.Log
import com.cavin.confluence.data.model.Candle
import com.cavin.confluence.data.model.Timeframe

/**
 * MOB-2.6 — lightweight perf harness helpers (fixtures / debug).
 *
 * DoD targets (plan):
 * - TF switch (cached): <100ms to interactive
 * - Cold 500 candles + 2 overlays: <500ms (overlays N/A in P2)
 * - Pan/zoom: ~60fps (device smoke)
 * - Live append ≠ full rebuild (MOB-2.5)
 */
object ChartPerf {
    private const val TAG = "ConfluenceChartPerf"

    fun <T> measureMs(label: String, block: () -> T): T {
        val t0 = SystemClock.elapsedRealtime()
        return try {
            block()
        } finally {
            val dt = SystemClock.elapsedRealtime() - t0
            Log.d(TAG, "$label ${dt}ms")
        }
    }

    fun logSeriesStats(tf: Timeframe, raw: Int, drawn: Int) {
        Log.d(TAG, "tf=${tf.wire} raw=$raw drawn=$drawn lod=${raw > drawn} depthHint=${CandleLod.depthCapHint(tf)}")
    }

    /** Synthetic stress: ensure LOD keeps draw list ≤ maxPoints. */
    fun assertLodBudget(candles: List<Candle>, tf: Timeframe, maxPoints: Int = CandleLod.DEFAULT_MAX_POINTS): Boolean {
        val out = CandleLod.maybeDecimate(candles, tf, maxPoints)
        val ok = out.size <= maxPoints || candles.size <= maxPoints
        Log.d(TAG, "lodBudget ok=$ok in=${candles.size} out=${out.size} max=$maxPoints")
        return ok
    }
}
