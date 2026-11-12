package org.changlianxi.app.call

import org.changlianxi.app.model.FamilyContact

/** 完全本地，不访问通信系统，不产生真实呼叫。 */
class DemoCallGateway : CallGateway {
    override suspend fun startVideoCall(contact: FamilyContact): CallStartResult = CallStartResult.DemoStarted
}