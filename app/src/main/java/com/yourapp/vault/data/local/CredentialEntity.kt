package com.yourapp.vault.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.yourapp.vault.domain.model.Credential
import com.yourapp.vault.domain.model.CredentialCategory

@Entity(
    tableName = "credentials",
    indices = [Index(value = ["title"]), Index(value = ["username"])]
)
data class CredentialEntity(
    @PrimaryKey val id: String,
    val title: String = "",
    val username: String = "",
    val password: String = "",
    val url: String? = null,
    val notes: String? = null,
    val category: String = CredentialCategory.OTHER.name,
    val description: String = "",
    val emailLogin: String? = null,
    val emailPassword: String? = null,
    val bankCustomerId: String? = null,
    val bankAccountNo: String? = null,
    val bankIfscCode: String? = null,
    val bankNetLogin: String? = null,
    val bankNetPassword: String? = null,
    val bankAppLogin: String? = null,
    val bankAppPassword: String? = null,
    val cardNumber: String? = null,
    val cardCvv: String? = null,
    val cardExpiry: String? = null,
    val identityId: String? = null,
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
    description = description,
    emailLogin = emailLogin,
    emailPassword = emailPassword,
    bankCustomerId = bankCustomerId,
    bankAccountNo = bankAccountNo,
    bankIfscCode = bankIfscCode,
    bankNetLogin = bankNetLogin,
    bankNetPassword = bankNetPassword,
    bankAppLogin = bankAppLogin,
    bankAppPassword = bankAppPassword,
    cardNumber = cardNumber,
    cardCvv = cardCvv,
    cardExpiry = cardExpiry,
    identityId = identityId,
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
    description = description,
    emailLogin = emailLogin,
    emailPassword = emailPassword,
    bankCustomerId = bankCustomerId,
    bankAccountNo = bankAccountNo,
    bankIfscCode = bankIfscCode,
    bankNetLogin = bankNetLogin,
    bankNetPassword = bankNetPassword,
    bankAppLogin = bankAppLogin,
    bankAppPassword = bankAppPassword,
    cardNumber = cardNumber,
    cardCvv = cardCvv,
    cardExpiry = cardExpiry,
    identityId = identityId,
    createdAt = createdAt,
    updatedAt = updatedAt
)
