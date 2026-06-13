package com.sg.watchmarket.alerts

import android.app.job.JobParameters
import android.app.job.JobService

class PriceAlertJobService : JobService() {
    @Volatile
    private var workerThread: Thread? = null

    override fun onStartJob(params: JobParameters): Boolean {
        workerThread = Thread {
            try {
                PriceAlertRunner.checkNow(applicationContext)
            } finally {
                jobFinished(params, false)
            }
        }.apply {
            name = "watch-market-price-alerts"
            start()
        }
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        workerThread?.interrupt()
        workerThread = null
        return true
    }
}
