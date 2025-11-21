package com.example.uth_socials.data.repository

import android.util.Log
import com.example.uth_socials.data.post.Category
import com.example.uth_socials.data.post.CategoryDeletionResult
import com.example.uth_socials.data.util.SecurityValidator
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers

class CategoryRepository {
    private val db = FirebaseFirestore.getInstance()
    private val categoriesCollection = db.collection("categories")

    /**
     * Lấy danh sách categories đã sắp xếp theo order (một lần)
     */
    suspend fun getCategories(): List<Category> {
        return try {
            val snapshot = categoriesCollection
                .orderBy("order", Query.Direction.ASCENDING)
                .get()
                .await()

            val categories = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Category::class.java)?.copy(id = doc.id)
            }

            // Nếu chưa có categories, tạo categories mặc định
            if (categories.isEmpty()) {
                defaultCategories()
                return Category.DEFAULT_CATEGORIES.sortedBy { it.order }
            }

            categories
        } catch (e: Exception) {
            Log.e("CategoryRepository", "Error fetching categories", e)
            Category.DEFAULT_CATEGORIES.sortedBy { it.order }
        }
    }

    /**
     * Lắng nghe thay đổi categories theo thời gian thực
     */
    fun getCategoriesFlow(): Flow<List<Category>> = callbackFlow {
        val listener = categoriesCollection
            .orderBy("order", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("CategoryRepository", "Error listening to categories", error)
                    if (error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        Log.w("CategoryRepository", "Permission denied listening to categories. Emitting empty list.")
                        trySend(emptyList()) // Gửi list rỗng thay vì crash
                    } else {
                        close(error) // Chỉ đóng với các lỗi nghiêm trọng khác
                    }
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    trySend(snapshot.documents)
                }
            }

        // Cleanup khi Flow bị hủy
        awaitClose { listener.remove() }
    }.map { documents ->
        val categories = documents.mapNotNull { doc ->
            doc.toObject(Category::class.java)?.copy(id = doc.id)
        }
        categories.ifEmpty {
            emptyList()
        }
    }.flowOn(Dispatchers.IO)

    suspend fun defaultCategories() {
        try {
            db.runBatch { batch ->
                Category.DEFAULT_CATEGORIES.forEach { category ->
                    val docRef = categoriesCollection.document(category.id)
                    batch.set(docRef, category)
                }
            }.await()
        } catch (e: Exception) {
            Log.e("CategoryRepository", "Error initializing default categories", e)
        }
    }

    suspend fun addCategory(category: Category, userId: String? = null): Result<Unit> = runCatching {
        if (!SecurityValidator.canModifyCategories(userId)) {
            throw SecurityException("Only admins can add categories")
        }

        categoriesCollection.document(category.id).set(category).await()
    }

    suspend fun updateCategory(category: Category, userId: String? = null): Result<Unit> = runCatching {
        if (!SecurityValidator.canModifyCategories(userId)) {
            throw SecurityException("Only admins can update categories")
        }

        categoriesCollection.document(category.id).set(category).await()
    }

    suspend fun getCategoryById(categoryId: String): Category? {
        return try {
            val snapshot = categoriesCollection.document(categoryId).get().await()
            snapshot.toObject(Category::class.java)?.copy(id = snapshot.id)
        } catch (e: Exception) {
            Log.e("CategoryRepository", "Error fetching category $categoryId", e)
            null
        }
    }

    /**
     * Kiểm tra category có tồn tại không
     */
    suspend fun categoryExists(categoryId: String): Boolean {
        return try {
            categoriesCollection.document(categoryId).get().await().exists()
        } catch (e: Exception) {
            Log.e("CategoryRepository", "Error checking category $categoryId", e)
            false
        }
    }

    /**
     * Đếm số posts đang sử dụng category này
     */
    suspend fun countPostsUsingCategory(categoryId: String): Int {
        return try {
            val postsCollection = FirebaseFirestore.getInstance().collection("posts")
            postsCollection.whereEqualTo("category", categoryId).get().await().size()
        } catch (e: Exception) {
            Log.e("CategoryRepository", "Error counting posts for category $categoryId", e)
            0
        }
    }

    /**
     * Migrate posts từ category cũ sang category mới
     */
    suspend fun migratePostsToCategory(oldCategoryId: String, newCategoryId: String): Result<Int> = runCatching {
        val postsCollection = FirebaseFirestore.getInstance().collection("posts")

        val postsToMigrate = postsCollection
            .whereEqualTo("category", oldCategoryId)
            .get()
            .await()
            .documents

        var migratedCount = 0

        val batch = FirebaseFirestore.getInstance().batch()
        postsToMigrate.forEach { doc ->
            batch.update(doc.reference, "category", newCategoryId)
            migratedCount++
        }

        batch.commit().await()
        migratedCount
    }

    /**
     * Xóa category với confirmation và post migration
     */
    suspend fun deleteCategoryWithConfirmation(
        categoryId: String,
        userId: String? = null,
        migrateToCategoryId: String? = null
    ): Result<CategoryDeletionResult> = runCatching {
        if (!SecurityValidator.canModifyCategories(userId)) {
            throw SecurityException("Only admins can delete categories")
        }
        if (Category.DEFAULT_CATEGORIES.any { it.id == categoryId }) {
            throw IllegalArgumentException("Cannot delete default categories")
        }

        val category = getCategoryById(categoryId)
            ?: throw IllegalArgumentException("Category not found")

        val postsCount = countPostsUsingCategory(categoryId)

        if (postsCount > 0) {
            val targetCategoryId = migrateToCategoryId ?: ""

            if (targetCategoryId.isNotEmpty() && !categoryExists(targetCategoryId)) {
                throw IllegalArgumentException("Target category does not exist")
            }

            // Migrate posts
            val migrationResult = migratePostsToCategory(categoryId, targetCategoryId)
            if (migrationResult.isFailure) {
                throw migrationResult.exceptionOrNull()
                    ?: RuntimeException("Failed to migrate posts")
            }
        }

        // Delete the category
        categoriesCollection.document(categoryId).delete().await()

        Log.d("CategoryRepository", "Deleted category: $categoryId by user: $userId, migrated $postsCount posts")

        CategoryDeletionResult(
            deletedCategory = category,
            postsMigrated = postsCount,
            migrationTarget = migrateToCategoryId ?: ""
        )
    }


}
