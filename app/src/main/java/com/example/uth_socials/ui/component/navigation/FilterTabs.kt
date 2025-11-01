package com.example.uth_socials.ui.component.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

import com.example.uth_socials.data.post.Category

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterTabs(
    categories: List<Category>,
    selectedCategory: Category?,
    onCategorySelected: (Category) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categories, key = { it.id }) { category -> // Sử dụng key để tối ưu hiệu năng
            FilterChip(
                selected = category.id == selectedCategory?.id,
                onClick = { onCategorySelected(category) },
                label = { Text(category.name) } // Hiển thị tên
            )
        }
    }
}
