package org.changlianxi.app.data

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PinStoreTest {
    @get:Rule val tmp = TemporaryFolder()

    private var now = 1_000_000L
    private fun store() = PinStore(tmp.root) { now }

    @Test fun `not set up initially`() {
        val s = store()
        assertFalse(s.isSetUp())
        assertEquals(PinStore.Result.NotSetUp, s.verify("1234"))
    }

    @Test fun `create and verify correct pin`() {
        val s = store()
        s.createPin("2468")
        assertTrue(s.isSetUp())
        assertEquals(PinStore.Result.Ok, s.verify("2468"))
    }

    @Test fun `wrong pin decrements remaining`() {
        val s = store()
        s.createPin("2468")
        assertEquals(PinStore.Result.Wrong(4), s.verify("0000"))
        assertEquals(PinStore.Result.Wrong(3), s.verify("1111"))
    }

    @Test fun `five failures lock out even correct pin`() {
        val s = store()
        s.createPin("2468")
        repeat(4) { s.verify("0000") }
        val locked = s.verify("0000")
        assertTrue(locked is PinStore.Result.Locked)
        assertEquals(300L, (locked as PinStore.Result.Locked).secondsLeft)
        assertTrue(s.verify("2468") is PinStore.Result.Locked)
    }

    @Test fun `lock expires after cooldown`() {
        val s = store()
        s.createPin("2468")
        repeat(5) { s.verify("0000") }
        now += 5 * 60_000L + 1
        assertEquals(PinStore.Result.Ok, s.verify("2468"))
    }

    @Test fun `success resets failure counter`() {
        val s = store()
        s.createPin("2468")
        repeat(3) { s.verify("0000") }
        assertEquals(PinStore.Result.Ok, s.verify("2468"))
        repeat(2) { s.verify("0000") }
        assertEquals(PinStore.Result.Wrong(2), s.verify("0000"))
    }

    @Test fun `pin plaintext never stored`() {
        val s = store()
        s.createPin("987654")
        val text = File(tmp.root, "pin.json").readText()
        assertFalse(text.contains("987654"))
        assertTrue(text.contains("salt"))
        assertTrue(text.contains("hash"))
    }

    @Test fun `invalid pin formats rejected`() {
        val s = store()
        for (bad in listOf("123", "1234567", "12ab", "")) {
            try {
                s.createPin(bad)
                throw AssertionError("应当拒绝: $bad")
            } catch (_: IllegalArgumentException) { }
        }
        assertFalse(s.isSetUp())
    }

    @Test fun `six digit pin accepted`() {
        val s = store()
        s.createPin("135790")
        assertEquals(PinStore.Result.Ok, s.verify("135790"))
    }

    @Test fun `persists across instances`() {
        store().createPin("2468")
        assertEquals(PinStore.Result.Ok, store().verify("2468"))
    }
}