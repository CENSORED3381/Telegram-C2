package com.serv.app.cmds

import com.serv.app.core.Config
import com.serv.app.core.Exec

object ShellCmd {
    fun run(args: String): String {
        if (args.isBlank()) return "uso: /sh <cmd>"
        val r = Exec.sh(args)
        val head = "[exit=${r.code} t=${r.ms}ms]\n"
        return head + Exec.truncate(r.out, Config.MAX_MSG)
    }
}
