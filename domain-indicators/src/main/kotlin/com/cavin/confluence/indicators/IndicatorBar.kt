package com.cavin.confluence.indicators

/**
 * MD-1.1 OHLCV+time projection used by the local calc engine.
 *
 * Field-identical to MD-1.1 `Candle` math inputs (`openTimeMs`, `closeTimeMs`,
 * OHLC, `volume`, `isFinal`). Callers map `Candle` 1:1 — this is not a
 * parallel/fake series and is not resampled.
 *
 * Identity `(venue, symbol, timeframe)` stays with the caller / MD layer.
 */
data class IndicatorBar(
    val openTimeMs: Long,
    val closeTimeMs: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double,
    val isFinal: Boolean = true,
) {
    fun isFiniteOhlcv(): Boolean =
        open.isFinite() &&
            high.isFinite() &&
            low.isFinite() &&
            close.isFinite() &&
            volume.isFinite()
}
