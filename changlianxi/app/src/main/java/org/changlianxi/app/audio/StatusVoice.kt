package org.changlianxi.app.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * 系统状态语音：固定短句，优先系统中文 TTS。
 * 本机无可用中文 TTS 时静默降级（界面文字提示仍在），不崩溃、不假定 TTS 一定可用。
 */
class StatusVoice(context: Context) {
    private var tts: TextToSpeech? = null

    @Volatile
    private var ready = false

    init {
        runCatching {
            tts = TextToSpeech(context.applicationContext) { status ->
                if (status != TextToSpeech.SUCCESS) return@TextToSpeech
                val result = tts?.setLanguage(Locale.CHINESE) ?: TextToSpeech.LANG_NOT_SUPPORTED
                ready = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
            }
        }
    }

    fun speak(text: String) {
        if (!ready) return
        runCatching { tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "clx-status") }
    }

    fun stop() {
        runCatching { tts?.stop() }
    }

    fun shutdown() {
        runCatching { tts?.stop() }
        runCatching { tts?.shutdown() }
        tts = null
        ready = false
    }
}