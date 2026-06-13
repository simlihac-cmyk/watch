package com.sg.watchmarket.data.repository

sealed interface MarketApiResult<out T> {
    data class Success<T>(
        val value: T,
        val isStale: Boolean = false,
    ) : MarketApiResult<T>

    data object Unauthorized : MarketApiResult<Nothing>
    data class HttpFailure(val statusCode: Int, val message: String) : MarketApiResult<Nothing>
    data class NetworkFailure(val message: String) : MarketApiResult<Nothing>
    data class InvalidResponse(val message: String) : MarketApiResult<Nothing>
}
