package com.example.uth_socials.ui.component.button

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.uth_socials.ui.theme.UthTeal

// Màu sắc có thể được định nghĩa ở đây hoặc trong file Theme
val DarkTealButton = Color(0xFF045D5D)
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        // Modifier này làm cho nút trở nên "responsive"
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = UthTeal)
    ) {
        Text(
            text = text,
            fontSize = 18.sp,
            color = Color.White
        )
    }
}

@Preview(showBackground = true, widthDp = 320)
@Composable
fun PrimaryButtonPreview() {
    PrimaryButton(text = "Bắt đầu", onClick = {})
}

@Preview(showBackground = true, widthDp = 320)
@Composable
fun PrimaryButtonDisabledPreview() {
    PrimaryButton(text = "Đang tải...", onClick = {}, enabled = false)
}