package org.changlianxi.app.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface CallTarget {
    @Serializable
    @SerialName("DEMO")
    data object Demo : CallTarget

    @Serializable
    @SerialName("WECHAT_VIDEO")
    data class WeChatVideo(val localBindingId: String) : CallTarget

    @Serializable
    @SerialName("PHONE")
    data class Phone(val number: String) : CallTarget
}

@Serializable
enum class DialMode { TAP_PHOTO, LARGE_BUTTON }

/** 真实蜂窝拨号方式：DIRECT_CALL 一键直拨（需 CALL_PHONE）；SYSTEM_DIALER 系统拨号盘（零权限）。 */
@Serializable
enum class PhoneDialMethod { DIRECT_CALL, SYSTEM_DIALER }

@Serializable
data class FamilyContact(
    val id: String,
    val displayName: String,
    val photoPath: String,
    val voicePath: String,
    val order: Int,
    val callTarget: CallTarget = CallTarget.Demo,
)

@Serializable
data class AppSettings(
    val dialMode: DialMode = DialMode.TAP_PHOTO,
    val autoPlayVoice: Boolean = true,
    val defaultContactId: String? = null,
    val phoneDialMethod: PhoneDialMethod = PhoneDialMethod.DIRECT_CALL,
)

@Serializable
data class AppConfig(
    val schemaVersion: Int = 1,
    val configVersion: Long = 0,
    val settings: AppSettings = AppSettings(),
    val contacts: List<FamilyContact> = emptyList(),
) {
    val defaultContact: FamilyContact?
        get() = contacts.firstOrNull { it.id == settings.defaultContactId } ?: contacts.firstOrNull()

    fun contact(id: String): FamilyContact? = contacts.firstOrNull { it.id == id }
}