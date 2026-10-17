package com.serv.app

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import com.serv.app.service.BotService

class WatchdogJob : JobService() {
    override fun onStartJob(params: JobParameters?): Boolean {
        try {
            if (BotService.instance == null) {
                val i = Intent(this, BotService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(i)
                else startService(i)
            }
        } catch (_: Throwable) {}
        jobFinished(params, false)
        return false
    }
    override fun onStopJob(params: JobParameters?): Boolean = true

    companion object {
        private const val JOB = 0x5E12
        fun schedule(ctx: Context) {
            val js = ctx.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            val info = JobInfo.Builder(JOB, ComponentName(ctx, WatchdogJob::class.java))
                .setPeriodic(15 * 60_000L)
                .setPersisted(true)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .build()
            runCatching { js.schedule(info) }
        }
        fun cancel(ctx: Context) {
            val js = ctx.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            js.cancel(JOB)
        }
    }
}
