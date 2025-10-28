package com.example.uth_socials.ui.screen.shop

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.uth_socials.R
import kotlinx.coroutines.sync.Mutex

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PostProductScreen(
    username: String = "Thien Huynh",
    onPostClick: () -> Unit = {},
) {
    val productName = remember { mutableStateOf("") }
    val description = remember { mutableStateOf("") }
    val price = remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            Modifier
                .height(100.dp)
                .fillMaxWidth()
                .drawBehind {
                    val strokeWidth = 2.dp.toPx()  // độ dày viền
                    val y = size.height - strokeWidth / 2  // nằm sát đáy box

                    drawLine(
                        color = Color.LightGray,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = strokeWidth
                    )
                }
        ) {
            //Logo
            Image(
                painter = painterResource(R.drawable.logo_uth),
                contentDescription = null,
                modifier = Modifier
                    .size(width = 160.dp, height = 38.dp)
                    .offset(x = 17.dp, y = 40.dp)
            )
        }

        // Header
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = R.drawable.avatar_thien),
                    contentDescription = "Avatar",
                    modifier = Modifier
                        .size(43.dp)
                        .shadow(6.dp, CircleShape)
                        .clip(CircleShape)
                        .background(Color.White)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = username,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black
                )
            }

            Button(
                onClick = { onPostClick() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF009688)),
                modifier = Modifier
                    .size(100.dp, 40.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Đăng", color = Color.White)
            }
        }

        LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            item {
                Text(
                    text = "Tên sản phẩm",
                    fontWeight = FontWeight.Normal,
                    fontSize = 18.sp,
                    color = Color.Black
                )
                OutlinedTextField(
                    value = productName.value,
                    onValueChange = { productName.value = it },
                    placeholder = { Text("Nhập tên sản phẩm") },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)
                        .shadow(4.dp, RoundedCornerShape(10.dp))
                        .background(Color.White, RoundedCornerShape(10.dp)),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFD0EBEB),
                        unfocusedContainerColor = Color(0xFFD0EBEB),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )
            }

            item {
                Text(
                    text = "Mô tả sản phẩm",
                    fontWeight = FontWeight.Normal,
                    fontSize = 18.sp,
                    color = Color.Black
                )
                OutlinedTextField(
                    value = description.value,
                    onValueChange = { description.value = it },
                    placeholder = { Text("Nhập mô tả sản phẩm") },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)
                        .shadow(4.dp, RoundedCornerShape(10.dp))
                        .background(Color.White, RoundedCornerShape(10.dp)),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFD0EBEB),
                        unfocusedContainerColor = Color(0xFFD0EBEB),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )
            }
            //Price product
            item {
                Text(
                    text = "Giá sản phẩm",
                    fontWeight = FontWeight.Normal,
                    fontSize = 18.sp,
                    color = Color.Black
                )
                OutlinedTextField(
                    value = price.value,
                    onValueChange = { price.value = it },
                    placeholder = { Text("Nhập giá sản phẩm") },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)
                        .shadow(4.dp, RoundedCornerShape(10.dp))
                        .background(Color.White, RoundedCornerShape(10.dp)),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFD0EBEB),
                        unfocusedContainerColor = Color(0xFFD0EBEB),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            // Image picker
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AddCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Chọn ảnh")
                }
            }

            item {
                // Bottom Tabs
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Bài viết",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black
                    )
                    Text(
                        "Sản phẩm",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black,
                        modifier = Modifier
                            .drawBehind {
                                drawLine(
                                    color = Color(0xFF009688),
                                    start = Offset(size.width*0.1f, size.height + 5f),
                                    end = Offset(size.width*0.9f, size.height + 5f),
                                    strokeWidth = 8f,
                                    cap = StrokeCap.Round
                                )
                            }
                    )
                }
            }
        }
    }
}
