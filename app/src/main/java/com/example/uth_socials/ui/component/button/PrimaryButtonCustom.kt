package com.example.uth_socials.ui.component.button

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Màu sắc có thể được định nghĩa ở đây hoặc trong file Theme

/**
 * Nút chính có thể tái sử dụng trong toàn bộ ứng dụng.
 * Nút này tự động co giãn theo chiều rộng.
 *
 * @param text Văn bản hiển thị trên nút.
 * @param onClick Hành động khi nút được nhấn.
 * @param modifier Modifier để tùy chỉnh thêm từ bên ngoài.
 * @param enabled Trạng thái bật/tắt của nút.
 */
@Composable
fun PrimaryButton(
    buttonColor : ButtonColors,
    text: String,
    textColor : Color,
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
        colors = buttonColor
    ) {
        Text(
            text = text,
            fontSize = 18.sp,
            color = textColor
        )
    }
}

//@Preview(showBackground = true, widthDp = 320)
//@Composable
//fun PrimaryButtonPreview() {
//    PrimaryButton(text = "Bắt đầu", onClick = {})
//}
//
//@Preview(showBackground = true, widthDp = 320)
//@Composable
//fun PrimaryButtonDisabledPreview() {
//    PrimaryButton(text = "Đang tải...", onClick = {}, enabled = false)
//}