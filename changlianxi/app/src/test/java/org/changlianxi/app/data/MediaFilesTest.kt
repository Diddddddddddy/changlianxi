package org.changlianxi.app.data

import java.io.ByteArrayInputStream
import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class MediaFilesTest {
    @get:Rule val tmp = TemporaryFolder()

    private val media get() = MediaFiles(tmp.root)

    @Test fun `staging commit moves files to final paths`() {
        val m = media
        m.writeStagingPhoto("a", ByteArrayInputStream(byteArrayOf(1, 2)))
        m.writeStagingVoice("a", ByteArrayInputStream(byteArrayOf(3, 4)))
        assertTrue(m.stagingPhoto("a").isFile)
        m.commitMedia("a")
        assertFalse(m.stagingPhoto("a").exists())
        assertFalse(m.stagingVoice("a").exists())
        assertTrue(m.photoFile("a").isFile)
        assertTrue(m.voiceFile("a").isFile)
        assertTrue(m.mediaReady("a"))
    }

    @Test fun `commit without staging keeps existing final files`() {
        val m = media
        m.writeStagingPhoto("a", ByteArrayInputStream(byteArrayOf(1)))
        m.writeStagingVoice("a", ByteArrayInputStream(byteArrayOf(2)))
        m.commitMedia("a")
        m.commitMedia("a")
        assertTrue(m.mediaReady("a"))
    }

    @Test fun `commit replaces old final file`() {
        val m = media
        m.writeStagingPhoto("a", ByteArrayInputStream(byteArrayOf(1)))
        m.writeStagingVoice("a", ByteArrayInputStream(byteArrayOf(2)))
        m.commitMedia("a")
        m.writeStagingPhoto("a", ByteArrayInputStream(byteArrayOf(9, 9, 9)))
        m.commitMedia("a")
        assertTrue(m.photoFile("a").readBytes().contentEquals(byteArrayOf(9, 9, 9)))
    }

    @Test fun `discard staging leaves no residue`() {
        val m = media
        m.writeStagingPhoto("a", ByteArrayInputStream(byteArrayOf(1)))
        m.writeStagingVoice("a", ByteArrayInputStream(byteArrayOf(2)))
        m.discardStaging("a")
        assertFalse(m.stagingPhoto("a").exists())
        assertFalse(m.stagingVoice("a").exists())
        assertFalse(m.mediaReady("a"))
    }

    @Test fun `delete only removes target contact media`() {
        val m = media
        for (id in listOf("a", "b")) {
            m.writeStagingPhoto(id, ByteArrayInputStream(byteArrayOf(1)))
            m.writeStagingVoice(id, ByteArrayInputStream(byteArrayOf(2)))
            m.commitMedia(id)
        }
        m.deleteContactMedia("a")
        assertFalse(m.mediaReady("a"))
        assertTrue(m.mediaReady("b"))
    }

    @Test fun `empty input rejected`() {
        try {
            media.writeStagingPhoto("a", ByteArrayInputStream(ByteArray(0)))
            throw AssertionError("应当拒绝空文件")
        } catch (_: IllegalStateException) { }
        assertFalse(media.stagingPhoto("a").exists())
    }

    @Test fun `no temp residue after write`() {
        val m = media
        m.writeStagingPhoto("a", ByteArrayInputStream(byteArrayOf(1)))
        val staging = File(tmp.root, "media/staging")
        assertTrue(staging.listFiles()!!.none { it.name.endsWith(".tmp") })
    }

    @Test fun `playback voice uses committed file only`() {
        val m = media
        // 仅 staging（未提交）：老人首页不应播放
        m.writeStagingVoice("a", ByteArrayInputStream(byteArrayOf(1, 2)))
        assertTrue(m.previewVoice("a") != null)
        assertTrue(m.playbackVoice("a") == null)
        // 提交后可播放
        m.writeStagingPhoto("a", ByteArrayInputStream(byteArrayOf(1)))
        m.commitMedia("a")
        assertTrue(m.playbackVoice("a") != null)
        // 缺失返回 null，不崩溃
        assertTrue(m.playbackVoice("missing") == null)
    }
}