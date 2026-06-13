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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
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
import com.sg.watchmarket.data.repository.FakeMarketRepository
import com.sg.watchmarket.data.repository.FastApiMarketRepository
import com.sg.watchmarket.data.repository.MarketApiResult
import com.sg.watchmarket.data.repository.MarketRepository
import kotlinx.coroutines.launch

@Composable
fun SearchAssetScreen(
    onBack: () -> Unit,
    onAssetAdded: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current.applicationContext
    val repository = remember(context) {
        FastApiMarketRepository(
            cache = SharedPreferencesMarketResponseCache(context),
        )
    }

    SearchAssetScreen(
        repository = repository,
        onBack = onBack,
        onAssetAdded = onAssetAdded,
        modifier = modifier,
    )
}

@Composable
fun SearchAssetScreen(
    repository: MarketRepository,
    onBack: () -> Unit,
    onAssetAdded: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by remember { mutableStateOf("") }
    var searchKey by remember { mutableIntStateOf(0) }
    var results by remember { mutableStateOf(emptyList<AssetDto>()) }
    var isLoading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("Ticker or code") }
    var addingAssetId by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(searchKey) {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isBlank()) {
            results = emptyList()
            message = "Ticker or code"
            return@LaunchedEffect
        }

        isLoading = true
        message = "Searching"
        when (val result = repository.searchAssets(trimmedQuery)) {
            is MarketApiResult.Success -> {
                results = result.value
                message = if (result.value.isEmpty()) "No match" else "Tap to add"
            }
            else -> {
                results = emptyList()
                message = result.toUserMessage("search")
            }
        }
        isLoading = false
    }

    fun addAsset(asset: AssetDto) {
        coroutineScope.launch {
            addingAssetId = asset.id
            message = "Adding"
            when (val result = repository.addAsset(asset)) {
                is MarketApiResult.Success -> onAssetAdded()
                else -> message = result.toUserMessage("asset")
            }
            addingAssetId = null
        }
    }

    Scaffold(
        timeText = { TimeText() },
    ) {
        SearchAssetContent(
            query = query,
            onQueryChange = { query = it },
            results = results,
            message = message,
            isLoading = isLoading,
            addingAssetId = addingAssetId,
            onSearch = { searchKey += 1 },
            onAddAsset = ::addAsset,
            onBack = onBack,
            modifier = modifier,
        )
    }
}

@Composable
@OptIn(ExperimentalWearFoundationApi::class)
private fun SearchAssetContent(
    query: String,
    onQueryChange: (String) -> Unit,
    results: List<AssetDto>,
    message: String,
    isLoading: Boolean,
    addingAssetId: String?,
    onSearch: () -> Unit,
    onAddAsset: (AssetDto) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ScalingLazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 14.dp,
            top = 34.dp,
            end = 14.dp,
            bottom = 24.dp,
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            SearchHeader(onBack = onBack)
        }
        item {
            SearchInput(
                query = query,
                onQueryChange = onQueryChange,
                onSearch = onSearch,
            )
        }
        item {
            Text(
                text = if (isLoading) "Searching" else message,
                color = Color(0xFFB6C2D0),
                style = MaterialTheme.typography.caption2,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        items(
            items = results,
            key = { "${it.provider}:${it.symbol}:${it.id}" },
        ) { asset ->
            SearchResultRow(
                asset = asset,
                isAdding = addingAssetId == asset.id,
                onClick = { onAddAsset(asset) },
            )
        }
    }
}

@Composable
private fun SearchHeader(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth(0.94f)
            .heightIn(min = 34.dp),
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
        Text(
            text = "Add",
            style = MaterialTheme.typography.title3,
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun SearchInput(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth(0.96f)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colors.surface)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            singleLine = true,
            textStyle = TextStyle(
                color = MaterialTheme.colors.onSurface,
                fontSize = MaterialTheme.typography.body1.fontSize,
            ),
            cursorBrush = SolidColor(MaterialTheme.colors.primary),
            decorationBox = { innerTextField ->
                if (query.isBlank()) {
                    Text(
                        text = "SPCX",
                        color = Color(0xFF748096),
                        style = MaterialTheme.typography.body1,
                        maxLines = 1,
                    )
                }
                innerTextField()
            },
        )
        Text(
            text = "Go",
            modifier = Modifier
                .clickable(role = Role.Button, onClick = onSearch)
                .padding(horizontal = 5.dp, vertical = 4.dp),
            color = MaterialTheme.colors.primary,
            style = MaterialTheme.typography.button,
            maxLines = 1,
        )
    }
}

@Composable
private fun SearchResultRow(
    asset: AssetDto,
    isAdding: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth(0.96f)
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colors.surface)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 9.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = compactAssetDisplay(asset.display),
                    style = MaterialTheme.typography.body1,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${asset.symbol} ${asset.currency}",
                    color = Color(0xFFB6C2D0),
                    style = MaterialTheme.typography.caption2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = if (isAdding) "..." else "+",
                color = MaterialTheme.colors.primary,
                style = MaterialTheme.typography.title3,
                maxLines = 1,
                textAlign = TextAlign.End,
            )
        }
    }
}

private fun MarketApiResult<*>.toUserMessage(resource: String): String =
    when (this) {
        is MarketApiResult.Success -> "$resource ok"
        MarketApiResult.Unauthorized -> "Unauthorized"
        is MarketApiResult.HttpFailure -> message.ifBlank { "HTTP $statusCode" }
        is MarketApiResult.NetworkFailure -> "Network failed"
        is MarketApiResult.InvalidResponse -> "Invalid response"
    }

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
private fun SearchAssetContentPreview() {
    MaterialTheme {
        SearchAssetScreen(
            repository = FakeMarketRepository(),
            onBack = {},
            onAssetAdded = {},
        )
    }
}
