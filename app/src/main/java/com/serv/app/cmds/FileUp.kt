package com.serv.app.cmds

import android.content.Context
import com.serv.app.service.BotService

object FileUp {
    fun begin(ctx: Context, dest: String): String {
        if (dest.isBlank()) return "uso: /up <caminho_destino>"
        BotService.pendingUploadPath = dest
        BotService.pendingUploadUntil = System.currentTimeMillis() + 120_000
        return "OK: aguardando documento (120s). Destino: $dest"
    }
}
