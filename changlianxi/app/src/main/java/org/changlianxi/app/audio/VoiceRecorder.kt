package org.changlianxi.app.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

/** 短语音录制（AAC/m4a）。stop 返回时长毫秒；失败或过短返回 -1 并清理文件。 */
class VoiceRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var target: File? = null
    private var startedAt = 0L

    val isRecording: Boolean get() = recorder != null

    fun start(file: File) {
        check(recorder == null) { "已在录制中" }
        file.parentFile?.mkdirs()
        val rec = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(context) else {
            @Suppress("DEPRECATION") MediaRecorder()
        }
        rec.setAudioSource(MediaRecorder.AudioSource.MIC)
        rec.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        rec.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        rec.setAudioEncodingBitRate(128_000)
        rec.setAudioSamplingRate(44_100)
        rec.setMaxDuration(MAX_DURATION_MS)
        rec.setOutputFile(file.absolutePath)
        runCatching {
            rec.prepare()
            rec.start()
        }.onFailure {
            runCatching { rec.release() }
            file.delete()
            throw it
        }
        recorder = rec
        target = file
        startedAt = System.currentTimeMillis()
    }

    fun stop(): Long {
        val rec = recorder ?: return -1
        val file = target
        recorder = null
        target = null
        val duration = System.currentTimeMillis() - startedAt
        val ok = runCatching { rec.stop() }.isSuccess
        runCatching { rec.release() }
        if (!ok || duration < MIN_DURATION_MS || file == null || file.length() == 0L) {
            file?.delete()
            return -1
        }
        return duration
    }

    fun cancel() {
        val rec = recorder ?: return
        recorder = null
        val file = target
        target = null
        runCatching { rec.stop() }
        runCatching { rec.release() }
        file?.delete()
    }

    private companion object {
        const val MAX_DURATION_MS = 60_000
        const val MIN_DURATION_MS = 800
    }
}