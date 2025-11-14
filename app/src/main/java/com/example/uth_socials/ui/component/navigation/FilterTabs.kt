package com.example.uth_socials.ui.component.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

import com.example.uth_socials.data.post.Category

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterTabs(
    categories: List<Category>,
    selectedCategory: Category?,
    onCategorySelected: (Category) -> Unit,
    isLoading: Boolean = false,
    error: String? = null
) {
    when {
        isLoading -> {
            // Loading state
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(3) { // Show 3 skeleton chips
                    FilterChip(
                        selected = false,
                        onClick = {},
                        enabled = false,
                        label = {
                            Text("")
                        }
                    )
                }
            }
        }
        error != null -> {
            // Error state - show a single disabled chip
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = false,
                        onClick = {},
                        enabled = false,
                        label = {
                            Text("Lỗi tải danh mục")
                        }
                    )
                }
            }
        }
        categories.isEmpty() -> {
            // Empty state
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = false,
                        onClick = {},
                        enabled = false,
                        label = {
                            Text("Không có danh mục")
                        }
                    )
                }
            }
        }
        else -> {
            // Normal state
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories, key = { it.id }) { category ->
                    FilterChip(
                        selected = category.id == selectedCategory?.id,
                        onClick = { onCategorySelected(category) },
                        label = { Text(category.name) }
                    )
                }
            }
        }
    }
}
