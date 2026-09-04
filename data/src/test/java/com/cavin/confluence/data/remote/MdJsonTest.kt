package com.cavin.confluence.data.remote

import com.cavin.confluence.data.model.HealthStatus
import com.cavin.confluence.data.model.Timeframe
import com.cavin.confluence.data.model.Venue
import com.google.common.truth.Truth.assertThat
import org.json.JSONObject
import org.junit.Test

class MdJsonTest {
    @Test
    fun parsesCandleFixture() {
        val json = javaClass.classLoader!!
            .getResourceAsStream("md-fixtures/candle.json")!!
            .bufferedReader().readText()
        val c = MdJson.candle(JSONObject(json))
        assertThat(c.venue).isEqualTo(Venue.BINANCE)
        assertThat(c.timeframe).isEqualTo(Timeframe.H1)
        assertThat(c.isFinal).isTrue()
        assertThat(c.openTimeMs).isEqualTo(1_725_400_800_000L)
    }

    @Test
    fun parsesHealthFixture() {
        val json = javaClass.classLoader!!
            .getResourceAsStream("md-fixtures/health-ok.json")!!
            .bufferedReader().readText()
        val h = MdJson.health(JSONObject(json))
        assertThat(h.status).isEqualTo(HealthStatus.OK)
        assertThat(h.activeTimeframes).contains(Timeframe.H1)
    }

    @Test
    fun parsesHistoryEnvelope() {
        val json = javaClass.classLoader!!
            .getResourceAsStream("md-fixtures/history.json")!!
            .bufferedReader().readText()
        val list = MdJson.historyResponse(JSONObject(json))
        assertThat(list).hasSize(1)
        assertThat(list.single().isFinal).isTrue()
    }
}
