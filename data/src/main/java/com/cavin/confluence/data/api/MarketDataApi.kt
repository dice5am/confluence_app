package com.cavin.confluence.data.api

import com.cavin.confluence.data.model.Candle
import com.cavin.confluence.data.model.MarketHealth
import com.cavin.confluence.data.model.Timeframe
import com.cavin.confluence.data.model.Venue
import kotlinx.coroutines.flow.Flow

/**
 * Arch-B Market Data API surface (MD-1.1 shaped).
 *
 * Device holds **no** exchange API keys. No trade execution.
 *
 * ## History GET
 * Returns **closed-only** candles (`isFinal=true`), ordered by `openTimeMs` ascending.
 * Never raw exchange payloads. Respects MD history depth caps.
 *
 * ## Live stream
 * May include forming bars (`isFinal=false`). Closed bar: exactly one
 * `isFinal=true` event per primary key. Charts may paint forming; alerts
 * consume finals only for triggers (Alerts lane).
 *
 * ## Calendar
 * Consumer calendar anchors (MTD/YTD): America/Toronto (LOCKED).
 */
interface MarketDataApi {

    /**
     * History GET — closed candles only (`isFinal=true`).
     *
     * @param fromMs Inclusive lower bound on openTimeMs (UTC ms), or null.
     * @param toMs Inclusive upper bound on openTimeMs (UTC ms), or null.
     */
    suspend fun getHistory(
        venue: Venue,
        symbol: String = Candle.SYMBOL_BTCUSDT,
        timeframe: Timeframe,
        fromMs: Long? = null,
        toMs: Long? = null,
    ): List<Candle>

    /**
     * Live candle updates for a subscription. May emit forming bars.
     */
    fun observeLive(
        venue: Venue,
        symbol: String = Candle.SYMBOL_BTCUSDT,
        timeframe: Timeframe,
    ): Flow<Candle>

    /**
     * Health poll / subscribe surface.
     */
    fun observeHealth(
        venue: Venue,
        symbol: String = Candle.SYMBOL_BTCUSDT,
    ): Flow<MarketHealth>

    suspend fun getHealth(
        venue: Venue,
        symbol: String = Candle.SYMBOL_BTCUSDT,
    ): MarketHealth
}

/**
 * Lightweight quote snapshot for Home hub (derived from latest candle + health).
 * Not a parallel schema — convenience projection over MD-1.1 candles.
 */
data class QuoteSnapshot(
    val venue: Venue,
    val symbol: String,
    val lastPrice: Double,
    /** Percent change vs prior closed candle close; null if unavailable. */
    val percentChange: Double?,
    val health: MarketHealth,
    val asOfSourceTsMs: Long,
)

interface QuoteApi {
    suspend fun getQuote(
        venue: Venue = Venue.BINANCE,
        symbol: String = Candle.SYMBOL_BTCUSDT,
    ): QuoteSnapshot
}
