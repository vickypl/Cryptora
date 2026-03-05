package com.yourapp.vault.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.yourapp.vault.data.local.toDomain
import com.yourapp.vault.data.repository.VaultRepository
import com.yourapp.vault.domain.model.Credential
import com.yourapp.vault.security.VaultBackupManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.paging.cachedIn
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.net.Uri

class VaultViewModel(
    private val repository: VaultRepository,
    private val backupManager: VaultBackupManager? = null,
    private val backupDirectoryProvider: (() -> android.net.Uri?)? = null,
    private val masterPasswordProvider: (() -> CharArray?)? = null
) : ViewModel() {
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    val credentials: Flow<PagingData<Credential>> = _query
        .debounce(300L)
        .distinctUntilChanged()
        .flatMapLatest { query ->
            Pager(
                config = PagingConfig(
                    pageSize = 30,
                    prefetchDistance = 10,
                    enablePlaceholders = false
                ),
                pagingSourceFactory = {
                    if (query.isBlank()) repository.getCredentialsPaged()
                    else repository.searchCredentialsPaged("%${query.trim()}%")
                }
            ).flow
        }
        .map { pagingData -> pagingData.map { it.toDomain() } }
        .cachedIn(viewModelScope)

    val credentialCount: StateFlow<Int> = repository.observeCredentialCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    private var activeSyncPassword: CharArray? = null

    fun setQuery(value: String) {
        _query.value = value
    }


    fun registerBackupTarget(directoryUri: android.net.Uri, masterPassword: String) {
        val activeUri = backupDirectoryProvider?.invoke() ?: return
        if (activeUri != directoryUri) return
        activeSyncPassword?.fill('\u0000')
        activeSyncPassword = masterPassword.toCharArray()
        syncBackupWithPassword(masterPassword.toCharArray())
    }

    suspend fun importBackup(
        backupUri: Uri,
        importPassword: String,
        currentMasterPassword: String,
        onBackupTargetActivated: (Uri) -> Unit
    ): Result<Int> {
        val manager = backupManager ?: return Result.failure(IllegalStateException("Backup manager unavailable"))
        return runCatching {
            val importPasswordChars = importPassword.toCharArray()
            val restored = try {
                manager.restoreVault(backupUri, importPasswordChars).getOrElse { throw it }
            } finally {
                importPasswordChars.fill('\u0000')
            }

            repository.upsertAll(restored)

            val syncPassword = currentMasterPassword.toCharArray()
            try {
                manager.writeVault(backupUri, repository.listAllCredentials(), syncPassword)
                    .getOrElse { throw it }
            } finally {
                syncPassword.fill('\u0000')
            }

            onBackupTargetActivated(backupUri)
            activeSyncPassword?.fill('\u0000')
            activeSyncPassword = currentMasterPassword.toCharArray()

            restored.size
        }
    }

    fun save(credential: Credential) {
        viewModelScope.launch {
            repository.upsert(credential.copy(updatedAt = System.currentTimeMillis()))
            syncBackup()
        }
    }

    fun delete(credential: Credential) {
        viewModelScope.launch {
            repository.delete(credential)
            syncBackup()
        }
    }


    suspend fun reEncryptBackupWithNewMasterPassword(newPassword: String): Result<Unit> {
        val manager = backupManager ?: return Result.failure(IllegalStateException("Backup manager unavailable"))
        val directory = backupDirectoryProvider?.invoke()
            ?: return Result.failure(IllegalStateException("Backup directory is not configured"))
        return runCatching {
            val snapshot = repository.listAllCredentials()
            val passwordChars = newPassword.toCharArray()
            try {
                withContext(Dispatchers.IO) {
                    manager.writeVault(directory, snapshot, passwordChars)
                        .getOrElse { throw it }
                }
                activeSyncPassword?.fill('\u0000')
                activeSyncPassword = newPassword.toCharArray()
            } finally {
                passwordChars.fill('\u0000')
            }
        }
    }


    private fun syncBackupWithPassword(password: CharArray) {
        val manager = backupManager ?: return
        val directory = backupDirectoryProvider?.invoke() ?: run {
            password.fill('\u0000')
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val snapshot = repository.listAllCredentials()
                manager.writeVault(directory, snapshot, password)
                    .getOrElse { throw it }
                Log.d(TAG, "syncBackup success")
            } catch (e: Exception) {
                Log.e(TAG, "syncBackup failed", e)
            } finally {
                password.fill('\u0000')
            }
        }
    }

    private fun syncBackup() {
        val password = activeSyncPassword?.copyOf() ?: masterPasswordProvider?.invoke() ?: run {
            Log.w(TAG, "syncBackup skipped: no master password in active session")
            return
        }
        syncBackupWithPassword(password)
    }

    override fun onCleared() {
        super.onCleared()
        activeSyncPassword?.fill('\u0000')
        activeSyncPassword = null
    }

    companion object {
        private const val TAG = "VaultViewModel"
    }

}
