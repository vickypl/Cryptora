package com.yourapp.vault.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.yourapp.vault.domain.model.CredentialCategory
import com.yourapp.vault.ui.CredentialFormState

@Composable
fun CategoryFields(
    category: CredentialCategory,
    state: CredentialFormState,
    onStateChange: (CredentialFormState) -> Unit,
    onCopy: (String, String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        when (category) {
            CredentialCategory.EMAIL -> {
                CopyableField("Description", state.description, onValueChange = { onStateChange(state.copy(description = it)) }, onCopy = { onCopy("Description", it) })
                CopyableField("Login", state.emailLogin, onValueChange = { onStateChange(state.copy(emailLogin = it)) }, onCopy = { onCopy("Login", it) })
                CopyableField("Password", state.emailPassword, isPassword = true, onValueChange = { onStateChange(state.copy(emailPassword = it)) }, onCopy = { onCopy("Password", it) })
                CopyableField("Notes", state.notes, onValueChange = { onStateChange(state.copy(notes = it)) }, onCopy = { onCopy("Notes", it) })
            }

            CredentialCategory.BANK -> {
                CopyableField("Description", state.description, onValueChange = { onStateChange(state.copy(description = it)) }, onCopy = { onCopy("Description", it) })
                CopyableField("Customer ID", state.bankCustomerId, onValueChange = { onStateChange(state.copy(bankCustomerId = it)) }, onCopy = { onCopy("Customer ID", it) })
                CopyableField("Account No", state.bankAccountNo, onValueChange = { onStateChange(state.copy(bankAccountNo = it.filter(Char::isDigit))) }, onCopy = { onCopy("Account No", it) })
                CopyableField("IFSC Code", state.bankIfscCode, onValueChange = { onStateChange(state.copy(bankIfscCode = it)) }, onCopy = { onCopy("IFSC Code", it) })
                CopyableField("Net Banking Login", state.bankNetLogin, onValueChange = { onStateChange(state.copy(bankNetLogin = it)) }, onCopy = { onCopy("Net Banking Login", it) })
                CopyableField("Net Banking Password", state.bankNetPassword, isPassword = true, onValueChange = { onStateChange(state.copy(bankNetPassword = it)) }, onCopy = { onCopy("Net Banking Password", it) })
                CopyableField("App Login", state.bankAppLogin, onValueChange = { onStateChange(state.copy(bankAppLogin = it)) }, onCopy = { onCopy("App Login", it) })
                CopyableField("App Password", state.bankAppPassword, isPassword = true, onValueChange = { onStateChange(state.copy(bankAppPassword = it)) }, onCopy = { onCopy("App Password", it) })
                CopyableField("Notes", state.notes, onValueChange = { onStateChange(state.copy(notes = it)) }, onCopy = { onCopy("Notes", it) })
            }

            CredentialCategory.CARD -> {
                val digitsOnly = state.cardNumber.filter(Char::isDigit)
                CopyableField("Description", state.description, onValueChange = { onStateChange(state.copy(description = it)) }, onCopy = { onCopy("Description", it) })
                CopyableField(
                    label = "Card Number (without hyphens)",
                    value = digitsOnly,
                    onValueChange = { raw -> onStateChange(state.copy(cardNumber = raw.filter(Char::isDigit).take(16))) },
                    onCopy = { onCopy("Card Number (without hyphens)", it) }
                )
                CopyableField(
                    label = "Card Number (with hyphens)",
                    value = digitsOnly.chunked(4).joinToString("-"),
                    onValueChange = null,
                    onCopy = { onCopy("Card Number (with hyphens)", it) }
                )
                CopyableField("CVV", state.cardCvv, isPassword = true, onValueChange = { onStateChange(state.copy(cardCvv = it.take(4))) }, onCopy = { onCopy("CVV", it) })
                CopyableField("Expiry Date (MM/YY)", state.cardExpiry, onValueChange = { onStateChange(state.copy(cardExpiry = it)) }, onCopy = { onCopy("Expiry Date", it) })
                CopyableField("Notes", state.notes, onValueChange = { onStateChange(state.copy(notes = it)) }, onCopy = { onCopy("Notes", it) })
            }

            CredentialCategory.IDENTITY -> {
                val rawIdentity = state.identityId.replace("-", "")
                CopyableField("Description", state.description, onValueChange = { onStateChange(state.copy(description = it)) }, onCopy = { onCopy("Description", it) })
                CopyableField(
                    label = "ID (without hyphens)",
                    value = rawIdentity,
                    onValueChange = { raw -> onStateChange(state.copy(identityId = raw.replace("-", ""))) },
                    onCopy = { onCopy("ID (without hyphens)", it) }
                )
                CopyableField(
                    label = "ID (with hyphens)",
                    value = rawIdentity.chunked(4).joinToString("-"),
                    onValueChange = null,
                    onCopy = { onCopy("ID (with hyphens)", it) }
                )
                CopyableField("Notes", state.notes, onValueChange = { onStateChange(state.copy(notes = it)) }, onCopy = { onCopy("Notes", it) })
            }

            CredentialCategory.OTHER -> {
                CopyableField("Description", state.description, onValueChange = { onStateChange(state.copy(description = it)) }, onCopy = { onCopy("Description", it) })
                CopyableField("Title", state.title, onValueChange = { onStateChange(state.copy(title = it)) }, onCopy = { onCopy("Title", it) })
                CopyableField("Username", state.username, onValueChange = { onStateChange(state.copy(username = it)) }, onCopy = { onCopy("Username", it) })
                CopyableField("Password", state.password, isPassword = true, onValueChange = { onStateChange(state.copy(password = it)) }, onCopy = { onCopy("Password", it) })
                CopyableField("URL", state.url, onValueChange = { onStateChange(state.copy(url = it)) }, onCopy = { onCopy("URL", it) })
                CopyableField("Notes", state.notes, onValueChange = { onStateChange(state.copy(notes = it)) }, onCopy = { onCopy("Notes", it) })
            }
        }
    }
}
