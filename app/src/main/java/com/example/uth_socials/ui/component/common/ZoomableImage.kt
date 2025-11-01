package com.example.uth_socials.ui.component.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.launch

@Composable
fun ZoomableImage(
    imageUrl: String,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()

    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    // Sử dụng animate*AsState để các thay đổi được mượt mà
    val animatedScale by animateFloatAsState(targetValue = scale)
    val animatedOffsetX by animateFloatAsState(targetValue = offsetX)
    val animatedOffsetY by animateFloatAsState(targetValue = offsetY)

    Box(
        modifier = modifier
            .clip(RectangleShape) // Không nên bo góc ở đây, hãy bo góc ở ngoài Pager
            .pointerInput(Unit) {
                // Bắt sự kiện double tap
                detectTapGestures(
                    onDoubleTap = {
                        coroutineScope.launch {
                            if (scale > 1f) {
                                // Nếu đang zoom, double tap sẽ reset về 1f
                                scale = 1f
                                offsetX = 0f
                                offsetY = 0f
                            } else {
                                // Nếu chưa zoom, double tap sẽ zoom to lên 2.5f
                                scale = 2.5f
                                // Nâng cao: bạn có thể thêm logic để zoom vào vị trí tap nếu muốn
                            }
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                // Bắt sự kiện kéo, thả, zoom
                detectTransformGestures { _, pan, zoom, _ ->
                    val newScale = (scale * zoom).coerceIn(1f, 4f)
                    scale = newScale

                    if (scale > 1f) {
                        // Khi đã zoom, cho phép di chuyển ảnh
                        val maxOffsetX = (size.width * (scale - 1)) / 2
                        val maxOffsetY = (size.height * (scale - 1)) / 2

                        // Cập nhật và giới hạn giá trị offset
                        val newOffsetX = (offsetX + pan.x * scale)
                        val newOffsetY = (offsetY + pan.y * scale)

                        offsetX = newOffsetX.coerceIn(-maxOffsetX, maxOffsetX)
                        offsetY = newOffsetY.coerceIn(-maxOffsetY, maxOffsetY)
                    } else {
                        // Khi scale < 1f (đã reset), không cho phép di chuyển
                        // Điều này cho phép Pager nhận cử chỉ vuốt
                        offsetX = 0f
                        offsetY = 0f
                    }
                }
            }
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(imageUrl)
                .crossfade(true)
                .build(),
            contentDescription = "Post image",
            contentScale = ContentScale.Crop, // Dùng Crop để ảnh luôn lấp đầy khung
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    // Áp dụng các giá trị state đã được làm mượt
                    scaleX = animatedScale,
                    scaleY = animatedScale,
                    translationX = animatedOffsetX,
                    translationY = animatedOffsetY
                )
        )
    }
}