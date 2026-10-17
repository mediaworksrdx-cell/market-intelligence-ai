package com.example.marketintelligence.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.marketintelligence.domain.repository.NotificationRepository
import com.example.marketintelligence.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
    notificationRepository: NotificationRepository
) : ViewModel() {
    val unreadNotificationCount: StateFlow<Int> = notificationRepository.unreadCount

    val isDarkMode: StateFlow<Boolean> = settingsRepository.isDarkMode
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    private val _pendingRoute = MutableStateFlow<String?>(null)
    val pendingRoute: StateFlow<String?> = _pendingRoute.asStateFlow()

    fun navigateTo(route: String?) {
        _pendingRoute.value = route
    }
}
