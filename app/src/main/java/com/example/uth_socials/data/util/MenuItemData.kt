package com.example.uth_socials.data.util

import androidx.compose.ui.graphics.vector.ImageVector

data class MenuItemData(
    val text: String,
    val icon: ImageVector? = null,
    val onClick: () -> Unit
)