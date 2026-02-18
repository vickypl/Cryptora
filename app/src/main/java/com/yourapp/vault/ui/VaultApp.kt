package com.yourapp.vault.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.yourapp.vault.domain.model.Credential
import com.yourapp.vault.security.PasswordGenerator
import com.yourapp.vault.util.SecureClipboard
import com.yourapp.vault.viewmodel.VaultViewModel
import kotlinx.coroutines.launch

@Composable
fun VaultApp(
    setupDone: Boolean,
    rooted: Boolean,
    unlocked: Boolean,
    biometricEnabled: Boolean,
    onSetup: (master: String, pin: String?) -> Unit,
    onUnlock: (password: String?, pin: String?) -> Boolean,
    onBiometricToggle: (Boolean) -> Unit,
    onRequireLock: () -> Unit,
    lockoutMs: Long,
    vaultViewModel: VaultViewModel?
) {
    when {
        !setupDone -> SetupScreen(onSetup)
        !unlocked -> UnlockScreen(onUnlock = onUnlock, lockoutMs = lockoutMs)
        else -> VaultHome(rooted, biometricEnabled, onBiometricToggle, onRequireLock, vaultViewModel)
    }
}

@Composable
private fun SetupScreen(onSetup: (String, String?) -> Unit) {
    var master by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Initial Setup")
        OutlinedTextField(value = master, onValueChange = { master = it }, label = { Text("Master Password") })
        OutlinedTextField(value = pin, onValueChange = { pin = it }, label = { Text("Optional PIN") })
        Button(onClick = { if (master.length >= 8) onSetup(master, pin.ifBlank { null }) }) { Text("Create Vault") }
    }
}

@Composable
private fun UnlockScreen(onUnlock: (String?, String?) -> Boolean, lockoutMs: Long) {
    var password by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Unlock Vault")
        if (lockoutMs > 0) {
            Text("Too many failed attempts. Retry in ${lockoutMs / 1000}s")
        }
        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Master Password") })
        OutlinedTextField(value = pin, onValueChange = { pin = it }, label = { Text("PIN") })
        Button(
            enabled = lockoutMs == 0L,
            onClick = {
                val success = onUnlock(password.ifBlank { null }, pin.ifBlank { null })
                if (!success) error = "Authentication failed"
            }
        ) { Text("Unlock") }
        error?.let { Text(it) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VaultHome(
    rooted: Boolean,
    biometricEnabled: Boolean,
    onBiometricToggle: (Boolean) -> Unit,
    onRequireLock: () -> Unit,
    viewModel: VaultViewModel?
) {
    if (viewModel == null) {
        Text("Vault unavailable")
        return
    }

    val list by viewModel.filtered.collectAsStateWithLifecycleCompat()
    val query by viewModel.query.collectAsStateWithLifecycleCompat()
    var adding by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Credential?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val secureClipboard = remember(context) { SecureClipboard(context) }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbar) },
        floatingActionButton = { FloatingActionButton(onClick = { adding = true }) { Text("+") } }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (rooted) Text("Warning: rooted device detected")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Biometric")
                Switch(checked = biometricEnabled, onCheckedChange = onBiometricToggle)
                TextButton(onClick = onRequireLock) { Text("Lock") }
            }
            OutlinedTextField(value = query, onValueChange = viewModel::setQuery, label = { Text("Search") }, modifier = Modifier.fillMaxWidth())
            LazyColumn(contentPadding = PaddingValues(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(list, key = { it.id }) { item ->
                    Card(modifier = Modifier.fillMaxWidth().clickable { selected = item }) {
                        Column(Modifier.padding(12.dp)) {
                            Text(item.title)
                            Text(item.username)
                            Text(item.category)
                        }
                    }
                }
            }
        }
    }

    if (adding) {
        AddCredentialDialog(
            onDismiss = { adding = false },
            onSave = {
                viewModel.save(it)
                adding = false
            }
        )
    }

    selected?.let { item ->
        CredentialDetailDialog(
            credential = item,
            onDismiss = { selected = null },
            onDelete = {
                viewModel.delete(item)
                selected = null
            },
            onCopyPassword = {
                secureClipboard.copyPassword(item.password)
                scope.launch { snackbar.showSnackbar("Password copied and will clear in 15s") }
            }
        )
    }
}

@Composable
private fun AddCredentialDialog(onDismiss: () -> Unit, onSave: (Credential) -> Unit) {
    var title by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("General") }
    var url by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = {
                onSave(
                    Credential(
                        title = title,
                        username = username,
                        password = password,
                        category = category,
                        url = url.ifBlank { null },
                        notes = notes.ifBlank { null }
                    )
                )
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("Title") })
                OutlinedTextField(username, { username = it }, label = { Text("Username") })
                OutlinedTextField(password, { password = it }, label = { Text("Password") })
                TextButton(onClick = {
                    password = PasswordGenerator.generate(16, upper = true, lower = true, digits = true, symbols = true)
                }) { Text("Generate Password") }
                OutlinedTextField(category, { category = it }, label = { Text("Category") })
                OutlinedTextField(url, { url = it }, label = { Text("URL") })
                OutlinedTextField(notes, { notes = it }, label = { Text("Notes") })
            }
        }
    )
}

@Composable
private fun CredentialDetailDialog(
    credential: Credential,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onCopyPassword: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { Button(onClick = onCopyPassword) { Text("Copy Password") } },
        dismissButton = {
            Row {
                TextButton(onClick = onDelete) { Text("Delete") }
                TextButton(onClick = onDismiss) { Text("Close") }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Title: ${credential.title}")
                Text("Username: ${credential.username}")
                Text("Password: ••••••••")
                credential.url?.let { Text("URL: $it") }
                credential.notes?.let { Text("Notes: $it") }
                Text("Category: ${credential.category}")
            }
        }
    )
}
