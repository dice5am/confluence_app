package com.cavin.confluence.feature.chart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cavin.confluence.data.fake.FakeMarketDataApi
import com.cavin.confluence.data.model.Candle
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
    val crosshair: Candle? = null,
    val error: String? = null,
)

/**
 * MOB-2.1 — loads fixture closed candles for the Canvas spike.
 * Real MD client wiring is MOB-2.9.
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
            _ui.update { it.copy(loading = true, error = null) }
            runCatching {
                api.getHistory(
                    venue = Venue.BINANCE,
                    timeframe = _ui.value.timeframe,
                )
            }.onSuccess { list ->
                _ui.update {
                    it.copy(loading = false, candles = list, error = if (list.isEmpty()) "No candles" else null)
                }
            }.onFailure { e ->
                _ui.update { it.copy(loading = false, error = e.message ?: "Load failed") }
            }
        }
    }

    fun onCrosshair(candle: Candle?) {
        _ui.update { it.copy(crosshair = candle) }
    }

    companion object {
        fun factory(tf: Timeframe = Timeframe.H1): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ChartViewModel(initialTf = tf) as T
                }
            }
    }
}
