package com.sg.watchmarket.data.api

import com.sg.watchmarket.data.dto.AssetDto
import com.sg.watchmarket.data.dto.CandleResponseDto
import com.sg.watchmarket.data.dto.QuoteDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface MarketApi {
    @GET("v1/assets")
    suspend fun getAssets(): Response<List<AssetDto>>

    @GET("v1/quotes")
    suspend fun getQuotes(
        @Query("ids") ids: String,
    ): Response<List<QuoteDto>>

    @GET("v1/candles")
    suspend fun getCandles(
        @Query("id") id: String,
        @Query("tf") tf: String,
        @Query("limit") limit: Int,
    ): Response<CandleResponseDto>
}
