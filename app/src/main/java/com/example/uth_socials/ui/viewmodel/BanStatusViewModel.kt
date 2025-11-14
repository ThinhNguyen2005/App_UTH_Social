package com.example.uth_socials.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class BanStatus(
    val isBanned: Boolean = false,
    val banReason: String? = null,
    val bannedAt: Timestamp? = null,
    val bannedBy: String? = null
)

class BanStatusViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    private val _banStatus = MutableStateFlow<BanStatus>(BanStatus())
    val banStatus: StateFlow<BanStatus> = _banStatus.asStateFlow()
    
    private var listenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null
    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        if (firebaseAuth.currentUser != null) {
            Log.d("BanStatusViewModel", "Auth state changed: User is IN. Setting up listener.")
            setupBanStatusListener(firebaseAuth.currentUser!!.uid)
        } else {
            listenerRegistration?.remove()
            listenerRegistration = null
            _banStatus.value = BanStatus()
            Log.d("BanStatusViewModel", "Auth state changed: User is OUT. Listener removed.")
        }
    }
    
    init {
        auth.addAuthStateListener(authStateListener)
        val currentUser = auth.currentUser
        if (currentUser != null) {
            Log.d("BanStatusViewModel", "Init: User already logged in. Setting up listener.")
            setupBanStatusListener(currentUser.uid)
        } else {
            Log.d("BanStatusViewModel", "Init: No user logged in.")
        }
    }

    private fun setupBanStatusListener(userId: String) {
        listenerRegistration?.remove()

        val userDocRef = db.collection("users").document(userId)

        listenerRegistration = userDocRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("BanStatusViewModel", "Error listening to ban status", error)
                if (error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                    _banStatus.value = BanStatus()
                }
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                val isBanned = snapshot.getBoolean("isBanned") ?: false
                val banReason = snapshot.getString("banReason")
                val bannedAt = snapshot.getTimestamp("bannedAt")
                val bannedBy = snapshot.getString("bannedBy")

                _banStatus.value = BanStatus(
                    isBanned = isBanned,
                    banReason = banReason,
                    bannedAt = bannedAt,
                    bannedBy = bannedBy
                )

                Log.d("BanStatusViewModel", "Ban status updated: isBanned=$isBanned, reason=$banReason")
            } else {
                Log.w("BanStatusViewModel", "User document doesn't exist for $userId")
                _banStatus.value = BanStatus()
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        listenerRegistration?.remove()
        listenerRegistration = null
        Log.d("BanStatusViewModel", "Ban status listener removed")
    }
}

