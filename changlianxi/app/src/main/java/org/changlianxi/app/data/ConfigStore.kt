package org.changlianxi.app.data

import java.io.File
import kotlinx.serialization.json.Json
import org.changlianxi.app.model.AppConfig

/** 配置持久化：临时文件 + 原子替换；读取时结构校验与修正；损坏文件备份后按未配置处理。 */
class ConfigStore(private val dir: File) {
    private val json = Json {
        classDiscriminator = "type"
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private val configFile = File(dir, "config.json")

    fun load(): AppConfig? {
        if (!configFile.isFile) return null
        val text = runCatching { configFile.readText() }.getOrNull() ?: return null
        val config = try {
            json.decodeFromString<AppConfig>(text)
        } catch (_: Exception) {
            configFile.renameTo(File(dir, "config.json.corrupt-${System.currentTimeMillis()}"))
            return null
        }
        return normalize(config)
    }

    /** 原子写入；configVersion 自增。失败抛出，旧配置保持不变。 */
    fun save(config: AppConfig): AppConfig {
        val normalized = normalize(config).copy(configVersion = config.configVersion + 1)
        dir.mkdirs()
        val tmp = File(dir, "config.json.tmp")
        tmp.writeText(json.encodeToString(AppConfig.serializer(), normalized))
        check(tmp.length() > 0) { "配置写入失败" }
        if (!tmp.renameTo(configFile)) {
            // Windows/部分文件系统 rename 不覆盖已存在目标
            configFile.delete()
            check(tmp.renameTo(configFile)) { "配置原子替换失败" }
        }
        return normalized
    }

    private fun normalize(config: AppConfig): AppConfig {
        val ordered = config.contacts.sortedBy { it.order }.mapIndexed { i, c -> c.copy(order = i) }
        val defaultId = config.settings.defaultContactId?.takeIf { id -> ordered.any { it.id == id } }
            ?: ordered.firstOrNull()?.id
        return config.copy(contacts = ordered, settings = config.settings.copy(defaultContactId = defaultId))
    }
}