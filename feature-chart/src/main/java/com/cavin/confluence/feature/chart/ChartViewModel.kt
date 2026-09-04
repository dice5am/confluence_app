package com.cavin.confluence.feature.chart

import android.app.Application
import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cavin.confluence.data.api.MarketDataApi
import com.cavin.confluence.data.fake.FakeFixtures
import com.cavin.confluence.data.model.Candle
import com.cavin.confluence.data.model.HealthStatus
import com.cavin.confluence.data.model.MarketHealth
import com.cavin.confluence.data.model.Timeframe
import com.cavin.confluence.data.model.Venue
import com.cavin.confluence.data.remote.MarketDataFactory
import com.cavin.confluence.data.snapshot.MdSnapshotStore
import com.cavin.confluence.data.snapshot.SnapshotMarketDataApi
import com.cavin.confluence.data.remote.ResilientMarketDataApi
import com.cavin.confluence.data.series.CandleSeries
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
    /** True when ResilientMarketDataApi fell back to fixtures. */
    val usingFixtures: Boolean = false,
    val lastLiveAppendMs: Long? = null,
    /** Non-null when serving frozen Binance historical assets (no live WS). */
    val snapshotBanner: String? = null,
)

/**
 * MOB-2.1–2.9 chart VM — real MD client (Arch-B) + live tip append (MOB-2.5).
 */
class ChartViewModel(
    app: Application,
    private val api: MarketDataApi = MarketDataFactory.create(app),
    initialTf: Timeframe? = null,
) : AndroidViewModel(app) {

    private val tfPrefs = ChartTfPreferences(app)

    /** Full series before LOD — live updates mutate tip here, then re-project draw list. */
    private var rawSeries: List<Candle> = emptyList()

    private var liveJob: Job? = null
    private var healthJob: Job? = null

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
        liveJob?.cancel()
        healthJob?.cancel()
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
            Log.d(PERF, "$label ${dt}ms" + if (isTfSwitch) " (DoD <100ms cached)" else "")
            rawSeries = raw
            val fixtures = (api as? ResilientMarketDataApi)?.usingFixtures == true
            val snapBanner = when {
                api is SnapshotMarketDataApi -> MdSnapshotStore.bannerLabel
                health.note?.startsWith("Historical snapshot") == true -> health.note
                MdSnapshotStore.isLoaded() && fixtures -> MdSnapshotStore.bannerLabel
                else -> null
            }
            _ui.update {
                it.copy(
                    loading = false,
                    candles = drawn,
                    rawCandleCount = raw.size,
                    health = health,
                    venue = Venue.BINANCE,
                    error = if (drawn.isEmpty()) "No candles" else null,
                    lastTfSwitchMs = if (isTfSwitch) dt else it.lastTfSwitchMs,
                    usingFixtures = fixtures || api is SnapshotMarketDataApi,
                    snapshotBanner = snapBanner,
                )
            }
            // Snapshot mode: no live WS; still observe static health once.
            if (api !is SnapshotMarketDataApi) {
                startLive(tf)
            }
            startHealth()
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

    /**
     * MOB-2.5 — subscribe live; upsert tip without full history reload.
     */
    private fun startLive(tf: Timeframe) {
        liveJob?.cancel()
        liveJob = viewModelScope.launch {
            api.observeLive(venue = Venue.BINANCE, timeframe = tf).collect { tick ->
                val t0 = SystemClock.elapsedRealtime()
                val nextRaw = CandleSeries.applyLive(rawSeries, tick)
                if (nextRaw === rawSeries) return@collect
                rawSeries = nextRaw
                // Tip-only path: if under LOD budget, swap tip / append without rebucket.
                val drawn = if (nextRaw.size <= CandleLod.DEFAULT_MAX_POINTS) {
                    nextRaw
                } else {
                    // Keep overview LOD of the body; tip stays accurate via full rebucket (rare).
                    CandleLod.maybeDecimate(nextRaw, tf)
                }
                val dt = SystemClock.elapsedRealtime() - t0
                Log.d(PERF, "liveAppend:${tf.wire} ${dt}ms tipFinal=${tick.isFinal} (no full history reload)")
                _ui.update {
                    it.copy(
                        candles = drawn,
                        rawCandleCount = nextRaw.size,
                        lastLiveAppendMs = dt,
                        usingFixtures = (api as? ResilientMarketDataApi)?.usingFixtures == true,
                    )
                }
            }
        }
    }

    private fun startHealth() {
        healthJob?.cancel()
        healthJob = viewModelScope.launch {
            api.observeHealth(Venue.BINANCE).collect { h ->
                _ui.update { it.copy(health = h) }
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
        private const val PERF = "ConfluenceChartPerf"

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
