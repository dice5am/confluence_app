package com.cavin.confluence.data.snapshot

import android.content.Context
import com.cavin.confluence.data.model.Candle
import com.cavin.confluence.data.model.Timeframe
import com.cavin.confluence.data.model.Venue
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Frozen Binance BTCUSDT kline snapshot packaged in assets (no live WS / no API keys).
 *
 * Asset layout: `assets/md_snapshot/meta.json` + `BTCUSDT_<tf>.json`.
 * Keeps UI/data hooks simple for an incoming design-system PR — data only.
 */
object MdSnapshotStore {

    private const val ASSET_DIR = "md_snapshot"
    private val loaded = AtomicBoolean(false)

    @Volatile
    var cutoffMs: Long = 0L
        private set

    @Volatile
    var cutoffUtcLabel: String = ""
        private set

    @Volatile
    var source: String = "binance"
        private set

    private val byTf = LinkedHashMap<Timeframe, List<Candle>>()

    val bannerLabel: String
        get() = "Historical snapshot · as of $cutoffUtcLabel"

    fun isLoaded(): Boolean = loaded.get()

    @Synchronized
    fun ensureLoaded(context: Context) {
        if (loaded.get()) return
        val assets = context.applicationContext.assets
        val meta = JSONObject(assets.open("$ASSET_DIR/meta.json").bufferedReader().use { it.readText() })
        cutoffMs = meta.getLong("cutoffMs")
        cutoffUtcLabel = meta.getString("cutoffUtc")
        source = meta.optString("source", "binance")

        for (tf in Timeframe.entries) {
            val name = "$ASSET_DIR/BTCUSDT_${tf.wire}.json"
            val root = JSONObject(assets.open(name).bufferedReader().use { it.readText() })
            val arr = root.getJSONArray("candles")
            val list = ArrayList<Candle>(arr.length())
            val ingest = cutoffMs
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    Candle(
                        venue = Venue.BINANCE,
                        symbol = Candle.SYMBOL_BTCUSDT,
                        timeframe = tf,
                        openTimeMs = o.getLong("openTimeMs"),
                        closeTimeMs = o.getLong("closeTimeMs"),
                        open = o.getDouble("open"),
                        high = o.getDouble("high"),
                        low = o.getDouble("low"),
                        close = o.getDouble("close"),
                        volume = o.getDouble("volume"),
                        isFinal = o.optBoolean("isFinal", true),
                        sourceTsMs = o.optLong("sourceTsMs", o.getLong("closeTimeMs")),
                        ingestTsMs = ingest,
                    ),
                )
            }
            byTf[tf] = list
        }
        loaded.set(true)
    }

    fun candles(timeframe: Timeframe): List<Candle> {
        check(loaded.get()) { "MdSnapshotStore not loaded" }
        return byTf[timeframe].orEmpty()
    }

    fun lastClosed(timeframe: Timeframe = Timeframe.H1): Candle? =
        candles(timeframe).lastOrNull { it.isFinal }
}
