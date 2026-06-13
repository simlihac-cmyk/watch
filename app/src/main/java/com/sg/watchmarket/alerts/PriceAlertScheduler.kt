package com.sg.watchmarket.alerts

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context

object PriceAlertScheduler {
    fun schedule(context: Context) {
        val appContext = context.applicationContext
        val scheduler = appContext.getSystemService(JobScheduler::class.java) ?: return
        val component = ComponentName(appContext, PriceAlertJobService::class.java)
        val jobInfo = JobInfo.Builder(JobId, component)
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            .setPeriodic(CheckIntervalMillis)
            .setPersisted(true)
            .build()

        scheduler.schedule(jobInfo)
    }

    fun runOnce(context: Context) {
        val appContext = context.applicationContext
        Thread {
            PriceAlertRunner.checkNow(appContext)
        }.apply {
            name = "watch-market-price-alerts-once"
            start()
        }
    }

    private const val JobId = 5017
    private const val CheckIntervalMillis = 15L * 60L * 1000L
}
