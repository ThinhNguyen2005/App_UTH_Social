package com.example.uth_socials.ui.screen.market

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.uth_socials.ui.theme.SearchBg

@Composable
fun SearchBar(
    modifier: Modifier = Modifier,
    query: String = "", // Nhận query từ ViewModel
    hint: String = "Tìm sản phẩm...",
    onQueryChange: (String) -> Unit = {}, // Gọi khi text thay đổi
    onSearch: (String) -> Unit = {}, // Gọi khi nhấn search
    onClear: () -> Unit = {} // Gọi khi nhấn clear
) {
    var isFocused by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current

    val borderColor by animateColorAsState(
        targetValue = if (isFocused) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        else Color.Transparent,
        label = "borderColorAnim"
    )

    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange, // Cập nhật ngay khi text thay đổi
        placeholder = {
            Text(
                text = hint,
                color = Color.DarkGray.copy(alpha = 0.6f),
                fontSize = 16.sp
            )
        },
        singleLine = true,
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = Color.DarkGray
            )
        },
        trailingIcon = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (query.isNotEmpty()) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear",
                        tint = Color.DarkGray,
                        modifier = Modifier
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) {
                                onClear() // Gọi callback clear
                                keyboardController?.hide()
                            }
                            .padding(4.dp)
                    )
                }
            }
        },
        textStyle = TextStyle(
            color = Color.Black,
            fontSize = 18.sp,
            fontWeight = FontWeight.Normal
        ),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(
            onSearch = {
                onSearch(query)
                keyboardController?.hide()
            }
        ),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .shadow(4.dp, RoundedCornerShape(28.dp))
            .onFocusChanged { isFocused = it.isFocused },
        shape = RoundedCornerShape(28.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = SearchBg,
            unfocusedContainerColor = SearchBg,
            disabledContainerColor = SearchBg,
            focusedBorderColor = borderColor,
            unfocusedBorderColor = borderColor,
            cursorColor = MaterialTheme.colorScheme.primary
        )
    )
}

@Preview(showBackground = true)
@Composable
fun SearchBarPreview() {
    var query by remember { mutableStateOf("") }
    SearchBar(
        query = query,
        onQueryChange = { query = it },
        onSearch = { /* Handle search */ },
        onClear = { query = "" }
    )
}