package com.serv.app.cmds

import java.io.File

object FileDown {
    fun run(path: String): String {
        if (path.isBlank()) return "uso: /dl <caminho>"
        val f = File(path)
        if (!f.exists()) return "ERRO: não existe: $path"
        if (!f.canRead()) return "ERRO: sem permissão de leitura"
        if (f.isDirectory) return "ERRO: é diretório"
        if (f.length() > 45L * 1024 * 1024) return "ERRO: >45MB (limite do BotAPI)"
        return "__FILE__:${f.absolutePath}"
    }
}
