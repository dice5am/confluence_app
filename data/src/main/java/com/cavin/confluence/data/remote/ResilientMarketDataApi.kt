package com.cavin.confluence.data.remote

import android.util.Log
import com.cavin.confluence.data.api.MarketDataApi
import com.cavin.confluence.data.fake.FakeMarketDataApi
import com.cavin.confluence.data.model.Candle
import com.cavin.confluence.data.model.HealthStatus
import com.cavin.confluence.data.model.MarketHealth
import com.cavin.confluence.data.model.Timeframe
import com.cavin.confluence.data.model.Venue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart

/**
 * Prefers real MD HTTP; falls back to fixtures when the service is unreachable
 * so chart UX still works offline / on agent boxes without docker MD.
 */
class ResilientMarketDataApi(
    private val http: MarketDataApi,
    private val fake: MarketDataApi = FakeMarketDataApi(),
) : MarketDataApi {

    @Volatile
    var usingFixtures: Boolean = false
        private set

    override suspend fun getHistory(
        venue: Venue,
        symbol: String,
        timeframe: Timeframe,
        fromMs: Long?,
        toMs: Long?,
    ): List<Candle> {
        return try {
            val list = http.getHistory(venue, symbol, timeframe, fromMs, toMs)
            usingFixtures = false
            list
        } catch (e: Exception) {
            Log.w(TAG, "history failed → fixtures: ${e.message}")
            usingFixtures = true
            fake.getHistory(venue, symbol, timeframe, fromMs, toMs)
        }
    }

    override fun observeLive(
        venue: Venue,
        symbol: String,
        timeframe: Timeframe,
    ): Flow<Candle> =
        http.observeLive(venue, symbol, timeframe)
            .onStart { /* connected */ }
            .catch { e ->
                Log.w(TAG, "live SSE failed → fake live: ${e.message}")
                usingFixtures = true
                fake.observeLive(venue, symbol, timeframe).collect { emit(it) }
            }

    override fun observeHealth(
        venue: Venue,
        symbol: String,
    ): Flow<MarketHealth> =
        http.observeHealth(venue, symbol).catch { e ->
            Log.w(TAG, "health stream failed: ${e.message}")
            fake.observeHealth(venue, symbol).collect { emit(it) }
        }

    override suspend fun getHealth(
        venue: Venue,
        symbol: String,
    ): MarketHealth {
        return try {
            http.getHealth(venue, symbol).also { usingFixtures = false }
        } catch (e: Exception) {
            Log.w(TAG, "health failed → fixtures: ${e.message}")
            usingFixtures = true
            fake.getHealth(venue, symbol).copy(
                status = HealthStatus.DISCONNECTED,
                note = "MD unreachable — fixtures (${e.message})",
            )
        }
    }

    companion object {
        private const val TAG = "ConfluenceMdClient"
    }
}
