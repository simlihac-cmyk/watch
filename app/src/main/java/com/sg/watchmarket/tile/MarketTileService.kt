package com.sg.watchmarket.tile

import android.content.ComponentName
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.sg.watchmarket.MainActivity
import com.sg.watchmarket.data.cache.SharedPreferencesMarketResponseCache
import com.sg.watchmarket.data.dto.QuoteDto
import java.util.Locale
import kotlin.math.abs
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.LayoutElementBuilders.Column
import androidx.wear.protolayout.LayoutElementBuilders.Spacer
import androidx.wear.protolayout.ActionBuilders.launchAction
import androidx.wear.protolayout.ResourceBuilders.Resources
import androidx.wear.protolayout.TimelineBuilders.Timeline
import androidx.wear.protolayout.material3.MaterialScope
import androidx.wear.protolayout.material3.Typography.BODY_LARGE
import androidx.wear.protolayout.material3.Typography.BODY_MEDIUM
import androidx.wear.protolayout.material3.Typography.TITLE_SMALL
import androidx.wear.protolayout.material3.materialScope
import androidx.wear.protolayout.material3.primaryLayout
import androidx.wear.protolayout.material3.text
import androidx.wear.protolayout.material3.textButton
import androidx.wear.protolayout.modifiers.clickable
import androidx.wear.protolayout.types.layoutString
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.RequestBuilders.ResourcesRequest
import androidx.wear.tiles.TileBuilders.Tile
import androidx.wear.tiles.TileService

private const val ResourcesVersion = "1"
private val TileAssetIds = listOf("BTC", "ETH")

class MarketTileService : TileService() {
    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<Tile?> {
        val quotes = SharedPreferencesMarketResponseCache(this)
            .getQuotes(TileAssetIds)
            .orEmpty()

        val timeline = Timeline.fromLayoutElement(
            materialScope(this, requestParams.deviceConfiguration) {
                marketTileLayout(
                    quotes = quotes,
                    openAppClick = clickable(
                        launchAction(
                            ComponentName(
                                packageName,
                                MainActivity::class.java.name,
                            ),
                        ),
                    ),
                )
            },
        )

        return Futures.immediateFuture(
            Tile.Builder()
                .setResourcesVersion(ResourcesVersion)
                .setTileTimeline(timeline)
                .build(),
        )
    }

    override fun onTileResourcesRequest(requestParams: ResourcesRequest): ListenableFuture<Resources> =
        Futures.immediateFuture(
            Resources.Builder()
                .setVersion(ResourcesVersion)
                .build(),
        )
}

private fun MaterialScope.marketTileLayout(
    quotes: List<QuoteDto>,
    openAppClick: androidx.wear.protolayout.ModifiersBuilders.Clickable,
): LayoutElementBuilders.LayoutElement =
    primaryLayout(
        titleSlot = {
            text(
                text = "Market glance".layoutString,
                typography = TITLE_SMALL,
            )
        },
        mainSlot = {
            if (quotes.isEmpty()) {
                emptyTileContent()
            } else {
                quoteTileContent(quotes)
            }
        },
        bottomSlot = {
            textButton(
                onClick = openAppClick,
                labelContent = {
                    text("Open".layoutString)
                },
            )
        },
    )

private fun emptyTileContent(): LayoutElementBuilders.LayoutElement =
    Column.Builder()
        .addContent(
            text(
                text = "Open app".layoutString,
                typography = BODY_LARGE,
            ),
        )
        .addContent(tileSpacer(4f))
        .addContent(
            text(
                text = "to load quotes".layoutString,
                typography = BODY_MEDIUM,
            ),
        )
        .build()

private fun quoteTileContent(quotes: List<QuoteDto>): LayoutElementBuilders.LayoutElement {
    val normalizedQuotes = quotes
        .sortedBy { quote -> TileAssetIds.indexOf(quote.id.uppercase(Locale.US)).takeIf { it >= 0 } ?: 99 }
        .take(2)

    val builder = Column.Builder()
    normalizedQuotes.forEachIndexed { index, quote ->
        if (index > 0) {
            builder.addContent(tileSpacer(5f))
        }
        builder.addContent(
            text(
                text = quote.toTileLine().layoutString,
                typography = BODY_LARGE,
            ),
        )
    }
    return builder.build()
}

private fun tileSpacer(heightDp: Float): LayoutElementBuilders.LayoutElement =
    Spacer.Builder()
        .setHeight(dp(heightDp))
        .build()

private fun QuoteDto.toTileLine(): String {
    val label = display
        .ifBlank { id }
        .substringBefore("/")
        .trim()
        .ifBlank { id }
    return "$label ${formatTilePrice(price, currency)} ${formatTileChange(changeRate24h)}"
}

private fun formatTilePrice(
    price: Double,
    currency: String,
): String {
    val absolutePrice = abs(price)
    val sign = if (price < 0.0) "-" else ""
    if (absolutePrice >= 1_000_000.0) {
        return String.format(Locale.US, "%s%.2fM", sign, absolutePrice / 1_000_000.0)
    }
    if (absolutePrice >= 10_000.0) {
        return String.format(Locale.US, "%s%.1fK", sign, absolutePrice / 1_000.0)
    }
    if (absolutePrice >= 1_000.0) {
        return String.format(Locale.US, "%s%.2fK", sign, absolutePrice / 1_000.0)
    }

    val decimals = when {
        currency.equals("KRW", ignoreCase = true) -> 0
        absolutePrice >= 1.0 -> 2
        absolutePrice >= 0.01 -> 4
        absolutePrice > 0.0 -> 8
        else -> 2
    }
    return String.format(Locale.US, "%,.${decimals}f", price)
}

private fun formatTileChange(changeRate: Double): String =
    String.format(Locale.US, "%+.2f%%", changeRate * 100.0)
