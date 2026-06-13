package com.sg.watchmarket.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.ExperimentalWearFoundationApi
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import com.sg.watchmarket.data.cache.SharedPreferencesMarketResponseCache
import com.sg.watchmarket.data.dto.AssetDto
import com.sg.watchmarket.data.dto.QuoteDto
import com.sg.watchmarket.data.repository.FastApiMarketRepository
import com.sg.watchmarket.data.repository.MarketApiResult
import com.sg.watchmarket.data.repository.MarketRepository
import com.sg.watchmarket.state.WatchListItem
import com.sg.watchmarket.state.WatchListUiState
import java.util.Locale
import kotlin.math.abs

@Composable
fun WatchListScreen(
    onAssetSelected: (String) -> Unit,
    onSearchRequested: () -> Unit,
    reloadKey: Int,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current.applicationContext
    val repository = remember(context) {
        FastApiMarketRepository(
            cache = SharedPreferencesMarketResponseCache(context),
        )
    }

    WatchListScreen(
        repository = repository,
        onAssetSelected = onAssetSelected,
        onSearchRequested = onSearchRequested,
        externalReloadKey = reloadKey,
        modifier = modifier,
    )
}

@Composable
fun WatchListScreen(
    repository: MarketRepository,
    onAssetSelected: (String) -> Unit,
    onSearchRequested: () -> Unit,
    externalReloadKey: Int = 0,
    modifier: Modifier = Modifier,
) {
    var state by remember { mutableStateOf<WatchListUiState>(WatchListUiState.Loading) }
    var lastGoodItems by remember { mutableStateOf(emptyList<WatchListItem>()) }
    var reloadKey by remember { mutableStateOf(0) }

    fun staleItems(): List<WatchListItem> =
        lastGoodItems.map { it.copy(isStale = true) }

    fun setError(message: String) {
        state = WatchListUiState.Error(
            message = message,
            staleItems = staleItems(),
        )
    }

    suspend fun loadWatchList() {
        state = if (lastGoodItems.isEmpty()) {
            WatchListUiState.Loading
        } else {
            WatchListUiState.Loaded(
                items = staleItems(),
                isStale = true,
            )
        }

        val assets = when (val result = repository.getAssets()) {
            is MarketApiResult.Success -> result.value
            else -> {
                setError(result.toUserMessage("assets"))
                return
            }
        }
        val assetIds = assets
            .map { it.id.trim().uppercase(Locale.US) }
            .filter { it.isNotBlank() }
            .distinct()
        if (assetIds.isEmpty()) {
            setError("No assets were returned.")
            return
        }

        val quoteLoad = loadQuotesWithFallback(
            repository = repository,
            assetIds = assetIds,
        )
        if (quoteLoad.quotes.isEmpty()) {
            setError(quoteLoad.warning ?: "No quotes were returned.")
            return
        }

        val items = buildWatchListItems(
            assets = assets,
            quotes = quoteLoad.quotes,
            ids = assetIds,
            isStale = quoteLoad.isStale || quoteLoad.warning != null,
        )

        if (items.isEmpty()) {
            setError("No quotes were returned.")
            return
        }

        lastGoodItems = items
        state = if (quoteLoad.warning == null) {
            WatchListUiState.Loaded(
                items = items,
                isStale = quoteLoad.isStale,
            )
        } else {
            WatchListUiState.Error(
                message = quoteLoad.warning,
                staleItems = items.map { it.copy(isStale = true) },
            )
        }
    }

    LaunchedEffect(repository, reloadKey, externalReloadKey) {
        loadWatchList()
    }

    WatchListContent(
        state = state,
        onRetry = { reloadKey += 1 },
        onAssetSelected = onAssetSelected,
        onSearchRequested = onSearchRequested,
        modifier = modifier,
    )
}

@Composable
fun WatchListContent(
    state: WatchListUiState,
    onRetry: () -> Unit,
    onAssetSelected: (String) -> Unit,
    onSearchRequested: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        timeText = { TimeText() },
    ) {
        when (state) {
            WatchListUiState.Loading -> CenterStatus(
                title = "Watch Market",
                message = "Loading quotes",
                modifier = modifier,
            )

            is WatchListUiState.Loaded -> WatchList(
                items = state.items,
                isStale = state.isStale,
                onRetry = onRetry,
                onAssetSelected = onAssetSelected,
                onSearchRequested = onSearchRequested,
                modifier = modifier,
            )

            is WatchListUiState.Error -> {
                if (state.staleItems.isEmpty()) {
                    CenterStatus(
                        title = "Unable to load",
                        message = state.message,
                        action = "Retry",
                        onAction = onRetry,
                        modifier = modifier,
                    )
                } else {
                    WatchList(
                        items = state.staleItems,
                        isStale = true,
                        errorMessage = state.message,
                        onRetry = onRetry,
                        onAssetSelected = onAssetSelected,
                        onSearchRequested = onSearchRequested,
                        modifier = modifier,
                    )
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalWearFoundationApi::class)
private fun WatchList(
    items: List<WatchListItem>,
    isStale: Boolean,
    onRetry: () -> Unit,
    onAssetSelected: (String) -> Unit,
    onSearchRequested: () -> Unit,
    modifier: Modifier = Modifier,
    errorMessage: String? = null,
) {
    ScalingLazyColumn(
        modifier = modifier.fillMaxSize(),
        autoCentering = null,
        contentPadding = PaddingValues(
            start = 12.dp,
            top = 52.dp,
            end = 12.dp,
            bottom = 18.dp,
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        item {
            WatchListHeader(onSearchRequested = onSearchRequested)
        }

        if (isStale) {
            item {
                StatusPill(
                    text = errorMessage ?: "Stale data",
                    action = "Retry",
                    onAction = onRetry,
                )
            }
        }

        items(
            items = items,
            key = { it.id },
        ) { item ->
            WatchListRow(
                item = item,
                onClick = { onAssetSelected(item.id) },
            )
        }
    }
}

@Composable
private fun WatchListHeader(
    onSearchRequested: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth(0.96f)
            .heightIn(min = 28.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Market",
            style = MaterialTheme.typography.title3,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
        Text(
            text = "+",
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .clickable(role = Role.Button, onClick = onSearchRequested)
                .padding(horizontal = 8.dp, vertical = 3.dp),
            color = MaterialTheme.colors.primary,
            style = MaterialTheme.typography.title3,
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun WatchListRow(
    item: WatchListItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val changeColor = when {
        item.changeRate24h > 0.0 -> Color(0xFF4ADE80)
        item.changeRate24h < 0.0 -> Color(0xFFF87171)
        else -> MaterialTheme.colors.onSurface
    }

    Box(
        modifier = modifier
            .fillMaxWidth(0.98f)
            .heightIn(min = 42.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colors.surface)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 7.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = compactAssetDisplay(item.display),
                modifier = Modifier.weight(0.8f),
                style = MaterialTheme.typography.body2,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${formatPrice(item.price, item.currency)} ${item.currency}",
                modifier = Modifier.weight(1.35f),
                color = Color(0xFFE5E7EB),
                style = MaterialTheme.typography.caption1,
                textAlign = TextAlign.End,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            ChangeBadge(
                text = formatChangeRate(item.changeRate24h),
                color = changeColor,
            )
        }
    }
}

@Composable
private fun ChangeBadge(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier
            .clip(RoundedCornerShape(9.dp))
            .background(color.copy(alpha = 0.16f))
            .padding(horizontal = 5.dp, vertical = 2.dp),
        color = color,
        style = MaterialTheme.typography.caption2,
        maxLines = 1,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun StatusPill(
    text: String,
    action: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth(0.94f)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colors.surface)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.caption2,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = action,
            modifier = Modifier
                .clickable(role = Role.Button, onClick = onAction)
                .padding(horizontal = 6.dp, vertical = 4.dp),
            color = MaterialTheme.colors.primary,
            style = MaterialTheme.typography.caption1,
            maxLines = 1,
        )
    }
}

@Composable
private fun CenterStatus(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 28.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.title3,
                textAlign = TextAlign.Center,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.body2,
                textAlign = TextAlign.Center,
            )
            if (action != null && onAction != null) {
                Text(
                    text = action,
                    modifier = Modifier
                        .clickable(role = Role.Button, onClick = onAction)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    color = MaterialTheme.colors.primary,
                    style = MaterialTheme.typography.button,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

private fun buildWatchListItems(
    assets: List<AssetDto>,
    quotes: List<QuoteDto>,
    ids: List<String>,
    isStale: Boolean,
): List<WatchListItem> {
    val assetsById = assets.associateBy { it.id.uppercase(Locale.US) }
    val quotesById = quotes.associateBy { it.id.uppercase(Locale.US) }

    return ids.mapNotNull { requestedId ->
        val id = requestedId.uppercase(Locale.US)
        val asset = assetsById[id]
        val quote = quotesById[id] ?: return@mapNotNull null

        WatchListItem(
            id = id,
            display = asset?.display?.ifBlank { quote.display.ifBlank { id } }
                ?: quote.display.ifBlank { id },
            price = quote.price,
            currency = quote.currency.ifBlank { asset?.currency.orEmpty() },
            changeRate24h = quote.changeRate24h,
            isStale = isStale,
        )
    }
}

private data class QuoteLoadResult(
    val quotes: List<QuoteDto>,
    val isStale: Boolean,
    val warning: String? = null,
)

private suspend fun loadQuotesWithFallback(
    repository: MarketRepository,
    assetIds: List<String>,
): QuoteLoadResult {
    return when (val result = repository.getQuotes(assetIds)) {
        is MarketApiResult.Success -> QuoteLoadResult(
            quotes = result.value,
            isStale = result.isStale,
        )
        else -> {
            val quotes = mutableListOf<QuoteDto>()
            val failedIds = mutableListOf<String>()
            var hasStaleQuote = false

            assetIds.forEach { assetId ->
                when (val singleResult = repository.getQuotes(listOf(assetId))) {
                    is MarketApiResult.Success -> {
                        quotes += singleResult.value
                        hasStaleQuote = hasStaleQuote || singleResult.isStale
                    }
                    else -> failedIds += assetId
                }
            }

            QuoteLoadResult(
                quotes = quotes,
                isStale = hasStaleQuote || quotes.isNotEmpty(),
                warning = if (quotes.isEmpty()) {
                    result.toUserMessage("quotes")
                } else {
                    unavailableQuotesMessage(failedIds)
                },
            )
        }
    }
}

private fun unavailableQuotesMessage(failedIds: List<String>): String =
    if (failedIds.isEmpty()) {
        "Some quotes are unavailable."
    } else {
        "Unavailable: ${failedIds.take(3).joinToString(", ")}"
    }

private fun MarketApiResult<*>.toUserMessage(resource: String): String =
    when (this) {
        is MarketApiResult.Success -> "$resource loaded"
        MarketApiResult.Unauthorized -> "Unauthorized. Check the debug bearer token."
        is MarketApiResult.HttpFailure -> "Could not load $resource: ${message.ifBlank { "HTTP $statusCode" }}."
        is MarketApiResult.NetworkFailure -> "Network failed while loading $resource."
        is MarketApiResult.InvalidResponse -> "Invalid $resource response."
    }

private fun formatPrice(
    price: Double,
    currency: String,
): String {
    val absolutePrice = abs(price)
    val decimals = when {
        currency.equals("KRW", ignoreCase = true) -> 0
        absolutePrice >= 1_000.0 -> 0
        absolutePrice >= 1.0 -> 2
        absolutePrice >= 0.01 -> 4
        absolutePrice > 0.0 -> 8
        else -> 2
    }
    return String.format(Locale.US, "%,.${decimals}f", price)
}

private fun formatChangeRate(changeRate: Double): String =
    String.format(Locale.US, "%+.2f%%", changeRate * 100.0)

private fun compactAssetDisplay(display: String): String =
    display
        .substringBefore("/")
        .trim()
        .ifBlank { display }

@Preview(
    device = "id:wearos_small_round",
    showSystemUi = true,
    backgroundColor = 0xFF000000,
)
@Composable
private fun WatchListContentPreview() {
    MaterialTheme {
        WatchListContent(
            state = WatchListUiState.Loaded(items = previewItems),
            onRetry = {},
            onAssetSelected = {},
            onSearchRequested = {},
        )
    }
}

@Preview(
    device = "id:wearos_small_round",
    showSystemUi = true,
    backgroundColor = 0xFF000000,
)
@Composable
private fun WatchListStaleContentPreview() {
    MaterialTheme {
        WatchListContent(
            state = WatchListUiState.Error(
                message = "Network failed while loading quotes.",
                staleItems = previewItems.map { it.copy(isStale = true) },
            ),
            onRetry = {},
            onAssetSelected = {},
            onSearchRequested = {},
        )
    }
}

private val previewItems = listOf(
    WatchListItem(
        id = "BTC",
        display = "BTC/USDT",
        price = 65000.12,
        currency = "USDT",
        changeRate24h = 0.015,
    ),
    WatchListItem(
        id = "ETH",
        display = "ETH/USDT",
        price = 3450.34,
        currency = "USDT",
        changeRate24h = -0.0062,
    ),
    WatchListItem(
        id = "SOL",
        display = "SOL/USDT",
        price = 142.78,
        currency = "USDT",
        changeRate24h = 0.031,
    ),
    WatchListItem(
        id = "XRP",
        display = "XRP/USDT",
        price = 0.52,
        currency = "USDT",
        changeRate24h = -0.0142,
    ),
)
