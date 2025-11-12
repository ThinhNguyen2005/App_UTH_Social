package com.example.uth_socials.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uth_socials.data.repository.UserRepository
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BanStatus(
    val isBanned: Boolean = false,
    val banReason: String? = null,
    val bannedAt: Timestamp? = null,
    val bannedBy: String? = null
)

/**
 * BanStatusViewModel - Quản lý trạng thái ban của user hiện tại
 * 
 * Chức năng:
 * - Theo dõi trạng thái ban real-time từ Firestore
 * - Cung cấp StateFlow để UI có thể observe
 * - Tích hợp với MainActivity listener
 */
class BanStatusViewModel : ViewModel() {
    private val userRepository = UserRepository()
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    private val _banStatus = MutableStateFlow<BanStatus>(BanStatus())
    val banStatus: StateFlow<BanStatus> = _banStatus.asStateFlow()
    
    private var listenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null
    
    init {
        setupBanStatusListener()
    }
    
    /**
     * Setup real-time listener để theo dõi thay đổi trạng thái ban
     */
    private fun setupBanStatusListener() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.d("BanStatusViewModel", "No current user, skipping listener setup")
            return
        }
        
        val userDocRef = db.collection("users").document(currentUser.uid)
        
        listenerRegistration = userDocRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("BanStatusViewModel", "Error listening to ban status", error)
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
            }
        }
        
        Log.d("BanStatusViewModel", "Ban status listener setup for user: ${currentUser.uid}")
    }
    
    /**
     * Load ban status từ Firestore (one-time)
     */
    fun loadBanStatus() {
        viewModelScope.launch {
            val currentUser = auth.currentUser ?: return@launch
            try {
                val user = userRepository.getUser(currentUser.uid)
                if (user != null) {
                    _banStatus.value = BanStatus(
                        isBanned = user.isBanned,
                        banReason = user.banReason,
                        bannedAt = user.bannedAt,
                        bannedBy = user.bannedBy
                    )
                }
            } catch (e: Exception) {
                Log.e("BanStatusViewModel", "Error loading ban status", e)
            }
        }
    }
    
    /**
     * Check xem user hiện tại có bị ban không
     */
    fun isUserBanned(): Boolean {
        return _banStatus.value.isBanned
    }
    
    override fun onCleared() {
        super.onCleared()
        listenerRegistration?.remove()
        listenerRegistration = null
        Log.d("BanStatusViewModel", "Ban status listener removed")
    }
}

