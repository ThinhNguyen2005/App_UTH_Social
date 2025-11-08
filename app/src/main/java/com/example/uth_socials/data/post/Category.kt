package com.example.uth_socials.data.post

import androidx.compose.runtime.Immutable

@Immutable
data class Category(
    val id: String = "",
    val name: String = "",
    val order: Int = 0
) {
    companion object {
        // Default categories for new installations
        val DEFAULT_CATEGORIES = listOf(
            Category(id = "all", name = "Tất cả", order = 0),
            Category(id = "latest", name = "Mới nhất", order = 1),
            Category(id = "study", name = "Học tập", order = 2),
            Category(id = "social", name = "Xã hội", order = 3),
            Category(id = "entertainment", name = "Giải trí", order = 4),
            Category(id = "technology", name = "Công nghệ", order = 5)
        )
    }
}
/**
 * Result của category deletion
 */
data class CategoryDeletionResult(
    val deletedCategory: Category,
    val postsMigrated: Int,
    val migrationTarget: String
)