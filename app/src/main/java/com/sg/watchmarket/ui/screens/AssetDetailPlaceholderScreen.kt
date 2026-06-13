package com.sg.watchmarket.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText

@Composable
fun AssetDetailPlaceholderScreen(
    assetId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        timeText = { TimeText() },
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp, vertical = 28.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = assetId,
                    style = MaterialTheme.typography.title2,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "Detail placeholder",
                    style = MaterialTheme.typography.body2,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "Back",
                    modifier = Modifier
                        .clickable(role = Role.Button, onClick = onBack)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    color = MaterialTheme.colors.primary,
                    style = MaterialTheme.typography.button,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
