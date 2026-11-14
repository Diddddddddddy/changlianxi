package org.changlianxi.app.ui.elder

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import kotlinx.coroutines.withTimeoutOrNull
import org.changlianxi.app.model.AppConfig
import org.changlianxi.app.model.DialMode
import org.changlianxi.app.model.FamilyContact
import org.changlianxi.app.ui.FilePhoto

/**
 * 老人首页：一屏一位家人，上下滑动换人。
 * 顶部安全留白长按 4 秒进入家属 PIN 验证；照片区域不承担配置入口。
 */
@Composable
fun ElderHomeScreen(
    config: AppConfig,
    photoFile: (String) -> File?,
    initialContactId: String?,
    onSettled: (String) -> Unit,
    onDial: (String) -> Unit,
    onFamilyEntry: () -> Unit,
) {
    val contacts = config.contacts
    if (contacts.isEmpty()) return

    val initialId = initialContactId?.takeIf { id -> contacts.any { it.id == id } }
        ?: config.defaultContact?.id
    val initialIndex = contacts.indexOfFirst { it.id == initialId }.coerceAtLeast(0)
    val count = contacts.size
    val loop = count > 1
    // 循环衔接：虚拟页取模映射联系人，末位再下滑回首位、首位再上滑回末位；单人保持不循环。
    val initialPage = if (loop) {
        val mid = Int.MAX_VALUE / 2
        mid - (mid % count) + initialIndex
    } else 0
    val pagerState = rememberPagerState(initialPage = initialPage) { if (loop) Int.MAX_VALUE else 1 }
    val settled = !pagerState.isScrollInProgress && pagerState.currentPage == pagerState.settledPage
    val contactAt: (Int) -> FamilyContact = { page -> contacts[if (loop) page % count else 0] }

    LaunchedEffect(pagerState, contacts) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            onSettled(contactAt(page).id)
        }
    }
    // 家属调整顺序后按 contactId 恢复当前目标，不依赖旧 index
    LaunchedEffect(config.contacts) {
        val currentId = contactAt(pagerState.settledPage).id
        val index = contacts.indexOfFirst { it.id == currentId }
        if (index < 0) return@LaunchedEffect
        val cur = pagerState.currentPage
        val target = if (loop) cur - (cur % count) + index else index
        if (target != cur) pagerState.scrollToPage(target)
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            val contact = contactAt(page)
            ContactPage(
                contact = contact,
                photoFile = photoFile(contact.id),
                configVersion = config.configVersion,
                dialMode = config.settings.dialMode,
                dialEnabled = settled,
                onDial = onDial,
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .fillMaxWidth()
                .height(72.dp)
                .pointerInput(Unit) {
                    val slop = viewConfiguration.touchSlop
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val deadline = SystemClock.uptimeMillis() + FAMILY_HOLD_MS
                        var held = true
                        while (held) {
                            val remaining = deadline - SystemClock.uptimeMillis()
                            if (remaining <= 0) break
                            val event = withTimeoutOrNull(remaining) { awaitPointerEvent() } ?: break
                            val change = event.changes.firstOrNull { it.id == down.id }
                            if (change == null || !change.pressed ||
                                (change.position - down.position).getDistance() > slop
                            ) held = false
                        }
                        if (held && SystemClock.uptimeMillis() >= deadline) onFamilyEntry()
                    }
                }
        )
    }
}

@Composable
private fun ContactPage(
    contact: FamilyContact,
    photoFile: File?,
    configVersion: Long,
    dialMode: DialMode,
    dialEnabled: Boolean,
    onDial: (String) -> Unit,
) {
    val tapToDial = dialMode == DialMode.TAP_PHOTO
    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (tapToDial) Modifier.clickable(enabled = dialEnabled) { onDial(contact.id) }
                else Modifier
            )
    ) {
        FilePhoto(photoFile, configVersion, Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))))
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(contact.displayName, fontSize = 52.sp, fontWeight = FontWeight.Bold, color = Color.White)
            if (tapToDial) {
                Spacer(Modifier.height(8.dp))
                Icon(
                    Icons.Default.Call,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(28.dp),
                )
            } else {
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = { onDial(contact.id) },
                    enabled = dialEnabled,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().height(88.dp),
                ) {
                    Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(32.dp))
                    Spacer(Modifier.size(12.dp))
                    Text("打电话", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private const val FAMILY_HOLD_MS = 4000L