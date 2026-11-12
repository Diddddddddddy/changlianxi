package org.changlianxi.app.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import java.io.File

/** 单实例语音播放：任何新播放取代旧播放；stop 后旧回调一律作废。播放期间持有音频焦点，失焦即停。 */
class VoicePlayer(context: Context) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var player: MediaPlayer? = null
    private var generation = 0
    private var playing = false

    private val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
        )
        .setOnAudioFocusChangeListener { change ->
            if (change == AudioManager.AUDIOFOCUS_LOSS || change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) stop()
        }
        .build()

    val isPlaying: Boolean get() = playing

    fun play(file: File, onFinished: () -> Unit = {}) {
        stop()
        if (audioManager.requestAudioFocus(focusRequest) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) return
        val gen = generation
        val mp = MediaPlayer()
        player = mp
        runCatching {
            mp.setDataSource(file.absolutePath)
            mp.setOnPreparedListener {
                if (gen != generation) return@setOnPreparedListener
                playing = true
                it.start()
            }
            mp.setOnCompletionListener {
                if (gen != generation) return@setOnCompletionListener
                stop()
                onFinished()
            }
            mp.setOnErrorListener { _, _, _ ->
                if (gen == generation) stop()
                true
            }
            mp.prepareAsync()
        }.onFailure { stop() }
    }

    fun stop() {
        generation++
        playing = false
        audioManager.abandonAudioFocusRequest(focusRequest)
        player?.let { mp ->
            player = null
            runCatching { mp.stop() }
            runCatching { mp.release() }
        }
    }
}