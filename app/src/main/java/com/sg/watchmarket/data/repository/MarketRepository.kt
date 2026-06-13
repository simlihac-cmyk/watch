package com.sg.watchmarket.data.repository

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.google.gson.stream.MalformedJsonException
import com.sg.watchmarket.data.api.MarketApi
import com.sg.watchmarket.data.api.MarketApiClient
import com.sg.watchmarket.data.cache.MarketResponseCache
import com.sg.watchmarket.data.cache.NoOpMarketResponseCache
import com.sg.watchmarket.data.dto.AssetDto
import com.sg.watchmarket.data.dto.CandleResponseDto
import com.sg.watchmarket.data.dto.QuoteDto
import java.io.IOException
import java.util.Locale
import kotlin.coroutines.cancellation.CancellationException
import retrofit2.Response

interface MarketRepository {
    suspend fun getAssets(): MarketApiResult<List<AssetDto>>

    suspend fun getQuotes(ids: List<String>): MarketApiResult<List<QuoteDto>>

    suspend fun getCandles(
        id: String,
        tf: String,
        limit: Int,
    ): MarketApiResult<CandleResponseDto>
}

class FastApiMarketRepository(
    private val api: MarketApi = MarketApiClient.create(),
    private val cache: MarketResponseCache = NoOpMarketResponseCache,
) : MarketRepository {
    override suspend fun getAssets(): MarketApiResult<List<AssetDto>> =
        safeApiCall { api.getAssets() }

    override suspend fun getQuotes(ids: List<String>): MarketApiResult<List<QuoteDto>> {
        val normalizedIds = ids.normalizedIds()
        return when (val result = safeApiCall { api.getQuotes(normalizedIds.joinToString(",")) }) {
            is MarketApiResult.Success -> {
                cache.setQuotes(normalizedIds, result.value)
                result
            }
            is MarketApiResult.NetworkFailure -> {
                cache.getQuotes(normalizedIds)
                    ?.let { MarketApiResult.Success(it, isStale = true) }
                    ?: result
            }
            else -> result
        }
    }

    override suspend fun getCandles(
        id: String,
        tf: String,
        limit: Int,
    ): MarketApiResult<CandleResponseDto> {
        val normalizedId = id.uppercase(Locale.US)
        return when (
            val result = safeApiCall {
                api.getCandles(
                    id = normalizedId,
                    tf = tf,
                    limit = limit,
                )
            }
        ) {
            is MarketApiResult.Success -> {
                cache.setCandles(normalizedId, tf, result.value)
                result
            }
            is MarketApiResult.NetworkFailure -> {
                cache.getCandles(normalizedId, tf, limit)
                    ?.let { MarketApiResult.Success(it, isStale = true) }
                    ?: result
            }
            else -> result
        }
    }
}

private suspend fun <T : Any> safeApiCall(
    call: suspend () -> Response<T>,
): MarketApiResult<T> {
    return try {
        call().toMarketApiResult()
    } catch (exc: CancellationException) {
        throw exc
    } catch (exc: MalformedJsonException) {
        MarketApiResult.InvalidResponse(exc.message ?: "Malformed JSON response")
    } catch (exc: IOException) {
        MarketApiResult.NetworkFailure(exc.message ?: "Network request failed")
    } catch (exc: JsonParseException) {
        MarketApiResult.InvalidResponse(exc.message ?: "Invalid JSON response")
    } catch (exc: IllegalStateException) {
        MarketApiResult.InvalidResponse(exc.message ?: "Invalid response")
    }
}

private fun <T : Any> Response<T>.toMarketApiResult(): MarketApiResult<T> {
    if (code() == 401) {
        return MarketApiResult.Unauthorized
    }

    if (!isSuccessful) {
        return MarketApiResult.HttpFailure(
            statusCode = code(),
            message = fastApiErrorMessage(),
        )
    }

    return body()?.let { MarketApiResult.Success(it) }
        ?: MarketApiResult.InvalidResponse("Response body was empty")
}

private fun List<String>.normalizedIds(): List<String> =
    map { it.trim().uppercase(Locale.US) }
        .filter { it.isNotBlank() }
        .distinct()

private fun Response<*>.fastApiErrorMessage(): String {
    val fallback = message().ifBlank { "HTTP ${code()}" }
    val rawBody = errorBody()?.string()?.takeIf { it.isNotBlank() } ?: return fallback

    return try {
        val detail = ErrorGson
            .fromJson(rawBody, JsonElement::class.java)
            ?.asJsonObject
            ?.get("detail")
            ?: return fallback

        when {
            detail.isJsonPrimitive -> detail.asString.ifBlank { fallback }
            detail.isJsonArray -> "Request validation failed."
            else -> fallback
        }
    } catch (exc: RuntimeException) {
        fallback
    }
}

private val ErrorGson = Gson()
