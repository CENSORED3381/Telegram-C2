package com.serv.app.core

import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

object Http {
    private const val UA = "Mozilla/5.0 (Linux; Android 12) ServSync/1.0"

    fun get(url: String, connectMs: Int = 15000, readMs: Int = 45000): String {
        val c = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = connectMs
            readTimeout = readMs
            setRequestProperty("User-Agent", UA)
            setRequestProperty("Accept", "application/json")
        }
        return try {
            val code = c.responseCode
            val stream = if (code in 200..299) c.inputStream else c.errorStream
            stream?.use { it.readText() } ?: ""
        } finally {
            c.disconnect()
        }
    }

    fun postJson(url: String, body: String, timeout: Int = 30000): String {
        val c = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 15000
            readTimeout = timeout
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("User-Agent", UA)
        }
        return try {
            c.outputStream.use { it.write(body.toByteArray()) }
            val code = c.responseCode
            val stream = if (code in 200..299) c.inputStream else c.errorStream
            stream?.use { it.readText() } ?: ""
        } finally {
            c.disconnect()
        }
    }

    /** Retorna o JSON ou lança exception com mensagem clara. */
    fun tgCall(method: String, payload: JSONObject): JSONObject {
        val url = "${Config.API_BASE}${Config.TOKEN}/$method"
        val raw = postJson(url, payload.toString())
        if (raw.isBlank()) throw RuntimeException("vazio")
        val j = JSONObject(raw)
        if (!j.optBoolean("ok", false)) {
            throw RuntimeException("tg:${j.optString("description", "erro")}")
        }
        return j
    }

    fun downloadTo(url: String, out: java.io.File): Long {
        val c = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15000
            readTimeout = 60000
            setRequestProperty("User-Agent", UA)
        }
        try {
            val code = c.responseCode
            if (code !in 200..299) throw RuntimeException("http $code")
            c.inputStream.use { input ->
                out.outputStream().use { output ->
                    val buf = ByteArray(64 * 1024)
                    var total = 0L
                    while (true) {
                        val n = input.read(buf)
                        if (n <= 0) break
                        output.write(buf, 0, n)
                        total += n
                    }
                    return total
                }
            }
        } finally { c.disconnect() }
    }

    fun readAll(i: InputStream): ByteArray {
        val b = ByteArrayOutputStream()
        val buf = ByteArray(8 * 1024)
        while (true) {
            val n = i.read(buf); if (n <= 0) break
            b.write(buf, 0, n)
        }
        return b.toByteArray()
    }
}
