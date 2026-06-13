package com.sg.watchmarket.data.api

import com.sg.watchmarket.data.dto.AssetDto
import com.sg.watchmarket.data.dto.CandleResponseDto
import com.sg.watchmarket.data.dto.IndicatorDto
import com.sg.watchmarket.data.dto.QuoteDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
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

    @GET("v1/indicators")
    suspend fun getIndicators(
        @Query("id") id: String,
        @Query("tf") tf: String,
    ): Response<IndicatorDto>

    @GET("v1/search")
    suspend fun searchAssets(
        @Query("q") query: String,
    ): Response<List<AssetDto>>

    @POST("v1/watchlist")
    suspend fun addWatchlistAsset(
        @Body asset: AssetDto,
    ): Response<AssetDto>

    @DELETE("v1/watchlist/{id}")
    suspend fun deleteWatchlistAsset(
        @Path("id") id: String,
    ): Response<Unit>
}
