package org.changlianxi.app.data

import java.util.UUID
import org.changlianxi.app.model.AppConfig
import org.changlianxi.app.model.CallTarget
import org.changlianxi.app.model.DialMode
import org.changlianxi.app.model.FamilyContact

/** 联系人业务规则。同步 API，调用方负责放到 IO 线程。所有变更经 ConfigStore 原子落盘。 */
class ContactRepository(private val store: ConfigStore, private val media: MediaFiles) {

    fun load(): AppConfig? = store.load()

    fun newContactId(): String = UUID.randomUUID().toString()

    /**
     * 提交联系人：staging 媒体先转正并验证，再写配置。
     * 媒体不齐备时抛异常，旧配置不变；staging 由调用方决定保留（继续编辑）或丢弃。
     */
    fun commitContact(id: String, displayName: String, makeDefault: Boolean): AppConfig {
        val name = displayName.trim()
        require(name.isNotEmpty()) { "请填写称呼" }
        media.commitMedia(id)
        val current = store.load() ?: AppConfig()
        val existing = current.contact(id)
        val contact = FamilyContact(
            id = id,
            displayName = name,
            photoPath = "media/photo/$id.jpg",
            voicePath = "media/voice/$id.m4a",
            order = existing?.order ?: current.contacts.size,
            callTarget = existing?.callTarget ?: CallTarget.Demo,
        )
        val contacts = if (existing == null) current.contacts + contact
        else current.contacts.map { if (it.id == id) contact else it }
        val defaultId = when {
            makeDefault -> id
            current.settings.defaultContactId != null -> current.settings.defaultContactId
            else -> id
        }
        return store.save(
            current.copy(contacts = contacts, settings = current.settings.copy(defaultContactId = defaultId))
        )
    }

    fun deleteContact(id: String): AppConfig {
        val current = store.load() ?: return AppConfig()
        val contacts = current.contacts.filterNot { it.id == id }
        val defaultId = current.settings.defaultContactId.takeIf { it != id && contacts.any { c -> c.id == it } }
            ?: contacts.firstOrNull()?.id
        val saved = store.save(
            current.copy(contacts = contacts, settings = current.settings.copy(defaultContactId = defaultId))
        )
        media.deleteContactMedia(id)
        return saved
    }

    /** direction: -1 上移，+1 下移。边界不动。按 id 操作，与分页 index 无关。 */
    fun moveContact(id: String, direction: Int): AppConfig {
        val current = store.load() ?: return AppConfig()
        val index = current.contacts.indexOfFirst { it.id == id }
        val target = index + direction
        if (index < 0 || target !in current.contacts.indices) return current
        val mutable = current.contacts.toMutableList()
        mutable[index] = mutable[target].also { mutable[target] = mutable[index] }
        return store.save(current.copy(contacts = mutable.mapIndexed { i, c -> c.copy(order = i) }))
    }

    fun setDefault(id: String): AppConfig {
        val current = store.load() ?: return AppConfig()
        require(current.contacts.any { it.id == id }) { "默认联系人必须在列表中" }
        return store.save(current.copy(settings = current.settings.copy(defaultContactId = id)))
    }

    fun setDialMode(mode: DialMode): AppConfig {
        val current = store.load() ?: return AppConfig()
        return store.save(current.copy(settings = current.settings.copy(dialMode = mode)))
    }

    fun setAutoPlayVoice(enabled: Boolean): AppConfig {
        val current = store.load() ?: return AppConfig()
        return store.save(current.copy(settings = current.settings.copy(autoPlayVoice = enabled)))
    }
}