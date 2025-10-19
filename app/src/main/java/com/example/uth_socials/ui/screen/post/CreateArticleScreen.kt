package com.example.uth_socials.ui.screen.post

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.uth_socials.R
import com.example.uth_socials.ui.component.button.PrimaryButton
import com.example.uth_socials.ui.component.navigation.HomeBottomNavigation
import com.example.uth_socials.ui.logo.HomeTopAppBarPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateArticleScreen() {
    var selectedTabIndex by remember { mutableStateOf(0) }

    Scaffold(
        topBar = { HomeTopAppBarPrimary() },
        bottomBar = { HomeBottomNavigation() }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(Color.White)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher_foreground),
                        contentDescription = "Avatar",
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Text("SKT_DucT1", fontWeight = FontWeight.Medium)
                }

                PrimaryButton(
                    buttonColor = ButtonDefaults.buttonColors(containerColor = Color(0xFF007E8F)),
                    text = "Đăng",
                    textColor = Color.White,
                    modifier = Modifier
                        .width(100.dp)
                        .height(40.dp),
                    onClick = {})
            }


            when (selectedTabIndex) {
                0 -> ArticleCompose()
                1 -> ProductCompose()
            }

            TextButton(onClick = { /* chọn ảnh */ }) {
                Icon(
                    imageVector = Icons.Default.AddCircle,
                    contentDescription = "Chọn ảnh",
                    tint = Color.Black
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Chọn ảnh", color = Color.Black)
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
            ) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(
                        120.dp,
                        alignment = Alignment.CenterHorizontally
                    )

                ) {
                    TabButton(
                        text = "Bài viết",
                        isSelected = selectedTabIndex == 0,
                        onClick = { selectedTabIndex = 0 }
                    )

                    // Tab "Sản phẩm"
                    TabButton(
                        text = "Sản phẩm",
                        isSelected = selectedTabIndex == 1,
                        onClick = { selectedTabIndex = 1 }
                    )
                }
            }

        }
    }
}

@Composable
fun ArticleCompose() {
    var text by remember { mutableStateOf("") }

    Spacer(modifier = Modifier.height(8.dp))

    // --- Text input ---
    BasicTextField(
        value = text,
        onValueChange = { text = it },
        textStyle = TextStyle(fontSize = 16.sp),
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .background(Color(0xFFF2F2F2), shape = MaterialTheme.shapes.small)
            .padding(12.dp),
        decorationBox = { innerTextField ->
            if (text.isEmpty()) Text("Chia sẻ suy nghĩ của bạn?", color = Color.Gray)
            innerTextField()
        }
    )

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductCompose(){
    var name by remember { mutableStateOf(TextFieldValue("")) }
    var description by remember { mutableStateOf(TextFieldValue("")) }
    var price by remember { mutableStateOf(TextFieldValue("")) }

    // Dropdown menu state
    var expanded by remember { mutableStateOf(false) }
    val productTypes = listOf("Sách", "Quần áo", "Đồ điện tử", "Khác")
    var selectedType by remember { mutableStateOf(productTypes[0]) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        // --- Tên sản phẩm ---
        Text(text = "Tên sản phẩm", fontSize = 14.sp)
        TextField(
            value = name,
            onValueChange = { name = it },
            placeholder = { Text("Nhập tên sản phẩm") },
            modifier = Modifier
                .fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = TextFieldDefaults.colors(
                unfocusedContainerColor = Color(0xFFCFE9E9),
                focusedContainerColor = Color(0xFFCFE9E9),
                unfocusedIndicatorColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent
            )
        )

        // --- Mô tả sản phẩm ---
        Text(text = "Mô tả sản phẩm", fontSize = 14.sp)
        TextField(
            value = description,
            onValueChange = { description = it },
            placeholder = { Text("Nhập mô tả sản phẩm") },
            modifier = Modifier
                .fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = TextFieldDefaults.colors(
                unfocusedContainerColor = Color(0xFFCFE9E9),
                focusedContainerColor = Color(0xFFCFE9E9),
                unfocusedIndicatorColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent
            )
        )

        // --- Loại sản phẩm ---
        Text(text = "Loại sản phẩm", fontSize = 14.sp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                TextField(
                    value = selectedType,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ExposedDropdownMenuDefaults.textFieldColors(
                        unfocusedContainerColor = Color(0xFFCFE9E9),
                        focusedContainerColor = Color(0xFFCFE9E9),
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent
                    )
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    productTypes.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type) },
                            onClick = {
                                selectedType = type
                                expanded = false
                            }
                        )
                    }
                }
            }
        }

        // --- Giá ---
        Text(text = "Giá", fontSize = 14.sp)
        TextField(
            value = price,
            onValueChange = { price = it },
            placeholder = { Text("Nhập giá") },
            modifier = Modifier
                .fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = TextFieldDefaults.colors(
                unfocusedContainerColor = Color(0xFFCFE9E9),
                focusedContainerColor = Color(0xFFCFE9E9),
                unfocusedIndicatorColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent
            )
        )

    }
}

@Composable
fun TabButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.textButtonColors(contentColor = Color.Black)
    ) {
        // 2. Animate một giá trị float từ 0.0f (không vẽ) đến 1.0f (vẽ toàn bộ)
        val fraction by animateFloatAsState(
            targetValue = if (isSelected) 1f else 0f,
            animationSpec = tween(durationMillis = 400), // Điều chỉnh tốc độ animation
            label = "underlineFraction"
        )

        Text(
            text = text,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
            modifier = Modifier
                .padding(bottom = 4.dp) // Tạo không gian cho gạch chân
                .drawBehind { // Vẽ gạch chân
                    // Chỉ vẽ khi fraction > 0 để tối ưu
                    if (fraction > 0f) {
                        val strokeWidth = 2.dp.toPx()
                        val y = size.height - strokeWidth / 2 // Vị trí y của đường kẻ

                        // Điểm giữa của Text
                        val centerX = size.width / 2

                        // 3. Tính toán điểm bắt đầu và kết thúc dựa trên fraction
                        // Điểm bắt đầu sẽ di chuyển từ giữa -> trái
                        val startX = centerX * (1 - fraction)

                        // Điểm kết thúc sẽ di chuyển từ giữa -> phải
                        val endX = centerX + (centerX * fraction)

                        drawLine(
                            color = Color(0xFF007E8F),
                            start = Offset(startX, y),
                            end = Offset(endX, y),
                            strokeWidth = strokeWidth
                        )
                    }
                }
        )
    }
}

@Composable
fun HeaderCompose() {

}


@Preview(name = "Portrait Mode", showBackground = true, widthDp = 360, heightDp = 780)
@Composable
fun CreateArticleScreenPreview() {
    CreateArticleScreen()
}
