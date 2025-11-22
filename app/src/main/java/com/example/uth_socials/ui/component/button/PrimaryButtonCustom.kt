package com.example.uth_socials.ui.component.button

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


@Composable
fun PrimaryButton(
    buttonColor: ButtonColors,
    text: String,
    textColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Box(
        modifier = modifier
            .shadow(
                elevation = 10.dp,               // độ nổi
                shape = RoundedCornerShape(12.dp),
                ambientColor = Color(0x55000000), // bóng nhẹ
                spotColor = Color(0x55000000)
            )
    ) {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = buttonColor,
            elevation = ButtonDefaults.buttonElevation(0.dp) // bỏ elevation mặc định
        ) {
            Text(
                text = text,
                fontSize = 18.sp,
                color = textColor
            )
        }
    }
}
