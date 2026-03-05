package com.yourapp.vault.domain.model

import java.util.UUID

data class Credential(
    val id: String = UUID.randomUUID().toString(),
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
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
