package com.cavin.confluence.feature.chart

import com.cavin.confluence.data.model.Timeframe
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

/**
 * Dynamic axis label helpers — densify/format from the visible window + TF.
 * Times shown in America/Toronto (app calendar lock).
 */
object ChartAxisLabels {
    private val tz: TimeZone = TimeZone.getTimeZone("America/Toronto")

    fun formatPrice(v: Float): String = when {
        v >= 100_000f -> String.format(Locale.US, "%.0f", v)
        v >= 10_000f -> String.format(Locale.US, "%.1f", v)
        v >= 1_000f -> String.format(Locale.US, "%.2f", v)
        else -> String.format(Locale.US, "%.4f", v)
    }

    fun formatTime(openTimeMs: Long, tf: Timeframe): String {
        val pattern = when (tf) {
            Timeframe.M1, Timeframe.M5, Timeframe.M15 -> "HH:mm"
            Timeframe.H1, Timeframe.H4 -> "MMM d HH:mm"
            Timeframe.D1, Timeframe.W1 -> "MMM d"
        }
        val fmt = SimpleDateFormat(pattern, Locale.US).apply { timeZone = tz }
        return fmt.format(Date(openTimeMs))
    }

    /** Nice Y tick prices covering [lo, hi]. */
    fun priceTicks(lo: Float, hi: Float, targetCount: Int = 5): List<Float> {
        if (!lo.isFinite() || !hi.isFinite() || hi <= lo) return emptyList()
        val span = (hi - lo).toDouble()
        val rough = span / (targetCount - 1).coerceAtLeast(1)
        val niceStep = niceNum(rough, round = true)
        val niceLo = floor(lo / niceStep) * niceStep
        val niceHi = ceil(hi / niceStep) * niceStep
        val out = ArrayList<Float>()
        var v = niceLo
        var guard = 0
        while (v <= niceHi + niceStep * 0.5 && guard < 24) {
            if (v >= lo - niceStep * 0.01 && v <= hi + niceStep * 0.01) {
                out.add(v.toFloat())
            }
            v += niceStep
            guard++
        }
        return out
    }

    /** Indices into the visible candle window for X labels (readable density). */
    fun timeSlotIndices(visibleCount: Int, minPxBetween: Float, candleWidthPx: Float): List<Int> {
        if (visibleCount <= 0) return emptyList()
        val step = maxOf(1, ceil(minPxBetween / candleWidthPx.coerceAtLeast(1f)).toInt())
        val out = ArrayList<Int>()
        var i = 0
        while (i < visibleCount) {
            out.add(i)
            i += step
        }
        val last = visibleCount - 1
        if (out.lastOrNull() != last) out.add(last)
        return out
    }

    private fun niceNum(range: Double, round: Boolean): Double {
        if (range <= 0.0) return 1.0
        val exp = floor(log10(range))
        val frac = range / 10.0.pow(exp)
        val nice = when {
            round && frac < 1.5 -> 1.0
            round && frac < 3.0 -> 2.0
            round && frac < 7.0 -> 5.0
            round -> 10.0
            frac <= 1.0 -> 1.0
            frac <= 2.0 -> 2.0
            frac <= 5.0 -> 5.0
            else -> 10.0
        }
        return nice * 10.0.pow(exp)
    }
}
