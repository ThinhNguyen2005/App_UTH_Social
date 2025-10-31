package com.example.uth_socials.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// 🌙 DARK MODE
private val DarkColorScheme = darkColorScheme(
    primary = UthTeal,              // Màu nhấn chính
    secondary = TealSecondary,      // Màu phụ
    background = Color(0xFF121212), // Nền tổng thể
    surface = Color(0xFF1E1E1E),    // Màu nền cho card, topbar, nav
    onPrimary = Color.White,        // Màu chữ/icon trên nền primary
    onSecondary = Color.White,
    onBackground = Color.White,     // Màu chữ trên nền background
    onSurface = Color.White         // Màu chữ/icon trên bề mặt
)

// ☀️ LIGHT MODE
private val LightColorScheme = lightColorScheme(
    primary = UthTeal,
    secondary = TealSecondary,
    background = Color(0xFFFFFFFF),
    surface = Color(0xFFFFFFFF),
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color.Black,
    onSurface = Color.Black
)

@Composable
fun UTH_SocialsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
