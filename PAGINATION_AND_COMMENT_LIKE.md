# 🎯 2 Cải Thiện Chính

## ✅ 1. Pagination Đúng Cách cho Infinite Scroll

### Trong HomeModel.kt:

```kotlin
// Thêm PaginationState
data class PaginationState(
    val currentPage: Int = 0,
    val pageSize: Int = 10,
    val hasMore: Boolean = true,
    val isLoadingMore: Boolean = false
)

// Thêm vào HomeUiState
data class HomeUiState(
    ...
    val paginationState: PaginationState = PaginationState()
)

// Thêm method onLoadMore() - QUAN TRỌNG
fun onLoadMore() {
    // Kiểm tra điều kiện
    // Gọi: postRepository.getPostsByPage(categoryId, page, pageSize)
    // Cộng thêm posts mới vào danh sách cũ
    // Update paginationState (page++, hasMore, isLoadingMore)
}
```

**Cách dùng trong HomeScreen:**
```kotlin
// Detect khi scroll gần cuối
if (lastVisibleIndex >= posts.size - 5) {
    viewModel.onLoadMore()  // Tải thêm!
}

// Hiển thị loading
if (uiState.paginationState.isLoadingMore) {
    CircularProgressIndicator()
}
```

---

## ✅ 2. Comment Like Lưu Server

### Trong HomeModel.kt:

```kotlin
fun onCommentLikeClicked(commentId: String) {
    // 1. Cập nhật UI ngay (optimistic update)
    
    // 2. 🔸 GỌI SERVER - QUAN TRỌNG!
    try {
        postRepository.toggleCommentLikeStatus(commentId, isCurrentlyLiked)
    } catch (e: Exception) {
        // Khôi phục nếu lỗi
    }
}
```

### Trong PostRepository.kt:

```kotlin
suspend fun toggleCommentLikeStatus(commentId: String, isCurrentlyLiked: Boolean) {
    val currentUserId = auth.currentUser?.uid ?: return
    
    val commentRef = db.collection("comments").document(commentId)
    
    if (isCurrentlyLiked) {
        // Unlike
        commentRef.update(
            "likedBy", FieldValue.arrayRemove(currentUserId),
            "likes", FieldValue.increment(-1)
        ).await()
    } else {
        // Like
        commentRef.update(
            "likedBy", FieldValue.arrayUnion(currentUserId),
            "likes", FieldValue.increment(1)
        ).await()
    }
}
```

**Firestore:**
```
comments collection
└─ comment_123
   ├─ likes: 5
   ├─ likedBy: ["user_1", "user_2", ...]  ← User được thêm vào đây
   └─ ...
```

---

## 📊 So Sánh

| Tính Năng | Trước | Sau |
|-----------|-------|-----|
| **Infinite Scroll** | ❌ Lấy tất cả bài từ đầu | ✅ Pagination từng trang |
| **Comment Like** | ❌ Không gọi server | ✅ Gọi server, lưu Firebase |

---

## 🔧 Thay Đổi File

### 1. HomeModel.kt
- ✅ Thêm `PaginationState` class
- ✅ Thêm `paginationState` vào `HomeUiState`
- ✅ Thêm method `onLoadMore()`
- ✅ Fix method `onCommentLikeClicked()` - gọi server

### 2. PostRepository.kt
- ✅ Thêm method `getPostsByPage(categoryId, page, pageSize)`
- ✅ Thêm method `toggleCommentLikeStatus(commentId, isCurrentlyLiked)`

---

## ✅ Linter Check

```
✅ HomeModel.kt - No errors
✅ PostRepository.kt - No errors
```

---

## 🎯 Next Steps

1. **Update HomeScreen.kt** - Thêm scroll listener để detect gần cuối
2. **Test pagination** - Scroll xuống, tải thêm bài viết
3. **Test comment like** - Like comment, check Firebase

---

**Status:** ✅ Hoàn thành
**Complexity:** Đơn giản, tập trung
**Ready to test:** 👍 Yes
