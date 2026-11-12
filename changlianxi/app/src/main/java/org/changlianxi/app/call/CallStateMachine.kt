package org.changlianxi.app.call

/** P0 呼叫状态：保持「请求发起 ≠ 已拨通 ≠ 已接听」的语义区分。 */
sealed interface CallPhase {
    data object Idle : CallPhase
    data class Preparing(val contactId: String) : CallPhase
    data class DemoRinging(val contactId: String) : CallPhase
    data class DemoConnected(val contactId: String) : CallPhase
    data class IncomingDemo(val contactId: String) : CallPhase
    data class Failed(val contactId: String, val reason: CallError) : CallPhase
    data class Finished(val contactId: String) : CallPhase
}

/** 当前状态锁定的联系人 id；Idle 为 null。 */
val CallPhase.contactIdOrNull: String?
    get() = when (this) {
        is CallPhase.Preparing -> contactId
        is CallPhase.DemoRinging -> contactId
        is CallPhase.DemoConnected -> contactId
        is CallPhase.IncomingDemo -> contactId
        is CallPhase.Failed -> contactId
        is CallPhase.Finished -> contactId
        CallPhase.Idle -> null
    }

/**
 * 纯逻辑呼叫状态机：每次迁移须匹配当前 requestId 与源状态，过期／重复事件一律拒绝。
 * 主线程 confined，不依赖 Android，可 JVM 单测。
 */
class CallStateMachine {
    var phase: CallPhase = CallPhase.Idle
        private set
    var requestId: String? = null
        private set

    val contactId: String?
        get() = when (val p = phase) {
            is CallPhase.Preparing -> p.contactId
            is CallPhase.DemoRinging -> p.contactId
            is CallPhase.DemoConnected -> p.contactId
            is CallPhase.IncomingDemo -> p.contactId
            is CallPhase.Failed -> p.contactId
            is CallPhase.Finished -> p.contactId
            CallPhase.Idle -> null
        }

    val isActive: Boolean
        get() = phase != CallPhase.Idle && phase !is CallPhase.Finished && phase !is CallPhase.Failed

    /** Idle → Preparing；仅 Idle 接受，防双击／重复呼叫。 */
    fun request(contactId: String, requestId: String): Boolean {
        if (phase != CallPhase.Idle) return false
        this.requestId = requestId
        phase = CallPhase.Preparing(contactId)
        return true
    }

    /** Idle → IncomingDemo（模拟来电）。 */
    fun incoming(contactId: String, requestId: String): Boolean {
        if (phase != CallPhase.Idle) return false
        this.requestId = requestId
        phase = CallPhase.IncomingDemo(contactId)
        return true
    }

    /** Preparing → DemoRinging（演示通话启动）。 */
    fun demoStarted(requestId: String): Boolean {
        val p = phase as? CallPhase.Preparing ?: return false
        if (requestId != this.requestId) return false
        phase = CallPhase.DemoRinging(p.contactId)
        return true
    }

    /** DemoRinging → DemoConnected（演示对方接听）。 */
    fun demoAnswer(requestId: String): Boolean {
        val p = phase as? CallPhase.DemoRinging ?: return false
        if (requestId != this.requestId) return false
        phase = CallPhase.DemoConnected(p.contactId)
        return true
    }

    /** DemoRinging → Failed（演示无人接听，不声称真实通话）。 */
    fun demoNoAnswer(requestId: String): Boolean {
        val p = phase as? CallPhase.DemoRinging ?: return false
        if (requestId != this.requestId) return false
        phase = CallPhase.Failed(p.contactId, CallError.TIMEOUT_UNCONFIRMED)
        return true
    }

    /** Preparing/DemoRinging → Failed（断网／失败）。 */
    fun fail(requestId: String, reason: CallError): Boolean {
        if (requestId != this.requestId) return false
        if (phase !is CallPhase.Preparing && phase !is CallPhase.DemoRinging) return false
        val cid = contactId ?: return false
        phase = CallPhase.Failed(cid, reason)
        return true
    }

    /** IncomingDemo → DemoConnected（点照片接听）。 */
    fun incomingAnswer(requestId: String): Boolean {
        val p = phase as? CallPhase.IncomingDemo ?: return false
        if (requestId != this.requestId) return false
        phase = CallPhase.DemoConnected(p.contactId)
        return true
    }

    /** IncomingDemo → Finished（红色拒接）。 */
    fun incomingDecline(requestId: String): Boolean {
        val p = phase as? CallPhase.IncomingDemo ?: return false
        if (requestId != this.requestId) return false
        phase = CallPhase.Finished(p.contactId)
        return true
    }

    /** DemoConnected → Finished（红色挂断，只执行一次）。 */
    fun hangup(requestId: String): Boolean {
        val p = phase as? CallPhase.DemoConnected ?: return false
        if (requestId != this.requestId) return false
        phase = CallPhase.Finished(p.contactId)
        return true
    }

    /** Preparing/DemoRinging → Finished（用户取消拨号）。 */
    fun cancel(requestId: String): Boolean {
        if (requestId != this.requestId) return false
        if (phase !is CallPhase.Preparing && phase !is CallPhase.DemoRinging) return false
        val cid = contactId ?: return false
        phase = CallPhase.Finished(cid)
        return true
    }

    /** Finished/Failed → Idle，回到原联系人。 */
    fun reset() {
        phase = CallPhase.Idle
        requestId = null
    }
}