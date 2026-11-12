package org.changlianxi.app.ui.elder

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import org.changlianxi.app.call.CallPhase
import org.changlianxi.app.ui.FilePhoto

/** 本地模拟呼叫页：按状态机渲染，明确标注演示，不产生真实呼叫。演示控件驱动分支。 */
@Composable
fun DemoCallScreen(
    phase: CallPhase,
    name: String,
    photoFile: File?,
    configVersion: Long,
    onAnswer: () -> Unit,
    onNoAnswer: () -> Unit,
    onNetworkFail: () -> Unit,
    onBack: () -> Unit,
) {
    val status: String
    val subtitle: String
    val primaryLabel: String
    when (phase) {
        is CallPhase.DemoRinging -> { status = "等待对方接听"; subtitle = "演示呼叫，非真实通话"; primaryLabel = "取消" }
        is CallPhase.DemoConnected -> { status = "模拟已接通"; subtitle = "演示画面，非真实通话"; primaryLabel = "挂断" }
        is CallPhase.Failed -> { status = "电话没有打出去"; subtitle = "演示失败提示，未真实拨出"; primaryLabel = "返回" }
        else -> { status = "正在呼叫"; subtitle = "不会真实拨出电话"; primaryLabel = "取消" }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF1B1B1B))) {
        FilePhoto(photoFile, configVersion, Modifier.fillMaxSize())
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)))

        Column(
            modifier = Modifier.align(Alignment.Center).fillMaxWidth().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "模拟演示",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 16.dp, vertical = 6.dp),
            )
            Spacer(Modifier.height(24.dp))
            Text(status, fontSize = 26.sp, color = Color.White.copy(alpha = 0.85f))
            Spacer(Modifier.height(8.dp))
            Text(name, fontSize = 56.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(12.dp))
            Text(subtitle, fontSize = 18.sp, color = Color.White.copy(alpha = 0.7f))
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))))
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (phase is CallPhase.DemoRinging) {
                Text("演示控制", fontSize = 14.sp, color = Color.White.copy(alpha = 0.6f))
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    DemoControlButton("模拟对方接听", onAnswer, Modifier.weight(1f))
                    DemoControlButton("模拟无人接听", onNoAnswer, Modifier.weight(1f))
                }
                Spacer(Modifier.height(12.dp))
                DemoControlButton("模拟断网失败", onNetworkFail, Modifier.fillMaxWidth())
                Spacer(Modifier.height(16.dp))
            }
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().height(72.dp),
            ) {
                Text(primaryLabel, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun DemoControlButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.18f), contentColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.height(52.dp),
    ) {
        Text(label, fontSize = 16.sp, fontWeight = FontWeight.Medium)
    }
}