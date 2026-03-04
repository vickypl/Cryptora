package com.yourapp.vault.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.yourapp.vault.domain.model.Credential

@Entity(
    tableName = "credentials",
    indices = [Index(value = ["title"]), Index(value = ["username"])]
)
data class CredentialEntity(
    @PrimaryKey val id: String,
    val title: String,
    val username: String,
    val password: String,
    val url: String?,
    val notes: String?,
    val category: String,
    val createdAt: Long,
    val updatedAt: Long
)

fun CredentialEntity.toDomain() = Credential(
    id = id,
    title = title,
    username = username,
    password = password,
    url = url,
    notes = notes,
    category = category,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun Credential.toEntity() = CredentialEntity(
    id = id,
    title = title,
    username = username,
    password = password,
    url = url,
    notes = notes,
    category = category,
    createdAt = createdAt,
    updatedAt = updatedAt
)
