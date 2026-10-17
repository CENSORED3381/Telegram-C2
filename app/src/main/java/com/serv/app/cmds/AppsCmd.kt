package com.serv.app.cmds

import android.content.Context
import android.content.pm.ApplicationInfo

object AppsCmd {
    fun run(ctx: Context): String {
        return try {
            val pm = ctx.packageManager
            val apps = pm.getInstalledApplications(0)
                .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }
                .sortedBy { it.packageName }
            apps.joinToString("\n") { "${it.packageName} (${it.loadLabel(pm)})" }
        } catch (t: Throwable) { "ERRO: ${t.message}" }
    }
}
