package com.cavin.confluence.feature.chart

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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChartUiState(
    val loading: Boolean = true,
    val candles: List<Candle> = emptyList(),
    val timeframe: Timeframe = Timeframe.H1,
    val venue: Venue = Venue.BINANCE,
    val health: MarketHealth? = null,
    val crosshair: Candle? = null,
    val showVolume: Boolean = true,
    val error: String? = null,
)

/**
 * MOB-2.1–2.8 chart VM — fixtures until MOB-2.9 wires real MD client.
 */
class ChartViewModel(
    private val api: FakeMarketDataApi = FakeMarketDataApi(),
    initialTf: Timeframe = Timeframe.H1,
) : ViewModel() {

    private val _ui = MutableStateFlow(ChartUiState(timeframe = initialTf))
    val uiState: StateFlow<ChartUiState> = _ui.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val tf = _ui.value.timeframe
            _ui.update { it.copy(loading = true, error = null, crosshair = null) }
            runCatching {
                val candles = api.getHistory(venue = Venue.BINANCE, timeframe = tf)
                val health = api.getHealth(Venue.BINANCE)
                candles to health
            }.onSuccess { (list, health) ->
                _ui.update {
                    it.copy(
                        loading = false,
                        candles = list,
                        health = health,
                        venue = Venue.BINANCE,
                        error = if (list.isEmpty()) "No candles" else null,
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
    }

    fun setTimeframe(tf: Timeframe) {
        if (tf == _ui.value.timeframe) return
        _ui.update { it.copy(timeframe = tf) }
        refresh()
    }

    fun onCrosshair(candle: Candle?) {
        _ui.update { it.copy(crosshair = candle) }
    }

    fun toggleVolume() {
        _ui.update { it.copy(showVolume = !it.showVolume) }
    }

    companion object {
        fun factory(initialTimeframe: Timeframe = Timeframe.H1): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ChartViewModel(initialTf = initialTimeframe) as T
                }
            }
    }
}
