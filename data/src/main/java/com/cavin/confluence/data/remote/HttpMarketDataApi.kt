package com.cavin.confluence.data.remote

import com.cavin.confluence.data.api.MarketDataApi
import com.cavin.confluence.data.model.Candle
import com.cavin.confluence.data.model.MarketHealth
import com.cavin.confluence.data.model.Timeframe
import com.cavin.confluence.data.model.Venue
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * MOB-2.9 — HTTP + SSE client for the Arch-B Market Data service.
 * Device holds **no** exchange keys; talks only to [baseUrl].
 */
class HttpMarketDataApi(
    private val baseUrl: String,
    private val client: OkHttpClient = defaultClient(),
    private val sseClient: OkHttpClient = defaultSseClient(client),
) : MarketDataApi {

    override suspend fun getHistory(
        venue: Venue,
        symbol: String,
        timeframe: Timeframe,
        fromMs: Long?,
        toMs: Long?,
    ): List<Candle> = withContext(Dispatchers.IO) {
        val (defaultFrom, defaultTo) = HistoryWindow.fromToMs(timeframe)
        val from = fromMs ?: defaultFrom
        val to = toMs ?: defaultTo
        val url = MdEndpoints.history(
            baseUrl = baseUrl,
            venue = venue.wire,
            symbol = symbol,
            timeframe = timeframe.wire,
            fromMs = from,
            toMs = to,
        )
        val body = getJson(url)
        MdJson.historyResponse(body)
    }

    override fun observeLive(
        venue: Venue,
        symbol: String,
        timeframe: Timeframe,
    ): Flow<Candle> = callbackFlow {
        val url = MdEndpoints.liveStream(
            baseUrl = baseUrl,
            symbol = symbol,
            timeframe = timeframe.wire,
            finalsOnly = false,
        )
        val request = Request.Builder().url(url).get().build()
        val listener = object : EventSourceListener() {
            override fun onEvent(
                eventSource: EventSource,
                id: String?,
                type: String?,
                data: String,
            ) {
                if (type != null && type != "candle") return
                runCatching {
                    val c = MdJson.candle(JSONObject(data))
                    if (c.venue == venue && c.symbol == symbol && c.timeframe == timeframe) {
                        trySend(c)
                    }
                }
            }

            override fun onFailure(
                eventSource: EventSource,
                t: Throwable?,
                response: Response?,
            ) {
                close(t ?: IllegalStateException("SSE failed: ${response?.code}"))
            }
        }
        val es = EventSources.createFactory(sseClient).newEventSource(request, listener)
        awaitClose { es.cancel() }
    }

    override fun observeHealth(
        venue: Venue,
        symbol: String,
    ): Flow<MarketHealth> = flow {
        // Poll health; SSE also emits health events but chart uses poll + live candle path.
        while (true) {
            emit(getHealth(venue, symbol))
            kotlinx.coroutines.delay(15_000)
        }
    }

    override suspend fun getHealth(
        venue: Venue,
        symbol: String,
    ): MarketHealth = withContext(Dispatchers.IO) {
        val body = getJson(MdEndpoints.health(baseUrl))
        MdJson.health(body)
    }

    private fun getJson(url: String): JSONObject {
        val request = Request.Builder().url(url).get().build()
        client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) {
                throw IllegalStateException("MD HTTP ${resp.code} for $url")
            }
            val text = resp.body?.string().orEmpty()
            return JSONObject(text)
        }
    }

    companion object {
        fun defaultClient(): OkHttpClient =
            OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build()

        fun defaultSseClient(base: OkHttpClient = defaultClient()): OkHttpClient =
            base.newBuilder()
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .build()
    }
}
