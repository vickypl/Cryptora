package com.yourapp.vault.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SessionViewModel : ViewModel() {
    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked = _isUnlocked.asStateFlow()

    private var lastActiveAt: Long = 0L
    private var unlockedAt: Long = 0L
    private var masterPassword: CharArray? = null
    private var lockJob: Job? = null

    fun unlock() {
        _isUnlocked.value = true
        unlockedAt = System.currentTimeMillis()
        markActive()
    }

    fun unlockWithPassword(password: String) {
        setMasterPassword(password)
        unlock()
    }

    fun lock() {
        _isUnlocked.value = false
        unlockedAt = 0L
        lockJob?.cancel()
        masterPassword?.fill('\u0000')
        masterPassword = null
    }

    fun setMasterPassword(password: String) {
        masterPassword?.fill('\u0000')
        masterPassword = password.toCharArray()
    }

    fun updateMasterPassword(newPassword: String) {
        setMasterPassword(newPassword)
    }

    fun getMasterPassword(): CharArray? = masterPassword?.copyOf()

    fun markActive() {
        lastActiveAt = System.currentTimeMillis()
    }

    fun resetInactivityTimer(timeoutMs: Long = 180_000L) {
        if (!_isUnlocked.value) return
        markActive()
        lockJob?.cancel()
        lockJob = viewModelScope.launch(Dispatchers.Default) {
            delay(timeoutMs)
            if (_isUnlocked.value && System.currentTimeMillis() - lastActiveAt >= timeoutMs) {
                lock()
            }
        }
    }

    fun sessionRemainingMs(maxSessionMs: Long = 300_000L): Long {
        if (!_isUnlocked.value || unlockedAt == 0L) return maxSessionMs
        val elapsed = System.currentTimeMillis() - unlockedAt
        return (maxSessionMs - elapsed).coerceAtLeast(0)
    }

    fun lockIfSessionExpired(maxSessionMs: Long = 300_000L) {
        if (_isUnlocked.value && sessionRemainingMs(maxSessionMs) <= 0L) {
            lock()
        }
    }
}
