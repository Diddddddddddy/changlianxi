package org.changlianxi.app.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.changlianxi.app.audio.StatusVoice
import org.changlianxi.app.audio.VoicePlayer
import org.changlianxi.app.audio.VoiceRecorder
import org.changlianxi.app.call.CallError
import org.changlianxi.app.call.CallGateway
import org.changlianxi.app.call.CallPhase
import org.changlianxi.app.call.CallStartResult
import org.changlianxi.app.call.CallStateMachine
import org.changlianxi.app.call.RealCallGateway
import org.changlianxi.app.call.RealCallPlanner
import org.changlianxi.app.call.StatusPhrases
import org.changlianxi.app.data.ConfigStore
import org.changlianxi.app.data.ContactRepository
import org.changlianxi.app.data.ImageImporter
import org.changlianxi.app.data.MediaFiles
import org.changlianxi.app.data.PinStore
import org.changlianxi.app.model.AppConfig
import org.changlianxi.app.model.CallTarget
import org.changlianxi.app.model.DialMode
import org.changlianxi.app.model.PhoneDialMethod

class AppViewModel(app: Application) : AndroidViewModel(app) {

    sealed interface Screen {
        data object Loading : Screen
        data object SetupWelcome : Screen
        data object CreatePin : Screen
        data object Elder : Screen
        data object VerifyPin : Screen
        data object Family : Screen
        data object Editor : Screen
        data object Call : Screen
    }

    data class EditorState(
        val contactId: String,
        val isNew: Boolean,
        val name: String = "",
        val phone: String = "",
        val hasPhoto: Boolean = false,
        val hasVoice: Boolean = false,
        val makeDefault: Boolean = false,
        val recording: Boolean = false,
        val recordSeconds: Int = 0,
        val previewPlaying: Boolean = false,
        val photoVersion: Int = 0,
        val saving: Boolean = false,
        val error: String? = null,
    )

    private val repo = ContactRepository(ConfigStore(app.filesDir), MediaFiles(app.filesDir))
    private val media = MediaFiles(app.filesDir)
    private val pinStore = PinStore(app.filesDir)
    private val player = VoicePlayer(app)
    private val statusVoice = StatusVoice(app)
    private val recorder = VoiceRecorder(app)
    private val gateway: CallGateway = RealCallGateway(app) { _config.value?.settings }
    private val machine = CallStateMachine()
    private var recordTicker: Job? = null
    private var settleVoiceJob: Job? = null

    /** 老人首页当前停稳的联系人；跨页面切换后按 id 恢复，不依赖旧 index。 */
    var elderContactId: String? = null
        private set

    /** 页面停稳后延迟 300ms 播放本人录音；换页立即取消旧任务并停播，快速连滑只播最终页。 */
    fun onElderSettled(contactId: String) {
        elderContactId = contactId
        settleVoiceJob?.cancel()
        player.stop()
        val cfg = _config.value ?: return
        if (!cfg.settings.autoPlayVoice || _screen.value != Screen.Elder) return
        settleVoiceJob = viewModelScope.launch {
            delay(300)
            if (elderContactId != contactId || _screen.value != Screen.Elder) return@launch
            val file = media.playbackVoice(contactId) ?: return@launch
            player.play(file)
        }
    }

    private fun cancelSettleVoice() {
        settleVoiceJob?.cancel()
        settleVoiceJob = null
        player.stop()
    }

    private val _screen = MutableStateFlow<Screen>(Screen.Loading)
    val screen: StateFlow<Screen> = _screen

    private val _call = MutableStateFlow<CallPhase>(CallPhase.Idle)
    val call: StateFlow<CallPhase> = _call

    private val _config = MutableStateFlow<AppConfig?>(null)
    val config: StateFlow<AppConfig?> = _config

    private val _editor = MutableStateFlow<EditorState?>(null)
    val editor: StateFlow<EditorState?> = _editor

    init {
        viewModelScope.launch {
            val loaded = withContext(Dispatchers.IO) { repo.load() }
            _config.value = loaded
            _screen.value = when {
                loaded != null && loaded.contacts.isNotEmpty() -> Screen.Elder
                pinStore.isSetUp() -> Screen.Family
                else -> Screen.SetupWelcome
            }
        }
    }

    // ---------- 首装与 PIN ----------

    fun startSetup() { _screen.value = Screen.CreatePin }

    /** 返回错误文案；null 表示成功。 */
    fun createPin(pin: String, confirm: String): String? {
        if (!pin.matches(Regex("\\d{4,6}"))) return "PIN 需为 4—6 位数字"
        if (pin != confirm) return "两次输入不一致"
        pinStore.createPin(pin)
        _screen.value = Screen.Family
        return null
    }

    fun verifyPin(pin: String): String? {
        return when (val r = pinStore.verify(pin)) {
            is PinStore.Result.Ok -> { _screen.value = Screen.Family; null }
            is PinStore.Result.Wrong -> "PIN 不对，还可以试 ${r.remainingAttempts} 次"
            is PinStore.Result.Locked -> "尝试次数过多，请 ${r.secondsLeft / 60 + 1} 分钟后再试"
            is PinStore.Result.NotSetUp -> "尚未设置 PIN"
        }
    }

    fun requestFamilyEntry() {
        cancelSettleVoice()
        _screen.value = Screen.VerifyPin
    }

    fun cancelVerify() { _screen.value = Screen.Elder }

    // ---------- 家属列表操作 ----------

    private fun mutate(block: () -> AppConfig) {
        viewModelScope.launch {
            _config.value = withContext(Dispatchers.IO) { block() }
        }
    }

    fun deleteContact(id: String) = mutate { repo.deleteContact(id) }
    fun moveContact(id: String, direction: Int) = mutate { repo.moveContact(id, direction) }
    fun setDefault(id: String) = mutate { repo.setDefault(id) }
    fun setDialMode(mode: DialMode) = mutate { repo.setDialMode(mode) }
    fun setPhoneDialMethod(method: PhoneDialMethod) = mutate { repo.setPhoneDialMethod(method) }
    fun setAutoPlayVoice(enabled: Boolean) = mutate { repo.setAutoPlayVoice(enabled) }

    fun finishFamily() {
        if (_config.value?.contacts.isNullOrEmpty()) return
        _screen.value = Screen.Elder
    }

    // ---------- 拨号与演示呼叫状态机 ----------

    private fun syncCall() { _call.value = machine.phase }

    /** 只接受老人首页的一次有效点击；呼叫期间锁定 contactId，过期结果丢弃。 */
    fun dial(contactId: String) {
        if (_screen.value != Screen.Elder) return
        val contact = _config.value?.contact(contactId) ?: return
        val requestId = java.util.UUID.randomUUID().toString()
        if (!machine.request(contactId, requestId)) return  // 防双击／重复呼叫
        cancelSettleVoice()
        statusVoice.speak(StatusPhrases.REQUEST)
        syncCall()
        _screen.value = Screen.Call
        viewModelScope.launch {
            val result = gateway.startVideoCall(contact)
            if (machine.requestId != requestId) return@launch  // 过期事务丢弃
            when (result) {
                is CallStartResult.Failed -> {
                    machine.fail(requestId, result.error)
                    failAndShow(result.error)
                }
                is CallStartResult.HandoffStarted -> { machine.handoff(requestId); syncCall() }
                else -> { machine.demoStarted(requestId); syncCall() }
            }
        }
    }

    /** 演示控件：模拟默认家人来电。 */
    fun simulateIncoming() {
        if (_screen.value != Screen.Elder && _screen.value != Screen.Family) return
        val contact = _config.value?.defaultContact ?: return
        val requestId = java.util.UUID.randomUUID().toString()
        if (!machine.incoming(contact.id, requestId)) return
        cancelSettleVoice()
        syncCall()
        _screen.value = Screen.Call
    }

    /** 演示控件：对方接听。 */
    fun demoAnswer() {
        val rid = machine.requestId ?: return
        if (machine.demoAnswer(rid)) syncCall()
    }

    /** 演示控件：无人接听 → 失败回退。 */
    fun demoNoAnswer() {
        val rid = machine.requestId ?: return
        if (machine.demoNoAnswer(rid)) failAndShow(CallError.TIMEOUT_UNCONFIRMED)
    }

    /** 演示控件：断网失败 → 正确语音与返回路径，不声称已接通。 */
    fun simulateNetworkFail() {
        val rid = machine.requestId ?: return
        if (machine.fail(rid, CallError.NETWORK_UNAVAILABLE)) failAndShow(CallError.NETWORK_UNAVAILABLE)
    }

    /** 模拟来电：点照片接听。 */
    fun incomingAnswer() {
        val rid = machine.requestId ?: return
        if (machine.incomingAnswer(rid)) syncCall()
    }

    /** 模拟来电：红色拒接 → 返回原照片。 */
    fun incomingDecline() {
        val rid = machine.requestId ?: return
        if (machine.incomingDecline(rid)) endCall()
    }

    /** 模拟已接通：红色挂断，只结束一次。 */
    fun hangup() {
        val rid = machine.requestId ?: return
        if (machine.hangup(rid)) endCall()
    }

    /** 取消拨号（Preparing/Ringing）→ 返回原照片。 */
    fun cancelCall() {
        val rid = machine.requestId ?: return
        if (machine.cancel(rid)) endCall()
    }

    /** 系统返回：按当前状态分派（接通挂断／来电拒接／失败返回／其余取消）。 */
    fun backFromCall() {
        when (machine.phase) {
            is CallPhase.DemoConnected -> hangup()
            is CallPhase.IncomingDemo -> incomingDecline()
            is CallPhase.Failed -> endCall()
            is CallPhase.Handoff -> endCall()
            else -> cancelCall()
        }
    }

    private fun failAndShow(reason: CallError) {
        syncCall()
        statusVoice.speak(StatusPhrases.forError(reason))
    }

    private fun endCall() {
        statusVoice.stop()
        machine.reset()
        syncCall()
        _screen.value = Screen.Elder
    }

    // ---------- 联系人编辑器 ----------

    fun openEditor(contactId: String?) {
        cancelSettleVoice()
        val isNew = contactId == null
        val id = contactId ?: repo.newContactId()
        val existing = _config.value?.contact(id)
        _editor.value = EditorState(
            contactId = id,
            isNew = isNew,
            name = existing?.displayName ?: "",
            phone = (existing?.callTarget as? CallTarget.Phone)?.number ?: "",
            hasPhoto = media.photoReady(id),
            hasVoice = media.voiceReady(id),
            makeDefault = existing != null && existing.id == _config.value?.settings?.defaultContactId,
        )
        _screen.value = Screen.Editor
    }

    fun updateName(name: String) { _editor.value = _editor.value?.copy(name = name, error = null) }
    fun updatePhone(phone: String) { _editor.value = _editor.value?.copy(phone = phone, error = null) }
    fun updateMakeDefault(value: Boolean) { _editor.value = _editor.value?.copy(makeDefault = value) }

    fun importPhoto(uri: Uri) {
        val state = _editor.value ?: return
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val bytes = ImageImporter.importCompressed(getApplication<Application>(), uri)
                    media.writeStagingPhoto(state.contactId, bytes.inputStream())
                }
            }.onSuccess {
                _editor.value = _editor.value?.copy(hasPhoto = true, photoVersion = state.photoVersion + 1, error = null)
            }.onFailure {
                _editor.value = _editor.value?.copy(error = "照片导入失败，请换一张试试")
            }
        }
    }

    fun importVoice(uri: Uri) {
        val state = _editor.value ?: return
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver.openInputStream(uri)?.use { input ->
                        media.writeStagingVoice(state.contactId, input)
                    } ?: error("无法读取音频")
                }
            }.onSuccess {
                _editor.value = _editor.value?.copy(hasVoice = true, error = null)
            }.onFailure {
                _editor.value = _editor.value?.copy(error = "录音导入失败，请换一个文件")
            }
        }
    }

    fun startRecording() {
        val state = _editor.value ?: return
        if (state.recording) return
        player.stop()
        runCatching { recorder.start(media.stagingVoice(state.contactId)) }
            .onSuccess {
                _editor.value = state.copy(recording = true, recordSeconds = 0, previewPlaying = false, error = null)
                recordTicker = viewModelScope.launch {
                    while (true) {
                        delay(1000)
                        val cur = _editor.value ?: break
                        if (!cur.recording) break
                        _editor.value = cur.copy(recordSeconds = cur.recordSeconds + 1)
                    }
                }
            }
            .onFailure {
                _editor.value = state.copy(error = "无法开始录音，请检查麦克风权限")
            }
    }

    fun stopRecording() {
        val state = _editor.value ?: return
        if (!state.recording) return
        recordTicker?.cancel()
        val duration = recorder.stop()
        _editor.value = if (duration > 0) {
            state.copy(recording = false, hasVoice = true, recordSeconds = (duration / 1000).toInt(), error = null)
        } else {
            state.copy(recording = false, error = "录音太短，请按住多说一句话")
        }
    }

    fun playPreview() {
        val state = _editor.value ?: return
        val file = media.previewVoice(state.contactId) ?: return
        player.play(file) { _editor.value = _editor.value?.copy(previewPlaying = false) }
        _editor.value = state.copy(previewPlaying = true)
    }

    fun stopPreview() {
        player.stop()
        _editor.value = _editor.value?.copy(previewPlaying = false)
    }

    fun saveContact() {
        val state = _editor.value ?: return
        if (state.saving) return
        if (state.name.isBlank()) { _editor.value = state.copy(error = "请填写称呼"); return }
        if (!state.hasPhoto) { _editor.value = state.copy(error = "请选择一张照片"); return }
        if (!state.hasVoice) { _editor.value = state.copy(error = "请录制或导入一段本人声音"); return }
        val phone = state.phone.trim()
        if (phone.isNotEmpty() && !RealCallPlanner.isDialable(phone)) {
            _editor.value = state.copy(error = "电话号码看起来不对，请检查"); return
        }
        val callTarget = if (phone.isEmpty()) CallTarget.Demo else CallTarget.Phone(phone)
        if (state.recording) stopRecording()
        player.stop()
        _editor.value = _editor.value?.copy(saving = true, error = null)
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) { repo.commitContact(state.contactId, state.name, state.makeDefault, callTarget) }
            }.onSuccess {
                _config.value = it
                _editor.value = null
                _screen.value = Screen.Family
            }.onFailure { e ->
                _editor.value = _editor.value?.copy(saving = false, error = e.message ?: "保存失败，请重试")
            }
        }
    }

    fun cancelEdit() {
        val state = _editor.value
        recordTicker?.cancel()
        if (recorder.isRecording) recorder.cancel()
        player.stop()
        if (state != null) media.discardStaging(state.contactId)
        _editor.value = null
        _screen.value = Screen.Family
    }

    fun previewPhotoFile(id: String): File? = media.previewPhoto(id)

    // ---------- 生命周期 ----------

    /** 退到后台：停音频、停录音、结束模拟呼叫；已配置时锁定家属页，恢复需重新验证 PIN。 */
    fun onBackground() {
        cancelSettleVoice()
        statusVoice.stop()
        recordTicker?.cancel()
        if (recorder.isRecording) recorder.cancel()
        val s = _screen.value
        if (s == Screen.Call) {
            machine.reset()
            syncCall()
            _screen.value = Screen.Elder
            return
        }
        if (!_config.value?.contacts.isNullOrEmpty() &&
            (s == Screen.Family || s == Screen.Editor || s == Screen.VerifyPin)
        ) {
            if (s == Screen.Editor) _editor.value?.let { media.discardStaging(it.contactId) }
            _editor.value = null
            _screen.value = Screen.Elder
        }
    }

    override fun onCleared() {
        player.stop()
        statusVoice.shutdown()
        recorder.cancel()
    }
}