package com.sg.watchmarket.data.dto

data class CandleResponseDto(
    val id: String,
    val tf: String,
    val currency: String,
    val source: String,
    val candles: List<CandleDto>,
)
