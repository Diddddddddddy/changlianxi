package org.changlianxi.app.call

import org.changlianxi.app.model.CallTarget
import org.changlianxi.app.model.PhoneDialMethod

/** 真实拨号的纯决策结果。不依赖 Android，可 JVM 单测。 */
sealed interface CallPlan {
    data object Demo : CallPlan
    data class PlaceCall(val action: String, val number: String) : CallPlan
    data class Reject(val error: CallError) : CallPlan
}

/**
 * 失败关闭的拨号决策：号码缺失/非法→拒绝；直拨未授权→安全回退系统拨号盘。
 * 绝不因权限或配置缺失而乱拨、空拨。
 */
object RealCallPlanner {
    const val ACTION_CALL = "android.intent.action.CALL"
    const val ACTION_DIAL = "android.intent.action.DIAL"

    fun plan(target: CallTarget, method: PhoneDialMethod, callPhoneGranted: Boolean): CallPlan =
        when (target) {
            is CallTarget.Demo -> CallPlan.Demo
            is CallTarget.WeChatVideo -> CallPlan.Reject(CallError.WECHAT_UNAVAILABLE)
            is CallTarget.Phone -> {
                val raw = target.number.trim()
                if (!isDialable(raw)) CallPlan.Reject(CallError.MISSING_BINDING)
                else {
                    val action =
                        if (method == PhoneDialMethod.DIRECT_CALL && callPhoneGranted) ACTION_CALL
                        else ACTION_DIAL
                    CallPlan.PlaceCall(action, normalize(raw))
                }
            }
        }

    /** 仅允许数字与常见电话符号，且至少 3 位、至多 15 位数字（E.164）。 */
    fun isDialable(number: String): Boolean {
        if (number.any { it !in "0123456789+-() " }) return false
        return number.count { it.isDigit() } in 3..15
    }

    /** 归一化为 tel: 可用形式：保留前导 +，其余仅留数字。 */
    fun normalize(number: String): String =
        (if (number.trim().startsWith("+")) "+" else "") + number.filter { it.isDigit() }
}