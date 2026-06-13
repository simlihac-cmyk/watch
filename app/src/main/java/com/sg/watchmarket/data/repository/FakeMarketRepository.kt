package com.sg.watchmarket.data.repository

import com.sg.watchmarket.data.dto.AssetDto
import com.sg.watchmarket.data.dto.CandleDto
import com.sg.watchmarket.data.dto.CandleResponseDto
import com.sg.watchmarket.data.dto.IndicatorDto
import com.sg.watchmarket.data.dto.QuoteDto

class FakeMarketRepository : MarketRepository {
    private val dynamicAssets = mutableListOf<AssetDto>()

    override suspend fun getAssets(): MarketApiResult<List<AssetDto>> =
        MarketApiResult.Success(fakeAssets + dynamicAssets)

    override suspend fun getQuotes(ids: List<String>): MarketApiResult<List<QuoteDto>> {
        val requestedIds = ids.map { it.uppercase() }.toSet()
        return MarketApiResult.Success(fakeQuotes.filter { it.id in requestedIds })
    }

    override suspend fun getCandles(
        id: String,
        tf: String,
        limit: Int,
    ): MarketApiResult<CandleResponseDto> {
        val normalizedId = id.uppercase()
        val currency = fakeAssets
            .firstOrNull { it.id.uppercase() == normalizedId }
            ?.currency
            ?: "USD"

        return MarketApiResult.Success(
            CandleResponseDto(
                id = normalizedId,
                tf = tf,
                currency = currency,
                source = "server",
                candles = fakeCandles.take(limit.coerceAtLeast(0)),
            ),
        )
    }

    override suspend fun getIndicators(
        id: String,
        tf: String,
    ): MarketApiResult<IndicatorDto> =
        MarketApiResult.Success(
            IndicatorDto(
                id = id.uppercase(),
                tf = tf,
                currency = "USDT",
                rsi14 = 58.7,
                currentCandleVolume = 1_240_000.0,
                volume24h = 803_500_000.0,
                volumeAvg7d = 770_000_000.0,
                volumeAvg30d = 735_000_000.0,
                volumeAvg180d = 710_000_000.0,
                volumeAvg365d = 690_000_000.0,
                volumeCurrency = "USDT",
                timestamp = 1710000000000,
            ),
        )

    override suspend fun searchAssets(query: String): MarketApiResult<List<AssetDto>> {
        val normalizedQuery = query.trim().uppercase()
        if (normalizedQuery.isBlank()) {
            return MarketApiResult.Success(emptyList())
        }

        val configuredMatches = fakeAssets.filter { asset ->
            listOf(asset.id, asset.display, asset.symbol, asset.currency)
                .any { it.uppercase().contains(normalizedQuery) }
        }

        return MarketApiResult.Success(configuredMatches)
    }

    override suspend fun addAsset(asset: AssetDto): MarketApiResult<AssetDto> {
        if ((fakeAssets + dynamicAssets).none { it.id.equals(asset.id, ignoreCase = true) }) {
            dynamicAssets.add(asset.copy(id = asset.id.uppercase()))
        }
        return MarketApiResult.Success(asset)
    }

    override suspend fun removeAsset(id: String): MarketApiResult<Unit> {
        dynamicAssets.removeAll { it.id.equals(id, ignoreCase = true) }
        return MarketApiResult.Success(Unit)
    }

    private companion object {
        val fakeAssets = listOf(
            AssetDto(
                id = "BTC",
                display = "BTC/USDT",
                provider = "binance",
                symbol = "BTCUSDT",
                currency = "USDT",
            ),
            AssetDto(
                id = "ETH",
                display = "ETH/USDT",
                provider = "binance",
                symbol = "ETHUSDT",
                currency = "USDT",
            ),
            AssetDto(
                id = "SOL",
                display = "SOL/USDT",
                provider = "binance",
                symbol = "SOLUSDT",
                currency = "USDT",
            ),
            AssetDto(
                id = "XRP",
                display = "XRP/USDT",
                provider = "binance",
                symbol = "XRPUSDT",
                currency = "USDT",
            ),
        )

        val fakeQuotes = listOf(
            QuoteDto(
                id = "BTC",
                display = "BTC/USDT",
                currency = "USDT",
                price = 65000.12,
                changeRate24h = 0.015,
                timestamp = 1710000000000,
            ),
            QuoteDto(
                id = "ETH",
                display = "ETH/USDT",
                currency = "USDT",
                price = 3450.34,
                changeRate24h = -0.0062,
                timestamp = 1710000000000,
            ),
            QuoteDto(
                id = "SOL",
                display = "SOL/USDT",
                currency = "USDT",
                price = 142.78,
                changeRate24h = 0.031,
                timestamp = 1710000000000,
            ),
            QuoteDto(
                id = "XRP",
                display = "XRP/USDT",
                currency = "USDT",
                price = 0.52,
                changeRate24h = -0.0142,
                timestamp = 1710000000000,
            ),
        )

        val fakeCandles = listOf(
            CandleDto(
                t = 1710000000000,
                o = 65000.0,
                h = 65100.0,
                l = 64900.0,
                c = 65050.0,
                v = 12.5,
            ),
        )
    }
}
