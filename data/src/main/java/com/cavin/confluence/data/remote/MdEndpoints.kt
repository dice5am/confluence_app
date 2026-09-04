package com.cavin.confluence.data.remote

/**
 * Arch-B Market Data service endpoints (device never talks to exchanges).
 *
 * Default base URL targets the Android emulator loopback to the host MD docker
 * (`docker compose` on :8080). Override via [MdConfigPrefs].
 */
object MdEndpoints {
    /** Emulator → host machine localhost. */
    const val DEFAULT_BASE_URL = "http://10.0.2.2:8080"

    fun history(
        baseUrl: String,
        venue: String,
        symbol: String,
        timeframe: String,
        fromMs: Long,
        toMs: Long,
    ): String =
        trimSlash(baseUrl) +
            "/v1/candles?venue=$venue&symbol=$symbol&timeframe=$timeframe&fromMs=$fromMs&toMs=$toMs"

    fun liveStream(
        baseUrl: String,
        symbol: String,
        timeframe: String,
        finalsOnly: Boolean = false,
    ): String {
        val fo = if (finalsOnly) "&finalsOnly=1" else ""
        return trimSlash(baseUrl) +
            "/v1/candles/stream?symbol=$symbol&timeframe=$timeframe$fo"
    }

    fun health(baseUrl: String): String = trimSlash(baseUrl) + "/v1/health"

    private fun trimSlash(base: String): String = base.trimEnd('/')
}
