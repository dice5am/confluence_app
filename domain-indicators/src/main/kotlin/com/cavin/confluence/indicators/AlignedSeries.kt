package com.cavin.confluence.indicators

/**
 * Per-bar derived series aligned to the fenced closed-candle `openTimeMs`.
 * `null` means undefined (warmup / insufficient history). Gaps are not invented.
 */
data class AlignedSeries(
    val openTimeMs: List<Long>,
    val values: List<Double?>,
) {
    init {
        require(openTimeMs.size == values.size) {
            "series length mismatch: times=${openTimeMs.size} values=${values.size}"
        }
    }

    val size: Int get() = values.size

    fun finiteCount(): Int = values.count { it.isFiniteNumber() }

    fun lastFinite(): Double? = values.asReversed().firstOrNull { it.isFiniteNumber() }

    fun lastFiniteOpenTimeMs(): Long? {
        for (i in values.indices.reversed()) {
            if (values[i].isFiniteNumber()) return openTimeMs[i]
        }
        return null
    }

    companion object {
        fun aligned(bars: List<IndicatorBar>, values: List<Double?>): AlignedSeries {
            require(bars.size == values.size)
            return AlignedSeries(bars.map { it.openTimeMs }, values)
        }

        fun copies(bars: List<IndicatorBar>, selector: (IndicatorBar) -> Double): AlignedSeries =
            AlignedSeries(bars.map { it.openTimeMs }, bars.map { selector(it) })
    }
}

internal fun Double?.isFiniteNumber(): Boolean = this != null && isFinite()
