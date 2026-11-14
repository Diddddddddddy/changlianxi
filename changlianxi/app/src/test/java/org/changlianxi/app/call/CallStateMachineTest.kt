package org.changlianxi.app.call

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CallStateMachineTest {
    private val m = CallStateMachine()

    @Test fun `request only accepted from Idle - double click rejected`() {
        assertTrue(m.request("a", "r1"))
        assertEquals(CallPhase.Preparing("a"), m.phase)
        // 第二次点击（新 requestId）被拒绝，仍是同一事务（AC-09）
        assertFalse(m.request("a", "r2"))
        assertEquals("r1", m.requestId)
        assertEquals(CallPhase.Preparing("a"), m.phase)
    }

    @Test fun `outgoing full flow prepare ring connect hangup`() {
        m.request("a", "r1")
        assertTrue(m.demoStarted("r1"))
        assertEquals(CallPhase.DemoRinging("a"), m.phase)
        assertTrue(m.demoAnswer("r1"))
        assertEquals(CallPhase.DemoConnected("a"), m.phase)
        assertTrue(m.hangup("r1"))
        assertEquals(CallPhase.Finished("a"), m.phase)
        m.reset()
        assertEquals(CallPhase.Idle, m.phase)
        assertNull(m.requestId)
    }

    @Test fun `hangup executes only once`() {
        m.request("a", "r1"); m.demoStarted("r1"); m.demoAnswer("r1")
        assertTrue(m.hangup("r1"))
        assertFalse(m.hangup("r1"))  // 已 Finished，再次挂断无效（AC-13）
        assertEquals(CallPhase.Finished("a"), m.phase)
    }

    @Test fun `stale requestId rejected`() {
        m.request("a", "r1")
        assertFalse(m.demoStarted("old"))
        assertEquals(CallPhase.Preparing("a"), m.phase)
        assertTrue(m.demoStarted("r1"))
        assertFalse(m.demoAnswer("r1-stale"))
        assertEquals(CallPhase.DemoRinging("a"), m.phase)
    }

    @Test fun `contactId locked across transitions`() {
        m.request("a", "r1")
        m.demoStarted("r1"); m.demoAnswer("r1")
        assertEquals("a", m.contactId)  // 全程锁定 a，不因 index 变化改目标（AC-10）
        m.hangup("r1")
        assertEquals("a", m.contactId)
    }

    @Test fun `no answer leads to Failed not Connected`() {
        m.request("a", "r1"); m.demoStarted("r1")
        assertTrue(m.demoNoAnswer("r1"))
        assertEquals(CallPhase.Failed("a", CallError.TIMEOUT_UNCONFIRMED), m.phase)
        assertFalse(m.demoAnswer("r1"))  // Failed 后不能再假装接通
    }

    @Test fun `network failure from preparing`() {
        m.request("a", "r1")
        assertTrue(m.fail("r1", CallError.NETWORK_UNAVAILABLE))
        assertEquals(CallPhase.Failed("a", CallError.NETWORK_UNAVAILABLE), m.phase)
    }

    @Test fun `incoming answer then hangup`() {
        assertTrue(m.incoming("b", "r1"))
        assertEquals(CallPhase.IncomingDemo("b"), m.phase)
        assertTrue(m.incomingAnswer("r1"))
        assertEquals(CallPhase.DemoConnected("b"), m.phase)
        assertTrue(m.hangup("r1"))
        assertEquals(CallPhase.Finished("b"), m.phase)
    }

    @Test fun `incoming decline returns to finished`() {
        m.incoming("b", "r1")
        assertTrue(m.incomingDecline("r1"))
        assertEquals(CallPhase.Finished("b"), m.phase)
        assertFalse(m.incomingAnswer("r1"))  // 拒接后不能再接听
    }

    @Test fun `cancel from preparing and ringing`() {
        m.request("a", "r1")
        assertTrue(m.cancel("r1"))
        assertEquals(CallPhase.Finished("a"), m.phase)
        m.reset()
        m.request("a", "r2"); m.demoStarted("r2")
        assertTrue(m.cancel("r2"))
        assertEquals(CallPhase.Finished("a"), m.phase)
    }

    @Test fun `incoming rejected while outgoing active`() {
        m.request("a", "r1")
        assertFalse(m.incoming("b", "r2"))  // 非 Idle 不接受来电
        assertEquals(CallPhase.Preparing("a"), m.phase)
    }

    @Test fun `isActive reflects lifecycle`() {
        assertFalse(m.isActive)
        m.request("a", "r1")
        assertTrue(m.isActive)
        m.demoStarted("r1"); m.demoAnswer("r1"); m.hangup("r1")
        assertFalse(m.isActive)
    }

    @Test fun `real call handoff from preparing`() {
        m.request("a", "r1")
        assertTrue(m.handoff("r1"))
        assertEquals(CallPhase.Handoff("a"), m.phase)
        assertEquals("a", m.contactId)
        assertFalse(m.demoStarted("r1"))  // 交棒后不得再假装演示接通
        assertFalse(m.demoAnswer("r1"))
        m.reset()
        assertEquals(CallPhase.Idle, m.phase)
        assertNull(m.requestId)
    }

    @Test fun `handoff rejects stale requestId`() {
        m.request("a", "r1")
        assertFalse(m.handoff("old"))
        assertEquals(CallPhase.Preparing("a"), m.phase)
    }

    @Test fun `handoff only from preparing`() {
        m.request("a", "r1"); m.demoStarted("r1")
        assertFalse(m.handoff("r1"))  // DemoRinging 不能交棒
        assertEquals(CallPhase.DemoRinging("a"), m.phase)
    }

    @Test fun `new request blocked while handed off`() {
        m.request("a", "r1"); m.handoff("r1")
        assertFalse(m.request("b", "r2"))  // 真实呼叫进行中锁定，不接受新拨打
        assertEquals("a", m.contactId)
    }
}