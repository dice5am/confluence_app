package com.cavin.confluence.feature.home

import com.cavin.confluence.data.api.QuoteSnapshot
import com.cavin.confluence.data.model.HealthStatus

/** Home hub UI state (MOB-1.4). Insight-only — no Buy/Sell. */
sealed interface HomeUiState {
    data object Loading : HomeUiState
    data object Empty : HomeUiState
    data class Error(val message: String) : HomeUiState
    data class Ready(
        val quote: QuoteSnapshot,
        val unreadAlertCount: Int = 0,
    ) : HomeUiState
}

fun HealthStatus.toChipLabel(): String = when (this) {
    HealthStatus.OK -> "Live"
    HealthStatus.DEGRADED -> "Degraded"
    HealthStatus.STALE -> "Stale"
    HealthStatus.DISCONNECTED -> "Offline"
}
