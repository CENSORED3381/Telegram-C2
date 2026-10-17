package com.serv.app.core

import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

object Exec {
    data class Result(val out: String, val code: Int, val ms: Long)

    fun sh(cmd: String, timeoutSec: Long = 60): Result {
        val start = System.currentTimeMillis()
        return try {
            val pb = ProcessBuilder().apply {
                command("/system/bin/sh", "-c", cmd)
                redirectErrorStream(true)
            }
            val p = pb.start()
            val buf = StringBuilder()
            p.inputStream.bufferedReader().use { r: BufferedReader ->
                var line = r.readLine()
                while (line != null) { buf.append(line).append('\n'); line = r.readLine() }
            }
            val ok = p.waitFor(timeoutSec, TimeUnit.SECONDS)
            if (!ok) { p.destroyForcibly(); return Result(buf.toString() + "\n[timeout]", -1, System.currentTimeMillis() - start) }
            Result(buf.toString(), p.exitValue(), System.currentTimeMillis() - start)
        } catch (t: Throwable) {
            Result("ERRO exec: ${t.message}", -1, System.currentTimeMillis() - start)
        }
    }

    fun truncate(s: String, max: Int): String =
        if (s.length <= max) s else s.substring(0, max) + "\n…[truncado ${s.length - max} chars]"

    fun listFilesRecursive(root: File, filter: (File) -> Boolean = { it.isFile }): Sequence<File> {
        return sequence {
            if (!root.exists()) return@sequence
            val stack = ArrayDeque<File>().apply { add(root) }
            while (stack.isNotEmpty()) {
                val f = stack.removeLast()
                if (f.isDirectory) f.listFiles()?.forEach { stack.add(it) }
                else if (filter(f)) yield(f)
            }
        }
    }
}
