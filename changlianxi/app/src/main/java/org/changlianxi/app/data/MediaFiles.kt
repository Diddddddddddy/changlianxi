package org.changlianxi.app.data

import java.io.File
import java.io.InputStream
import org.changlianxi.app.model.FamilyContact

/** 私有媒体文件：staging 写入 → commit 原子改名 → 配置提交。文件名与联系人 id 绑定。 */
class MediaFiles(root: File) {
    private val photoDir = File(root, "media/photo")
    private val voiceDir = File(root, "media/voice")
    private val stagingDir = File(root, "media/staging")

    fun photoFile(id: String) = File(photoDir, "$id.jpg")
    fun voiceFile(id: String) = File(voiceDir, "$id.m4a")
    fun stagingPhoto(id: String) = File(stagingDir, "$id.jpg")
    fun stagingVoice(id: String) = File(stagingDir, "$id.m4a")

    fun writeStagingPhoto(id: String, input: InputStream) = write(stagingPhoto(id), input)
    fun writeStagingVoice(id: String, input: InputStream) = write(stagingVoice(id), input)

    /** staging 改名为正式文件；无 staging 时保留已有正式文件。返回后两个正式文件必须存在且非空。 */
    fun commitMedia(id: String) {
        moveIfExists(stagingPhoto(id), photoFile(id))
        moveIfExists(stagingVoice(id), voiceFile(id))
        check(isReadable(photoFile(id))) { "照片文件缺失或为空" }
        check(isReadable(voiceFile(id))) { "录音文件缺失或为空" }
    }

    fun discardStaging(id: String) {
        stagingPhoto(id).delete()
        stagingVoice(id).delete()
    }

    fun deleteContactMedia(id: String) {
        photoFile(id).delete()
        voiceFile(id).delete()
        discardStaging(id)
    }

    fun mediaReady(id: String) = isReadable(photoFile(id)) && isReadable(voiceFile(id))

    fun photoReady(id: String) = isReadable(photoFile(id)) || isReadable(stagingPhoto(id))
    fun voiceReady(id: String) = isReadable(voiceFile(id)) || isReadable(stagingVoice(id))

    /** 编辑预览：优先未提交的 staging，其次正式文件。 */
    fun previewPhoto(id: String): File? = stagingPhoto(id).takeIf { isReadable(it) } ?: photoFile(id).takeIf { isReadable(it) }
    fun previewVoice(id: String): File? = stagingVoice(id).takeIf { isReadable(it) } ?: voiceFile(id).takeIf { isReadable(it) }

    /** 老人首页播放：仅用已提交的正式录音，不碰未提交 staging；缺失返回 null（不崩溃）。 */
    fun playbackVoice(id: String): File? = voiceFile(id).takeIf { isReadable(it) }

    private fun write(target: File, input: InputStream) {
        target.parentFile?.mkdirs()
        val tmp = File(target.parentFile, target.name + ".tmp")
        input.use { src -> tmp.outputStream().use { src.copyTo(it) } }
        if (!isReadable(tmp)) {
            tmp.delete()
            error("媒体写入失败")
        }
        if (target.exists()) target.delete()
        check(tmp.renameTo(target)) { "媒体文件改名失败" }
    }

    private fun moveIfExists(from: File, to: File) {
        if (!from.exists()) return
        to.parentFile?.mkdirs()
        if (to.exists()) to.delete()
        check(from.renameTo(to)) { "媒体文件提交失败" }
    }

    private fun isReadable(f: File) = f.isFile && f.length() > 0 && f.canRead()
}