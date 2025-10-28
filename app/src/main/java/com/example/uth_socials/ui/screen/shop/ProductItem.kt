package com.example.uth_socials.ui.screen.shop

import androidx.compose.foundation.Image
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.uth_socials.data.model.Product
import com.example.uth_socials.R

@Composable
fun ProductItem(
    product: Product,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {} //Callback khi click
){
    var isFav by remember { mutableStateOf(product.isFavorite) }
    Column (
        Modifier.fillMaxWidth().height(260.dp)
            .clickable{ onClick() }
    ) {
        Box(Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(id = product.imageRes ?: R.drawable.default_img_product),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .clip(RoundedCornerShape(20.dp)),
                contentScale = ContentScale.Crop
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
                text = "${product.price.toInt()}.000 ₫",
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
