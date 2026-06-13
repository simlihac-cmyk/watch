package com.sg.watchmarket.state

import com.sg.watchmarket.data.dto.CandleDto
import com.sg.watchmarket.data.dto.IndicatorDto

data class AssetDetailData(
    val assetId: String,
    val display: String,
    val timeframe: String,
    val currency: String,
    val latestClose: Double,
    val latestTimestamp: Long,
    val candleCount: Int,
    val candles: List<CandleDto>,
    val indicators: IndicatorDto? = null,
    val indicatorMessage: String? = null,
    val isStale: Boolean = false,
)

sealed interface AssetDetailUiState {
    data object Loading : AssetDetailUiState

    data class Loaded(
        val data: AssetDetailData,
    ) : AssetDetailUiState

    data class Error(
        val message: String,
        val staleData: AssetDetailData? = null,
    ) : AssetDetailUiState
}
