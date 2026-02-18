package com.yourapp.vault.data.repository

import com.yourapp.vault.data.local.CredentialDao
import com.yourapp.vault.data.local.toDomain
import com.yourapp.vault.data.local.toEntity
import com.yourapp.vault.domain.model.Credential
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class VaultRepository(private val dao: CredentialDao) {
    fun observeCredentials(): Flow<List<Credential>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun upsert(credential: Credential) = dao.upsert(credential.toEntity())

    suspend fun getCredential(id: String): Credential? = dao.getById(id)?.toDomain()

    suspend fun delete(credential: Credential) = dao.delete(credential.toEntity())
}
