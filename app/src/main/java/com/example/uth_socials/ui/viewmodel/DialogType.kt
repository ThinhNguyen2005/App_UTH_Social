package com.example.uth_socials.ui.viewmodel


sealed class DialogType {
    data object None : DialogType()

    data class DeletePost(
        val postId: String
    ) : DialogType()

    data class BlockUser(
        val userId: String,
        val username: String = ""
    ) : DialogType()

    data class UnblockUser(
        val userId: String,
        val username: String = ""
    ) : DialogType()

}