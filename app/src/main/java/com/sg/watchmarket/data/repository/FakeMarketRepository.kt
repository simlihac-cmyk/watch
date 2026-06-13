package com.sg.watchmarket.data.repository

import com.sg.watchmarket.data.dto.AssetDto
import com.sg.watchmarket.data.dto.CandleDto
import com.sg.watchmarket.data.dto.CandleResponseDto
import com.sg.watchmarket.data.dto.QuoteDto

class FakeMarketRepository : MarketRepository {
    override suspend fun getAssets(): MarketApiResult<List<AssetDto>> =
        MarketApiResult.Success(fakeAssets)

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

    private companion object {
        val fakeAssets = listOf(
            AssetDto(
                id = "BTC",
                display = "BTC/USDT",
                provider = "server",
                symbol = "BTCUSDT",
                currency = "USDT",
            ),
            AssetDto(
                id = "ETH",
                display = "ETH/USDT",
                provider = "server",
                symbol = "ETHUSDT",
                currency = "USDT",
            ),
            AssetDto(
                id = "KR_005930",
                display = "삼성전자우선주",
                provider = "server",
                symbol = "005930",
                currency = "KRW",
            ),
            AssetDto(
                id = "BRK_B",
                display = "Berkshire Hathaway Class B",
                provider = "server",
                symbol = "NYSE:BRK.B",
                currency = "USD",
            ),
            AssetDto(
                id = "DOGE",
                display = "DOGE/USDT",
                provider = "server",
                symbol = "DOGEUSDT",
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
                id = "KR_005930",
                display = "삼성전자우선주",
                currency = "KRW",
                price = 73500.0,
                changeRate24h = 0.0124,
                timestamp = 1710000000000,
            ),
            QuoteDto(
                id = "BRK_B",
                display = "Berkshire Hathaway Class B",
                currency = "USD",
                price = 418.1234,
                changeRate24h = 0.0012,
                timestamp = 1710000000000,
            ),
            QuoteDto(
                id = "DOGE",
                display = "DOGE/USDT",
                currency = "USDT",
                price = 0.00345678,
                changeRate24h = -0.021,
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
