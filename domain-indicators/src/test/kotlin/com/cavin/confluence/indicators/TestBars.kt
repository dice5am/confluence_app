package com.cavin.confluence.indicators

internal object TestBars {
    fun bar(
        openTimeMs: Long,
        closeTimeMs: Long = openTimeMs + 3_599_999L,
        open: Double,
        high: Double = open,
        low: Double = open,
        close: Double = open,
        volume: Double = 1.0,
        isFinal: Boolean = true,
    ): IndicatorBar = IndicatorBar(
        openTimeMs = openTimeMs,
        closeTimeMs = closeTimeMs,
        open = open,
        high = high,
        low = low,
        close = close,
        volume = volume,
        isFinal = isFinal,
    )

    fun closes(
        values: DoubleArray,
        startOpenMs: Long = 1_000_000L,
        stepMs: Long = 3_600_000L,
    ): List<IndicatorBar> = values.mapIndexed { i, close ->
        val openTime = startOpenMs + i * stepMs
        bar(
            openTimeMs = openTime,
            closeTimeMs = openTime + stepMs - 1,
            open = close,
            high = close,
            low = close,
            close = close,
            volume = 1.0,
        )
    }
}
