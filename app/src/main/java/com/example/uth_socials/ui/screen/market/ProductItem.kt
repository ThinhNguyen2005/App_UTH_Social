package com.example.uth_socials.ui.screen.market

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.uth_socials.R
import com.example.uth_socials.data.market.Product
import com.google.firebase.Timestamp
import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

@Composable
fun ProductItem(
    product: Product,
    onClick: () -> Unit = {} //Callback khi click
){
    Column (modifier = Modifier
        .fillMaxWidth().height(260.dp)
            .clickable{ onClick() }
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            AsyncImage(
                model = product.imageUrl ?: R.drawable.default_image,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .clip(RoundedCornerShape(12.dp)),
            )
        }

        //Price & Name
        Column(modifier = Modifier.padding(vertical = 6.dp)) {
            Text(
                text = product.name,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = formatVND(product.price),
                fontSize = 16.sp,
                maxLines = 1,
                color = Color(0xFFFF0000),
                fontWeight = FontWeight.Medium
            )
        }
    }
}
fun formatVND(price: Double): String {
    val locale = Locale("vi", "VN")
    val formatter = NumberFormat.getCurrencyInstance(locale)
    return formatter.format(price)
}
fun getRelativeTimeString(timestamp: Timestamp?): String {
    // Nếu date null, trả về mặc định
    if (timestamp == null) return "Thời gian không xác định"

    // Lấy thời gian hiện tại (milliseconds)
    val now = System.currentTimeMillis()

    // Lấy thời gian của sản phẩm (milliseconds)
    val productTime = timestamp.toDate().time

    // Tính khoảng cách thời gian (milliseconds)
    val diffInMillis = now - productTime

    // Nếu thời gian âm (lỗi dữ liệu), trả về mặc định
    if (diffInMillis < 0) return "Vừa xong"

    // Chuyển đổi sang các đơn vị thời gian
    val seconds = TimeUnit.MILLISECONDS.toSeconds(diffInMillis)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(diffInMillis)
    val hours = TimeUnit.MILLISECONDS.toHours(diffInMillis)
    val days = TimeUnit.MILLISECONDS.toDays(diffInMillis)

    // Logic hiển thị theo mức độ ưu tiên
    return when {
        // Dưới 1 phút: "Vừa xong"
        seconds < 60 -> "Vừa xong"

        // Dưới 1 giờ: hiển thị phút
        minutes < 60 -> "$minutes phút trước"

        // Dưới 1 ngày: hiển thị giờ
        hours < 24 -> "$hours giờ trước"

        // Dưới 7 ngày: hiển thị ngày
        days < 7 -> "$days ngày trước"

        // Dưới 30 ngày: hiển thị tuần
        days < 30 -> {
            val weeks = days / 7
            "$weeks tuần trước"
        }

        // Dưới 365 ngày: hiển thị tháng
        days < 365 -> {
            val months = days / 30
            "$months tháng trước"
        }

        // Trên 1 năm: hiển thị năm
        else -> {
            val years = days / 365
            "$years năm trước"
        }
    }
}
