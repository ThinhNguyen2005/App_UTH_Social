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
import com.example.uth_socials.ui.component.common.formatVND

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
