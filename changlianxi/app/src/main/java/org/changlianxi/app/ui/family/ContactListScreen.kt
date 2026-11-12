package org.changlianxi.app.ui.family

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.changlianxi.app.model.AppConfig
import org.changlianxi.app.model.DialMode
import org.changlianxi.app.ui.FilePhoto

@Composable
fun ContactListScreen(
    config: AppConfig,
    photoFile: (String) -> java.io.File?,
    onAdd: () -> Unit,
    onEdit: (String) -> Unit,
    onDelete: (String) -> Unit,
    onMove: (String, Int) -> Unit,
    onSetDefault: (String) -> Unit,
    onDialMode: (DialMode) -> Unit,
    onAutoPlay: (Boolean) -> Unit,
    onSimulateIncoming: () -> Unit,
    onFinish: () -> Unit,
) {
    var deleting by rememberSaveable { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Text("家属设置", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text("老人打开 App 会直接看到默认家人的大照片。", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))

        if (config.contacts.isEmpty()) {
            Text("还没有家人，请先添加至少一位。", fontSize = 17.sp)
            Spacer(Modifier.height(12.dp))
        }

        config.contacts.forEachIndexed { index, contact ->
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    FilePhoto(photoFile(contact.id), config.configVersion, Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)))
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(contact.displayName, fontSize = 20.sp, fontWeight = FontWeight.Medium)
                        if (contact.id == config.settings.defaultContactId) {
                            Text("默认打开", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                        } else {
                            TextButton(onClick = { onSetDefault(contact.id) }, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                                Text("设为默认", fontSize = 13.sp)
                            }
                        }
                    }
                    IconButton(onClick = { onMove(contact.id, -1) }, enabled = index > 0) {
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "上移")
                    }
                    IconButton(onClick = { onMove(contact.id, 1) }, enabled = index < config.contacts.lastIndex) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "下移")
                    }
                    IconButton(onClick = { onEdit(contact.id) }) {
                        Icon(Icons.Default.Edit, contentDescription = "编辑")
                    }
                    IconButton(onClick = { deleting = contact.id }) {
                        Icon(Icons.Default.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Button(onClick = onAdd, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("添加家人", fontSize = 18.sp)
        }

        Spacer(Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(Modifier.height(16.dp))

        Text("拨打方式", fontSize = 18.sp, fontWeight = FontWeight.Medium)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            RadioButton(selected = config.settings.dialMode == DialMode.TAP_PHOTO, onClick = { onDialMode(DialMode.TAP_PHOTO) })
            Text("点照片拨打（推荐）", fontSize = 16.sp)
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            RadioButton(selected = config.settings.dialMode == DialMode.LARGE_BUTTON, onClick = { onDialMode(DialMode.LARGE_BUTTON) })
            Text("独立大按钮拨打", fontSize = 16.sp)
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("翻页停稳后自动播放本人录音", fontSize = 16.sp, modifier = Modifier.weight(1f))
            Switch(checked = config.settings.autoPlayVoice, onCheckedChange = onAutoPlay)
        }

        Spacer(Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(Modifier.height(16.dp))
        Text("演示控件", fontSize = 18.sp, fontWeight = FontWeight.Medium)
        Text("用于比赛演示，不产生真实呼叫。", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = onSimulateIncoming,
            enabled = config.contacts.isNotEmpty(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ),
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) { Text("模拟来电（演示）", fontSize = 18.sp) }

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onFinish,
            enabled = config.contacts.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) { Text("完成，进入老人首页", fontSize = 20.sp) }
        Spacer(Modifier.height(16.dp))
    }

    val deletingId = deleting
    if (deletingId != null) {
        val name = config.contact(deletingId)?.displayName ?: ""
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("删除「$name」？") },
            text = { Text("照片和录音会一并删除。如果删的是默认家人，会自动改为列表第一位。") },
            confirmButton = {
                TextButton(onClick = { onDelete(deletingId); deleting = null }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("取消") } },
        )
    }
}