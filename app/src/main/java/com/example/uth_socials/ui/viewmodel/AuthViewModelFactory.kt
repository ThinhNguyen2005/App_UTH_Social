package com.example.uth_socials.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.uth_socials.data.repository.UserRepository
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.firebase.auth.FirebaseAuth

class AuthViewModelFactory(
    private val auth: FirebaseAuth,
    private val userRepository: UserRepository,
    private val googleClient: GoogleSignInClient
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(auth, userRepository, googleClient) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}