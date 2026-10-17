package com.serv.app.core

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

object Perms {
    fun has(ctx: Context, p: String): Boolean =
        ContextCompat.checkSelfPermission(ctx, p) == PackageManager.PERMISSION_GRANTED

    fun missing(ctx: Context, vararg ps: String): List<String> =
        ps.filter { !has(ctx, it) }

    fun describeMissing(ps: List<String>): String =
        if (ps.isEmpty()) "" else "Permissões faltando: ${ps.joinToString(", ")}. Conceda via adb: pm grant com.serv.app <permissão>"
}
