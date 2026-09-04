package com.cavin.confluence.data.remote

import com.cavin.confluence.data.model.Candle
import com.cavin.confluence.data.model.HealthStatus
import com.cavin.confluence.data.model.MarketHealth
import com.cavin.confluence.data.model.Timeframe
import com.cavin.confluence.data.model.Venue
import org.json.JSONArray
import org.json.JSONObject

/** MD-1.1 JSON ↔ domain (Arch-B wire; no exchange payloads). */
object MdJson {

    fun candle(obj: JSONObject): Candle = Candle(
        venue = Venue.fromWire(obj.getString("venue")),
        symbol = obj.getString("symbol"),
        timeframe = Timeframe.fromWire(obj.getString("timeframe")),
        openTimeMs = obj.getLong("openTimeMs"),
        closeTimeMs = obj.getLong("closeTimeMs"),
        open = obj.getDouble("open"),
        high = obj.getDouble("high"),
        low = obj.getDouble("low"),
        close = obj.getDouble("close"),
        volume = obj.getDouble("volume"),
        isFinal = obj.getBoolean("isFinal"),
        sourceTsMs = obj.getLong("sourceTsMs"),
        ingestTsMs = obj.getLong("ingestTsMs"),
        gap = obj.optBooleanOrNull("gap"),
        stale = obj.optBooleanOrNull("stale"),
        reconciled = obj.optBooleanOrNull("reconciled"),
        sourceMismatch = obj.optBooleanOrNull("sourceMismatch"),
    )

    fun candles(arr: JSONArray): List<Candle> =
        (0 until arr.length()).map { candle(arr.getJSONObject(it)) }

    fun historyResponse(obj: JSONObject): List<Candle> =
        candles(obj.getJSONArray("candles"))

    fun health(obj: JSONObject): MarketHealth {
        val tfs = obj.optJSONArray("activeTimeframes")?.let { arr ->
            (0 until arr.length()).mapNotNull { i ->
                runCatching { Timeframe.fromWire(arr.getString(i)) }.getOrNull()
            }
        }
        return MarketHealth(
            status = HealthStatus.fromWire(obj.getString("status")),
            lastSourceTsMs = obj.getLong("lastSourceTsMs"),
            venue = Venue.fromWire(obj.getString("venue")),
            symbol = obj.optString("symbol", Candle.SYMBOL_BTCUSDT),
            gapCount = if (obj.has("gapCount") && !obj.isNull("gapCount")) obj.getInt("gapCount") else null,
            activeTimeframes = tfs,
            note = if (obj.has("note") && !obj.isNull("note")) {
                obj.getString("note").takeIf { it.isNotBlank() }
            } else null,
        )
    }

    private fun JSONObject.optBooleanOrNull(key: String): Boolean? =
        if (!has(key) || isNull(key)) null else getBoolean(key)
}
