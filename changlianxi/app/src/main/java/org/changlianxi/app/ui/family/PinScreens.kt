package org.changlianxi.app.ui.family

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SetupWelcomeScreen(onStart: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("常联系", fontSize = 44.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        Text("想你了，就见一面。", fontSize = 22.sp)
        Spacer(Modifier.height(48.dp))
        Text("首次使用，请家属先完成设置：", fontSize = 22.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text("设置 PIN → 添加家人照片和本人录音 → 交给老人使用", fontSize = 18.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(48.dp))
        Button(onClick = onStart, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Text("家属开始设置", fontSize = 20.sp)
        }
    }
}

@Composable
fun CreatePinScreen(onCreate: (String, String) -> String?) {
    var pin by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    PinScaffold(title = "设置家属 PIN", subtitle = "4—6 位数字，用于进入家属设置。请家属牢记，不要告诉老人。") {
        OutlinedTextField(
            value = pin, onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) { pin = it; error = null } },
            label = { Text("输入 PIN") }, singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = confirm, onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) { confirm = it; error = null } },
            label = { Text("再输入一次") }, singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.fillMaxWidth(),
        )
        PinError(error)
        Button(
            onClick = { error = onCreate(pin, confirm) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) { Text("确定", fontSize = 20.sp) }
    }
}

@Composable
fun VerifyPinScreen(onVerify: (String) -> String?, onCancel: () -> Unit) {
    var pin by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    PinScaffold(title = "输入家属 PIN", subtitle = "此页面仅供家属操作。") {
        OutlinedTextField(
            value = pin, onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) { pin = it; error = null } },
            label = { Text("PIN") }, singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.fillMaxWidth(),
        )
        PinError(error)
        Button(
            onClick = { error = onVerify(pin) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) { Text("进入设置", fontSize = 20.sp) }
        TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text("返回照片", fontSize = 16.sp) }
    }
}

@Composable
private fun PinScaffold(title: String, subtitle: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(title, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text(subtitle, fontSize = 16.sp, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(32.dp))
        content()
    }
}

@Composable
private fun PinError(error: String?) {
    Spacer(Modifier.height(8.dp))
    if (error != null) {
        Text(error, color = MaterialTheme.colorScheme.error, fontSize = 15.sp)
        Spacer(Modifier.height(8.dp))
    }
}