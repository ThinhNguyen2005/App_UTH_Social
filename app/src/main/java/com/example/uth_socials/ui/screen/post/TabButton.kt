package com.example.uth_socials.ui.screen.post

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun TabButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.textButtonColors(contentColor = Color.Black)
    ) {
        // 2. Animate một giá trị float từ 0.0f (không vẽ) đến 1.0f (vẽ toàn bộ)
        val fraction by animateFloatAsState(
            targetValue = if (isSelected) 1f else 0f,
            animationSpec = tween(durationMillis = 400), // Điều chỉnh tốc độ animation
            label = "underlineFraction"
        )

        Text(
            text = text,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
            modifier = Modifier
                .padding(bottom = 4.dp) // Tạo không gian cho gạch chân
                .drawBehind { // Vẽ gạch chân
                    // Chỉ vẽ khi fraction > 0 để tối ưu
                    if (fraction > 0f) {
                        val strokeWidth = 2.dp.toPx()
                        val y = size.height - strokeWidth / 2 // Vị trí y của đường kẻ

                        // Điểm giữa của Text
                        val centerX = size.width / 2

                        // 3. Tính toán điểm bắt đầu và kết thúc dựa trên fraction
                        // Điểm bắt đầu sẽ di chuyển từ giữa -> trái
                        val startX = centerX * (1 - fraction)

                        // Điểm kết thúc sẽ di chuyển từ giữa -> phải
                        val endX = centerX + (centerX * fraction)

                        drawLine(
                            color = Color(0xFF007E8F),
                            start = Offset(startX, y),
                            end = Offset(endX, y),
                            strokeWidth = strokeWidth
                        )
                    }
                }
        )
    }
}