package com.cavin.confluence.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cavin.confluence.data.api.QuoteApi
import com.cavin.confluence.data.fake.FakeQuoteApi
import com.cavin.confluence.data.model.HealthStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Home hub ViewModel. Uses [QuoteApi] stubs only (Arch-B / MOB-1.5 light).
 */
class HomeViewModel(
    private val quoteApi: QuoteApi,
    private val demoMode: DemoMode = DemoMode.READY,
) : ViewModel() {

    enum class DemoMode { LOADING, EMPTY, ERROR, READY, STALE }

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            when (demoMode) {
                DemoMode.LOADING -> _uiState.value = HomeUiState.Loading
                DemoMode.EMPTY -> _uiState.value = HomeUiState.Empty
                DemoMode.ERROR -> {
                    _uiState.value = HomeUiState.Error(
                        "Unable to load market snapshot. Insight data unavailable.",
                    )
                }
                DemoMode.READY, DemoMode.STALE -> {
                    _uiState.value = HomeUiState.Loading
                    runCatching { quoteApi.getQuote() }
                        .onSuccess { quote ->
                            val adjusted = if (demoMode == DemoMode.STALE) {
                                quote.copy(
                                    health = quote.health.copy(status = HealthStatus.STALE),
                                )
                            } else {
                                quote
                            }
                            _uiState.value = HomeUiState.Ready(
                                quote = adjusted,
                                unreadAlertCount = 2,
                            )
                        }
                        .onFailure { e ->
                            _uiState.value = HomeUiState.Error(
                                e.message ?: "Unable to load market snapshot.",
                            )
                        }
                }
            }
        }
    }

    companion object {
        fun factory(
            quoteApi: QuoteApi = FakeQuoteApi(),
            demoMode: DemoMode = DemoMode.READY,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HomeViewModel(quoteApi, demoMode) as T
            }
        }
    }
}
