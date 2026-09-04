package com.cavin.confluence.data.remote

import android.content.Context
import com.cavin.confluence.data.api.MarketDataApi
import com.cavin.confluence.data.snapshot.SnapshotMarketDataApi

/**
 * MOB-2.9 entry: default chart/home MD client.
 *
 * Phase-2 debug default = frozen Binance historical snapshot (assets), not live WS/HTTP.
 * Live Arch-B HTTP remains available via [createLive] when a tunnel/MD host is intentional.
 */
object MarketDataFactory {
    fun create(context: Context): MarketDataApi = SnapshotMarketDataApi(context)

    /** Optional live MD client (HTTP + SSE) for explicit wiring — not the debug APK default. */
    fun createLive(context: Context): MarketDataApi {
        val base = MdConfigPrefs(context).baseUrl()
        return ResilientMarketDataApi(http = HttpMarketDataApi(baseUrl = base))
    }
}
