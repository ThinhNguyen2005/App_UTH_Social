package com.example.uth_socials.data.util

object FirestoreConstants {
    // Collections
    const val USERS_COLLECTION = "users"
    const val POSTS_COLLECTION = "posts"
    const val REPORTS_COLLECTION = "reports"
    const val COMMENTS_COLLECTION = "comments"
    const val ADMIN_USERS_COLLECTION = "admin_users"

    // Fields - Users
    const val FIELD_USERNAME = "username"
    const val FIELD_AVATAR_URL = "avatarUrl"
    const val FIELD_IS_BANNED = "isBanned"
    const val FIELD_BANNED_AT = "bannedAt"
    const val FIELD_BANNED_BY = "bannedBy"
    const val FIELD_BAN_REASON = "banReason"
    const val FIELD_VIOLATION_COUNT = "violationCount"
    const val FIELD_WARNING_COUNT = "warningCount"
    const val FIELD_FOLLOWERS = "followers"
    const val FIELD_FOLLOWING = "following"
    const val FIELD_HIDDEN_POSTS = "hiddenPosts"
    const val FIELD_BLOCKED_USERS = "blockedUsers"
    const val FIELD_LAST_COMMENT_AT = "lastCommentAt"
    const val FIELD_ROLE = "role"
    const val FIELD_GRANTED_BY = "grantedBy"
    const val FIELD_GRANTED_AT = "grantedAt"
    const val FIELD_PERMISSIONS = "permissions"


    // Fields - Posts
    const val FIELD_USER_ID = "userId"
    const val FIELD_TIMESTAMP = "timestamp"
    const val FIELD_CATEGORY = "category"
    const val FIELD_LIKES = "likes"
    const val FIELD_LIKED_BY = "likedBy"
    const val FIELD_SAVED_BY = "savedBy"
    const val FIELD_SAVE_COUNT = "saveCount"
    const val FIELD_COMMENT_COUNT = "commentCount"
    const val FIELD_TEXT_CONTENT = "textContent"

    // Fields - Reports
    const val FIELD_STATUS = "status"
    const val FIELD_POST_ID = "postId"
    const val FIELD_REVIEWED_BY = "reviewedBy"
    const val FIELD_REVIEWED_AT = "reviewedAt"
    const val FIELD_ADMIN_ACTION = "adminAction"
    const val FIELD_ADMIN_NOTES = "adminNotes"

    // Values
    const val STATUS_PENDING = "pending"
    const val STATUS_DISMISSED = "dismissed"
    const val STATUS_REVIEWED = "reviewed"
}
