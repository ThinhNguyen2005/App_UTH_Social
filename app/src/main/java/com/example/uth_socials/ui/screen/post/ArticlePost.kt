package com.example.uth_socials.ui.screen.post

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.uth_socials.ui.viewmodel.PostViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticlePost(postViewModel: PostViewModel) {
    Spacer(modifier = Modifier.height(8.dp))

    // 🔹 State dropdown mở/đóng
    var expanded by remember { mutableStateOf(false) }

    // 🔹 Danh sách category lấy từ Firestore
    val categories by postViewModel.categories.collectAsState()

    // 🔹 Category được chọn
    val selectedCategory by postViewModel.selectedCategory.collectAsState()

    var categoryName by remember { mutableStateOf("") }

    // --- Ô nhập nội dung bài viết ---
    BasicTextField(
        value = postViewModel.content,
        onValueChange = { postViewModel.content = it },
        textStyle = TextStyle(fontSize = 16.sp),
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .background(Color(0xFFF3F3F3), shape = MaterialTheme.shapes.small)
            .padding(12.dp),
        decorationBox = { innerTextField ->
            if (postViewModel.content.isEmpty()) Text("Chia sẻ suy nghĩ của bạn?", color = Color.Gray)
            innerTextField()
        }
    )

    Spacer(modifier = Modifier.height(16.dp))

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
    ) {
        TextField(
            value = categoryName,
            onValueChange = {},
            readOnly = true,
            label = { Text("Chọn Category") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0xFFF3F3F3),
                unfocusedContainerColor = Color(0xFFF3F3F3),
                focusedIndicatorColor = Color(0xFF565656),
                unfocusedIndicatorColor = Color(0xFF919191),
                focusedLabelColor = Color(0xFF000000),
                cursorColor = Color(0xFFF3F3F3),
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black
            ),
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            categories.forEach { category ->
                if(category.name != "Mới nhất" && category.name != "Tất cả") {
                    DropdownMenuItem(
                        text = { Text(category.name) },
                        onClick = {
                            postViewModel.selectedCategory.value = category.id
                            categoryName = category.name
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}