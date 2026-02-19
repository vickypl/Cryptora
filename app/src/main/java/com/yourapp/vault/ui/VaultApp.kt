package com.yourapp.vault.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
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
    onSetup: (master: String) -> Result<Unit>,
    onUnlock: (password: String?) -> String?,
    onBiometricUnlock: (onResult: (String?) -> Unit) -> Unit,
    onBiometricToggle: (Boolean) -> Unit,
    onRequireLock: () -> Unit,
    onUserActivity: () -> Unit,
    lockoutMs: Long,
    vaultViewModel: VaultViewModel?
) {
    AnimatedContent(
        targetState = Triple(setupDone, unlocked, vaultViewModel),
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "vault-navigation"
    ) { (isSetupDone, isUnlocked, vm) ->
        when {
            !isSetupDone -> SetupScreen(onSetup, onUserActivity)
            !isUnlocked -> UnlockScreen(
                onUnlock = onUnlock,
                onBiometricUnlock = onBiometricUnlock,
                lockoutMs = lockoutMs,
                onUserActivity = onUserActivity,
                biometricEnabled = biometricEnabled
            )
            else -> VaultHome(rooted, biometricEnabled, onBiometricToggle, onRequireLock, onUserActivity, vm)
        }
    }
}

@Composable
private fun SetupScreen(onSetup: (String) -> Result<Unit>, onUserActivity: () -> Unit) {
    var master by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var creatingVault by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .imePadding()
            .padding(horizontal = 24.dp)
    ) {
        ElevatedCard(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .widthIn(max = 460.dp),
            shape = RoundedCornerShape(28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Create Your Vault",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Use a strong master password to protect your offline vault.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = master,
                    onValueChange = {
                        onUserActivity()
                        master = it
                        if (error != null) error = null
                    },
                    label = { Text("Master Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !creatingVault
                )

                AnimatedVisibility(visible = error != null) {
                    Text(
                        text = error.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Button(
                    onClick = {
                        onUserActivity()
                        val validationError = validateSetupInput(master)
                        if (validationError != null) {
                            error = validationError
                            return@Button
                        }

                        creatingVault = true
                        val result = onSetup(master)
                        creatingVault = false
                        if (result.isFailure) {
                            error = result.exceptionOrNull()?.message ?: "Unable to create vault. Please try again."
                        }
                    },
                    enabled = !creatingVault,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    if (creatingVault) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            modifier = Modifier.padding(vertical = 4.dp).size(20.dp)
                        )
                    } else {
                        Text("Create Vault", modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
            }
        }
    }
}

private fun validateSetupInput(master: String): String? {
    if (master.length < 12) {
        return "Master password must be at least 12 characters."
    }
    if (master.none(Char::isUpperCase) || master.none(Char::isLowerCase) || master.none(Char::isDigit) || !master.any { !it.isLetterOrDigit() }) {
        return "Use uppercase, lowercase, number, and symbol in master password."
    }
    return null
}

@Composable
private fun UnlockScreen(
    onUnlock: (String?) -> String?,
    onBiometricUnlock: (onResult: (String?) -> Unit) -> Unit,
    lockoutMs: Long,
    onUserActivity: () -> Unit,
    biometricEnabled: Boolean
) {
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Unlock Vault")
        if (lockoutMs > 0) {
            Text("Too many failed attempts. Retry in ${lockoutMs / 1000}s")
        }
        OutlinedTextField(
            value = password,
            onValueChange = { onUserActivity(); password = it },
            label = { Text("Master Password") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true
        )
        Button(
            enabled = lockoutMs == 0L,
            onClick = {
                onUserActivity()
                error = onUnlock(password.ifBlank { null })
            }
        ) { Text("Unlock with Master Password") }

        if (biometricEnabled) {
            Button(
                enabled = lockoutMs == 0L,
                onClick = {
                    onUserActivity()
                    onBiometricUnlock { unlockError ->
                        error = unlockError
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Unlock with Biometrics") }
        }
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
    onUserActivity: () -> Unit,
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
        floatingActionButton = { FloatingActionButton(onClick = { onUserActivity(); adding = true }) { Text("+") } }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (rooted) Text("Warning: rooted device detected")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Biometric")
                Switch(checked = biometricEnabled, onCheckedChange = { onUserActivity(); onBiometricToggle(it) })
                TextButton(onClick = { onUserActivity(); onRequireLock() }) { Text("Lock") }
            }
            OutlinedTextField(
                value = query,
                onValueChange = { onUserActivity(); viewModel.setQuery(it) },
                label = { Text("Search Vault") },
                supportingText = { Text("Title, username/email, type, URL or notes") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            LazyColumn(contentPadding = PaddingValues(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(list, key = { it.id }) { item ->
                    Card(modifier = Modifier.fillMaxWidth().clickable { onUserActivity(); selected = item }) {
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
                onUserActivity()
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
                onUserActivity()
                viewModel.delete(item)
                selected = null
            },
            onCopyPassword = {
                onUserActivity()
                secureClipboard.copyPassword(item.password)
                scope.launch { snackbar.showSnackbar("Password copied and will clear in 15s") }
            },
            onCopyUsername = {
                onUserActivity()
                secureClipboard.copyUsernameOrEmail(item.username)
                scope.launch { snackbar.showSnackbar("Username/email copied and will clear in 15s") }
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

    val canSave = title.isNotBlank() && username.isNotBlank() && password.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add Credential",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        confirmButton = {
            Button(
                enabled = canSave,
                onClick = {
                    onSave(
                        Credential(
                            title = title.trim(),
                            username = username.trim(),
                            password = password,
                            category = category.trim().ifBlank { "General" },
                            url = url.trim().ifBlank { null },
                            notes = notes.trim().ifBlank { null }
                        )
                    )
                },
                shape = RoundedCornerShape(14.dp)
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Store login details securely in your vault.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username / Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )
                FilledTonalButton(
                    onClick = {
                        password = PasswordGenerator.generate(16, upper = true, lower = true, digits = true, symbols = true)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Generate Strong Password")
                }
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Type / Category") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("URL (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
                    shape = RoundedCornerShape(14.dp)
                )
            }
        }
    )
}

@Composable
private fun CredentialDetailDialog(
    credential: Credential,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onCopyPassword: () -> Unit,
    onCopyUsername: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { Button(onClick = onCopyPassword) { Text("Copy Password") } },
        dismissButton = {
            Row {
                TextButton(onClick = onCopyUsername) { Text("Copy Username/Email") }
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
