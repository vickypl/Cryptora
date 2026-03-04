package com.yourapp.vault.ui

import com.yourapp.vault.domain.model.Credential
import com.yourapp.vault.domain.model.CredentialCategory
import java.util.UUID

data class CredentialFormState(
    val category: CredentialCategory = CredentialCategory.OTHER,
    val description: String = "",
    val notes: String = "",
    val title: String = "",
    val username: String = "",
    val password: String = "",
    val url: String = "",
    val emailLogin: String = "",
    val emailPassword: String = "",
    val bankCustomerId: String = "",
    val bankAccountNo: String = "",
    val bankIfscCode: String = "",
    val bankNetLogin: String = "",
    val bankNetPassword: String = "",
    val bankAppLogin: String = "",
    val bankAppPassword: String = "",
    val cardNumber: String = "",
    val cardCvv: String = "",
    val cardExpiry: String = "",
    val identityId: String = ""
) {
    fun isValid(): Boolean = when (category) {
        CredentialCategory.EMAIL -> emailLogin.isNotBlank()
        CredentialCategory.BANK -> bankAccountNo.isNotBlank()
        CredentialCategory.CARD -> cardNumber.isNotBlank()
        CredentialCategory.IDENTITY -> identityId.isNotBlank()
        CredentialCategory.OTHER -> title.isNotBlank() && username.isNotBlank() && password.isNotBlank()
    }

    fun toCredential(initial: Credential?): Credential {
        val normalizedCard = cardNumber.filter(Char::isDigit)
        val normalizedIdentity = identityId.replace("-", "")
        return Credential(
            id = initial?.id ?: UUID.randomUUID().toString(),
            createdAt = initial?.createdAt ?: System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            category = category.name,
            description = description.trim(),
            notes = notes.trim().ifBlank { null },
            title = title.trim(),
            username = username.trim(),
            password = password,
            url = url.trim().ifBlank { null },
            emailLogin = emailLogin.trim().ifBlank { null },
            emailPassword = emailPassword.ifBlank { null },
            bankCustomerId = bankCustomerId.trim().ifBlank { null },
            bankAccountNo = bankAccountNo.filter(Char::isDigit).ifBlank { null },
            bankIfscCode = bankIfscCode.trim().ifBlank { null },
            bankNetLogin = bankNetLogin.trim().ifBlank { null },
            bankNetPassword = bankNetPassword.ifBlank { null },
            bankAppLogin = bankAppLogin.trim().ifBlank { null },
            bankAppPassword = bankAppPassword.ifBlank { null },
            cardNumber = normalizedCard.ifBlank { null },
            cardCvv = cardCvv.trim().ifBlank { null },
            cardExpiry = cardExpiry.trim().ifBlank { null },
            identityId = normalizedIdentity.ifBlank { null }
        )
    }

    companion object {
        fun fromCredential(credential: Credential?): CredentialFormState {
            if (credential == null) return CredentialFormState()
            return CredentialFormState(
                category = CredentialCategory.fromStored(credential.category),
                description = credential.description,
                notes = credential.notes.orEmpty(),
                title = credential.title,
                username = credential.username,
                password = credential.password,
                url = credential.url.orEmpty(),
                emailLogin = credential.emailLogin.orEmpty(),
                emailPassword = credential.emailPassword.orEmpty(),
                bankCustomerId = credential.bankCustomerId.orEmpty(),
                bankAccountNo = credential.bankAccountNo.orEmpty(),
                bankIfscCode = credential.bankIfscCode.orEmpty(),
                bankNetLogin = credential.bankNetLogin.orEmpty(),
                bankNetPassword = credential.bankNetPassword.orEmpty(),
                bankAppLogin = credential.bankAppLogin.orEmpty(),
                bankAppPassword = credential.bankAppPassword.orEmpty(),
                cardNumber = credential.cardNumber.orEmpty(),
                cardCvv = credential.cardCvv.orEmpty(),
                cardExpiry = credential.cardExpiry.orEmpty(),
                identityId = credential.identityId.orEmpty()
            )
        }
    }
}
