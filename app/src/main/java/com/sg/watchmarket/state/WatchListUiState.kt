package com.sg.watchmarket.state

data class WatchListItem(
    val id: String,
    val display: String,
    val price: Double,
    val currency: String,
    val changeRate24h: Double,
    val isStale: Boolean = false,
)

sealed interface WatchListUiState {
    data object Loading : WatchListUiState

    data class Loaded(
        val items: List<WatchListItem>,
        val isStale: Boolean = false,
    ) : WatchListUiState

    data class Error(
        val message: String,
        val staleItems: List<WatchListItem> = emptyList(),
    ) : WatchListUiState
}
