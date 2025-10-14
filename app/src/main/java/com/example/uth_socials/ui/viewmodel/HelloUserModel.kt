package com.example.uth_socials.ui.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class HelloUiState(
    val pagerIndex: Int = 0
)
class HelloUserModel: ViewModel(){
    private val _uiState = MutableStateFlow(HelloUiState())
    val uiState: StateFlow<HelloUiState> = _uiState.asStateFlow()

    fun onStartClicked(){
        print("Nút Bắt đầu được nhắn!")
    }
    fun updatePagerIndex(newIndex: Int) {
        _uiState.update { currentState ->
            currentState.copy(pagerIndex = newIndex)
        }
    }

}