package com.example.uth_socials.data.repository

import android.util.Log
import com.example.uth_socials.data.post.Category
import com.example.uth_socials.data.post.CategoryDeletionResult
import com.example.uth_socials.data.util.SecurityValidator
import com.google.firebase.firestore.FirebaseFirestore
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
                initializeDefaultCategories()
                return Category.DEFAULT_CATEGORIES.sortedBy { it.order }
            }

            categories
        } catch (e: Exception) {
            Log.e("CategoryRepository", "Error fetching categories", e)
            // Fallback to default categories nếu có lỗi
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
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    // Emit raw documents to be processed on background thread
                    trySend(snapshot.documents)
                }
            }

        // Cleanup khi Flow bị hủy
        awaitClose { listener.remove() }
    }.map { documents ->
        // Process documents on background thread
        val categories = documents.mapNotNull { doc ->
            doc.toObject(Category::class.java)?.copy(id = doc.id)
        }

        // Nếu chưa có categories, emit empty list (HomeViewModel sẽ handle việc tạo categories mặc định)
        categories.ifEmpty {
            emptyList()
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Khởi tạo categories mặc định khi lần đầu sử dụng
     */
    suspend fun initializeDefaultCategories() {
        try {
            db.runBatch { batch ->
                Category.DEFAULT_CATEGORIES.forEach { category ->
                    val docRef = categoriesCollection.document(category.id)
                    batch.set(docRef, category)
                }
            }.await()
            Log.d("CategoryRepository", "Initialized default categories")
        } catch (e: Exception) {
            Log.e("CategoryRepository", "Error initializing default categories", e)
        }
    }

    /**
     * Thêm category mới (chỉ admin)
     * Security: Firebase Rules sẽ reject nếu không phải admin
     */
    suspend fun addCategory(category: Category, userId: String? = null): Result<Unit> = runCatching {
        if (!SecurityValidator.canModifyCategories(userId)) {
            throw SecurityException("Only admins can add categories")
        }

        categoriesCollection.document(category.id).set(category).await()
        Log.d("CategoryRepository", "Added category: ${category.name} by user: $userId")
    }

    /**
     * Cập nhật category (chỉ admin)
     * Security: Firebase Rules sẽ reject nếu không phải admin
     */
    suspend fun updateCategory(category: Category, userId: String? = null): Result<Unit> = runCatching {
        // Client-side validation để tối ưu UX
        if (!SecurityValidator.canModifyCategories(userId)) {
            throw SecurityException("Only admins can update categories")
        }

        categoriesCollection.document(category.id).set(category).await()
        Log.d("CategoryRepository", "Updated category: ${category.name} by user: $userId")
    }

    /**
     * Xóa category (chỉ admin, cẩn thận!)
     */
//    suspend fun deleteCategory(categoryId: String, userId: String? = null): Result<Unit> = runCatching {
//        // Validate admin permission
//        val isAdmin = AdminConfig.isSuperAdmin(userId) || adminRepository.isAdmin(userId ?: "")
//        if (!isAdmin) {
//            throw SecurityException("Only admins can delete categories")
//        }
//
//        // Prevent deleting default categories
//        if (Category.DEFAULT_CATEGORIES.any { it.id == categoryId }) {
//            throw IllegalArgumentException("Cannot delete default categories")
//        }
//
//        categoriesCollection.document(categoryId).delete().await()
//        Log.d("CategoryRepository", "Deleted category: $categoryId by user: $userId")
//    }

    /**
     * Lấy category theo ID
     */
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

        // Get all posts using old category
        val postsToMigrate = postsCollection
            .whereEqualTo("category", oldCategoryId)
            .get()
            .await()
            .documents

        var migratedCount = 0

        // Batch update posts
        val batch = FirebaseFirestore.getInstance().batch()
        postsToMigrate.forEach { doc ->
            batch.update(doc.reference, "category", newCategoryId)
            migratedCount++
        }

        batch.commit().await()

        Log.d("CategoryRepository", "Migrated $migratedCount posts from $oldCategoryId to $newCategoryId")
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
        // Client-side validation để tối ưu UX
        if (!SecurityValidator.canModifyCategories(userId)) {
            throw SecurityException("Only admins can delete categories")
        }

        // Prevent deleting default categories
        if (Category.DEFAULT_CATEGORIES.any { it.id == categoryId }) {
            throw IllegalArgumentException("Cannot delete default categories")
        }

        // Check if category exists
        val category = getCategoryById(categoryId)
            ?: throw IllegalArgumentException("Category not found")

        // Count posts using this category
        val postsCount = countPostsUsingCategory(categoryId)

        if (postsCount > 0) {
            // If no migration target specified, migrate to empty category
            val targetCategoryId = migrateToCategoryId ?: ""

            // Validate target category exists (if specified)
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
