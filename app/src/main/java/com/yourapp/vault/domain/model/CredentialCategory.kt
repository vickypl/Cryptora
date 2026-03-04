package com.yourapp.vault.domain.model

enum class CredentialCategory {
    EMAIL,
    BANK,
    CARD,
    IDENTITY,
    OTHER;

    companion object {
        fun fromStored(value: String?): CredentialCategory =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: OTHER
    }
}
