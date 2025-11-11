package com.example.uth_socials.ui.component.logo

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatBottomBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit
) {
    val surfaceColor = MaterialTheme.colorScheme.surface

    Surface(
        color = surfaceColor,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        // 👇 Tắt mọi insets tự động như TopAppBar
        modifier = Modifier
            .fillMaxWidth()

            .consumeWindowInsets(WindowInsets.ime) // "ăn" inset của bàn phím để Scaffold không xử lý trùng
            .imePadding()
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = text,
                onValueChange = onTextChange,
                placeholder = { Text("Nhập tin nhắn...") },
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 48.dp), //height(48.dp) = ép cứng chiều cao → nếu font + padding bên trong vượt giới hạn → chữ bị cắt.
                shape = RoundedCornerShape(24.dp),
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = MaterialTheme.colorScheme.primary
                ),
                textStyle = LocalTextStyle.current.copy(
                    lineHeight = 20.sp // thêm dòng này giúp text canh giữa vertical
                )
            )

            Spacer(Modifier.width(8.dp))

            IconButton(
                onClick = onSend,
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Gửi",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

