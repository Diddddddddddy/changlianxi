package org.changlianxi.app.ui.elder

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import org.changlianxi.app.ui.FilePhoto

/**
 * 真实蜂窝呼叫交棒页：诚实显示「正在拨打」，不声称已接通/已挂断。
 * 系统电话界面随即接管；退后台即回到老人首页。无演示徽标、无假接听/挂断控件。
 */
@Composable
fun RealCallScreen(
    name: String,
    photoFile: File?,
    configVersion: Long,
    onBack: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF1B1B1B))) {
        FilePhoto(photoFile, configVersion, Modifier.fillMaxSize())
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)))

        Column(
            modifier = Modifier.align(Alignment.Center).fillMaxWidth().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("正在拨打", fontSize = 26.sp, color = Color.White.copy(alpha = 0.85f))
            Spacer(Modifier.height(8.dp))
            Text(name, fontSize = 56.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(12.dp))
            Text("已交给系统电话，接通后由系统显示", fontSize = 18.sp, color = Color.White.copy(alpha = 0.7f))
        }

        Button(
            onClick = onBack,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 28.dp)
                .height(72.dp),
        ) {
            Text("返回", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
    }
}