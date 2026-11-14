package org.changlianxi.app.call

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import org.changlianxi.app.model.AppSettings
import org.changlianxi.app.model.FamilyContact
import org.changlianxi.app.model.PhoneDialMethod

/**
 * 真实蜂窝拨号网关：按 RealCallPlanner 的失败关闭决策发系统拨号 Intent。
 * Demo 目标仍走纯本地演示；微信目标暂未实现。号码仅来自本地配置，绝不上传或记日志。
 */
class RealCallGateway(
    private val context: Context,
    private val settings: () -> AppSettings?,
) : CallGateway {

    override suspend fun startVideoCall(contact: FamilyContact): CallStartResult {
        val method = settings()?.phoneDialMethod ?: PhoneDialMethod.DIRECT_CALL
        val granted = context.checkSelfPermission(Manifest.permission.CALL_PHONE) ==
            PackageManager.PERMISSION_GRANTED
        return when (val plan = RealCallPlanner.plan(contact.callTarget, method, granted)) {
            is CallPlan.Demo -> CallStartResult.DemoStarted
            is CallPlan.Reject -> CallStartResult.Failed(plan.error)
            is CallPlan.PlaceCall -> placeCall(plan)
        }
    }

    private fun placeCall(plan: CallPlan.PlaceCall): CallStartResult {
        val uri = Uri.parse("tel:" + plan.number)
        return try {
            context.startActivity(Intent(plan.action, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            CallStartResult.HandoffStarted
        } catch (_: SecurityException) {
            // 直拨权限被撤销：安全回退到零权限的系统拨号盘，绝不因此崩溃或乱拨。
            try {
                context.startActivity(
                    Intent(RealCallPlanner.ACTION_DIAL, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
                CallStartResult.HandoffStarted
            } catch (_: Exception) {
                CallStartResult.Failed(CallError.UNKNOWN)
            }
        } catch (_: Exception) {
            CallStartResult.Failed(CallError.UNKNOWN)
        }
    }
}