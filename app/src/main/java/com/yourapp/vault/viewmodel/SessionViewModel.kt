package com.yourapp.vault.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SessionViewModel : ViewModel() {
    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked = _isUnlocked.asStateFlow()

    private var lastActiveAt: Long = 0L

    fun unlock() {
        _isUnlocked.value = true
        markActive()
    }

    fun lock() {
        _isUnlocked.value = false
    }

    fun markActive() {
        lastActiveAt = System.currentTimeMillis()
    }

    fun lockIfInactive(timeoutMs: Long = 180_000L) {
        if (_isUnlocked.value && System.currentTimeMillis() - lastActiveAt > timeoutMs) {
            lock()
        }
    }
}
