package org.changlianxi.app.data

import java.io.ByteArrayInputStream
import org.changlianxi.app.model.DialMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ContactRepositoryTest {
    @get:Rule val tmp = TemporaryFolder()

    private lateinit var media: MediaFiles
    private lateinit var repo: ContactRepository

    @Before fun setup() {
        media = MediaFiles(tmp.root)
        repo = ContactRepository(ConfigStore(tmp.root), media)
    }

    private fun stage(id: String) {
        media.writeStagingPhoto(id, ByteArrayInputStream(byteArrayOf(1, 2, 3)))
        media.writeStagingVoice(id, ByteArrayInputStream(byteArrayOf(4, 5, 6)))
    }

    private fun add(id: String, name: String = "家人$id", makeDefault: Boolean = false) =
        repo.commitContact(id, name, makeDefault)

    @Test fun `three contacts persist with order and first default`() {
        stage("a"); add("a", "女儿")
        stage("b"); add("b", "儿子")
        stage("c"); add("c", "老伴")
        val config = repo.load()!!
        assertEquals(listOf("a", "b", "c"), config.contacts.map { it.id })
        assertEquals(listOf(0, 1, 2), config.contacts.map { it.order })
        assertEquals("a", config.settings.defaultContactId)
        assertEquals(listOf("女儿", "儿子", "老伴"), config.contacts.map { it.displayName })
        assertTrue(config.contacts.all { media.mediaReady(it.id) })
    }

    @Test fun `commit without media fails and keeps old config`() {
        stage("a"); add("a")
        val before = repo.load()!!
        try {
            repo.commitContact("b", "儿子", false)
            throw AssertionError("应当因缺媒体而失败")
        } catch (_: IllegalStateException) { }
        assertEquals(before.contacts.map { it.id }, repo.load()!!.contacts.map { it.id })
    }

    @Test fun `commit without voice fails`() {
        media.writeStagingPhoto("a", ByteArrayInputStream(byteArrayOf(1)))
        try {
            repo.commitContact("a", "女儿", false)
            throw AssertionError("应当因缺录音而失败")
        } catch (_: IllegalStateException) { }
        assertNull(repo.load())
    }

    @Test fun `blank name rejected`() {
        stage("a")
        try {
            repo.commitContact("a", "  ", false)
            throw AssertionError("应当因空称呼而失败")
        } catch (_: IllegalArgumentException) { }
    }

    @Test fun `makeDefault switches the only default`() {
        stage("a"); add("a")
        stage("b"); add("b", makeDefault = true)
        val config = repo.load()!!
        assertEquals("b", config.settings.defaultContactId)
        assertEquals("b", config.defaultContact!!.id)
    }

    @Test fun `setDefault requires existing contact`() {
        stage("a"); add("a")
        try {
            repo.setDefault("ghost")
            throw AssertionError("应当拒绝无效默认")
        } catch (_: IllegalArgumentException) { }
        assertEquals("a", repo.load()!!.settings.defaultContactId)
    }

    @Test fun `deleting default reassigns to first remaining`() {
        stage("a"); add("a")
        stage("b"); add("b")
        stage("c"); add("c")
        val config = repo.deleteContact("a")
        assertEquals("b", config.settings.defaultContactId)
        assertEquals(listOf("b", "c"), config.contacts.map { it.id })
        assertEquals(listOf(0, 1), config.contacts.map { it.order })
        assertFalse(media.mediaReady("a"))
        assertTrue(media.mediaReady("b"))
        assertTrue(media.mediaReady("c"))
    }

    @Test fun `deleting last contact clears config`() {
        stage("a"); add("a")
        val config = repo.deleteContact("a")
        assertTrue(config.contacts.isEmpty())
        assertNull(config.settings.defaultContactId)
        assertNull(repo.load()!!.defaultContact)
    }

    @Test fun `move up and down keeps ids and reindexes`() {
        stage("a"); add("a")
        stage("b"); add("b")
        stage("c"); add("c")
        assertEquals(listOf("a", "c", "b"), repo.moveContact("c", -1).contacts.map { it.id })
        assertEquals(listOf(0, 1, 2), repo.load()!!.contacts.map { it.order })
        assertEquals(listOf("c", "a", "b"), repo.moveContact("c", -1).contacts.map { it.id })
        assertEquals(listOf("a", "c", "b"), repo.moveContact("c", +1).contacts.map { it.id })
    }

    @Test fun `move at boundary is a no-op`() {
        stage("a"); add("a")
        stage("b"); add("b")
        val version = repo.load()!!.configVersion
        assertEquals(listOf("a", "b"), repo.moveContact("a", -1).contacts.map { it.id })
        assertEquals(listOf("a", "b"), repo.moveContact("b", +1).contacts.map { it.id })
        assertEquals(version, repo.load()!!.configVersion)
    }

    @Test fun `update keeps id order and target`() {
        stage("a"); add("a", "女儿")
        stage("b"); add("b")
        val config = repo.commitContact("a", "大女儿", false)
        assertEquals(listOf("a", "b"), config.contacts.map { it.id })
        assertEquals("大女儿", config.contact("a")!!.displayName)
        assertEquals(0, config.contact("a")!!.order)
        assertEquals("a", config.settings.defaultContactId)
    }

    @Test fun `dial mode and autoplay persist`() {
        stage("a"); add("a")
        repo.setDialMode(DialMode.LARGE_BUTTON)
        repo.setAutoPlayVoice(false)
        val config = repo.load()!!
        assertEquals(DialMode.LARGE_BUTTON, config.settings.dialMode)
        assertFalse(config.settings.autoPlayVoice)
    }

    @Test fun `survives store reopen`() {
        stage("a"); add("a", "女儿")
        stage("b"); add("b", "儿子", makeDefault = true)
        val reopened = ContactRepository(ConfigStore(tmp.root), MediaFiles(tmp.root))
        val config = reopened.load()!!
        assertEquals(2, config.contacts.size)
        assertEquals("b", config.defaultContact!!.id)
        assertEquals("儿子", config.defaultContact!!.displayName)
    }
}