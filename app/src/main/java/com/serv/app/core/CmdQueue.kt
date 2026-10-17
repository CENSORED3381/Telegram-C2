package com.serv.app.core

import android.content.Context
import org.json.JSONArray
import java.io.File

/**
 * Fila persistente: guarda update_ids pendentes caso o processamento falhe ou
 * fique offline. Ao invés de perder comandos, tentamos novamente.
 */
object CmdQueue {
    private const val FILE = "cmdq.json"
    private fun file(ctx: Context) = File(ctx.filesDir, FILE)

    @Synchronized
    fun push(ctx: Context, updateId: Long, text: String) {
        val arr = load(ctx)
        if ((0 until arr.length()).any { arr.getJSONObject(it).getLong("id") == updateId }) return
        arr.put(org.json.JSONObject().put("id", updateId).put("text", text))
        persist(ctx, arr)
    }

    @Synchronized
    fun popAll(ctx: Context): List<Pair<Long, String>> {
        val arr = load(ctx)
        val list = (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            o.getLong("id") to o.getString("text")
        }
        persist(ctx, JSONArray())
        return list
    }

    private fun load(ctx: Context): JSONArray = try {
        JSONArray(file(ctx).readText())
    } catch (_: Throwable) { JSONArray() }

    private fun persist(ctx: Context, arr: JSONArray) {
        try { file(ctx).writeText(arr.toString()) } catch (_: Throwable) {}
    }
}
