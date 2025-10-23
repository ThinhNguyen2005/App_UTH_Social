package com.example.uth_socials.ui.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * ViewModel dùng cho Preview hoặc test UI mà không cần Firebase
 */
class MockProfileViewModel(
    isOwner: Boolean = true,
    userId: String = "mock_user_123"
) : ViewModel() {

    private val _uiState = MutableStateFlow(createMockUiState(isOwner, userId))
    val uiState: StateFlow<ProfileUiState> = _uiState
}
