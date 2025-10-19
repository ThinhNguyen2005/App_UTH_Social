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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun Article() {
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

    TextButton(onClick = { /* chọn ảnh */ }) {
        Icon(
            imageVector = Icons.Default.AddCircle,
            contentDescription = "Chọn ảnh",
            tint = Color.Black
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text("Chọn ảnh", color = Color.Black)
    }

}