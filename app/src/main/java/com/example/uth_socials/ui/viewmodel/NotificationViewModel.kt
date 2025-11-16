package com.example.uth_socials.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uth_socials.data.notification.Notification
import com.example.uth_socials.data.post.Category
import com.example.uth_socials.data.repository.NotificationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NotificationViewModel : ViewModel(){

    private val notificationRepository : NotificationRepository = NotificationRepository()

    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    private val _isNotReadNotifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications: StateFlow<List<Notification>> = _notifications.asStateFlow()
    val isNotReadNotifications: StateFlow<List<Notification>> = _isNotReadNotifications.asStateFlow()

    init {
        loadNotifications()
        loadNotReadNotifications()
    }

    private fun loadNotifications(){
        viewModelScope.launch {
            notificationRepository.listenNotificationsChanged().collect { list ->
                _notifications.value = list
                _isNotReadNotifications.value = notificationRepository.getNotReadNotification()
            }
        }
    }

    private fun loadNotReadNotifications() {
        viewModelScope.launch {
            _isNotReadNotifications.value = notificationRepository.getNotReadNotification()
        }
    }


    fun deleteNotification(id: String) {
        viewModelScope.launch {
            notificationRepository.deleteNotification(id)
            //_notifications.value = notificationRepository.getNotifications()
        }
    }

    fun markAsRead(notification: Notification) {
        viewModelScope.launch {
            notificationRepository.markAsRead(notification.id)

            _isNotReadNotifications.value = _isNotReadNotifications.value.map {
                if (it.id == notification.id) it.copy(isRead = true) else it
            }

            _isNotReadNotifications.value = notificationRepository.getNotReadNotification()
        }
    }

    fun removeLocal(id: String) {
        _notifications.value = _notifications.value.filterNot { it.id == id }
    }

    /** Khôi phục lại item nếu người dùng chọn "Hoàn tác" */
    fun restoreNotification(notification: Notification) {
        _notifications.value = listOf(notification) + _notifications.value
    }

}