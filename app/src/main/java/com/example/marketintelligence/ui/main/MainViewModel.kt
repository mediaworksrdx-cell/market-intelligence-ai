package com.example.marketintelligence.ui.main

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor() : ViewModel() {
    private val _unreadNotificationCount = MutableStateFlow(3)
    val unreadNotificationCount = _unreadNotificationCount.asStateFlow()
}
