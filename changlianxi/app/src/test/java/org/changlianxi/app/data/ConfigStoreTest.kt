package org.changlianxi.app.data

import java.io.File
import org.changlianxi.app.model.AppConfig
import org.changlianxi.app.model.AppSettings
import org.changlianxi.app.model.CallTarget
import org.changlianxi.app.model.DialMode
import org.changlianxi.app.model.FamilyContact
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ConfigStoreTest {
    @get:Rule val tmp = TemporaryFolder()

    private fun contact(id: String, order: Int) = FamilyContact(
        id = id, displayName = "家人$id", photoPath = "media/photo/$id.jpg",
        voicePath = "media/voice/$id.m4a", order = order,
    )

    @Test fun `save then load roundtrip`() {
        val store = ConfigStore(tmp.root)
        val config = AppConfig(
            settings = AppSettings(dialMode = DialMode.LARGE_BUTTON, autoPlayVoice = false, defaultContactId = "b"),
            contacts = listOf(contact("a", 0), contact("b", 1)),
        )
        store.save(config)
        val loaded = store.load()!!
        assertEquals(DialMode.LARGE_BUTTON, loaded.settings.dialMode)
        assertFalse(loaded.settings.autoPlayVoice)
        assertEquals("b", loaded.settings.defaultContactId)
        assertEquals(listOf("a", "b"), loaded.contacts.map { it.id })
        assertEquals(CallTarget.Demo, loaded.contacts[0].callTarget)
        assertEquals(1, loaded.schemaVersion)
    }

    @Test fun `configVersion increments on each save`() {
        val store = ConfigStore(tmp.root)
        val v1 = store.save(AppConfig(contacts = listOf(contact("a", 0))))
        val v2 = store.save(v1)
        assertEquals(1L, v1.configVersion)
        assertEquals(2L, v2.configVersion)
        assertEquals(2L, store.load()!!.configVersion)
    }

    @Test fun `no temp file left after save`() {
        val store = ConfigStore(tmp.root)
        store.save(AppConfig(contacts = listOf(contact("a", 0))))
        assertFalse(File(tmp.root, "config.json.tmp").exists())
        assertTrue(File(tmp.root, "config.json").isFile)
    }

    @Test fun `corrupt json is backed up and treated as unconfigured`() {
        File(tmp.root, "config.json").writeText("{not valid json!!")
        val store = ConfigStore(tmp.root)
        assertNull(store.load())
        assertFalse(File(tmp.root, "config.json").exists())
        assertTrue(tmp.root.listFiles()!!.any { it.name.startsWith("config.json.corrupt-") })
    }

    @Test fun `load reorders by order and reindexes`() {
        val store = ConfigStore(tmp.root)
        store.save(AppConfig(contacts = listOf(contact("c", 5), contact("a", 1), contact("b", 3))))
        val loaded = store.load()!!
        assertEquals(listOf("a", "b", "c"), loaded.contacts.map { it.id })
        assertEquals(listOf(0, 1, 2), loaded.contacts.map { it.order })
    }

    @Test fun `invalid default falls back to first contact`() {
        val store = ConfigStore(tmp.root)
        store.save(AppConfig(
            settings = AppSettings(defaultContactId = "ghost"),
            contacts = listOf(contact("a", 0), contact("b", 1)),
        ))
        assertEquals("a", store.load()!!.settings.defaultContactId)
    }

    @Test fun `empty config has null default`() {
        val store = ConfigStore(tmp.root)
        store.save(AppConfig())
        assertNull(store.load()!!.settings.defaultContactId)
    }

    @Test fun `load without file returns null`() {
        assertNull(ConfigStore(tmp.root).load())
    }

    @Test fun `wechat target survives roundtrip`() {
        val store = ConfigStore(tmp.root)
        val c = contact("a", 0).copy(callTarget = CallTarget.WeChatVideo("binding-1"))
        store.save(AppConfig(contacts = listOf(c)))
        assertEquals(CallTarget.WeChatVideo("binding-1"), store.load()!!.contacts[0].callTarget)
    }
}