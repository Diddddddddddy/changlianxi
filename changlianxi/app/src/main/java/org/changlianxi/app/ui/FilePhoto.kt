package org.changlianxi.app.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 私有目录照片加载（导入时已压缩到 ≤1600px，可直接 decode）。 */
@Composable
fun FilePhoto(file: File?, version: Long, modifier: Modifier, contentScale: ContentScale = ContentScale.Crop) {
    var bitmap by remember(file?.absolutePath, version) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(file?.absolutePath, version) {
        bitmap = withContext(Dispatchers.IO) {
            file?.let { runCatching { BitmapFactory.decodeFile(it.absolutePath)?.asImageBitmap() }.getOrNull() }
        }
    }
    val b = bitmap
    if (b != null) {
        Image(bitmap = b, contentDescription = null, modifier = modifier, contentScale = contentScale)
    } else {
        Box(modifier.background(Color(0xFFE3E3DC)))
    }
}