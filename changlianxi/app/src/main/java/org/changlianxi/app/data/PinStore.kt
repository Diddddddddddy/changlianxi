package org.changlianxi.app.data

import java.io.File
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** 家属 PIN：4—6 位数字，PBKDF2 加盐哈希落盘，不明文、不打日志。连续失败锁定冷却。 */
class PinStore(
    private val dir: File,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    @Serializable
    private data class Record(
        val salt: String,
        val hash: String,
        val iterations: Int,
        val failedAttempts: Int = 0,
        val lockUntil: Long = 0,
    )

    private val json = Json { ignoreUnknownKeys = true }
    private val pinFile = File(dir, "pin.json")

    fun isSetUp(): Boolean = read() != null

    fun createPin(pin: String) {
        require(pin.matches(Regex("\\d{4,6}"))) { "PIN 需为 4—6 位数字" }
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val hash = hash(pin, salt, ITERATIONS)
        write(Record(salt.hex(), hash.hex(), ITERATIONS))
    }

    sealed interface Result {
        data object Ok : Result
        data class Wrong(val remainingAttempts: Int) : Result
        data class Locked(val secondsLeft: Long) : Result
        data object NotSetUp : Result
    }

    fun verify(pin: String): Result {
        val record = read() ?: return Result.NotSetUp
        val now = clock()
        if (record.lockUntil > now) return Result.Locked((record.lockUntil - now + 999) / 1000)
        val candidate = hash(pin, record.salt.unhex(), record.iterations)
        return if (MessageDigest.isEqual(candidate, record.hash.unhex())) {
            write(record.copy(failedAttempts = 0, lockUntil = 0))
            Result.Ok
        } else {
            val failed = record.failedAttempts + 1
            val locked = failed >= MAX_ATTEMPTS
            write(record.copy(failedAttempts = if (locked) 0 else failed, lockUntil = if (locked) now + LOCK_MILLIS else 0))
            if (locked) Result.Locked(LOCK_MILLIS / 1000) else Result.Wrong(MAX_ATTEMPTS - failed)
        }
    }

    private fun hash(pin: String, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, iterations, 256)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
    }

    private fun read(): Record? {
        if (!pinFile.isFile) return null
        return runCatching { json.decodeFromString<Record>(pinFile.readText()) }.getOrNull()
    }

    private fun write(record: Record) {
        dir.mkdirs()
        val tmp = File(dir, "pin.json.tmp")
        tmp.writeText(json.encodeToString(Record.serializer(), record))
        if (!tmp.renameTo(pinFile)) {
            pinFile.delete()
            check(tmp.renameTo(pinFile)) { "PIN 存储失败" }
        }
    }

    private fun ByteArray.hex() = joinToString("") { "%02x".format(it) }
    private fun String.unhex() = ByteArray(length / 2) { substring(it * 2, it * 2 + 2).toInt(16).toByte() }

    private companion object {
        const val ITERATIONS = 120_000
        const val MAX_ATTEMPTS = 5
        const val LOCK_MILLIS = 5 * 60_000L
    }
}