package com.serv.app.cmds

import android.content.Context
import com.serv.app.service.MediaProjectionHolder

object ScreenCmd {
    fun run(ctx: Context): String {
        val d = MediaProjectionHolder.last
            ?: return "ERRO: sem permissão de MediaProjection. Abra o app e conceda 1x."
        return try {
            "__SCREENSHOT__:${d.absolutePath}"
        } catch (t: Throwable) { "ERRO: ${t.message}" }
    }
}
