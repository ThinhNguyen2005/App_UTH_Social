package com.example.uth_socials.ui.screen.shop

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.uth_socials.data.model.Product
import com.example.uth_socials.R

@Composable
fun ProductItem(
    product: Product,
    onClick: () -> Unit = {} //Callback khi click
){
    var isFav by remember { mutableStateOf(product.isFavorite) }
    Column (modifier = Modifier
        .fillMaxWidth().height(260.dp)
            .clickable{ onClick() }
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            AsyncImage(
                model = product.imageUrl ?: R.drawable.default_img_product,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .clip(RoundedCornerShape(20.dp)),
            )
            IconButton(
                onClick = {isFav = !isFav},
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
            ) {
                Icon(
                    imageVector = if (isFav) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "favorite",
                    tint = if (isFav) Color.Red else Color.DarkGray
                )
            }
        }

        //Price & Name
        Column(modifier = Modifier.padding(vertical = 6.dp)) {
            Text(
//                text = "${product.price.toInt()}.000 ₫",
                text = formatVND(product.price),
                fontSize = 20.sp,
                color = Color(0xFFFF0000),
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = product.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = Color.Black
            )
        }
    }
}

fun formatVND(amount: Double): String {
    val formatter = java.text.NumberFormat.getInstance()
    return formatter.format(amount) + " 000₫"
}
