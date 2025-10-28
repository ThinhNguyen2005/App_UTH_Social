package com.example.uth_socials.ui.screen.shop

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.uth_socials.R
import com.example.uth_socials.ui.viewmodel.ShopViewModel

@Composable
fun ShopScreen(
    viewModel : ShopViewModel = ShopViewModel(),
) {
    //lay data tu ViewModel
    val products by viewModel.products.collectAsState()

    Column(Modifier.fillMaxSize()) {
        Box(
            Modifier.height(239.dp).fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp))
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFB6FDFF),
                            Color(0xFF00F8FF)
                        )
                    )
                )
        ){
            //Logo
            Image(
                painter = painterResource(R.drawable.logo_uth),
                contentDescription = null,
                modifier = Modifier
                    .size(width = 160.dp, height = 38.dp)
                    .offset(x = 17.dp, y = 40.dp)
            )

            // Title + Subtitle
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 106.dp)
            ) {
                Text(
                    text = "Trang bán hàng",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Text(
                    text = "Mua thì hời, bán thì lời",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.DarkGray
                )
            }
        }

        //Search
        Box(
            modifier = Modifier.fillMaxWidth()
                .offset(y = -30.dp)
        ) {
            SearchBar(
                modifier = Modifier
                    .align(Alignment.TopCenter), // Horizontally center
                onSearch = { query ->
                    viewModel.search(query) // Trigger the search in the ViewModel
                }
            )
        }

        //Products
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            Modifier.fillMaxSize().offset(y = -30.dp)
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFE5FEFF),
                            Color(0xFF2CC3C9)
                        )
                    )
                )
            .padding(horizontal = 16.dp).padding(top = 18.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            items(products.size){ product ->
                ProductItem(
                    product = products[product],
                    onClick = { /* Handle item click */ }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ShopScreenPreview() {
    ShopScreen()
}
