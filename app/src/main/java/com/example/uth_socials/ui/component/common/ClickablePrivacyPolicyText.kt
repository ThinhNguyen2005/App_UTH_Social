package com.example.uth_socials.ui.component.common

import android.content.Intent
import androidx.compose.foundation.text.ClickableText
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import com.example.uth_socials.ui.screen.UthTeal
import androidx.core.net.toUri

@Composable
fun ClickablePrivacyPolicyText() {
    val context = LocalContext.current
    val privacyPolicyUrl = "https://www.google.com" // THAY URL

    val annotatedString = buildAnnotatedString {
        withStyle(style = SpanStyle(color = Color.Gray, fontSize = 14.sp)) {
            append("Khi nhấp vào Bắt đầu, bạn đồng ý với ")
        }
        pushStringAnnotation(tag = "URL", annotation = privacyPolicyUrl)
        withStyle(style = SpanStyle(color = UthTeal, fontSize = 14.sp)) {
            append("Chính sách quyền riêng tư của UTH.")
        }
        pop()
    }

    ClickableText(
        text = annotatedString,
        onClick = { offset ->
            annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset)
                .firstOrNull()?.let { annotation ->
                    val intent = Intent(Intent.ACTION_VIEW, annotation.item.toUri())
                    context.startActivity(intent)
                }
        },
        style = TextStyle(textAlign = TextAlign.Center)
    )
}