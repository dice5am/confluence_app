package com.cavin.confluence.data.model

/**
 * MD-1.1 venue enum. Never blend series across venues.
 */
enum class Venue(val wire: String) {
    BINANCE("binance"),
    BYBIT("bybit");

    companion object {
        fun fromWire(value: String): Venue =
            entries.first { it.wire == value }
    }
}
