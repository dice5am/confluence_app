package com.cavin.confluence.data.snapshot

import android.content.Context
import com.cavin.confluence.data.api.MarketDataApi
import com.cavin.confluence.data.api.QuoteApi
import com.cavin.confluence.data.api.QuoteSnapshot
import com.cavin.confluence.data.model.Candle
import com.cavin.confluence.data.model.HealthStatus
import com.cavin.confluence.data.model.MarketHealth
import com.cavin.confluence.data.model.Timeframe
import com.cavin.confluence.data.model.Venue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf

/**
 * Offline MarketDataApi backed by packaged Binance historical klines.
 * No live WebSocket / SSE. TF switches return the matching frozen series.
 */
class SnapshotMarketDataApi(
    context: Context,
) : MarketDataApi {

    init {
        MdSnapshotStore.ensureLoaded(context)
    }

    override suspend fun getHistory(
        venue: Venue,
        symbol: String,
        timeframe: Timeframe,
        fromMs: Long?,
        toMs: Long?,
    ): List<Candle> {
        require(venue == Venue.BINANCE) { "snapshot is Binance-only" }
        return MdSnapshotStore.candles(timeframe)
            .asSequence()
            .filter { it.isFinal }
            .filter { fromMs == null || it.openTimeMs >= fromMs }
            .filter { toMs == null || it.openTimeMs <= toMs }
            .toList()
    }

    override fun observeLive(
        venue: Venue,
        symbol: String,
        timeframe: Timeframe,
    ): Flow<Candle> = emptyFlow() // frozen snapshot — no live WS

    override fun observeHealth(
        venue: Venue,
        symbol: String,
    ): Flow<MarketHealth> = flowOf(snapshotHealth(venue, symbol))

    override suspend fun getHealth(
        venue: Venue,
        symbol: String,
    ): MarketHealth = snapshotHealth(venue, symbol)

    private fun snapshotHealth(venue: Venue, symbol: String): MarketHealth =
        MarketHealth(
            status = HealthStatus.OK,
            lastSourceTsMs = MdSnapshotStore.cutoffMs,
            venue = venue,
            symbol = symbol,
            gapCount = 0,
            activeTimeframes = Timeframe.entries.toList(),
            note = MdSnapshotStore.bannerLabel,
        )
}

class SnapshotQuoteApi(
    context: Context,
) : QuoteApi {
    private val md = SnapshotMarketDataApi(context)

    override suspend fun getQuote(
        venue: Venue,
        symbol: String,
    ): QuoteSnapshot {
        val candles = md.getHistory(venue, symbol, Timeframe.H1)
        require(candles.isNotEmpty()) { "snapshot empty" }
        val last = candles.last()
        val prev = candles.getOrNull(candles.lastIndex - 1)
        val pct = prev?.let { ((last.close - it.close) / it.close) * 100.0 }
        return QuoteSnapshot(
            venue = last.venue,
            symbol = last.symbol,
            lastPrice = last.close,
            percentChange = pct,
            health = md.getHealth(venue, symbol),
            asOfSourceTsMs = last.sourceTsMs,
        )
    }
}
