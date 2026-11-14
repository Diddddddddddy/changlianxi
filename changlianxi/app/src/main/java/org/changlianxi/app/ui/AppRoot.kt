package org.changlianxi.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.changlianxi.app.call.CallPhase
import org.changlianxi.app.call.contactIdOrNull
import org.changlianxi.app.model.AppConfig
import org.changlianxi.app.ui.elder.DemoCallScreen
import org.changlianxi.app.ui.elder.ElderHomeScreen
import org.changlianxi.app.ui.elder.IncomingCallScreen
import org.changlianxi.app.ui.elder.RealCallScreen
import org.changlianxi.app.ui.family.ContactEditorScreen
import org.changlianxi.app.ui.family.ContactListScreen
import org.changlianxi.app.ui.family.CreatePinScreen
import org.changlianxi.app.ui.family.SetupWelcomeScreen
import org.changlianxi.app.ui.family.VerifyPinScreen
import org.changlianxi.app.ui.theme.ChanglianxiTheme

@Composable
fun AppRoot(vm: AppViewModel) {
    val screen by vm.screen.collectAsStateWithLifecycle()
    val config by vm.config.collectAsStateWithLifecycle()
    val editor by vm.editor.collectAsStateWithLifecycle()
    val current = screen

    ChanglianxiTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            when (current) {
                AppViewModel.Screen.Loading -> Box(Modifier.fillMaxSize())
                AppViewModel.Screen.SetupWelcome -> SetupWelcomeScreen(onStart = vm::startSetup)
                AppViewModel.Screen.CreatePin -> CreatePinScreen(onCreate = vm::createPin)
                AppViewModel.Screen.VerifyPin -> {
                    BackHandler { vm.cancelVerify() }
                    VerifyPinScreen(onVerify = vm::verifyPin, onCancel = vm::cancelVerify)
                }
                AppViewModel.Screen.Family -> {
                    BackHandler(enabled = config?.contacts?.isNotEmpty() == true) { vm.finishFamily() }
                    ContactListScreen(
                    config = config ?: AppConfig(),
                    photoFile = vm::previewPhotoFile,
                    onAdd = { vm.openEditor(null) },
                    onEdit = vm::openEditor,
                    onDelete = vm::deleteContact,
                    onMove = vm::moveContact,
                    onSetDefault = vm::setDefault,
                    onDialMode = vm::setDialMode,
                    onPhoneDialMethod = vm::setPhoneDialMethod,
                    onAutoPlay = vm::setAutoPlayVoice,
                    onSimulateIncoming = vm::simulateIncoming,
                    onFinish = vm::finishFamily,
                    )
                }
                AppViewModel.Screen.Editor -> editor?.let { state ->
                    ContactEditorScreen(
                        state = state,
                        photoFile = vm.previewPhotoFile(state.contactId),
                        onName = vm::updateName,
                        onPhone = vm::updatePhone,
                        onMakeDefault = vm::updateMakeDefault,
                        onPhotoPicked = vm::importPhoto,
                        onVoicePicked = vm::importVoice,
                        onStartRecord = vm::startRecording,
                        onStopRecord = vm::stopRecording,
                        onPlayPreview = vm::playPreview,
                        onStopPreview = vm::stopPreview,
                        onSave = vm::saveContact,
                        onCancel = vm::cancelEdit,
                    )
                }
                AppViewModel.Screen.Elder -> config?.let {
                    ElderHomeScreen(
                        config = it,
                        photoFile = vm::previewPhotoFile,
                        initialContactId = vm.elderContactId,
                        onSettled = vm::onElderSettled,
                        onDial = vm::dial,
                        onFamilyEntry = vm::requestFamilyEntry,
                    )
                }
                AppViewModel.Screen.Call -> {
                    val phase by vm.call.collectAsStateWithLifecycle()
                    val cid = phase.contactIdOrNull ?: ""
                    val name = config?.contact(cid)?.displayName ?: "家人"
                    BackHandler { vm.backFromCall() }
                    if (phase is CallPhase.IncomingDemo) {
                        IncomingCallScreen(
                            name = name,
                            photoFile = vm.previewPhotoFile(cid),
                            configVersion = config?.configVersion ?: 0L,
                            onAnswer = vm::incomingAnswer,
                            onDecline = vm::incomingDecline,
                        )
                    } else if (phase is CallPhase.Handoff) {
                        RealCallScreen(
                            name = name,
                            photoFile = vm.previewPhotoFile(cid),
                            configVersion = config?.configVersion ?: 0L,
                            onBack = vm::backFromCall,
                        )
                    } else {
                        DemoCallScreen(
                            phase = phase,
                            name = name,
                            photoFile = vm.previewPhotoFile(cid),
                            configVersion = config?.configVersion ?: 0L,
                            onAnswer = vm::demoAnswer,
                            onNoAnswer = vm::demoNoAnswer,
                            onNetworkFail = vm::simulateNetworkFail,
                            onBack = vm::backFromCall,
                        )
                    }
                }
            }
        }
    }
}