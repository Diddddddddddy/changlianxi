package org.changlianxi.app.ui.family

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import java.io.File
import org.changlianxi.app.ui.AppViewModel
import org.changlianxi.app.ui.FilePhoto

@Composable
fun ContactEditorScreen(
    state: AppViewModel.EditorState,
    photoFile: File?,
    onName: (String) -> Unit,
    onPhone: (String) -> Unit,
    onMakeDefault: (Boolean) -> Unit,
    onPhotoPicked: (android.net.Uri) -> Unit,
    onVoicePicked: (android.net.Uri) -> Unit,
    onStartRecord: () -> Unit,
    onStopRecord: () -> Unit,
    onPlayPreview: () -> Unit,
    onStopPreview: () -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    var showDiscard by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = !state.saving) { showDiscard = true }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) onPhotoPicked(uri)
    }
    val audioPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) onVoicePicked(uri)
    }
    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) onStartRecord()
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Text(if (state.isNew) "添加家人" else "编辑家人", fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = state.name,
            onValueChange = onName,
            label = { Text("称呼（如：女儿）") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = state.phone,
            onValueChange = onPhone,
            label = { Text("手机号（选填，用于真实拨打）") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            "填了号码：老人点照片拨打真实电话；留空：仅演示。号码仅存本机，不上传、不进公开演示。",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))

        Text("照片", fontSize = 18.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(8.dp))
        FilePhoto(photoFile, state.photoVersion.toLong(), Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(12.dp)))
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            modifier = Modifier.fillMaxWidth().height(48.dp),
        ) { Text(if (state.hasPhoto) "换一张照片" else "选择照片", fontSize = 16.sp) }

        Spacer(Modifier.height(20.dp))
        HorizontalDivider()
        Spacer(Modifier.height(16.dp))

        Text("本人声音", fontSize = 18.sp, fontWeight = FontWeight.Medium)
        Text("请这位家人自己录一句话，例如「妈，想我了就点照片」。建议 3—8 秒。", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (state.recording) {
                Button(
                    onClick = onStopRecord,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.weight(1f).height(48.dp),
                ) { Text("停止录音（${state.recordSeconds}s）", fontSize = 16.sp) }
            } else {
                Button(
                    onClick = {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                            onStartRecord()
                        } else {
                            micPermission.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                ) { Text(if (state.hasVoice) "重新录音" else "开始录音", fontSize = 16.sp) }
            }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(
                onClick = { if (state.previewPlaying) onStopPreview() else onPlayPreview() },
                enabled = state.hasVoice && !state.recording,
                modifier = Modifier.weight(1f).height(48.dp),
            ) { Text(if (state.previewPlaying) "停止" else "试听", fontSize = 16.sp) }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = { audioPicker.launch(arrayOf("audio/*")) },
            enabled = !state.recording,
            modifier = Modifier.fillMaxWidth().height(48.dp),
        ) { Text("导入录音文件", fontSize = 16.sp) }
        if (state.hasVoice && state.recordSeconds > 8) {
            Text("这段录音有点长，建议简短一些，老人更容易听清。", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("打开 App 默认显示这位家人", fontSize = 16.sp, modifier = Modifier.weight(1f))
            Switch(checked = state.makeDefault, onCheckedChange = onMakeDefault)
        }

        if (state.error != null) {
            Spacer(Modifier.height(8.dp))
            Text(state.error!!, color = MaterialTheme.colorScheme.error, fontSize = 15.sp)
        }

        Spacer(Modifier.height(20.dp))
        Button(
            onClick = onSave,
            enabled = !state.saving,
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) { Text(if (state.saving) "保存中…" else "保存", fontSize = 20.sp) }
        TextButton(onClick = onCancel, enabled = !state.saving, modifier = Modifier.fillMaxWidth()) {
            Text("取消", fontSize = 16.sp)
        }
        Spacer(Modifier.height(16.dp))
    }

    if (showDiscard) {
        AlertDialog(
            onDismissRequest = { showDiscard = false },
            title = { Text("放弃修改？") },
            text = { Text("未保存的称呼、照片和录音会被丢弃。") },
            confirmButton = {
                TextButton(onClick = { showDiscard = false; onCancel() }) {
                    Text("放弃", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { showDiscard = false }) { Text("继续编辑") } },
        )
    }
}