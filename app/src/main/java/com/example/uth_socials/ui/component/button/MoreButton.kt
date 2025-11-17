package com.example.uth_socials.ui.component.button

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun MoreButton(
    onClick: () -> Unit,
    text: String,
    rotateIconDegress: Float
){
    Box(
        modifier = Modifier
            .fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Button(
            onClick = {
                onClick()
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), // nền nhạt
                contentColor = MaterialTheme.colorScheme.primary // màu text
            ),
            shape = RoundedCornerShape(16.dp), // bo góc
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 4.dp,
                pressedElevation = 2.dp
            ),
            modifier = Modifier
                .padding(vertical = 8.dp)
        ) {
            Text(
                text = text,
                fontWeight = FontWeight.Medium
            )
            Icon(
                imageVector = Icons.Default.ArrowDownward,
                contentDescription = "Thêm",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .rotate(rotateIconDegress)
            )
        }
    }
}