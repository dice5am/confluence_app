package com.cavin.confluence.data.model

/**
 * MD-1.1 candle.
 *
 * Primary key: (venue, symbol, timeframe, openTimeMs)
 *
 * Calendar anchors for consumer VWAP modes (MTD/YTD): **America/Toronto** (LOCKED).
 * MD does not compute VWAP; this note is for consumers only.
 *
 * @property openTimeMs Candle start, UTC ms, inclusive.
 * @property closeTimeMs Candle end, UTC ms (venue-native; exclusive of next open).
 * @property open Quote currency (USDT).
 * @property high Quote currency (USDT).
 * @property low Quote currency (USDT).
 * @property close Quote currency (USDT).
 * @property volume Base asset (BTC).
 * @property isFinal false = forming; true = closed.
 * @property sourceTsMs When exchange event / REST row was observed.
 * @property ingestTsMs When accepted into store.
 */
data class Candle(
    val venue: Venue,
    val symbol: String,
    val timeframe: Timeframe,
    val openTimeMs: Long,
    val closeTimeMs: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double,
    val isFinal: Boolean,
    val sourceTsMs: Long,
    val ingestTsMs: Long,
    /** Optional MD-1.1 quality flags (present/absent). */
    val gap: Boolean? = null,
    val stale: Boolean? = null,
    val reconciled: Boolean? = null,
    val sourceMismatch: Boolean? = null,
) {
    init {
        require(symbol == SYMBOL_BTCUSDT) {
            "v1 supports BTCUSDT only; got $symbol"
        }
    }

    /** Primary key tuple per MD-1.1. */
    val primaryKey: CandleKey
        get() = CandleKey(venue, symbol, timeframe, openTimeMs)

    companion object {
        const val SYMBOL_BTCUSDT = "BTCUSDT"
    }
}

/**
 * MD-1.1 primary key: (venue, symbol, timeframe, openTimeMs).
 */
data class CandleKey(
    val venue: Venue,
    val symbol: String,
    val timeframe: Timeframe,
    val openTimeMs: Long,
)
