package org.changlianxi.app.call

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class StatusPhrasesTest {
    @Test fun `network error maps to no-network phrase`() {
        assertEquals(StatusPhrases.NO_NETWORK, StatusPhrases.forError(CallError.NETWORK_UNAVAILABLE))
    }

    @Test fun `other errors map to failed phrase`() {
        for (e in CallError.entries.filter { it != CallError.NETWORK_UNAVAILABLE && it != CallError.MISSING_BINDING }) {
            assertEquals(StatusPhrases.FAILED, StatusPhrases.forError(e))
        }
    }

    @Test fun `missing binding maps to not-ready phrase`() {
        assertEquals(StatusPhrases.NOT_READY, StatusPhrases.forError(CallError.MISSING_BINDING))
    }

    @Test fun `request phrase distinct from connected or failed`() {
        assertNotEquals(StatusPhrases.REQUEST, StatusPhrases.FAILED)
        assertNotEquals(StatusPhrases.REQUEST, StatusPhrases.NO_NETWORK)
        assertNotEquals(StatusPhrases.FAILED, StatusPhrases.NO_NETWORK)
        assertNotEquals(StatusPhrases.NOT_READY, StatusPhrases.FAILED)
        assertNotEquals(StatusPhrases.NOT_READY, StatusPhrases.REQUEST)
    }
}