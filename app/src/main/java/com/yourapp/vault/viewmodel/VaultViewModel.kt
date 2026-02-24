package com.yourapp.vault.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourapp.vault.data.repository.VaultRepository
import com.yourapp.vault.domain.model.Credential
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class VaultViewModel(private val repository: VaultRepository) : ViewModel() {
    val credentials: StateFlow<List<Credential>> = repository.observeCredentials()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    val filtered: StateFlow<List<Credential>> = combine(credentials, _query) { list, query ->
        val q = query.trim().lowercase()
        if (q.isBlank()) list else list.filter {
            it.title.lowercase().contains(q) ||
                it.username.lowercase().contains(q) ||
                it.category.lowercase().contains(q) ||
                (it.url?.lowercase()?.contains(q) == true) ||
                (it.notes?.lowercase()?.contains(q) == true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setQuery(value: String) {
        _query.value = value
    }

    fun save(credential: Credential) {
        viewModelScope.launch { repository.upsert(credential.copy(updatedAt = System.currentTimeMillis())) }
    }

    fun delete(credential: Credential) {
        viewModelScope.launch { repository.delete(credential) }
    }
}
