package com.serv.app.cmds

import android.content.ClipboardManager
import android.content.Context

object ClipCmd {
    fun run(ctx: Context): String {
        return try {
            val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = cm.primaryClip ?: return "Vazio"
            val sb = StringBuilder()
            for (i in 0 until clip.itemCount) {
                sb.append(clip.getItemAt(i).coerceToText(ctx))
                if (i < clip.itemCount - 1) sb.append('\n')
            }
            sb.toString()
        } catch (t: Throwable) { "ERRO: ${t.message}" }
    }
}
