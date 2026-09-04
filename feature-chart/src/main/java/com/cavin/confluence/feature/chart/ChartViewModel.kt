package com.cavin.confluence.feature.chart

import android.app.Application
import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cavin.confluence.data.fake.FakeFixtures
import com.cavin.confluence.data.fake.FakeMarketDataApi
import com.cavin.confluence.data.model.Candle
import com.cavin.confluence.data.model.HealthStatus
import com.cavin.confluence.data.model.MarketHealth
import com.cavin.confluence.data.model.Timeframe
import com.cavin.confluence.data.model.Venue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ChartUiState(
    val loading: Boolean = true,
    val candles: List<Candle> = emptyList(),
    /** Pre-LOD count for perf / debug. */
    val rawCandleCount: Int = 0,
    val timeframe: Timeframe = Timeframe.H1,
    val venue: Venue = Venue.BINANCE,
    val health: MarketHealth? = null,
    val crosshair: Candle? = null,
    val showVolume: Boolean = true,
    val error: String? = null,
    val lastTfSwitchMs: Long? = null,
)

/**
 * MOB-2.1–2.6 chart VM — fixtures until MOB-2.9 wires real MD client.
 */
class ChartViewModel(
    app: Application,
    private val api: FakeMarketDataApi = FakeMarketDataApi(),
    initialTf: Timeframe? = null,
) : AndroidViewModel(app) {

    private val tfPrefs = ChartTfPreferences(app)

    private val _ui = MutableStateFlow(
        ChartUiState(timeframe = initialTf ?: tfPrefs.getLastUsedOrDefault()),
    )
    val uiState: StateFlow<ChartUiState> = _ui.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            loadHistory(isTfSwitch = false, t0 = null)
        }
    }

    fun setTimeframe(tf: Timeframe) {
        if (tf == _ui.value.timeframe) return
        val t0 = SystemClock.elapsedRealtime()
        tfPrefs.setLastUsed(tf)
        _ui.update { it.copy(timeframe = tf) }
        viewModelScope.launch {
            loadHistory(isTfSwitch = true, t0 = t0)
        }
    }

    private suspend fun loadHistory(isTfSwitch: Boolean, t0: Long?) {
        val tf = _ui.value.timeframe
        _ui.update { it.copy(loading = true, error = null, crosshair = null) }
        val label = if (isTfSwitch) "tfSwitch:${tf.wire}" else "loadHistory:${tf.wire}"
        val started = t0 ?: SystemClock.elapsedRealtime()
        runCatching {
            withContext(Dispatchers.Default) {
                val raw = api.getHistory(venue = Venue.BINANCE, timeframe = tf)
                val health = api.getHealth(Venue.BINANCE)
                val drawn = CandleLod.maybeDecimate(raw, tf)
                ChartPerf.logSeriesStats(tf, raw.size, drawn.size)
                ChartPerf.assertLodBudget(raw, tf)
                Triple(raw, drawn, health)
            }
        }.onSuccess { (raw, drawn, health) ->
            val dt = SystemClock.elapsedRealtime() - started
            Log.d("ConfluenceChartPerf", "$label ${dt}ms" + if (isTfSwitch) " (DoD <100ms cached)" else "")
            _ui.update {
                it.copy(
                    loading = false,
                    candles = drawn,
                    rawCandleCount = raw.size,
                    health = health,
                    venue = Venue.BINANCE,
                    error = if (drawn.isEmpty()) "No candles" else null,
                    lastTfSwitchMs = if (isTfSwitch) dt else it.lastTfSwitchMs,
                )
            }
        }.onFailure { e ->
            _ui.update {
                it.copy(
                    loading = false,
                    error = e.message ?: "Load failed",
                    health = FakeFixtures.sampleHealth(HealthStatus.DISCONNECTED),
                )
            }
        }
    }

    fun onCrosshair(candle: Candle?) {
        _ui.update { it.copy(crosshair = candle) }
    }

    fun toggleVolume() {
        _ui.update { it.copy(showVolume = !it.showVolume) }
    }

    companion object {
        fun factory(
            app: Application,
            initialTimeframe: Timeframe? = null,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ChartViewModel(app, initialTf = initialTimeframe) as T
                }
            }
    }
}
