package com.sg.watchmarket.data.dto

data class QuoteDto(
    val id: String,
    val display: String,
    val currency: String,
    val price: Double,
    val changeRate24h: Double,
    val timestamp: Long,
)
