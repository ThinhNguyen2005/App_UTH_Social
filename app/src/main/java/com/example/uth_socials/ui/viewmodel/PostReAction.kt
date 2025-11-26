package com.example.uth_socials.ui.viewmodel

import android.util.Log
import com.example.uth_socials.data.post.Comment
import com.example.uth_socials.data.post.Post
import com.example.uth_socials.data.repository.PostRepository
import com.example.uth_socials.data.util.SecurityValidator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * Handler chung cho các tương tác với Post (like, save, comment)
 * Có thể được sử dụng bởi nhiều ViewModel để tránh duplicate code
 */
class PostReAction(
    private val postRepository: PostRepository,
    private val coroutineScope: CoroutineScope  //  Thêm CoroutineScope

) {
    private val savingPosts = mutableSetOf<String>()

    /**
     * Xử lý like/unlike post
     */
    fun mutiLikePost(
        postId: String,
        posts: List<Post>,
        isUserBanned: Boolean,
        onBanDialog: () -> Unit,
        onPostsUpdate: (List<Post>) -> Unit,
        onError: (String) -> Unit
    ) {
        if (isUserBanned) {
            onBanDialog()
            return
        }

        coroutineScope.launch(Dispatchers.IO) {
            val postToUpdate = posts.find { it.id == postId } ?: return@launch
            val isCurrentlyLiked = postToUpdate.isLiked

            // Optimistic update
            val updatedPost = postToUpdate.copy(
                isLiked = !isCurrentlyLiked,
                likes = if (isCurrentlyLiked) postToUpdate.likes - 1 else postToUpdate.likes + 1
            )
            val updatedPosts = posts.map { if (it.id == postId) updatedPost else it }
            onPostsUpdate(updatedPosts)

            try {
                postRepository.toggleLikeStatus(postId, isCurrentlyLiked)
                Log.d("PostInteractionHandler", "Toggled like for post: $postId")
            } catch (e: Exception) {
                onPostsUpdate(posts) // Rollback
                onError("Lỗi không thể like bài viết. Vui lòng thử lại sau.")
                Log.e("PostInteractionHandler", "Error updating like status", e)
            }
        }
    }

    /**
     * Xử lý save/unsave post
     */
    fun mutiSavePost(
        postId: String,
        posts: List<Post>,
        isUserBanned: Boolean,
        onBanDialog: () -> Unit,
        onPostsUpdate: (List<Post>) -> Unit,
        onError: (String) -> Unit
    ) {
        if (isUserBanned) {
            onBanDialog()
            return
        }

        if (savingPosts.contains(postId)) return

        coroutineScope.launch(Dispatchers.IO) {
            val postToUpdate = posts.find { it.id == postId } ?: return@launch
            savingPosts.add(postId)

            // Optimistic update
            val updatedPost = postToUpdate.copy(
                isSaved = !postToUpdate.isSaved,
                saveCount = if (postToUpdate.isSaved) postToUpdate.saveCount - 1 else postToUpdate.saveCount + 1
            )
            val updatedPosts = posts.map { if (it.id == postId) updatedPost else it }
            onPostsUpdate(updatedPosts)

            try {
                postRepository.toggleSaveStatus(postId, postToUpdate.isSaved)
                Log.d("PostInteractionHandler", "Toggled save for post: $postId")
            } catch (e: Exception) {
                onPostsUpdate(posts) // Rollback
                onError("Lỗi không thể lưu bài viết. Vui lòng thử lại.")
                Log.e("PostInteractionHandler", "Error toggling save status", e)
            } finally {
                savingPosts.remove(postId)
            }
        }
    }

    /**
     * Xử lý like/unlike comment
     */
    fun mutiLikeComment(
        postId: String,
        commentId: String,
        comments: List<Comment>,
        onCommentsUpdate: (List<Comment>) -> Unit,
        onError: (String) -> Unit
    ) {
        coroutineScope.launch(Dispatchers.IO) {
            val commentToUpdate = comments.find { it.id == commentId } ?: return@launch
            val isCurrentlyLiked = commentToUpdate.liked

            // Optimistic update
            val updatedComment = commentToUpdate.copy(
                liked = !isCurrentlyLiked,
                likes = if (isCurrentlyLiked) commentToUpdate.likes - 1 else commentToUpdate.likes + 1
            )
            val updatedComments = comments.map { if (it.id == commentId) updatedComment else it }
            onCommentsUpdate(updatedComments)

            try {
                postRepository.toggleCommentLikeStatus(postId, commentId, isCurrentlyLiked)
                Log.d("PostInteractionHandler", "Toggled comment like: $commentId")
            } catch (e: Exception) {
                onCommentsUpdate(comments) // Rollback
                onError("Lỗi không thể like bình luận.")
                Log.e("PostInteractionHandler", "Error updating comment like status", e)
            }
        }
    }

    /**
     * Xử lý thêm comment
     */
    fun mutiAddComment(
        postId: String,
        commentText: String,
        isUserBanned: Boolean,
        onBanDialog: () -> Unit,
        onPosting: () -> Unit,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (commentText.isBlank()) return

        if (isUserBanned) {
            onBanDialog()
            return
        }

        coroutineScope.launch(Dispatchers.IO) {
            onPosting()

            val result = postRepository.addComment(postId, commentText)
            result.onSuccess {
                onSuccess()
                delay(1500)
            }.onFailure { e ->
                val errorMessage = when (e) {
                    is IllegalStateException -> e.message ?: "Lỗi không xác định"
                    else -> "Không thể gửi bình luận. Vui lòng thử lại."
                }
                onError(errorMessage)
                Log.e("PostInteractionHandler", "Failed to add comment", e)
            }
        }
    }

    fun mutiOnEditPostClicked(
        postId: String,
        posts: List<Post>,
        onShowEditDialog: (String, String) -> Unit  // postId, content
    ) {
        val post = posts.find { it.id == postId }
        if (post != null) {
            Log.d("PostReAction", "onEditPostClicked: $postId")
            onShowEditDialog(postId, post.textContent)
        }
    }

    /**
     * Xử lý cập nhật nội dung post
     */
    fun mutiSaveEditedPost(
        postId: String,
        newContent: String,
        posts: List<Post>,
        onSaving: () -> Unit,
        onPostsUpdate: (List<Post>) -> Unit,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val trimmedContent = newContent.trim()

        if (trimmedContent.isEmpty()) {
            Log.w("PostReAction", "Empty post content")
            onError("Nội dung không được để trống")
            return
        }

        coroutineScope.launch(Dispatchers.IO) {
            Log.d("PostReAction", "onSaveEditedPost: $postId, $trimmedContent")

            onSaving()

            try {
                val result = postRepository.updatePostContent(postId, trimmedContent)
                result.onSuccess {
                    Log.d("PostReAction", "Post updated successfully: $postId")
                    // Update local state
                    val updatedPosts = posts.map { post ->
                        if (post.id == postId) {
                            post.copy(textContent = trimmedContent)
                        } else {
                            post
                        }
                    }
                    onPostsUpdate(updatedPosts)
                    onSuccess()
                }.onFailure { e ->
                    val errorMessage = "Lỗi khi lưu: ${e.message ?: "Vui lòng thử lại"}"
                    onError(errorMessage)
                    Log.e("PostReAction", "Error updating post", e)
                }
            } catch (e: Exception) {
                val errorMessage = "Lỗi khi lưu: ${e.message ?: "Vui lòng thử lại"}"
                onError(errorMessage)
                Log.e("PostReAction", "Error updating post", e)
            }
        }
    }
    fun mutiOnDismissEditDialog(
        onDismiss: () -> Unit
    ) {
        onDismiss()
    }

    fun mutiHidePost(
        postId: String,
        posts: List<Post>,
        onPostsUpdate: (List<Post>) -> Unit,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val success = postRepository.hidePost(postId)

                if (success) {
                    val updatedPosts = posts.filter { it.id != postId }
                    onPostsUpdate(updatedPosts)
                    onSuccess("Đã ẩn bài viết")
                } else {
                    onError("Không thể ẩn bài viết")
                }

            } catch (e: Exception) {
                Log.e("PostReAction", "Error hiding post", e)
                onError("Lỗi khi ẩn bài viết: ${e.message}")
            }
        }
    }
    fun mutiSharePost(
        postId: String,
        onShareReady: (String) -> Unit
    ) {
        // Tạo nội dung chia sẻ (shareable content)
        val shareableContent = "Xem bài viết này trên UTH Socials: https://uthsocials://post/$postId"
        onShareReady(shareableContent)
    }

    /**
     * Xử lý mở comment sheet và load comments
     */
    fun mutiOnCommentClicked(
        postId: String,
        isUserBanned: Boolean,
        onBanDialog: () -> Unit,
        onOpenSheet: (String) -> Unit,  // postId
        onCommentsUpdate: (List<Comment>) -> Unit,
        onSheetLoading: (Boolean) -> Unit,
        onJobCreated: (Job) -> Unit  // Để ViewModel có thể cancel job
    ) {
        if (isUserBanned) {
            onBanDialog()
            return
        }

        onOpenSheet(postId)
        onSheetLoading(true)
        onCommentsUpdate(emptyList())

        // Bắt đầu một coroutine mới để lắng nghe bình luận
        val job = coroutineScope.launch(Dispatchers.IO) {
            postRepository.getCommentsFlow(postId).collect { comments ->
                onCommentsUpdate(comments)
                onSheetLoading(false)
            }
        }
        onJobCreated(job)

        Log.d("PostReAction", "Comment clicked for post: $postId")
    }

    /**
     * Xử lý đóng comment sheet
     */
    fun mutiOnDismissCommentSheet(
        commentsJob: Job?,
        onCloseSheet: () -> Unit
    ) {
        commentsJob?.cancel()
        onCloseSheet()
    }

    /**
     * Xử lý mở report dialog
     */
    fun mutiOnReportClicked(
        postId: String,
        isUserBanned: Boolean,
        onBanDialog: () -> Unit,
        onOpenReportDialog: (String) -> Unit  // postId
    ) {
        if (isUserBanned) {
            onBanDialog()
            return
        }

        onOpenReportDialog(postId)
    }

    /**
     * Xử lý submit report
     */
    fun mutiOnSubmitReport(
        reportingPostId: String,
        reason: String,
        description: String,
        posts: List<Post>,
        onReporting: () -> Unit,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (reason.isEmpty()) {
            onError("Vui lòng chọn lý do báo cáo")
            return
        }

        coroutineScope.launch(Dispatchers.IO) {
            onReporting()

            try {
                val post = posts.find { it.id == reportingPostId }
                if (post != null) {
                    val (isAdmin, _) = SecurityValidator.getCachedAdminStatus(post.userId)
                    if (isAdmin) {
                        throw IllegalArgumentException("Không thể báo cáo admin")
                    }
                }
                val success = postRepository.reportPost(reportingPostId, reason, description)
                if (success) {
                    onSuccess()
                    Log.d("PostReAction", "Report submitted successfully")
                } else {
                    val errorMsg = "Gửi báo cáo thất bại. Vui lòng thử lại."
                    onError(errorMsg)
                    Log.w("PostReAction", "Report submission failed: $errorMsg")
                }
            } catch (e: IllegalArgumentException) {
                val errorMsg = e.message ?: "Lỗi không xác định"
                onError(errorMsg)
                Log.w("PostReAction", "Report blocked: $errorMsg")
            } catch (e: Exception) {
                val errorMsg = "Lỗi khi gửi báo cáo: ${e.message ?: "Vui lòng thử lại"}"
                onError(errorMsg)
                Log.e("PostReAction", "Error submitting report", e)
            }
        }
    }

    /**
     * Xử lý đóng report dialog
     */
    fun mutiOnDismissReportDialog(
        onCloseReportDialog: () -> Unit
    ) {
        onCloseReportDialog()
    }

    /**
     * Xử lý mở delete dialog (cần check permission)
     */
    fun mutiOnDeleteClicked(
        postId: String,
        posts: List<Post>,
        currentUserId: String?,
        onShowDeleteDialog: (String) -> Unit,  // postId
        onError: (String) -> Unit
    ) {
        coroutineScope.launch(Dispatchers.IO) {
            val post = posts.find { it.id == postId } ?: run {
                onError("Không tìm thấy bài viết")
                return@launch
            }

            if (SecurityValidator.canDeletePost(currentUserId, post.userId)) {
                onShowDeleteDialog(postId)
            } else {
                onError("Bạn không có quyền xóa bài viết này")
            }
        }
    }

    /**
     * Xử lý xác nhận xóa bài viết
     */
    fun mutiOnConfirmDelete(
        postId: String,
        posts: List<Post>,
        onDeleting: () -> Unit,
        onPostsUpdate: (List<Post>) -> Unit,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        coroutineScope.launch(Dispatchers.IO) {
            onDeleting()

            try {
                val success = postRepository.deletePost(postId)
                if (success) {
                    val updatedPosts = posts.filter { it.id != postId }
                    onPostsUpdate(updatedPosts)
                    onSuccess("Đã xóa bài viết")
                    Log.d("PostReAction", "Post deleted successfully: $postId")
                } else {
                    onError("Không thể xóa bài viết")
                }
            } catch (e: Exception) {
                val errorMsg = "Lỗi khi xóa bài viết: ${e.message ?: "Vui lòng thử lại"}"
                onError(errorMsg)
                Log.e("PostReAction", "Error deleting post", e)
            }
        }
    }

}