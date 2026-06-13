package com.sg.watchmarket.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.TimeText
import com.sg.watchmarket.state.MarketUiState
import com.sg.watchmarket.ui.components.StatusMessage

@Composable
fun HomeScreen(
    state: MarketUiState = MarketUiState(),
) {
    MaterialTheme {
        Scaffold(
            timeText = { TimeText() },
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 28.dp),
                contentAlignment = Alignment.Center,
            ) {
                StatusMessage(
                    title = state.appName,
                    message = state.message,
                )
            }
        }
    }
}
