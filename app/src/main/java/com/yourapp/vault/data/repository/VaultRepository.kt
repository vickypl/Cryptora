package com.yourapp.vault.data.repository

import androidx.paging.PagingSource
import com.yourapp.vault.data.local.CredentialDao
import com.yourapp.vault.data.local.CredentialEntity
import com.yourapp.vault.data.local.toDomain
import com.yourapp.vault.data.local.toEntity
import com.yourapp.vault.domain.model.Credential

class VaultRepository(private val dao: CredentialDao) {
    fun getCredentialsPaged(): PagingSource<Int, CredentialEntity> = dao.getCredentialsPaged()

    fun searchCredentialsPaged(query: String): PagingSource<Int, CredentialEntity> = dao.searchCredentialsPaged(query)

    suspend fun upsert(credential: Credential) = dao.upsert(credential.toEntity())

    suspend fun upsertAll(credentials: List<Credential>) = dao.upsertAll(credentials.map { it.toEntity() })

    suspend fun listAllCredentials(): List<Credential> = dao.listAll().map { it.toDomain() }

    suspend fun getCredential(id: String): Credential? = dao.getById(id)?.toDomain()

    suspend fun delete(credential: Credential) = dao.delete(credential.toEntity())
}
