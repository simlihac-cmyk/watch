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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.ExperimentalWearFoundationApi
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import com.sg.watchmarket.data.cache.SharedPreferencesMarketResponseCache
import com.sg.watchmarket.data.dto.CandleResponseDto
import com.sg.watchmarket.data.dto.IndicatorDto
import com.sg.watchmarket.data.repository.FastApiMarketRepository
import com.sg.watchmarket.data.repository.MarketApiResult
import com.sg.watchmarket.data.repository.MarketRepository
import com.sg.watchmarket.state.AssetDetailData
import com.sg.watchmarket.state.AssetDetailUiState
import com.sg.watchmarket.ui.components.CandleChart
import java.util.Locale
import kotlin.math.abs

private const val CandleLimit = 60
private val SupportedTimeframes = listOf("5m", "1h", "1d", "1w")

@Composable
fun AssetDetailScreen(
    assetId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current.applicationContext
    val repository = remember(context) {
        FastApiMarketRepository(
            cache = SharedPreferencesMarketResponseCache(context),
        )
    }

    AssetDetailScreen(
        assetId = assetId,
        repository = repository,
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
fun AssetDetailScreen(
    assetId: String,
    repository: MarketRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val normalizedAssetId = remember(assetId) { assetId.uppercase(Locale.US) }
    var selectedTimeframe by remember(normalizedAssetId) {
        mutableStateOf(SupportedTimeframes.first())
    }
    var display by remember(normalizedAssetId) { mutableStateOf(normalizedAssetId) }
    var state by remember(normalizedAssetId) {
        mutableStateOf<AssetDetailUiState>(AssetDetailUiState.Loading)
    }
    var lastGoodData by remember(normalizedAssetId) { mutableStateOf<AssetDetailData?>(null) }
    var retryKey by remember { mutableStateOf(0) }

    LaunchedEffect(repository, normalizedAssetId) {
        val resolvedDisplay = when (val result = repository.getAssets()) {
            is MarketApiResult.Success -> {
                result.value
                    .firstOrNull { it.id.equals(normalizedAssetId, ignoreCase = true) }
                    ?.display
                    ?.ifBlank { normalizedAssetId }
                    ?: normalizedAssetId
            }
            else -> normalizedAssetId
        }

        display = resolvedDisplay
        lastGoodData = lastGoodData?.copy(display = resolvedDisplay)
        state = when (val currentState = state) {
            AssetDetailUiState.Loading -> currentState
            is AssetDetailUiState.Loaded -> AssetDetailUiState.Loaded(
                data = currentState.data.copy(display = resolvedDisplay),
            )
            is AssetDetailUiState.Error -> currentState.copy(
                staleData = currentState.staleData?.copy(display = resolvedDisplay),
            )
        }
    }

    LaunchedEffect(repository, normalizedAssetId, selectedTimeframe, retryKey) {
        val staleData = lastGoodData
            ?.takeIf { it.timeframe == selectedTimeframe }
            ?.copy(
                display = display,
                isStale = true,
            )
        state = staleData?.let { AssetDetailUiState.Loaded(it) } ?: AssetDetailUiState.Loading

        when (val result = repository.getCandles(normalizedAssetId, selectedTimeframe, CandleLimit)) {
            is MarketApiResult.Success -> {
                val detailData = result.value.toAssetDetailData(
                    assetId = normalizedAssetId,
                    display = display,
                    fallbackTimeframe = selectedTimeframe,
                )?.copy(isStale = result.isStale)

                if (detailData == null) {
                    state = AssetDetailUiState.Error(
                        message = "No candles were returned.",
                        staleData = staleData,
                    )
                } else {
                    lastGoodData = detailData
                    state = AssetDetailUiState.Loaded(detailData)

                    val indicatorData = when (
                        val indicatorResult = repository.getIndicators(
                            normalizedAssetId,
                            selectedTimeframe,
                        )
                    ) {
                        is MarketApiResult.Success -> detailData.copy(
                            indicators = indicatorResult.value,
                            indicatorMessage = null,
                        )
                        else -> detailData.copy(
                            indicatorMessage = "Unavailable",
                        )
                    }
                    lastGoodData = indicatorData
                    state = AssetDetailUiState.Loaded(indicatorData)
                }
            }
            else -> {
                state = AssetDetailUiState.Error(
                    message = result.toUserMessage("candles"),
                    staleData = staleData,
                )
            }
        }
    }

    AssetDetailContent(
        assetId = normalizedAssetId,
        display = display,
        selectedTimeframe = selectedTimeframe,
        state = state,
        onTimeframeSelected = { timeframe ->
            if (timeframe != selectedTimeframe) {
                selectedTimeframe = timeframe
            }
        },
        onRetry = { retryKey += 1 },
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
fun AssetDetailContent(
    assetId: String,
    display: String,
    selectedTimeframe: String,
    state: AssetDetailUiState,
    onTimeframeSelected: (String) -> Unit,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        timeText = { TimeText() },
    ) {
        when (state) {
            AssetDetailUiState.Loading -> DetailLoading(
                assetId = assetId,
                display = display,
                selectedTimeframe = selectedTimeframe,
                onTimeframeSelected = onTimeframeSelected,
                onBack = onBack,
                modifier = modifier,
            )

            is AssetDetailUiState.Loaded -> DetailLoaded(
                data = state.data,
                selectedTimeframe = selectedTimeframe,
                onTimeframeSelected = onTimeframeSelected,
                onRetry = onRetry,
                onBack = onBack,
                modifier = modifier,
            )

            is AssetDetailUiState.Error -> DetailError(
                assetId = assetId,
                display = display,
                selectedTimeframe = selectedTimeframe,
                message = state.message,
                staleData = state.staleData,
                onTimeframeSelected = onTimeframeSelected,
                onRetry = onRetry,
                onBack = onBack,
                modifier = modifier,
            )
        }
    }
}

@Composable
@OptIn(ExperimentalWearFoundationApi::class)
private fun DetailLoading(
    assetId: String,
    display: String,
    selectedTimeframe: String,
    onTimeframeSelected: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ScalingLazyColumn(
        modifier = modifier.fillMaxSize(),
        autoCentering = null,
        contentPadding = DetailContentPadding,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        item {
            DetailHeader(
                display = display,
                onBack = onBack,
            )
        }
        item {
            TimeframeSelector(
                selectedTimeframe = selectedTimeframe,
                onTimeframeSelected = onTimeframeSelected,
            )
        }
        item {
            CenterText(
                title = "Loading",
                message = "Fetching $selectedTimeframe candles",
            )
        }
    }
}

@Composable
@OptIn(ExperimentalWearFoundationApi::class)
private fun DetailLoaded(
    data: AssetDetailData,
    selectedTimeframe: String,
    onTimeframeSelected: (String) -> Unit,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    statusMessage: String? = null,
) {
    ScalingLazyColumn(
        modifier = modifier.fillMaxSize(),
        autoCentering = null,
        contentPadding = DetailContentPadding,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        item {
            DetailHeader(
                display = data.display,
                onBack = onBack,
            )
        }
        item {
            CandleChart(
                candles = data.candles,
                selectedTimeframe = selectedTimeframe,
                heightDp = 96,
            )
        }
        if (data.isStale) {
            item {
                StatusPill(
                    text = statusMessage ?: "Stale data",
                    action = "Retry",
                    onAction = onRetry,
                )
            }
        }
        item {
            TimeframeSelector(
                selectedTimeframe = selectedTimeframe,
                onTimeframeSelected = onTimeframeSelected,
            )
        }
        item {
            PriceSummary(data = data)
        }
        item {
            IndicatorPanel(
                indicators = data.indicators,
                message = data.indicatorMessage,
            )
        }
    }
}

@Composable
@OptIn(ExperimentalWearFoundationApi::class)
private fun DetailError(
    assetId: String,
    display: String,
    selectedTimeframe: String,
    message: String,
    staleData: AssetDetailData?,
    onTimeframeSelected: (String) -> Unit,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (staleData != null) {
        DetailLoaded(
            data = staleData,
            selectedTimeframe = selectedTimeframe,
            onTimeframeSelected = onTimeframeSelected,
            onRetry = onRetry,
            onBack = onBack,
            modifier = modifier,
            statusMessage = message,
        )
        return
    }

    ScalingLazyColumn(
        modifier = modifier.fillMaxSize(),
        autoCentering = null,
        contentPadding = DetailContentPadding,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        item {
            DetailHeader(
                display = display,
                onBack = onBack,
            )
        }
        item {
            TimeframeSelector(
                selectedTimeframe = selectedTimeframe,
                onTimeframeSelected = onTimeframeSelected,
            )
        }
        item {
            CenterText(
                title = "Unable to load",
                message = message,
                action = "Retry",
                onAction = onRetry,
            )
        }
    }
}

@Composable
private fun DetailHeader(
    display: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth(0.94f)
            .heightIn(min = 42.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Back",
            modifier = Modifier
                .align(Alignment.CenterStart)
                .clickable(role = Role.Button, onClick = onBack)
                .padding(horizontal = 4.dp, vertical = 4.dp),
            color = MaterialTheme.colors.primary,
            style = MaterialTheme.typography.caption1,
            maxLines = 1,
        )
        Column(
            modifier = Modifier.fillMaxWidth(0.78f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = compactAssetDisplay(display),
                style = MaterialTheme.typography.body1,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun PriceSummary(
    data: AssetDetailData,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth(0.94f)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colors.surface)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "${formatPrice(data.latestClose, data.currency)} ${data.currency}",
            style = MaterialTheme.typography.title3,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun IndicatorPanel(
    indicators: IndicatorDto?,
    message: String?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth(0.94f)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colors.surface)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(
            text = "Indicators",
            style = MaterialTheme.typography.caption1,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        if (indicators == null) {
            Text(
                text = message ?: "Loading",
                style = MaterialTheme.typography.caption2,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.72f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        } else {
            IndicatorRow("RSI 14", String.format(Locale.US, "%.1f", indicators.rsi14))
            IndicatorRow("24h Vol", formatVolume(indicators.volume24h, indicators.volumeCurrency))
            IndicatorRow("Now Vol", formatVolume(indicators.currentCandleVolume, indicators.volumeCurrency))
            IndicatorRow("Avg 7d", formatVolume(indicators.volumeAvg7d, indicators.volumeCurrency))
            IndicatorRow("Avg 30d", formatVolume(indicators.volumeAvg30d, indicators.volumeCurrency))
            IndicatorRow("Avg 6m", formatVolume(indicators.volumeAvg180d, indicators.volumeCurrency))
            IndicatorRow("Avg 1y", formatVolume(indicators.volumeAvg365d, indicators.volumeCurrency))
        }
    }
}

@Composable
private fun IndicatorRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(0.75f),
            style = MaterialTheme.typography.caption2,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.72f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = value,
            modifier = Modifier.weight(1.25f),
            style = MaterialTheme.typography.caption2,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun TimeframeSelector(
    selectedTimeframe: String,
    onTimeframeSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(0.94f),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SupportedTimeframes.forEach { timeframe ->
            val selected = timeframe == selectedTimeframe
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (selected) {
                            MaterialTheme.colors.primary
                        } else {
                            MaterialTheme.colors.surface
                        },
                    )
                    .clickable(role = Role.Button) {
                        onTimeframeSelected(timeframe)
                    }
                    .padding(vertical = if (selected) 8.dp else 7.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = timeframe,
                    color = if (selected) {
                        MaterialTheme.colors.onPrimary
                    } else {
                        MaterialTheme.colors.onSurface
                    },
                    style = MaterialTheme.typography.caption1,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
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
private fun CenterText(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth(0.94f)
            .padding(horizontal = 8.dp, vertical = 20.dp),
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

private fun CandleResponseDto.toAssetDetailData(
    assetId: String,
    display: String,
    fallbackTimeframe: String,
): AssetDetailData? {
    val latest = candles.lastOrNull() ?: return null

    return AssetDetailData(
        assetId = assetId,
        display = display,
        timeframe = tf.ifBlank { fallbackTimeframe },
        currency = currency,
        latestClose = latest.c,
        latestTimestamp = latest.t,
        candleCount = candles.size,
        candles = candles.takeLast(CandleLimit),
    )
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

private fun formatVolume(
    volume: Double,
    currency: String,
): String {
    val absoluteVolume = abs(volume)
    val sign = if (volume < 0.0) "-" else ""
    val compactValue = when {
        absoluteVolume >= 1_000_000_000_000.0 ->
            String.format(Locale.US, "%s%.2fT", sign, absoluteVolume / 1_000_000_000_000.0)
        absoluteVolume >= 1_000_000_000.0 ->
            String.format(Locale.US, "%s%.2fB", sign, absoluteVolume / 1_000_000_000.0)
        absoluteVolume >= 1_000_000.0 ->
            String.format(Locale.US, "%s%.2fM", sign, absoluteVolume / 1_000_000.0)
        absoluteVolume >= 1_000.0 ->
            String.format(Locale.US, "%s%.2fK", sign, absoluteVolume / 1_000.0)
        else -> String.format(Locale.US, "%,.2f", volume)
    }

    return if (currency.isBlank()) {
        compactValue
    } else {
        "$compactValue $currency"
    }
}

private fun compactAssetDisplay(display: String): String =
    display
        .substringBefore("/")
        .trim()
        .ifBlank { display }

private val DetailContentPadding = PaddingValues(
    start = 16.dp,
    top = 42.dp,
    end = 16.dp,
    bottom = 20.dp,
)

@Preview(
    device = "id:wearos_small_round",
    showSystemUi = true,
    backgroundColor = 0xFF000000,
)
@Composable
private fun AssetDetailContentPreview() {
    MaterialTheme {
        AssetDetailContent(
            assetId = "BTC",
            display = "BTC/USDT",
            selectedTimeframe = "5m",
            state = AssetDetailUiState.Loaded(
                data = previewDetailData,
            ),
            onTimeframeSelected = {},
            onRetry = {},
            onBack = {},
        )
    }
}

@Preview(
    device = "id:wearos_small_round",
    showSystemUi = true,
    backgroundColor = 0xFF000000,
)
@Composable
private fun AssetDetailErrorContentPreview() {
    MaterialTheme {
        AssetDetailContent(
            assetId = "BTC",
            display = "BTC/USDT",
            selectedTimeframe = "1h",
            state = AssetDetailUiState.Error(
                message = "Could not load candles: Unsupported timeframe: 1h.",
            ),
            onTimeframeSelected = {},
            onRetry = {},
            onBack = {},
        )
    }
}

private val previewDetailData = AssetDetailData(
    assetId = "BTC",
    display = "BTC/USDT",
    timeframe = "5m",
    currency = "USDT",
    latestClose = 65020.0,
    latestTimestamp = 1710000000000,
    candleCount = 60,
    indicators = com.sg.watchmarket.data.dto.IndicatorDto(
        id = "BTC",
        tf = "5m",
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
    candles = listOf(
        com.sg.watchmarket.data.dto.CandleDto(
            t = 1710000000000,
            o = 64900.0,
            h = 65150.0,
            l = 64880.0,
            c = 65050.0,
            v = 12.5,
        ),
        com.sg.watchmarket.data.dto.CandleDto(
            t = 1710000300000,
            o = 65050.0,
            h = 65220.0,
            l = 65010.0,
            c = 65180.0,
            v = 10.2,
        ),
        com.sg.watchmarket.data.dto.CandleDto(
            t = 1710000600000,
            o = 65180.0,
            h = 65200.0,
            l = 64970.0,
            c = 65020.0,
            v = 9.7,
        ),
    ),
)
