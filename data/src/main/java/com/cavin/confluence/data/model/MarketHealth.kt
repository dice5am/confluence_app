package com.cavin.confluence.data.model

/**
 * MD-1.1 market data health.
 *
 * Consumer rules:
 * - ok: normal
 * - degraded: AL policy; MO may badge
 * - stale (>60s no update): MO badge; AL MUST suppress new confluence fires
 * - disconnected: MO badge + O1 stale-cache OK; AL MUST suppress
 */
enum class HealthStatus(val wire: String) {
    OK("ok"),
    DEGRADED("degraded"),
    STALE("stale"),
    DISCONNECTED("disconnected");

    companion object {
        fun fromWire(value: String): HealthStatus =
            entries.first { it.wire == value }
    }
}

/**
 * @property lastSourceTsMs Last observed source timestamp from the feed.
 * @property gapCount Optional quality sidecars.
 * @property activeTimeframes Optional active TF list.
 * @property note Optional human-readable note.
 */
data class MarketHealth(
    val status: HealthStatus,
    val lastSourceTsMs: Long,
    val venue: Venue,
    val symbol: String = Candle.SYMBOL_BTCUSDT,
    val gapCount: Int? = null,
    val activeTimeframes: List<Timeframe>? = null,
    val note: String? = null,
)
