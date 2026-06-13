package com.sg.watchmarket.alerts

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.sg.watchmarket.MainActivity
import com.sg.watchmarket.R
import com.sg.watchmarket.config.WatchMarketConfig
import com.sg.watchmarket.data.dto.AssetDto
import com.sg.watchmarket.data.dto.QuoteDto
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import kotlin.math.absoluteValue

object PriceAlertRunner {
    private val gson = Gson()
    private val assetListType = object : TypeToken<List<AssetDto>>() {}.type
    private val quoteListType = object : TypeToken<List<QuoteDto>>() {}.type
    private val thresholds = listOf(5, 10, 15)

    fun checkNow(context: Context) {
        val appContext = context.applicationContext
        val baseUrl = WatchMarketConfig.serverBaseUrl.trim()
        val bearerToken = WatchMarketConfig.bearerToken.trim()
        if (baseUrl.isBlank() || bearerToken.isBlank()) {
            return
        }

        val assets = requestJson<List<AssetDto>>(
            path = "v1/assets",
            baseUrl = baseUrl,
            bearerToken = bearerToken,
            type = assetListType,
        ).orEmpty()
        val ids = assets
            .filter { it.provider.equals("binance", ignoreCase = true) }
            .map { it.id.trim().uppercase(Locale.US) }
            .filter { it.isNotBlank() }
            .distinct()
        if (ids.isEmpty()) {
            return
        }

        val encodedIds = URLEncoder.encode(ids.joinToString(","), Charsets.UTF_8.name())
        val quotes = requestJson<List<QuoteDto>>(
            path = "v1/quotes?ids=$encodedIds",
            baseUrl = baseUrl,
            bearerToken = bearerToken,
            type = quoteListType,
        ).orEmpty()
        if (quotes.isEmpty()) {
            return
        }

        val today = LocalDate.now(ZoneId.systemDefault()).toString()
        val stateStore = PriceAlertStateStore(appContext)
        stateStore.removeOldDates(today)

        quotes.forEach { quote ->
            maybeNotifyForQuote(
                context = appContext,
                stateStore = stateStore,
                date = today,
                quote = quote,
            )
        }
    }

    private fun maybeNotifyForQuote(
        context: Context,
        stateStore: PriceAlertStateStore,
        date: String,
        quote: QuoteDto,
    ) {
        val absoluteRate = quote.changeRate24h.absoluteValue
        val crossedThresholds = thresholds.filter { absoluteRate >= it / 100.0 }
        if (crossedThresholds.isEmpty()) {
            return
        }

        val unsentThresholds = crossedThresholds.filterNot { thresholdPercent ->
            stateStore.wasSent(date, quote.id, thresholdPercent)
        }
        val alertThreshold = unsentThresholds.maxOrNull() ?: return

        if (sendNotification(context, quote, alertThreshold)) {
            stateStore.markSent(
                date = date,
                assetId = quote.id,
                thresholdPercents = crossedThresholds,
            )
        }
    }

    private fun sendNotification(
        context: Context,
        quote: QuoteDto,
        thresholdPercent: Int,
    ): Boolean {
        if (!canPostNotifications(context)) {
            return false
        }

        val notificationManager = context.getSystemService(NotificationManager::class.java)
            ?: return false
        ensureChannel(notificationManager)

        val display = quote.display.substringBefore("/").ifBlank { quote.id }
        val signedRate = String.format(Locale.US, "%+.2f%%", quote.changeRate24h * 100.0)
        val price = formatPrice(quote.price, quote.currency)
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = Notification.Builder(context, ChannelId)
            .setSmallIcon(R.drawable.ic_watch_market_tile)
            .setContentTitle("$display $signedRate")
            .setContentText("$price ${quote.currency} crossed $thresholdPercent%")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setLocalOnly(true)
            .setCategory(Notification.CATEGORY_STATUS)
            .build()

        notificationManager.notify(notificationId(quote.id, thresholdPercent), notification)
        return true
    }

    private fun canPostNotifications(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    private fun ensureChannel(notificationManager: NotificationManager) {
        val channel = NotificationChannel(
            ChannelId,
            "Price alerts",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Alerts when a crypto asset moves 5%, 10%, or 15% in 24 hours."
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun notificationId(
        assetId: String,
        thresholdPercent: Int,
    ): Int = "$assetId:$thresholdPercent".hashCode() and Int.MAX_VALUE

    private fun formatPrice(
        price: Double,
        currency: String,
    ): String {
        val decimals = when {
            currency.equals("KRW", ignoreCase = true) -> 0
            price.absoluteValue >= 1_000.0 -> 0
            price.absoluteValue >= 1.0 -> 2
            price.absoluteValue >= 0.01 -> 4
            price.absoluteValue > 0.0 -> 8
            else -> 2
        }
        return String.format(Locale.US, "%,.${decimals}f", price)
    }

    private fun <T> requestJson(
        path: String,
        baseUrl: String,
        bearerToken: String,
        type: java.lang.reflect.Type,
    ): T? {
        val endpoint = "${baseUrl.trimEnd('/')}/${path.trimStart('/')}"
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 10_000
            setRequestProperty("Authorization", "Bearer $bearerToken")
            setRequestProperty("Accept", "application/json")
        }

        return try {
            if (connection.responseCode !in 200..299) {
                null
            } else {
                connection.inputStream.bufferedReader().use { reader ->
                    gson.fromJson(reader, type)
                }
            }
        } catch (exc: Exception) {
            null
        } finally {
            connection.disconnect()
        }
    }

    private const val ChannelId = "price_alerts"
}
