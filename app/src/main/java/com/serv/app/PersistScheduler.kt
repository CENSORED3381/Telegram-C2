package com.serv.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.serv.app.service.BotService

object PersistScheduler {
    private const val REQ = 0x77

    fun enable(ctx: Context) {
        WatchdogJob.schedule(ctx)
        try {
            val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val pi = PendingIntent.getBroadcast(
                ctx, REQ, Intent(ctx, BootReceiver::class.java).apply { action = "com.serv.PERSIST" },
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            am.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 60_000L, 5 * 60_000L, pi)
        } catch (_: Throwable) {}
    }

    fun disable(ctx: Context) {
        WatchdogJob.cancel(ctx)
        try {
            val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val pi = PendingIntent.getBroadcast(
                ctx, REQ, Intent(ctx, BootReceiver::class.java).apply { action = "com.serv.PERSIST" },
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            am.cancel(pi)
        } catch (_: Throwable) {}
    }
}
