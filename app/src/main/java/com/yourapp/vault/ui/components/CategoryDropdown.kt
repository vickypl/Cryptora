package com.yourapp.vault.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.yourapp.vault.domain.model.CredentialCategory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDropdown(
    selected: CredentialCategory,
    onCategorySelected: (CredentialCategory) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val labels = mapOf(
        CredentialCategory.EMAIL to "📧  Email",
        CredentialCategory.BANK to "🏦  Bank",
        CredentialCategory.CARD to "💳  Card",
        CredentialCategory.IDENTITY to "🪪  Identity",
        CredentialCategory.OTHER to "📁  Other"
    )

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = labels[selected] ?: "Other",
            onValueChange = {},
            readOnly = true,
            label = { Text("Category") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            CredentialCategory.entries.forEach { category ->
                DropdownMenuItem(
                    text = { Text(labels[category] ?: category.name) },
                    onClick = {
                        onCategorySelected(category)
                        expanded = false
                    }
                )
            }
        }
    }
}
