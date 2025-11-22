package com.example.uth_socials.ui.component.button



import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.*


@Composable
fun ComfirmAuthButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF06635A), // màu xanh đậm bạn dùng
            disabledContainerColor = Color(0xFFB0BEC5)
        ),
        elevation = ButtonDefaults.elevatedButtonElevation(4.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp,
                color = Color.White
            )
        } else {
            Text(
                text = text,
                fontSize = 18.sp,
                color = Color.White
            )
        }
    }
}
