package com.serv.app.core

import org.json.JSONObject
import java.io.DataOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

/**
 * Multipart/related upload usando HttpURLConnection puro.
 * Compatível com BotAPI para sendDocument/sendVoice/sendPhoto/sendVideo.
 */
object Multipart {

    fun sendFile(
        method: String,
        chatId: String,
        fieldName: String,
        file: File,
        mime: String,
        extra: Map<String, String> = emptyMap()
    ): JSONObject {
        val boundary = "----b${UUID.randomUUID().toString().replace("-", "")}"
        val url = URL("${Config.API_BASE}${Config.TOKEN}/$method")
        val c = url.openConnection() as HttpURLConnection
        c.requestMethod = "POST"
        c.doOutput = true
        c.connectTimeout = 20000
        c.readTimeout = 120000
        c.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
        c.setRequestProperty("User-Agent", "ServSync/1.0")
        c.setChunkedStreamingMode(64 * 1024)

        try {
            DataOutputStream(c.outputStream).use { out ->
                fun field(name: String, value: String) {
                    out.writeBytes("--$boundary\r\n")
                    out.writeBytes("Content-Disposition: form-data; name=\"$name\"\r\n\r\n")
                    out.writeBytes(value); out.writeBytes("\r\n")
                }
                field("chat_id", chatId)
                extra.forEach { (k, v) -> field(k, v) }

                out.writeBytes("--$boundary\r\n")
                out.writeBytes("Content-Disposition: form-data; name=\"$fieldName\"; filename=\"${file.name}\"\r\n")
                out.writeBytes("Content-Type: $mime\r\n\r\n")
                file.inputStream().use { it.copyTo(out, 64 * 1024) }
                out.writeBytes("\r\n--$boundary--\r\n")
                out.flush()
            }
            val code = c.responseCode
            val stream = if (code in 200..299) c.inputStream else c.errorStream
            val body = stream?.use { Http.readAll(it).toString(Charsets.UTF_8) } ?: ""
            if (body.isBlank()) throw RuntimeException("vazio ($code)")
            val j = JSONObject(body)
            if (!j.optBoolean("ok", false)) throw RuntimeException("tg:${j.optString("description")}")
            return j
        } finally {
            c.disconnect()
        }
    }
}
