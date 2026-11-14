package org.changlianxi.app.call

import org.changlianxi.app.model.CallTarget
import org.changlianxi.app.model.PhoneDialMethod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RealCallPlannerTest {
    @Test fun `demo target stays demo`() {
        assertEquals(CallPlan.Demo, RealCallPlanner.plan(CallTarget.Demo, PhoneDialMethod.DIRECT_CALL, true))
    }

    @Test fun `wechat target rejected as unavailable`() {
        assertEquals(
            CallPlan.Reject(CallError.WECHAT_UNAVAILABLE),
            RealCallPlanner.plan(CallTarget.WeChatVideo("b1"), PhoneDialMethod.DIRECT_CALL, true),
        )
    }

    @Test fun `blank or invalid number fails closed`() {
        for (n in listOf("", "   ", "12", "abc", "1234567890123456")) {
            assertEquals(
                CallPlan.Reject(CallError.MISSING_BINDING),
                RealCallPlanner.plan(CallTarget.Phone(n), PhoneDialMethod.DIRECT_CALL, true),
            )
        }
    }

    @Test fun `direct call with permission uses ACTION_CALL`() {
        assertEquals(
            CallPlan.PlaceCall(RealCallPlanner.ACTION_CALL, "13800138000"),
            RealCallPlanner.plan(CallTarget.Phone("13800138000"), PhoneDialMethod.DIRECT_CALL, true),
        )
    }

    @Test fun `direct call without permission falls back to ACTION_DIAL`() {
        assertEquals(
            CallPlan.PlaceCall(RealCallPlanner.ACTION_DIAL, "13800138000"),
            RealCallPlanner.plan(CallTarget.Phone("13800138000"), PhoneDialMethod.DIRECT_CALL, false),
        )
    }

    @Test fun `system dialer never needs permission`() {
        for (granted in listOf(true, false)) {
            assertEquals(
                CallPlan.PlaceCall(RealCallPlanner.ACTION_DIAL, "13800138000"),
                RealCallPlanner.plan(CallTarget.Phone("13800138000"), PhoneDialMethod.SYSTEM_DIALER, granted),
            )
        }
    }

    @Test fun `number normalized to digits with optional plus`() {
        assertEquals(
            CallPlan.PlaceCall(RealCallPlanner.ACTION_DIAL, "+8613800138000"),
            RealCallPlanner.plan(CallTarget.Phone("+86 138-0013-8000"), PhoneDialMethod.SYSTEM_DIALER, false),
        )
    }

    @Test fun `emulator short number dialable`() {
        assertTrue(RealCallPlanner.isDialable("5556"))
        assertEquals(
            CallPlan.PlaceCall(RealCallPlanner.ACTION_CALL, "5556"),
            RealCallPlanner.plan(CallTarget.Phone("5556"), PhoneDialMethod.DIRECT_CALL, true),
        )
    }
}