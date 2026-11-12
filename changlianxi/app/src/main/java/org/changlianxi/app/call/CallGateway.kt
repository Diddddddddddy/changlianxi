package org.changlianxi.app.call

import org.changlianxi.app.model.FamilyContact

interface CallGateway {
    suspend fun startVideoCall(contact: FamilyContact): CallStartResult
}

sealed interface CallStartResult {
    data object DemoStarted : CallStartResult
    data object HandoffStarted : CallStartResult
    data object DialingVerified : CallStartResult
    data class Failed(val error: CallError) : CallStartResult
}

enum class CallError {
    MISSING_CONTACT,
    MISSING_BINDING,
    NETWORK_UNAVAILABLE,
    WECHAT_UNAVAILABLE,
    ACCESSIBILITY_DISABLED,
    CONTACT_NOT_VERIFIED,
    TIMEOUT_UNCONFIRMED,
    CANCELLED,
    UNKNOWN,
}

/** 固定状态短句：与状态事件一一对应，绝不把「请求中」说成「已接通」。纯逻辑，可单测。 */
object StatusPhrases {
    const val REQUEST = "正在帮你打电话"
    const val NO_NETWORK = "手机没联网"
    const val FAILED = "电话没有打出去"

    fun forError(error: CallError): String =
        if (error == CallError.NETWORK_UNAVAILABLE) NO_NETWORK else FAILED
}