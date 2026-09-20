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
import com.cavin.confluence.indicators.DayOneIndicators
import com.cavin.confluence.indicators.IndicatorParams
import com.cavin.confluence.indicators.SnapshotCutoff
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
    val overlays: ChartOverlayVisibility = ChartOverlayVisibility(),
    val overlayPalette: ChartIndicatorPalette = ChartIndicatorPalette.Defaults,
    val params: IndicatorParams = IndicatorParams.DEFAULT,
    val indicators: DayOneIndicators? = null,
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
    private val overlayPrefs = ChartIndicatorPreferences(app)

    /** Full series before LOD — live updates mutate tip here, then re-project draw list. */
    private var rawSeries: List<Candle> = emptyList()
    private var latestIndicators: DayOneIndicators? = null
    private var latestCutoff: SnapshotCutoff = SnapshotCutoff.PACKAGED_2026_09_19

    private var liveJob: Job? = null
    private var healthJob: Job? = null

    private val _ui = MutableStateFlow(
        run {
            val overlays = overlayPrefs.loadVisibility()
            ChartUiState(
                timeframe = initialTf ?: tfPrefs.getLastUsedOrDefault(),
                overlays = overlays,
                overlayPalette = overlayPrefs.loadPalette(),
                params = overlayPrefs.loadParams(),
                showVolume = overlays.volume,
            )
        },
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
                val cutoff = resolveSnapshotCutoff()
                val params = _ui.value.params
                val indicators = evaluateDayOneIndicators(raw, cutoff, params)
                val fenced = candlesAlignedToIndicators(raw, indicators)
                val drawn = CandleLod.maybeDecimate(fenced, tf)
                ChartPerf.logSeriesStats(tf, fenced.size, drawn.size)
                ChartPerf.assertLodBudget(fenced, tf)
                ChartLoaded(raw = fenced, drawn = drawn, health = health, indicators = indicators, cutoff = cutoff)
            }
        }.onSuccess { loaded ->
            val dt = SystemClock.elapsedRealtime() - started
            Log.d(PERF, "$label ${dt}ms" + if (isTfSwitch) " (DoD <100ms cached)" else "")
            rawSeries = loaded.raw
            latestIndicators = loaded.indicators
            latestCutoff = loaded.cutoff
            val fixtures = (api as? ResilientMarketDataApi)?.usingFixtures == true
            val snapBanner = when {
                api is SnapshotMarketDataApi -> MdSnapshotStore.bannerLabel
                loaded.health.note?.startsWith("Historical snapshot") == true -> loaded.health.note
                MdSnapshotStore.isLoaded() && fixtures -> MdSnapshotStore.bannerLabel
                else -> null
            }
            _ui.update {
                it.copy(
                    loading = false,
                    candles = loaded.drawn,
                    rawCandleCount = loaded.raw.size,
                    health = loaded.health,
                    venue = Venue.BINANCE,
                    error = if (loaded.drawn.isEmpty()) "No candles" else null,
                    lastTfSwitchMs = if (isTfSwitch) dt else it.lastTfSwitchMs,
                    usingFixtures = fixtures || api is SnapshotMarketDataApi,
                    snapshotBanner = snapBanner,
                    indicators = loaded.indicators,
                    overlays = it.overlays.copy(volume = it.showVolume),
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
                val nextIndicators = when {
                    tick.isFinal -> evaluateDayOneIndicators(nextRaw, latestCutoff, _ui.value.params)
                    else -> latestIndicators
                }
                latestIndicators = nextIndicators
                val fenced = nextIndicators?.let { candlesAlignedToIndicators(nextRaw, it) } ?: nextRaw
                val drawn = if (fenced.size <= CandleLod.DEFAULT_MAX_POINTS) {
                    fenced
                } else {
                    CandleLod.maybeDecimate(fenced, tf)
                }
                val dt = SystemClock.elapsedRealtime() - t0
                Log.d(PERF, "liveAppend:${tf.wire} ${dt}ms tipFinal=${tick.isFinal} (no full history reload)")
                _ui.update {
                    it.copy(
                        candles = drawn,
                        rawCandleCount = fenced.size,
                        lastLiveAppendMs = dt,
                        usingFixtures = (api as? ResilientMarketDataApi)?.usingFixtures == true,
                        indicators = nextIndicators,
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
        toggleIndicator(ChartIndicatorId.VolumeRibbon)
    }

    fun toggleIndicator(id: ChartIndicatorId) {
        _ui.update {
            val next = it.overlays.toggle(id)
            overlayPrefs.saveVisibility(next)
            it.copy(
                overlays = next,
                showVolume = next.volume,
            )
        }
    }

    fun cycleIndicatorWell(id: ChartIndicatorId, wellIndex: Int) {
        _ui.update {
            val next = it.overlayPalette.cycleWell(id, wellIndex)
            overlayPrefs.savePalette(next)
            it.copy(overlayPalette = next)
        }
    }

    fun updateParams(next: IndicatorParams) {
        overlayPrefs.saveParams(next)
        _ui.update { it.copy(params = next) }
        viewModelScope.launch { recomputeFromRaw() }
    }

    private suspend fun recomputeFromRaw() {
        val raw = rawSeries
        if (raw.isEmpty()) return
        val cutoff = latestCutoff
        val params = _ui.value.params
        val tf = _ui.value.timeframe
        val indicators = withContext(Dispatchers.Default) {
            evaluateDayOneIndicators(raw, cutoff, params)
        }
        latestIndicators = indicators
        val fenced = candlesAlignedToIndicators(raw, indicators)
        val drawn = CandleLod.maybeDecimate(fenced, tf)
        _ui.update {
            it.copy(
                candles = drawn,
                rawCandleCount = fenced.size,
                indicators = indicators,
            )
        }
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

private data class ChartLoaded(
    val raw: List<Candle>,
    val drawn: List<Candle>,
    val health: MarketHealth,
    val indicators: DayOneIndicators,
    val cutoff: SnapshotCutoff,
)
