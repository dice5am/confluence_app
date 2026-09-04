package com.cavin.confluence.data

import com.cavin.confluence.data.fake.FakeFixtures
import com.cavin.confluence.data.fake.FakeMarketDataApi
import com.cavin.confluence.data.model.Candle
import com.cavin.confluence.data.model.HealthStatus
import com.cavin.confluence.data.model.Timeframe
import com.cavin.confluence.data.model.Venue
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * MOB-1.5.1 — DTO / wire mapping tests against MD-1.1 shaped domain types.
 * Pure mapping + fixture contract behavior (no exchange I/O).
 */
class Md11DtoMappingTest {

    @Test
    fun venue_fromWire_roundTrip() {
        assertThat(Venue.fromWire("binance")).isEqualTo(Venue.BINANCE)
        assertThat(Venue.fromWire("bybit")).isEqualTo(Venue.BYBIT)
        assertThat(Venue.BINANCE.wire).isEqualTo("binance")
        assertThat(Venue.BYBIT.wire).isEqualTo("bybit")
    }

    @Test
    fun timeframe_fromWire_coversDayOneSet() {
        val expected = mapOf(
            "1m" to Timeframe.M1,
            "5m" to Timeframe.M5,
            "15m" to Timeframe.M15,
            "1h" to Timeframe.H1,
            "4h" to Timeframe.H4,
            "1d" to Timeframe.D1,
            "1w" to Timeframe.W1,
        )
        expected.forEach { (wire, tf) ->
            assertThat(Timeframe.fromWire(wire)).isEqualTo(tf)
            assertThat(tf.wire).isEqualTo(wire)
        }
    }

    @Test
    fun healthStatus_fromWire_roundTrip() {
        listOf(
            "ok" to HealthStatus.OK,
            "degraded" to HealthStatus.DEGRADED,
            "stale" to HealthStatus.STALE,
            "disconnected" to HealthStatus.DISCONNECTED,
        ).forEach { (wire, status) ->
            assertThat(HealthStatus.fromWire(wire)).isEqualTo(status)
            assertThat(status.wire).isEqualTo(wire)
        }
    }

    @Test
    fun candle_primaryKey_matchesMd11Tuple() {
        val c = FakeFixtures.sampleClosedCandles(count = 1).single()
        val key = c.primaryKey
        assertThat(key.venue).isEqualTo(c.venue)
        assertThat(key.symbol).isEqualTo(c.symbol)
        assertThat(key.timeframe).isEqualTo(c.timeframe)
        assertThat(key.openTimeMs).isEqualTo(c.openTimeMs)
        assertThat(c.symbol).isEqualTo(Candle.SYMBOL_BTCUSDT)
    }

    @Test
    fun history_get_returnsClosedOnly() = runTest {
        val api = FakeMarketDataApi()
        val history = api.getHistory(
            venue = Venue.BINANCE,
            timeframe = Timeframe.H1,
        )
        assertThat(history).isNotEmpty()
        assertThat(history.all { it.isFinal }).isTrue()
    }

    @Test
    fun history_respectsFromToBounds() = runTest {
        val api = FakeMarketDataApi()
        val all = api.getHistory(Venue.BINANCE, timeframe = Timeframe.H1)
        val mid = all[all.size / 2].openTimeMs
        val filtered = api.getHistory(
            venue = Venue.BINANCE,
            timeframe = Timeframe.H1,
            fromMs = mid,
            toMs = all.last().openTimeMs,
        )
        assertThat(filtered.all { it.openTimeMs >= mid }).isTrue()
        assertThat(filtered.all { it.openTimeMs <= all.last().openTimeMs }).isTrue()
        assertThat(filtered.all { it.isFinal }).isTrue()
    }

    @Test
    fun sampleHealth_carriesLastSourceTsMs() {
        val h = FakeFixtures.sampleHealth(HealthStatus.STALE)
        assertThat(h.status).isEqualTo(HealthStatus.STALE)
        assertThat(h.lastSourceTsMs).isGreaterThan(0)
        assertThat(h.symbol).isEqualTo(Candle.SYMBOL_BTCUSDT)
    }
}
