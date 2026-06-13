package com.sg.watchmarket.data.api

import okhttp3.Interceptor
import okhttp3.Response

class AuthHeaderInterceptor(
    private val bearerToken: String,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        if (bearerToken.isBlank()) {
            return chain.proceed(request)
        }

        return chain.proceed(
            request.newBuilder()
                .header("Authorization", "Bearer $bearerToken")
                .build(),
        )
    }
}
