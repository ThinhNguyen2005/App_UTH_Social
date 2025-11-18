package com.example.uth_socials.ui.screen.post

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.uth_socials.ui.viewmodel.ProductViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductPost(productViewModel: ProductViewModel){

    // Dropdown menu state
    var expanded by remember { mutableStateOf(false) }
    val productTypes = listOf("Sách", "Quần áo", "Đồ điện tử", "Khác")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        // --- Tên sản phẩm ---
        Text(text = "Tên sản phẩm", fontSize = 14.sp)
        TextField(
            value = productViewModel.name,
            textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurfaceVariant),
            onValueChange = { productViewModel.name = it },
            placeholder = { Text("Nhập tên sản phẩm") },
            modifier = Modifier
                .fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = TextFieldDefaults.colors(
                //focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                //focusedIndicatorColor = MaterialTheme.colorScheme.surfaceVariant,
               // unfocusedIndicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                //cursorColor = MaterialTheme.colorScheme.surfaceVariant,
                //focusedTextColor = MaterialTheme.colorScheme.surfaceVariant,
                //unfocusedTextColor = MaterialTheme.colorScheme.surface,
                focusedPlaceholderColor = Color.Transparent,
                unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )

        // --- Mô tả sản phẩm ---
        Text(text = "Mô tả sản phẩm", fontSize = 14.sp)
        TextField(
            value = productViewModel.description,
            textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurfaceVariant),
            onValueChange = { productViewModel.description = it },
            placeholder = { Text("Nhập mô tả sản phẩm") },
            modifier = Modifier
                .fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = TextFieldDefaults.colors(
                //focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                //focusedIndicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                // unfocusedIndicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                //cursorColor = MaterialTheme.colorScheme.surfaceVariant,
                //focusedTextColor = MaterialTheme.colorScheme.surfaceVariant,
                //unfocusedTextColor = MaterialTheme.colorScheme.surface,
                focusedPlaceholderColor = Color.Transparent,
                unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant
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
                    value = productViewModel.type,
                    textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurfaceVariant),
                    label = { Text("Chọn loại sản phẩm", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    onValueChange = {productViewModel.type = it},
                    readOnly = true,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ExposedDropdownMenuDefaults.textFieldColors(
                        //focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        //focusedIndicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                        // unfocusedIndicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                        //cursorColor = MaterialTheme.colorScheme.surfaceVariant,
                        //focusedTextColor = MaterialTheme.colorScheme.surfaceVariant,
                        //unfocusedTextColor = MaterialTheme.colorScheme.surface,
                    )
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    productTypes.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            onClick = {
                                productViewModel.type = type
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
            value = productViewModel.price.toString(),
            textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurfaceVariant),
            onValueChange = { productViewModel.price = it.toDoubleOrNull() ?: 0.0 },
            placeholder = { Text("Nhập giá") },
            modifier = Modifier
                .fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = TextFieldDefaults.colors(
                //focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                //focusedIndicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                // unfocusedIndicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                //cursorColor = MaterialTheme.colorScheme.surfaceVariant,
                //focusedTextColor = MaterialTheme.colorScheme.surfaceVariant,
                //unfocusedTextColor = MaterialTheme.colorScheme.surface,
                focusedPlaceholderColor = Color.Transparent,
                unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )

    }
}

@Preview(showBackground = true)
@Composable
fun ProductPostPreview() {
  //  ProductPost(productViewModel = viewModel())
}