package com.example.uth_socials.ui.component.common

import java.text.NumberFormat
import java.util.Locale

fun formatVND(price: Double): String {
    val locale = Locale("vi", "VN")
    val formatter = NumberFormat.getCurrencyInstance(locale)
    return formatter.format(price)
}