package com.sg.watchmarket.data.dto

data class IndicatorDto(
    val id: String,
    val tf: String,
    val currency: String,
    val rsi14: Double,
    val currentCandleVolume: Double,
    val volume24h: Double,
    val volumeAvg7d: Double,
    val volumeAvg30d: Double,
    val volumeAvg180d: Double,
    val volumeAvg365d: Double,
    val volumeCurrency: String,
    val timestamp: Long,
)
