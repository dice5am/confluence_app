package com.cavin.confluence.data.remote

import android.content.Context
import com.cavin.confluence.data.api.MarketDataApi

/** MOB-2.9 entry: build the Arch-B MD client used by chart (and later alerts). */
object MarketDataFactory {
    fun create(context: Context): MarketDataApi {
        val base = MdConfigPrefs(context).baseUrl()
        return ResilientMarketDataApi(http = HttpMarketDataApi(baseUrl = base))
    }
}
