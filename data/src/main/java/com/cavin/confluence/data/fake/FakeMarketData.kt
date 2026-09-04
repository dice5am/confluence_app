package com.cavin.confluence.data.fake

import com.cavin.confluence.data.api.MarketDataApi
import com.cavin.confluence.data.api.QuoteApi
import com.cavin.confluence.data.api.QuoteSnapshot
import com.cavin.confluence.data.model.Candle
import com.cavin.confluence.data.model.HealthStatus
import com.cavin.confluence.data.model.MarketHealth
import com.cavin.confluence.data.model.Timeframe
import com.cavin.confluence.data.model.Venue
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.math.sin

/**
 * Fallback MD-1.1 fixtures (sine) for JVM tests when snapshot assets are not loaded.
 *
 * Runtime debug APK prefers [com.cavin.confluence.data.snapshot.MdSnapshotStore].
 * Zero exchange SDKs / API keys. Insight-only.
 */
object FakeFixtures {

    private const val HOUR_MS = 3_600_000L

    /** Deterministic sample closed 1h candles (Binance BTCUSDT). */
    fun sampleClosedCandles(
        venue: Venue = Venue.BINANCE,
        timeframe: Timeframe = Timeframe.H1,
        count: Int = 48,
        endOpenTimeMs: Long = 1_725_400_800_000L, // fixed fixture epoch
    ): List<Candle> {
        val tfMs = timeframeApproxMs(timeframe)
        val base = 95_000.0
        return (0 until count).map { i ->
            val openTime = endOpenTimeMs - (count - 1 - i) * tfMs
            val wave = sin(i / 5.0) * 400.0
            val open = base + wave
            val close = open + sin(i / 3.0) * 120.0
            val high = maxOf(open, close) + 80.0
            val low = minOf(open, close) - 80.0
            val now = endOpenTimeMs + tfMs
            Candle(
                venue = venue,
                symbol = Candle.SYMBOL_BTCUSDT,
                timeframe = timeframe,
                openTimeMs = openTime,
                closeTimeMs = openTime + tfMs,
                open = open,
                high = high,
                low = low,
                close = close,
                volume = 12.5 + (i % 7) * 0.8,
                isFinal = true,
                sourceTsMs = openTime + tfMs - 50,
                ingestTsMs = now,
            )
        }
    }

    fun sampleFormingCandle(
        venue: Venue = Venue.BINANCE,
        timeframe: Timeframe = Timeframe.H1,
        after: Candle,
    ): Candle {
        val tfMs = timeframeApproxMs(timeframe)
        val openTime = after.openTimeMs + tfMs
        return Candle(
            venue = venue,
            symbol = Candle.SYMBOL_BTCUSDT,
            timeframe = timeframe,
            openTimeMs = openTime,
            closeTimeMs = openTime + tfMs,
            open = after.close,
            high = after.close + 50.0,
            low = after.close - 30.0,
            close = after.close + 15.0,
            volume = 3.2,
            isFinal = false,
            sourceTsMs = openTime + 30_000,
            ingestTsMs = openTime + 30_100,
        )
    }

    fun sampleHealth(
        status: HealthStatus = HealthStatus.OK,
        venue: Venue = Venue.BINANCE,
        lastSourceTsMs: Long = 1_725_400_800_000L + HOUR_MS,
    ): MarketHealth = MarketHealth(
        status = status,
        lastSourceTsMs = lastSourceTsMs,
        venue = venue,
        symbol = Candle.SYMBOL_BTCUSDT,
        gapCount = 0,
        activeTimeframes = Timeframe.entries.toList(),
        note = when (status) {
            HealthStatus.OK -> "Fixture feed healthy"
            HealthStatus.DEGRADED -> "Fixture: partial TF"
            HealthStatus.STALE -> "Fixture: no update >60s"
            HealthStatus.DISCONNECTED -> "Fixture: disconnected"
        },
    )

    fun sampleQuote(
        health: MarketHealth = sampleHealth(),
    ): QuoteSnapshot {
        val candles = if (com.cavin.confluence.data.snapshot.MdSnapshotStore.isLoaded()) {
            com.cavin.confluence.data.snapshot.MdSnapshotStore.candles(Timeframe.H1)
        } else {
            sampleClosedCandles()
        }
        val last = candles.last()
        val prev = candles.getOrNull(candles.lastIndex - 1)
        val pct = prev?.let { ((last.close - it.close) / it.close) * 100.0 }
        val resolvedHealth = if (com.cavin.confluence.data.snapshot.MdSnapshotStore.isLoaded()) {
            health.copy(
                lastSourceTsMs = com.cavin.confluence.data.snapshot.MdSnapshotStore.cutoffMs,
                note = com.cavin.confluence.data.snapshot.MdSnapshotStore.bannerLabel,
            )
        } else {
            health
        }
        return QuoteSnapshot(
            venue = last.venue,
            symbol = last.symbol,
            lastPrice = last.close,
            percentChange = pct,
            health = resolvedHealth,
            asOfSourceTsMs = last.sourceTsMs,
        )
    }

    private fun timeframeApproxMs(tf: Timeframe): Long = when (tf) {
        Timeframe.M1 -> 60_000L
        Timeframe.M5 -> 300_000L
        Timeframe.M15 -> 900_000L
        Timeframe.H1 -> HOUR_MS
        Timeframe.H4 -> 14_400_000L
        Timeframe.D1 -> 86_400_000L
        Timeframe.W1 -> 604_800_000L
    }
}

/**
 * Fake [MarketDataApi] backed by [FakeFixtures].
 */
class FakeMarketDataApi(
    private val healthStatus: HealthStatus = HealthStatus.OK,
) : MarketDataApi {

    override suspend fun getHistory(
        venue: Venue,
        symbol: String,
        timeframe: Timeframe,
        fromMs: Long?,
        toMs: Long?,
    ): List<Candle> {
        // Prefer packaged Binance snapshot when loaded; else sine fixtures (unit tests).
        val base = if (com.cavin.confluence.data.snapshot.MdSnapshotStore.isLoaded()) {
            com.cavin.confluence.data.snapshot.MdSnapshotStore.candles(timeframe)
        } else {
            FakeFixtures.sampleClosedCandles(venue, timeframe)
        }
        return base
            .filter { it.isFinal }
            .filter { fromMs == null || it.openTimeMs >= fromMs }
            .filter { toMs == null || it.openTimeMs <= toMs }
    }

    override fun observeLive(
        venue: Venue,
        symbol: String,
        timeframe: Timeframe,
    ): Flow<Candle> = flow {
        val closed = FakeFixtures.sampleClosedCandles(venue, timeframe)
        val last = closed.last()
        emit(last)
        delay(500)
        emit(FakeFixtures.sampleFormingCandle(venue, timeframe, last))
    }

    override fun observeHealth(
        venue: Venue,
        symbol: String,
    ): Flow<MarketHealth> = flow {
        emit(FakeFixtures.sampleHealth(healthStatus, venue))
    }

    override suspend fun getHealth(
        venue: Venue,
        symbol: String,
    ): MarketHealth = FakeFixtures.sampleHealth(healthStatus, venue)
}

class FakeQuoteApi(
    private val healthStatus: HealthStatus = HealthStatus.OK,
) : QuoteApi {
    override suspend fun getQuote(
        venue: Venue,
        symbol: String,
    ): QuoteSnapshot = FakeFixtures.sampleQuote(
        health = FakeFixtures.sampleHealth(healthStatus, venue),
    )
}
