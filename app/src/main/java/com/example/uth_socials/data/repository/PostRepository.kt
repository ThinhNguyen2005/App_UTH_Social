package com.example.uth_socials.data.repository

import com.example.uth_socials.data.post.Comment
import com.example.uth_socials.data.post.Post
import com.example.uth_socials.data.post.Report
import com.example.uth_socials.data.util.SecurityValidator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import android.util.Log
import com.example.uth_socials.data.market.Product
import com.example.uth_socials.data.notification.Notification
import com.example.uth_socials.data.user.User
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.FlowPreview
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import com.example.uth_socials.data.util.FirestoreConstants

/**
 * PostRepository - Quản lý tất cả thao tác với dữ liệu bài viết (Posts)
 *
 * Chức năng chính:
 * - Lấy danh sách bài viết theo thời gian thực
 * - Quản lý tương tác bài viết (like, save, hide)
 * - Hệ thống bình luận và tương tác comment
 * - Báo cáo và xóa bài viết
 * - Bảo mật và validation client-side
 *
 * Kiến trúc: Repository pattern với Firebase Firestore
 * Bảo mật: Sử dụng SecurityValidator cho client-side validation
 */
class PostRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val productsCollection = db.collection(FirestoreConstants.PRODUCTS_COLLECTION)
    private val postsCollection = db.collection(FirestoreConstants.POSTS_COLLECTION)
    private val reportsCollection = db.collection(FirestoreConstants.REPORTS_COLLECTION)
    private val usersCollection = db.collection(FirestoreConstants.USERS_COLLECTION)

    // ✅ OPTIMIZATION: Cache enriched posts để tránh enrich lại
    private val enrichedPostsCache = ConcurrentHashMap<String, Post>()

    companion object {
        // ✅ OPTIMIZATION: Pagination limit - chỉ load 20 posts đầu tiên
        private const val POSTS_LIMIT = 20
        // ✅ OPTIMIZATION: Debounce time để gom nhiều updates (ms)
        private const val DEBOUNCE_TIME_MS = 300L
    }

    suspend fun uploadArticle(
        user : User?, content: String, category: String?, imageUrl : List<String>
    ): Boolean {
        return try {
            // Tạo bài viết
            val post = Post(
                id = UUID.randomUUID().toString(),
                username = user?.username ?: "",
                userAvatarUrl = user?.avatarUrl ?: "",
                userId = user?.id ?: "",
                textContent = content.trim(),
                textContentFormat = content.trim().lowercase(),
                imageUrls = imageUrl,
                category  = category
            )

            // Lưu lên Firestore
            postsCollection.document(post.id).set(post).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun uploadProduct(
        user: User?, name: String, description: String, type: String, price: Int, imageUrls : List<String>
    ): Boolean {
        return try {
            // Tạo bài viết
            val product = Product(
                id = UUID.randomUUID().toString(),
                name = name,
                userId = user?.id ?: "",
                userName = user?.username ?: "",
                userAvatar = user?.avatarUrl ?: "",
                description = description,
                type = type,
                price = price,
                imageUrls = imageUrls
            )

            // Lưu lên Firestore
            productsCollection.document(product.id).set(product).await()

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // ==========================================
    // POST DATA FUNCTIONS (Lấy dữ liệu bài viết)
    // ==========================================

    /**
     * Lấy danh sách bài viết theo thời gian thực với filtering theo category
     *
     * ✅ OPTIMIZED với:
     * - Pagination: Chỉ load 20 posts đầu tiên
     * - Debounce: Gom nhiều updates trong 300ms
     * - Cache: Tránh enrich lại posts không thay đổi
     *
     * @param categoryId ID của category muốn filter ("all", "latest", hoặc category cụ thể)
     * @return Flow<List<Post>> - Stream dữ liệu real-time
     *
     * Logic:
     * - Tạo query Firestore dựa trên categoryId với limit
     * - Sử dụng callbackFlow để tạo Flow từ SnapshotListener
     * - Debounce để giảm số lần emit
     * - Process documents trên background thread
     * - Enrich posts với thông tin user (liked/saved status) với cache
     *
     * Categories:
     * - "all": Tất cả bài viết
     * - "latest": Bài viết mới nhất (giống "all")
     * - category cụ thể: Chỉ bài viết trong category đó
     */
    @OptIn(FlowPreview::class)
    fun getPostsFlow(categoryId: String): Flow<List<Post>> = callbackFlow {
        // --- Category filtering logic với PAGINATION ---
        val query = when (categoryId) {
            // "all" - show ALL posts (with or without category)
            "all" -> postsCollection
                .orderBy(FirestoreConstants.FIELD_TIMESTAMP, Query.Direction.DESCENDING)
                .limit(POSTS_LIMIT.toLong()) // ✅ PAGINATION

            // "latest" - show latest posts (same as "all")
            "latest" -> postsCollection
                .orderBy(FirestoreConstants.FIELD_TIMESTAMP, Query.Direction.DESCENDING)
                .limit(POSTS_LIMIT.toLong()) // ✅ PAGINATION

            // Specific category - only posts with this category
            else -> {
                if (categoryId.isBlank()) {
                    Log.w("PostRepository", "Empty categoryId provided, showing all posts")
                    postsCollection
                        .orderBy(FirestoreConstants.FIELD_TIMESTAMP, Query.Direction.DESCENDING)
                        .limit(POSTS_LIMIT.toLong()) // ✅ PAGINATION
                } else {
                    postsCollection
                        .whereEqualTo(FirestoreConstants.FIELD_CATEGORY, categoryId)
                        .orderBy(FirestoreConstants.FIELD_TIMESTAMP, Query.Direction.DESCENDING)
                        .limit(POSTS_LIMIT.toLong()) // ✅ PAGINATION
                }
            }
        }

        // Lắng nghe thay đổi thời gian thực
        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                if (error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                    Log.w("CategoryRepository", "Permission denied listening to categories. Emitting empty list.")
                    trySend(emptyList()) // Gửi list rỗng thay vì crash
                } else {
                    close(error) // Chỉ đóng với các lỗi nghiêm trọng khác
                }
                return@addSnapshotListener
            }
            if (snapshot != null) {
                // Emit raw documents to be processed on background thread
                trySend(snapshot.documents)
            }
        }

        awaitClose { listener.remove() }
    }
        .debounce(DEBOUNCE_TIME_MS) // ✅ DEBOUNCE: Gom nhiều updates trong 300ms
        .map { documents ->
            val currentUserId = auth.currentUser?.uid
            documents.mapNotNull { doc ->
                try {
                    val postId = doc.id
                    val rawPost = doc.toObject(Post::class.java)

                    if (rawPost != null) {
                        // ✅ CACHE CHECK: Chỉ enrich nếu chưa có hoặc có thay đổi
                        val cachedPost = enrichedPostsCache[postId]
                        val shouldReEnrich = cachedPost == null ||
                            cachedPost.timestamp != rawPost.timestamp ||
                            cachedPost.likes != rawPost.likes ||
                            cachedPost.saveCount != rawPost.saveCount ||
                            cachedPost.commentCount != rawPost.commentCount

                        if (shouldReEnrich) {
                            val enriched = rawPost.enrich(currentUserId)?.copy(id = postId)
                            enriched?.let { enrichedPostsCache[postId] = it }
                            enriched
                        } else {
                            cachedPost // ✅ Sử dụng cache
                        }
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    Log.e("PostRepository", "Error mapping post ${doc.id}", e)
                    null
                }
            }
        }
        .flowOn(Dispatchers.IO)

    // ==========================================
    // POST INTERACTION FUNCTIONS (Tương tác bài viết)
    // ==========================================

    /**
     * Toggle trạng thái like/unlike của bài viết
     *
     * @param postId ID của bài viết
     * @param isCurrentlyLiked Trạng thái hiện tại (true = đang like)
     *
     * Logic:
     * - Validate user authentication
     * - Nếu đang like: giảm likes, remove userId từ likedBy array
     * - Nếu chưa like: tăng likes, add userId vào likedBy array
     * - Sử dụng FieldValue.increment() để atomic operations
     *
     * Security: Client-side permission check
     */
    suspend fun toggleLikeStatus(postId: String, isCurrentlyLiked: Boolean) {
        val currentUserId = auth.currentUser?.uid ?: return
        if (!SecurityValidator.checkCurrentUserId(currentUserId)) {
            Log.w("PostRepository", "toggleLikeStatus denied for $currentUserId")
            return
        }
        val postRef = postsCollection.document(postId)

        if (isCurrentlyLiked) {
            // Nếu đang thích -> Bỏ thích
            postRef.update(
                FirestoreConstants.FIELD_LIKES, FieldValue.increment(-1),
                FirestoreConstants.FIELD_LIKED_BY, FieldValue.arrayRemove(currentUserId)
            ).await()
        } else {
            // Nếu chưa thích -> Thích
            postRef.update(
                FirestoreConstants.FIELD_LIKES, FieldValue.increment(1),
                FirestoreConstants.FIELD_LIKED_BY, FieldValue.arrayUnion(currentUserId)
            ).await()
        }

        // ✅ OPTIMIZATION: Invalidate cache cho post này
        enrichedPostsCache.remove(postId)
    }

    /**
     * Toggle trạng thái save/unsave của bài viết
     *
     * @param postId ID của bài viết
     * @param isCurrentlySaved Trạng thái hiện tại (true = đã save)
     *
     * Logic:
     * - Validate user authentication
     * - Nếu đã save: giảm saveCount, remove userId từ savedBy array
     * - Nếu chưa save: tăng saveCount, add userId vào savedBy array
     * - Atomic operations với FieldValue.increment()
     *
     * Use case: Cho phép user lưu bài viết để xem sau
     */
    suspend fun toggleSaveStatus(postId: String, isCurrentlySaved: Boolean) {
        val userId = auth.currentUser?.uid ?: return
        if (!SecurityValidator.checkCurrentUserId(userId)) {
            Log.w("PostRepository", "toggleSaveStatus denied for $userId")
            return
        }
        val postRef = postsCollection.document(postId)

        if (isCurrentlySaved) {
            // Nếu đang lưu -> Bỏ lưu
            postRef.update(
                FirestoreConstants.FIELD_SAVED_BY, FieldValue.arrayRemove(userId),
                FirestoreConstants.FIELD_SAVE_COUNT, FieldValue.increment(-1)
            ).await()
        } else {
            // Nếu chưa lưu -> Lưu
            postRef.update(
                FirestoreConstants.FIELD_SAVED_BY, FieldValue.arrayUnion(userId),
                FirestoreConstants.FIELD_SAVE_COUNT, FieldValue.increment(1)
            ).await()
        }

        // ✅ OPTIMIZATION: Invalidate cache cho post này
        enrichedPostsCache.remove(postId)
    }


    /**
     * Lấy danh sách bài viết theo thời gian thực của một user cụ thể
     *
     * @param userId ID của user muốn lấy posts
     * @return Flow<List<Post>> - Stream real-time posts của user đó
     *
     * Logic:
     * - Real-time listener cho posts của user cụ thể
     * - Order by timestamp descending (mới nhất trước)
     * - Enrich posts với liked/saved status của current user
     * - Handle Firestore index errors gracefully
     *
     * Use case: Real-time updates cho profile screen
     */
    fun getPostsForUserFlow(userId: String): Flow<List<Post>> = callbackFlow {
        val query = try {
            postsCollection
                .whereEqualTo(FirestoreConstants.FIELD_USER_ID, userId)
                .orderBy(FirestoreConstants.FIELD_TIMESTAMP, Query.Direction.DESCENDING)
        } catch (e: Exception) {
            Log.e("PostRepository", "Index issue for getPostsForUserFlow. Using basic query.")
            Log.e("PostRepository", "Error: $e")
            postsCollection
                .whereEqualTo(FirestoreConstants.FIELD_USER_ID, userId)
        }

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                if (error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                    Log.w("PostRepository", "Permission denied listening to user posts. Emitting empty list.")
                    trySend(emptyList())
                } else {
                    close(error)
                }
                return@addSnapshotListener
            }

            if (snapshot != null) {
                trySend(snapshot.documents)
            }
        }

        awaitClose { listener.remove() }
    }.map { documents ->
        val currentUserId = auth.currentUser?.uid
        documents.mapNotNull { doc ->
            try {
                val post = doc.toObject(Post::class.java)
                post?.enrich(currentUserId)?.copy(id = doc.id)

            } catch (e: Exception) {
                Log.e("PostRepository", "Error mapping post ${doc.id}", e)
                null
            }
        }
    }.flowOn(Dispatchers.IO)


    // ==========================================
    // COMMENT SYSTEM FUNCTIONS (Hệ thống bình luận)
    // ==========================================

    /**
     * Thêm bình luận mới vào bài viết
     *
     * @param postId ID của bài viết muốn comment
     * @param commentText Nội dung bình luận
     * @return Result<Unit> - Thành công hoặc thất bại với error
     *
     * Logic:
     * - Validate authentication và comment content
     * - Kiểm tra user profile và trạng thái banned
     * - Rate limiting check (client-side)
     * - Tạo comment document với auto-generated ID
     * - Transaction: Tăng commentCount + set comment data + update lastCommentAt
     *
     * Security: Multiple validation layers, rate limiting
     * Transaction: Đảm bảo data consistency
     */
    suspend fun addComment(postId: String, commentText: String): Result<Unit> {
        return try {
            Log.d("PostRepository", "Starting addComment for post: $postId")

            val currentUserId = auth.currentUser?.uid
                ?: throw IllegalStateException("User not logged in")
            Log.d("PostRepository", "Current user ID: $currentUserId")
            val userRepository = UserRepository()
            val user = userRepository.getUser(currentUserId)
                ?: throw IllegalStateException("User profile not found. Cannot comment.")
            if (user.isBanned) {
                throw IllegalStateException("User is banned. Cannot comment.")
            }
            val postRef = postsCollection.document(postId)
            val newCommentRef = postRef.collection(FirestoreConstants.COMMENTS_COLLECTION).document()
            val userRef = usersCollection.document(currentUserId)

            val commentData = hashMapOf(
                "id" to newCommentRef.id,
                "userId" to currentUserId,
                "username" to user.username,
                "userAvatarUrl" to user.avatarUrl,
                "text" to commentText,
                "timestamp" to FieldValue.serverTimestamp(),
                "likedBy" to emptyList<String>(),
                "likes" to 0
            )

            Log.d("PostRepository", "Comment data prepared: $commentData")

            // ✅ TRANSACTION: TĂNG COMMENT COUNT + TẠO COMMENT + UPDATE USER STATS
            // BƯỚC 7: TRANSACTION - TĂNG COMMENT COUNT + TẠO COMMENT
            db.runTransaction { transaction ->
                Log.d("PostRepository", "Starting transaction")
                transaction.update(postRef, "commentCount", FieldValue.increment(1))
                transaction.set(newCommentRef, commentData)
                Log.d("PostRepository", "Transaction operations set")
            }.await()

            // BƯỚC 8: UPDATE LAST COMMENT TIME (RATE LIMITING)
            usersCollection.document(currentUserId)
                .update("lastCommentAt", FieldValue.serverTimestamp())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("PostRepository", "Failed to add comment", e)
            Result.failure(e)
        }
    }


    suspend fun addCommentReply(postId: String, commentParentname: String, commentParentId: String, commentText: String): Result<Unit> {
        return try {
            Log.d("PostRepository", "Starting addComment for post: $postId")

            val currentUserId = auth.currentUser?.uid
                ?: throw IllegalStateException("User not logged in")
            Log.d("PostRepository", "Current user ID: $currentUserId")
            val userRepository = UserRepository()
            val user = userRepository.getUser(currentUserId)
                ?: throw IllegalStateException("User profile not found. Cannot comment.")
            if (user.isBanned) {
                throw IllegalStateException("User is banned. Cannot comment.")
            }
            val postRef = postsCollection.document(postId)
            val newCommentRef = postRef.collection("comments").document()
            val userRef = usersCollection.document(currentUserId)

            val commentData = hashMapOf(
                "id" to newCommentRef.id,
                "userId" to currentUserId,
                "username" to user.username,
                "userAvatarUrl" to user.avatarUrl,
                "parentname" to commentParentname,
                "parentId" to commentParentId,
                "text" to commentText,
                "timestamp" to FieldValue.serverTimestamp(),
                "likedBy" to emptyList<String>(),
                "likes" to 0
            )

            Log.d("PostRepository", "Comment data prepared: $commentData")

            // BƯỚC 7: TRANSACTION - TĂNG COMMENT COUNT + TẠO COMMENT
            db.runTransaction { transaction ->
                Log.d("PostRepository", "Starting transaction")
                transaction.update(postRef, FirestoreConstants.FIELD_COMMENT_COUNT, FieldValue.increment(1))
                transaction.set(newCommentRef, commentData)
                transaction.update(userRef, FirestoreConstants.FIELD_LAST_COMMENT_AT, FieldValue.serverTimestamp())
                Log.d("PostRepository", "Transaction operations set")
            }.await()

            // BƯỚC 8: UPDATE LAST COMMENT TIME (RATE LIMITING)
            usersCollection.document(currentUserId)
                .update("lastCommentAt", FieldValue.serverTimestamp())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("PostRepository", "Failed to add comment", e)
            Result.failure(e)
        }
    }


    /**
     * Lấy danh sách bình luận theo thời gian thực của một bài viết
     *
     * @param postId ID của bài viết muốn lấy comments
     * @return Flow<List<Comment>> - Stream comments real-time
     *
     * Logic:
     * - Lắng nghe subcollection "comments" của post
     * - Order by timestamp ascending (cũ nhất trước)
     * - Process documents trên background thread
     * - Enrich comments với liked status của current user
     *
     * Features:
     * - Real-time updates khi có comment mới
     * - Automatic cleanup khi Flow cancelled
     * - Background processing để không block UI
     */

    fun getCommentsFlow(postId: String): Flow<List<Comment>> = callbackFlow {
        val listener = postsCollection.document(postId)
            .collection(FirestoreConstants.COMMENTS_COLLECTION)
            .orderBy(FirestoreConstants.FIELD_TIMESTAMP, Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    // Emit raw documents to be processed on background thread
                    trySend(snapshot.documents)
                }
            }
        awaitClose { listener.remove() } // Hủy listener khi Flow bị đóng
    }.map { documents ->
        // Process documents on background thread
        val currentUserId = auth.currentUser?.uid
        documents.mapNotNull { doc ->
            doc.toObject(Comment::class.java)?.let { comment ->
                val isLikedByCurrentUser = currentUserId?.let { comment.likedBy.contains(it) } == true
                comment.copy(
                    id = doc.id,
                    liked = isLikedByCurrentUser
                )
            }
        }
    }.flowOn(Dispatchers.IO)
    // ==========================================
    // POST MANAGEMENT FUNCTIONS (Quản lý bài viết)
    // ==========================================

    /**
     * Ẩn bài viết khỏi feed của user hiện tại
     *
     * @param postId ID của bài viết muốn ẩn
     * @return Boolean - true nếu thành công
     *
     * Logic:
     * - Validate authentication
     * - Add postId vào array "hiddenPosts" của user
     * - UI sẽ filter out các posts đã ẩn
     *
     * Use case: User không muốn thấy một số posts trong feed
     * Note: Đây là client-side hiding, không phải xóa hoàn toàn
     */
    suspend fun hidePost(postId: String): Boolean {
        val currentUserId = auth.currentUser?.uid ?: return false
        val userRef = usersCollection.document(currentUserId)

        return try {
            userRef.update(
                FirestoreConstants.FIELD_HIDDEN_POSTS, FieldValue.arrayUnion(postId)
            ).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Lấy danh sách ID của các bài viết đã bị ẩn bởi user hiện tại
     *
     * @return List<String> - Danh sách postId đã ẩn
     *
     * Logic:
     * - Query user document để lấy array "hiddenPosts"
     * - Parse array và filter chỉ lấy String values
     * - Return empty list nếu không có hoặc có lỗi
     *
     * Use case: UI filter out hidden posts khỏi feed
     */
    suspend fun getHiddenPostIds(): List<String> {
        val currentUserId = auth.currentUser?.uid ?: return emptyList()
        return try {
            val snapshot = usersCollection.document(currentUserId).get().await()
            (snapshot.get(FirestoreConstants.FIELD_HIDDEN_POSTS) as? List<*>)?.filterIsInstance<String>() ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // ==========================================
    // MODERATION FUNCTIONS (Chức năng điều hành)
    // ==========================================

    /**
     * Báo cáo bài viết vi phạm
     *
     * @param postId ID của bài viết bị báo cáo
     * @param reason Lý do báo cáo (spam, inappropriate, etc.)
     * @param description Mô tả chi tiết về vi phạm
     * @return Boolean - true nếu báo cáo thành công
     *
     * Logic:
     * - Validate authentication và permissions
     * - Không cho phép admin báo cáo (client-side check)
     * - Tạo document mới trong collection "reports"
     * - Lưu thông tin: postId, reporterId, reason, description, status
     *
     * Security: Client-side validation, admin không thể báo cáo
     * Use case: Moderation system để handle content violations
     */
    suspend fun reportPost(postId: String, reason: String, description: String): Boolean {
        val currentUserId = auth.currentUser?.uid ?: return false
        if (!SecurityValidator.canCreateReport(currentUserId, currentUserId)) {
            Log.w("PostRepository", "Cannot report: Invalid user")
            return false
        }

        return try {
            val report = Report(
                postId = postId,
                reportedBy = currentUserId,
                reason = reason,
                description = description,
                status = FirestoreConstants.STATUS_PENDING
            )
            reportsCollection.add(report).await()
            Log.d("PostRepository", "Report created successfully for post: $postId")
            true
        } catch (e: Exception) {
            Log.e("PostRepository", "Error reporting post: ${e.message}", e)
            false
        }
    }

    /**
     * Xóa bài viết hoàn toàn khỏi hệ thống
     *
     * @param postId ID của bài viết muốn xóa
     * @return Boolean - true nếu xóa thành công
     *
     * Logic:
     * - Validate authentication
     * - Lấy thông tin post để check ownership
     * - Client-side permission check (chủ bài hoặc admin)
     * - Xóa post document hoàn toàn
     * - Firebase Rules sẽ double-check permissions server-side
     *
     * Security: Double validation (client + server)
     * Permissions: Post owner OR Admin only
     * Warning: Action không thể undo
     */
    suspend fun deletePost(postId: String): Boolean {
        val currentUserId = auth.currentUser?.uid ?: return false
        val postRef = postsCollection.document(postId)

        return try {
            val snapshot = postRef.get().await()
            val ownerId = snapshot.getString(FirestoreConstants.FIELD_USER_ID) ?: return false

            if (SecurityValidator.canDeletePost(currentUserId, ownerId)) {
                postRef.delete().await()
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e("PostRepository", "Error deleting post", e)
            false
        }
    }

    suspend fun updatePostContent(postId: String, newContent: String): Result<Unit> {
        return try {
            val currentUserId = auth.currentUser?.uid
                ?: return Result.failure(IllegalStateException("User not logged in"))
            
            val postRef = postsCollection.document(postId)
            val postSnapshot = postRef.get().await()
            
            if (!postSnapshot.exists()) {
                return Result.failure(IllegalStateException("Post not found"))
            }
            
            val ownerId = postSnapshot.getString(FirestoreConstants.FIELD_USER_ID)
            if (ownerId != currentUserId) {
                return Result.failure(SecurityException("Only post owner can edit"))
            }
            
            postRef.update(FirestoreConstants.FIELD_TEXT_CONTENT, newContent).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("PostRepository", "Error updating post content", e)
            Result.failure(e)
        }
    }

    suspend fun toggleCommentLikeStatus(postId: String, commentId: String, isCurrentlyLiked: Boolean) {
        val currentUserId = auth.currentUser?.uid ?: return
        if (!SecurityValidator.checkCurrentUserId(currentUserId)) {
            Log.w("PostRepository", "toggleCommentLikeStatus denied for $currentUserId")
            return
        }

        try {
            val commentRef = postsCollection
                .document(postId)
                .collection(FirestoreConstants.COMMENTS_COLLECTION)
                .document(commentId)

            val commentSnapshot = commentRef.get().await()
            if (!commentSnapshot.exists()) {
                Log.w(
                    "PostRepository",
                    "toggleCommentLikeStatus: comment $commentId not found under post $postId"
                )
                return
            }

            if (isCurrentlyLiked) {
                commentRef.update(
                    FirestoreConstants.FIELD_LIKED_BY, FieldValue.arrayRemove(currentUserId),
                    FirestoreConstants.FIELD_LIKES, FieldValue.increment(-1)
                ).await()
            } else {
                commentRef.update(
                    FirestoreConstants.FIELD_LIKED_BY, FieldValue.arrayUnion(currentUserId),
                    FirestoreConstants.FIELD_LIKES, FieldValue.increment(1)
                ).await()
            }
        } catch (exception: Exception) {
            Log.e("PostRepository", "Error toggling comment like status", exception)
            throw exception
        }
    }

    private fun DocumentSnapshot.toPostOrNull(): Post? {
        val imageUrls = sanitizeStringList(get("imageUrls"), treatBlankAsEmpty = true)
            .filter { url ->
                val isValid = url.startsWith("http://") || url.startsWith("https://")
                if (!isValid && url.isNotEmpty()) {
                    Log.w("PostRepository", "Filtered invalid imageUrl: $url (Post ID: $id)")
                }
                isValid
            }
        val likedBy = sanitizeStringList(get(FirestoreConstants.FIELD_LIKED_BY))
        val savedBy = sanitizeStringList(get(FirestoreConstants.FIELD_SAVED_BY))

        return Post(
            timestamp = getTimestamp(FirestoreConstants.FIELD_TIMESTAMP),
            id = id,
            userId = getString(FirestoreConstants.FIELD_USER_ID) ?: "",
            username = getString(FirestoreConstants.FIELD_USERNAME) ?: "",
            userAvatarUrl = getString(FirestoreConstants.FIELD_AVATAR_URL) ?: "",
            textContent = getString(FirestoreConstants.FIELD_TEXT_CONTENT) ?: "",
            imageUrls = imageUrls,
            category = getString(FirestoreConstants.FIELD_CATEGORY) ?: "",
            likes = getLong(FirestoreConstants.FIELD_LIKES)?.toInt() ?: 0,
            commentCount = getLong(FirestoreConstants.FIELD_COMMENT_COUNT)?.toInt() ?: 0,
            shareCount = getLong("shareCount")?.toInt() ?: 0,
            saveCount = getLong(FirestoreConstants.FIELD_SAVE_COUNT)?.toInt() ?: 0,
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

    /**
     * ✅ OPTIMIZATION: Load more posts (pagination)
     * Sử dụng để load thêm posts khi user scroll xuống cuối
     *
     * @param categoryId ID của category
     * @param lastTimestamp Timestamp của post cuối cùng để làm cursor
     * @param limit Số posts muốn load (mặc định 20)
     * @return List<Post> - Danh sách posts mới
     */
    suspend fun loadMorePosts(
        categoryId: String,
        lastTimestamp: com.google.firebase.Timestamp?,
        limit: Int = POSTS_LIMIT
    ): List<Post> {
        val query = when (categoryId) {
            "all", "latest" -> {
                if (lastTimestamp != null) {
                    postsCollection
                        .orderBy(FirestoreConstants.FIELD_TIMESTAMP, Query.Direction.DESCENDING)
                        .startAfter(lastTimestamp)
                        .limit(limit.toLong())
                } else {
                    postsCollection
                        .orderBy(FirestoreConstants.FIELD_TIMESTAMP, Query.Direction.DESCENDING)
                        .limit(limit.toLong())
                }
            }
            else -> {
                if (lastTimestamp != null) {
                    postsCollection
                        .whereEqualTo(FirestoreConstants.FIELD_CATEGORY, categoryId)
                        .orderBy(FirestoreConstants.FIELD_TIMESTAMP, Query.Direction.DESCENDING)
                        .startAfter(lastTimestamp)
                        .limit(limit.toLong())
                } else {
                    postsCollection
                        .whereEqualTo(FirestoreConstants.FIELD_CATEGORY, categoryId)
                        .orderBy(FirestoreConstants.FIELD_TIMESTAMP, Query.Direction.DESCENDING)
                        .limit(limit.toLong())
                }
            }
        }

        val snapshot = query.get().await()
        val currentUserId = auth.currentUser?.uid

        return snapshot.documents.mapNotNull { doc ->
            try {
                val postId = doc.id
                val rawPost = doc.toObject(Post::class.java)

                if (rawPost != null) {
                    // Check cache trước
                    val cachedPost = enrichedPostsCache[postId]
                    val shouldReEnrich = cachedPost == null ||
                        cachedPost.timestamp != rawPost.timestamp ||
                        cachedPost.likes != rawPost.likes ||
                        cachedPost.saveCount != rawPost.saveCount ||
                        cachedPost.commentCount != rawPost.commentCount

                    if (shouldReEnrich) {
                        val enriched = rawPost.enrich(currentUserId)?.copy(id = postId)
                        enriched?.let { enrichedPostsCache[postId] = it }
                        enriched
                    } else {
                        cachedPost
                    }
                } else {
                    null
                }
            } catch (e: Exception) {
                Log.e("PostRepository", "Error mapping post ${doc.id}", e)
                null
            }
        }
    }

    /**
     * ✅ OPTIMIZATION: Clear cache khi cần (ví dụ: logout, refresh)
     */
    fun clearCache() {
        enrichedPostsCache.clear()
        Log.d("PostRepository", "Cache cleared")
    }
}

