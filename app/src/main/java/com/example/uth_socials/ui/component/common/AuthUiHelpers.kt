package com.example.uth_socials.ui.component.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class BannerTone { Error, Warning, Success, Info }

@Composable
fun StatusBanner(
    icon: ImageVector,
    title: String,
    message: String,
    tone: BannerTone,
    modifier: Modifier = Modifier
) {
    val (bg, fg) = when (tone) {
        BannerTone.Error -> Color(0xFFFDECEA) to Color(0xFFB3261E)
        BannerTone.Warning -> Color(0xFFFFF4E5) to Color(0xFFB76E00)
        BannerTone.Success -> Color(0xFFE7F4EE) to Color(0xFF1E6B47)
        BannerTone.Info -> Color(0xFFE6F4F2) to Color(0xFF06635A)
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(bg, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = fg,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.size(8.dp))
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = title,
                color = fg,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (message.isNotBlank()) {
                Text(
                    text = message,
                    color = fg.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
fun FieldHint(text: String, isError: Boolean = false, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp, start = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            color = if (isError) Color(0xFFB3261E) else Color(0xFF5C6B6A),
            fontSize = 12.sp
        )
    }
}

@Composable
fun DividerWithText(label: String, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(Color(0xFFE0E5E4))
        )
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp),
            color = Color(0xFF8A9695),
            fontSize = 12.sp
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(Color(0xFFE0E5E4))
        )
    }
}
