package com.sg.watchmarket.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.sg.watchmarket.data.dto.CandleDto
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

private const val MaxRenderedCandles = 120

@Composable
fun CandleChart(
    candles: List<CandleDto>,
    selectedTimeframe: String,
    modifier: Modifier = Modifier,
) {
    val visibleCandles = remember(candles) {
        candles
            .filter { it.hasFinitePrices() }
            .takeLast(MaxRenderedCandles)
    }

    Box(
        modifier = modifier
            .fillMaxWidth(0.88f)
            .height(88.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colors.surface),
        contentAlignment = Alignment.Center,
    ) {
        if (visibleCandles.isEmpty()) {
            Text(
                text = "No $selectedTimeframe candles",
                modifier = Modifier.padding(horizontal = 10.dp),
                color = Color(0xFFB6C2D0),
                style = MaterialTheme.typography.caption1,
                textAlign = TextAlign.Center,
            )
            return@Box
        }

        val risingColor = Color(0xFF4ADE80)
        val fallingColor = Color(0xFFF87171)
        val flatColor = Color(0xFFB6C2D0)
        val gridColor = MaterialTheme.colors.onSurface.copy(alpha = 0.12f)

        Canvas(modifier = Modifier.fillMaxSize()) {
            if (size.width < 4f || size.height < 4f) {
                return@Canvas
            }

            val leftPadding = 8.dp.toPx()
            val rightPadding = 8.dp.toPx()
            val topPadding = 9.dp.toPx()
            val bottomPadding = 9.dp.toPx()
            val chartWidth = size.width - leftPadding - rightPadding
            val chartHeight = size.height - topPadding - bottomPadding

            if (chartWidth <= 1f || chartHeight <= 1f) {
                return@Canvas
            }

            drawLine(
                color = gridColor,
                start = Offset(leftPadding, topPadding),
                end = Offset(leftPadding + chartWidth, topPadding),
                strokeWidth = 1f,
            )
            drawLine(
                color = gridColor,
                start = Offset(leftPadding, topPadding + chartHeight),
                end = Offset(leftPadding + chartWidth, topPadding + chartHeight),
                strokeWidth = 1f,
            )

            val minPrice = visibleCandles.minOf {
                min(min(it.o, it.c), min(it.h, it.l))
            }
            val maxPrice = visibleCandles.maxOf {
                max(max(it.o, it.c), max(it.h, it.l))
            }
            val rawRange = maxPrice - minPrice
            val scalePadding = if (rawRange == 0.0) {
                max(abs(maxPrice) * 0.001, 1.0)
            } else {
                rawRange * 0.05
            }
            val scaledMin = minPrice - scalePadding
            val scaledMax = maxPrice + scalePadding
            val scaledRange = max(scaledMax - scaledMin, 1.0)

            fun priceToY(price: Double): Float {
                val ratio = ((scaledMax - price) / scaledRange).toFloat()
                return (topPadding + ratio * chartHeight)
                    .coerceIn(topPadding, topPadding + chartHeight)
            }

            val count = visibleCandles.size
            val slotWidth = chartWidth / count
            val maxBodyWidth = max(1f, min(6.dp.toPx(), slotWidth * 0.82f))
            val bodyWidth = max(1f, min(slotWidth * 0.62f, maxBodyWidth))
            val wickWidth = max(1f, min(1.4.dp.toPx(), bodyWidth * 0.45f))

            visibleCandles.forEachIndexed { index, candle ->
                val centerX = leftPadding + slotWidth * index + slotWidth / 2f
                val openY = priceToY(candle.o)
                val closeY = priceToY(candle.c)
                val highY = priceToY(candle.h)
                val lowY = priceToY(candle.l)
                val candleColor = when {
                    candle.c > candle.o -> risingColor
                    candle.c < candle.o -> fallingColor
                    else -> flatColor
                }

                drawLine(
                    color = candleColor,
                    start = Offset(centerX, highY),
                    end = Offset(centerX, lowY),
                    strokeWidth = wickWidth,
                    cap = StrokeCap.Round,
                )

                val bodyTop = min(openY, closeY)
                val bodyHeight = max(abs(closeY - openY), 1f)
                drawRect(
                    color = candleColor,
                    topLeft = Offset(centerX - bodyWidth / 2f, bodyTop),
                    size = Size(bodyWidth, bodyHeight),
                )
            }
        }
    }
}

private fun CandleDto.hasFinitePrices(): Boolean =
    o.isFinite() && h.isFinite() && l.isFinite() && c.isFinite()
