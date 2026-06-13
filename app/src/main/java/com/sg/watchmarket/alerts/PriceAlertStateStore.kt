package com.sg.watchmarket.alerts

import android.content.Context
import java.util.Locale

class PriceAlertStateStore(context: Context) {
    private val preferences = context.getSharedPreferences(
        "watch_market_price_alerts",
        Context.MODE_PRIVATE,
    )

    fun wasSent(
        date: String,
        assetId: String,
        thresholdPercent: Int,
    ): Boolean =
        preferences.getBoolean(alertKey(date, assetId, thresholdPercent), false)

    fun markSent(
        date: String,
        assetId: String,
        thresholdPercents: List<Int>,
    ) {
        preferences.edit().apply {
            thresholdPercents.forEach { thresholdPercent ->
                putBoolean(alertKey(date, assetId, thresholdPercent), true)
            }
        }.apply()
    }

    fun removeOldDates(currentDate: String) {
        preferences.edit().apply {
            preferences.all.keys
                .filter { it.startsWith(AlertPrefix) && !it.startsWith("$AlertPrefix$currentDate:") }
                .forEach(::remove)
        }.apply()
    }

    private fun alertKey(
        date: String,
        assetId: String,
        thresholdPercent: Int,
    ): String = "$AlertPrefix$date:${assetId.uppercase(Locale.US)}:$thresholdPercent"

    private companion object {
        const val AlertPrefix = "sent:"
    }
}
