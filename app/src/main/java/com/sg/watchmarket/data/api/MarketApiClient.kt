package com.sg.watchmarket.data.api

import com.sg.watchmarket.config.WatchMarketConfig
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object MarketApiClient {
    fun create(
        serverBaseUrl: String = WatchMarketConfig.serverBaseUrl,
        bearerToken: String = WatchMarketConfig.bearerToken,
    ): MarketApi {
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(AuthHeaderInterceptor(bearerToken))
            .build()

        return Retrofit.Builder()
            .baseUrl(serverBaseUrl.withTrailingSlash())
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MarketApi::class.java)
    }
}

private fun String.withTrailingSlash(): String =
    if (endsWith("/")) this else "$this/"
