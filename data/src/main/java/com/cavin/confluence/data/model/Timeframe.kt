package com.cavin.confluence.data.model

/**
 * MD-1.1 timeframe enum — venue-native klines; no silent rollup.
 */
enum class Timeframe(val wire: String) {
    M1("1m"),
    M5("5m"),
    M15("15m"),
    H1("1h"),
    H4("4h"),
    D1("1d"),
    W1("1w");

    companion object {
        fun fromWire(value: String): Timeframe =
            entries.first { it.wire == value }
    }
}
