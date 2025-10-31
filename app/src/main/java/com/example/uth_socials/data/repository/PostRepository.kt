package com.example.uth_socials.data.repository

import com.example.uth_socials.data.post.Category
import com.example.uth_socials.data.post.Comment
import com.example.uth_socials.data.post.Post
import com.example.uth_socials.data.post.Report
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import android.util.Log

class PostRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val postsCollection = db.collection("posts")
    private val categoriesCollection = db.collection("categories")
    private val reportsCollection = db.collection("reports")
    private val usersCollection = db.collection("users")

    suspend fun getCategories(): List<Category> {
        return try {
            val snapshot = categoriesCollection.orderBy("order").get().await()
            snapshot.toObjects(Category::class.java).mapIndexed { index, category ->
                category.copy(id = snapshot.documents[index].id)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getPostsFlow(categoryId: String): Flow<List<Post>> = callbackFlow {
        val currentUserId = auth.currentUser?.uid

        // --- Giải quyết 🧩 a: Xử lý logic trùng lặp ---
        // Xây dựng câu query dựa trên categoryId
        val query = when (categoryId) {
            // "Tất cả" và "Mới nhất" dùng chung query, không cần lọc category
            "all", "latest" -> postsCollection.orderBy("timestamp", Query.Direction.DESCENDING)
            else -> postsCollection
                .whereEqualTo("category", categoryId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
        }

        // Lắng nghe thay đổi thời gian thực
        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val posts = snapshot.documents.mapNotNull { doc ->
                    doc.toPostOrNull()?.enrich(currentUserId)
                }
                trySend(posts) // Phát ra danh sách bài viết mới
            }
        }

        // Khi Flow bị hủy (ví dụ: ViewModel bị destroy), gỡ listener
        awaitClose { listener.remove() }
    }

    // 🔸 Xử lý Like/Unlike
    suspend fun toggleLikeStatus(postId: String, isCurrentlyLiked: Boolean) {
        val currentUserId = auth.currentUser?.uid ?: return
        val postRef = postsCollection.document(postId)

        if (isCurrentlyLiked) {
            // Nếu đang thích -> Bỏ thích
            postRef.update(
                "likes", FieldValue.increment(-1),
                "likedBy", FieldValue.arrayRemove(currentUserId)
            ).await()
        } else {
            // Nếu chưa thích -> Thích
            postRef.update(
                "likes", FieldValue.increment(1),
                "likedBy", FieldValue.arrayUnion(currentUserId)
            ).await()
        }
    }

    // 🔹 Cập nhật trạng thái Save (lưu/bỏ lưu)
    suspend fun toggleSaveStatus(postId: String, isCurrentlySaved: Boolean) {
        val userId = auth.currentUser?.uid ?: return
        val postRef = postsCollection.document(postId)

        if (isCurrentlySaved) {
            // Nếu đang lưu -> Bỏ lưu
            postRef.update(
                "savedBy", FieldValue.arrayRemove(userId),
                "saveCount", FieldValue.increment(-1)
            ).await()
        } else {
            // Nếu chưa lưu -> Lưu
            postRef.update(
                "savedBy", FieldValue.arrayUnion(userId),
                "saveCount", FieldValue.increment(1)
            ).await()
        }
    }
    suspend fun getPostsForUser(userId: String): List<Post> { // Đổi tên từ getUserPosts
        return try {
            val snapshot = postsCollection
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                // Có thể dùng lại hàm private toPostOrNull() nếu bạn có
                doc.toObject(Post::class.java)?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            Log.e("PostRepository", "Error fetching user posts", e)
            emptyList()
        }
    }

    suspend fun addComment(postId: String, commentText: String): Result<Unit> = runCatching {
        val currentUserId = auth.currentUser?.uid
            ?: throw IllegalStateException("User not logged in")

        // Lấy thông tin người dùng hiện tại để đính kèm vào bình luận
        val userDoc = db.collection("users").document(currentUserId).get().await()
        val username = userDoc.getString("username") ?: "User"
        val avatarUrl = userDoc.getString("avatarUrl") ?: ""

        val postRef = postsCollection.document(postId)
        val commentCollection = postRef.collection("comments")

        val commentData = hashMapOf(
            "userId" to currentUserId,
            "username" to username,
            "userAvatarUrl" to avatarUrl,
            "text" to commentText,
            "timestamp" to FieldValue.serverTimestamp()
        )

        db.runTransaction { transaction ->
            // Bước 1: Tăng commentCount trên bài viết
            transaction.update(postRef, "commentCount", FieldValue.increment(1))
            // Bước 2: Thêm document bình luận mới
            transaction.set(commentCollection.document(), commentData)
            // Transaction sẽ tự động commit hoặc rollback nếu có lỗi
        }.await()
    }

    /**
     * ✅ HÀM MỚI QUAN TRỌNG: Lấy danh sách bình luận theo thời gian thực.
     * Sử dụng Flow để tự động cập nhật UI khi có bình luận mới.
     */
    fun getCommentsFlow(postId: String): Flow<List<Comment>> = callbackFlow {
        val listener = postsCollection.document(postId)
            .collection("comments")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val comments = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Comment::class.java)?.copy(id = doc.id)
                    }
                    trySend(comments) // Gửi danh sách bình luận mới nhất
                }
            }
        awaitClose { listener.remove() } // Hủy listener khi Flow bị đóng
    }

    // --- 🔸 HÀM ẨN BÀI VIẾT ---
    suspend fun hidePost(postId: String): Boolean {
        val currentUserId = auth.currentUser?.uid ?: return false
        val userRef = usersCollection.document(currentUserId)

        return try {
            userRef.update(
                "hiddenPosts", FieldValue.arrayUnion(postId)
            ).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // --- 🔸 HÀM LẤY DANH SÁCH BÀI VIẾT ẨN ---
    suspend fun getHiddenPostIds(): List<String> {
        val currentUserId = auth.currentUser?.uid ?: return emptyList()
        return try {
            val snapshot = usersCollection.document(currentUserId).get().await()
            (snapshot.get("hiddenPosts") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // --- 🔸 HÀM BÁO CÁO BÀI VIẾT ---
    suspend fun reportPost(postId: String, reason: String, description: String): Boolean {
        val currentUserId = auth.currentUser?.uid ?: return false

        return try {
            val report = Report(
                postId = postId,
                reportedBy = currentUserId,
                reason = reason,
                description = description,
                status = "pending"
            )
            reportsCollection.add(report).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // 🔸 Xóa bài viết (chỉ chủ bài mới được xóa)
    suspend fun deletePost(postId: String): Boolean {
        val currentUserId = auth.currentUser?.uid ?: return false
        val postRef = postsCollection.document(postId)

        return try {
            val snapshot = postRef.get().await()
            val ownerId = snapshot.getString("userId")

            if (ownerId == currentUserId) {
                postRef.delete().await()
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    // 🔸 PAGINATION - Lấy posts theo trang (đơn giản, không dùng offset)
    suspend fun getPostsByPage(
        categoryId: String,
        page: Int,
        pageSize: Int = 10
    ): List<Post> {
        return try {
            val currentUserId = auth.currentUser?.uid

            // Xây dựng query theo category
            val query = when (categoryId) {
                "all", "latest" -> postsCollection
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit((pageSize * (page + 1)).toLong())
                else -> postsCollection
                    .whereEqualTo("category", categoryId)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit((pageSize * (page + 1)).toLong())
            }

            val snapshot = query.get().await()
            val allPosts = snapshot.documents.mapNotNull { doc ->
                doc.toPostOrNull()?.enrich(currentUserId)
            }
            
            // Lấy posts của trang hiện tại
            val start = page * pageSize
            val end = (page + 1) * pageSize
            if (start >= allPosts.size) {
                emptyList()
            } else {
                allPosts.subList(start, minOf(end, allPosts.size))
            }
        } catch (exception: Exception) {
            Log.e("PostRepository", "Error loading posts by page", exception)
            emptyList()
        }
    }

    // 🔸 COMMENT LIKE - Toggle like status cho comment
    suspend fun toggleCommentLikeStatus(commentId: String, isCurrentlyLiked: Boolean) {
        val currentUserId = auth.currentUser?.uid ?: return
        
        try {
            // Comments được lưu trong sub-collection của posts
            val commentsCollection = db.collection("comments")
            val commentRef = commentsCollection.document(commentId)

            if (isCurrentlyLiked) {
                // Unlike - xóa user khỏi likedBy array
                commentRef.update(
                    "likedBy", FieldValue.arrayRemove(currentUserId),
                    "likes", FieldValue.increment(-1)
                ).await()
            } else {
                // Like - thêm user vào likedBy array
                commentRef.update(
                    "likedBy", FieldValue.arrayUnion(currentUserId),
                    "likes", FieldValue.increment(1)
                ).await()
            }
        } catch (exception: Exception) {
            Log.e("PostRepository", "Error toggling comment like status", exception)
            throw exception
        }
    }

    private fun DocumentSnapshot.toPostOrNull(): Post? {
        val imageUrls = sanitizeStringList(get("imageUrls"), treatBlankAsEmpty = true)
        val likedBy = sanitizeStringList(get("likedBy"))
        val savedBy = sanitizeStringList(get("savedBy"))

        return Post(
            timestamp = getTimestamp("timestamp"),
            id = id,
            userId = getString("userId") ?: "",
            username = getString("username") ?: "",
            userAvatarUrl = getString("userAvatarUrl") ?: "",
            textContent = getString("textContent") ?: "",
            imageUrls = imageUrls,
            category = getString("category") ?: "",
            likes = getLong("likes")?.toInt() ?: 0,
            commentCount = getLong("commentCount")?.toInt() ?: 0,
            shareCount = getLong("shareCount")?.toInt() ?: 0,
            saveCount = getLong("saveCount")?.toInt() ?: 0,
            likedBy = likedBy,
            savedBy = savedBy
        )
    }

    private fun Post.enrich(currentUserId: String?): Post {
        val liked = currentUserId?.let { likedBy.contains(it) } ?: false
        val saved = currentUserId?.let { savedBy.contains(it) } ?: false
        return copy(isLiked = liked, isSaved = saved)
    }

    private fun sanitizeStringList(raw: Any?, treatBlankAsEmpty: Boolean = false): List<String> {
        return when (raw) {
            is List<*> -> raw.filterIsInstance<String>()
            is String -> {
                if (treatBlankAsEmpty && raw.isBlank()) emptyList() else listOf(raw)
            }
            null -> emptyList()
            else -> emptyList()
        }
    }
}

