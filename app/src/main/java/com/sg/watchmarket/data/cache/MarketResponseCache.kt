package com.sg.watchmarket.data.cache

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.google.gson.reflect.TypeToken
import com.sg.watchmarket.data.dto.CandleResponseDto
import com.sg.watchmarket.data.dto.QuoteDto
import java.lang.reflect.Type
import java.util.Locale

interface MarketResponseCache {
    fun getQuotes(ids: List<String>): List<QuoteDto>?

    fun setQuotes(ids: List<String>, quotes: List<QuoteDto>)

    fun getCandles(
        assetId: String,
        timeframe: String,
        limit: Int,
    ): CandleResponseDto?

    fun setCandles(
        assetId: String,
        timeframe: String,
        response: CandleResponseDto,
    )
}

object NoOpMarketResponseCache : MarketResponseCache {
    override fun getQuotes(ids: List<String>): List<QuoteDto>? = null

    override fun setQuotes(
        ids: List<String>,
        quotes: List<QuoteDto>,
    ) = Unit

    override fun getCandles(
        assetId: String,
        timeframe: String,
        limit: Int,
    ): CandleResponseDto? = null

    override fun setCandles(
        assetId: String,
        timeframe: String,
        response: CandleResponseDto,
    ) = Unit
}

class SharedPreferencesMarketResponseCache(
    context: Context,
    private val gson: Gson = Gson(),
) : MarketResponseCache {
    private val preferences = context.applicationContext.getSharedPreferences(
        CacheName,
        Context.MODE_PRIVATE,
    )

    override fun getQuotes(ids: List<String>): List<QuoteDto>? =
        readJson<List<QuoteDto>>(
            key = quoteKey(ids),
            type = QuoteListType,
        )?.filterByIds(ids)
            ?: readJson<List<QuoteDto>>(
                key = LatestQuotesKey,
                type = QuoteListType,
            )?.filterByIds(ids)

    override fun setQuotes(
        ids: List<String>,
        quotes: List<QuoteDto>,
    ) {
        writeJson(
            key = quoteKey(ids),
            payload = quotes,
        )
        writeJson(
            key = LatestQuotesKey,
            payload = quotes,
        )
    }

    override fun getCandles(
        assetId: String,
        timeframe: String,
        limit: Int,
    ): CandleResponseDto? =
        readJson<CandleResponseDto>(
            key = candleKey(assetId, timeframe),
            type = CandleResponseType,
        )?.let { response ->
            response.copy(candles = response.candles.takeLast(limit.coerceAtLeast(0)))
        }

    override fun setCandles(
        assetId: String,
        timeframe: String,
        response: CandleResponseDto,
    ) {
        writeJson(
            key = candleKey(assetId, timeframe),
            payload = response,
        )
    }

    private fun <T> readJson(
        key: String,
        type: Type,
    ): T? {
        val json = preferences.getString(key, null) ?: return null
        return try {
            gson.fromJson<T>(json, type)
        } catch (exc: JsonParseException) {
            null
        } catch (exc: RuntimeException) {
            null
        }
    }

    private fun writeJson(
        key: String,
        payload: Any,
    ) {
        try {
            preferences
                .edit()
                .putString(key, gson.toJson(payload))
                .apply()
        } catch (exc: RuntimeException) {
            // Cache writes are best effort and should never break market display.
        }
    }

    private companion object {
        const val CacheName = "watch_market_response_cache"
        const val LatestQuotesKey = "quotes:latest"
        val QuoteListType: Type = object : TypeToken<List<QuoteDto>>() {}.type
        val CandleResponseType: Type = object : TypeToken<CandleResponseDto>() {}.type
    }
}

private fun quoteKey(ids: List<String>): String =
    "quotes:" + ids.normalizedIds().joinToString(",")

private fun candleKey(
    assetId: String,
    timeframe: String,
): String = "candles:${assetId.uppercase(Locale.US)}:$timeframe"

private fun List<String>.normalizedIds(): List<String> =
    map { it.trim().uppercase(Locale.US) }
        .filter { it.isNotBlank() }
        .distinct()
        .sorted()

private fun List<QuoteDto>.filterByIds(ids: List<String>): List<QuoteDto> {
    val requestedIds = ids.normalizedIds().toSet()
    return filter { it.id.uppercase(Locale.US) in requestedIds }
}
