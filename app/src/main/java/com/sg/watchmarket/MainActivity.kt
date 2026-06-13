package com.sg.watchmarket

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.wear.compose.material.MaterialTheme
import com.sg.watchmarket.ui.screens.AssetDetailScreen
import com.sg.watchmarket.ui.screens.SearchAssetScreen
import com.sg.watchmarket.ui.screens.WatchListScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WatchMarketApp()
        }
    }
}

@Composable
private fun WatchMarketApp() {
    var selectedAssetId by rememberSaveable { mutableStateOf<String?>(null) }
    var isSearching by rememberSaveable { mutableStateOf(false) }
    var listReloadKey by rememberSaveable { mutableStateOf(0) }

    BackHandler(enabled = selectedAssetId != null || isSearching) {
        if (isSearching) {
            isSearching = false
        } else {
            selectedAssetId = null
        }
    }

    MaterialTheme {
        val assetId = selectedAssetId
        if (isSearching) {
            SearchAssetScreen(
                onBack = { isSearching = false },
                onAssetAdded = {
                    isSearching = false
                    listReloadKey += 1
                },
            )
        } else if (assetId == null) {
            WatchListScreen(
                onAssetSelected = { selectedAssetId = it },
                onSearchRequested = { isSearching = true },
                reloadKey = listReloadKey,
            )
        } else {
            AssetDetailScreen(
                assetId = assetId,
                onBack = { selectedAssetId = null },
            )
        }
    }
}
