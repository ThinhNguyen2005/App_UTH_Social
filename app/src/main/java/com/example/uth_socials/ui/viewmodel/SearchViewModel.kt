package com.example.uth_socials.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uth_socials.data.post.Post
import com.example.uth_socials.data.user.User
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SearchViewModel : ViewModel() {
    private val _searchPostResults = MutableStateFlow<List<Post>>(emptyList())
    val searchPostResults: StateFlow<List<Post>> = _searchPostResults

    private val _searchUserResults = MutableStateFlow<List<User>>(emptyList())
    val searchUserResults: StateFlow<List<User>> = _searchUserResults

    fun searchPosts(query: String) {
        viewModelScope.launch {
            val formatQuery = query.trim().lowercase()
            val snapshot = Firebase.firestore.collection("posts")
                .whereGreaterThanOrEqualTo("textContentFormat", formatQuery)
                .whereLessThanOrEqualTo("textContentFormat", formatQuery + "\uf8ff")
                .get()
                .await()

            _searchPostResults.value = snapshot.documents.mapNotNull { it.toObject(Post::class.java) }
        }
    }

    fun searchUsers(query: String){
        viewModelScope.launch {
            val formatQuery = query.trim().lowercase()
            val snapshot = Firebase.firestore.collection("users")
                .whereGreaterThanOrEqualTo("usernameFormat", formatQuery)
                .whereLessThanOrEqualTo("usernameFormat", formatQuery + "\uf8ff")
                .get()
                .await()

            _searchUserResults.value = snapshot.documents.mapNotNull { it.toObject(User::class.java) }
        }
    }
}