package com.yourapp.vault.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yourapp.vault.R
import com.yourapp.vault.domain.model.Credential
import com.yourapp.vault.security.PasswordGenerator
import com.yourapp.vault.util.SecureClipboard
import com.yourapp.vault.viewmodel.VaultViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

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
    selectedTheme: String,
    onThemeChange: (String) -> Unit,
    selectedSessionLimit: String,
    onSessionLimitChange: (String) -> Unit,
    onChangeMasterPassword: (current: String, next: String) -> String?,
    onRequireLock: () -> Unit,
    onUserActivity: () -> Unit,
    lockoutMs: Long,
    sessionRemainingMs: Long,
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
            else -> VaultHome(
                rooted = rooted,
                biometricEnabled = biometricEnabled,
                selectedTheme = selectedTheme,
                onThemeChange = onThemeChange,
                selectedSessionLimit = selectedSessionLimit,
                onSessionLimitChange = onSessionLimitChange,
                onChangeMasterPassword = onChangeMasterPassword,
                onBiometricToggle = onBiometricToggle,
                onRequireLock = onRequireLock,
                onUserActivity = onUserActivity,
                sessionRemainingMs = sessionRemainingMs,
                viewModel = vm
            )
        }
    }
}

@Composable
private fun SetupScreen(onSetup: (String) -> Result<Unit>, onUserActivity: () -> Unit) {
    var master by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var creatingVault by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

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
                        scope.launch {
                            val result = withContext(Dispatchers.Default) { onSetup(master) }
                            creatingVault = false
                            if (result.isFailure) {
                                error = result.exceptionOrNull()?.message
                                    ?: "Unable to create vault. Please verify device lock/biometric and try again."
                            }
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

private val MASTER_PASSWORD_REGEX = Regex("^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{12,}$")

private fun validateSetupInput(master: String): String? {
    if (!MASTER_PASSWORD_REGEX.matches(master)) {
        return "Use a password like Example@1234"
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
    var attemptedBiometric by remember { mutableStateOf(false) }

    LaunchedEffect(biometricEnabled, lockoutMs) {
        if (biometricEnabled && lockoutMs == 0L && !attemptedBiometric) {
            attemptedBiometric = true
            onBiometricUnlock { unlockError ->
                error = unlockError
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .imePadding()
            .padding(horizontal = 24.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.Center)
                .size(280.dp)
                .alpha(0.08f)
        )

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
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Secure Vault Login",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Authenticate to access your encrypted credentials.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                if (lockoutMs > 0) {
                    Text(
                        text = "Too many failed attempts. Retry in ${lockoutMs / 1000}s",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }

                OutlinedTextField(
                    value = password,
                    onValueChange = { onUserActivity(); password = it },
                    label = { Text("Master Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    enabled = lockoutMs == 0L,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )

                if (biometricEnabled) {
                    FilledTonalButton(
                        enabled = lockoutMs == 0L,
                        onClick = {
                            onUserActivity()
                            onBiometricUnlock { unlockError ->
                                error = unlockError
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) { Text("Unlock with Biometrics") }
                }

                Button(
                    enabled = lockoutMs == 0L,
                    onClick = {
                        onUserActivity()
                        error = onUnlock(password.ifBlank { null })
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) { Text("Unlock") }

                error?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VaultHome(
    rooted: Boolean,
    biometricEnabled: Boolean,
    onBiometricToggle: (Boolean) -> Unit,
    selectedTheme: String,
    onThemeChange: (String) -> Unit,
    selectedSessionLimit: String,
    onSessionLimitChange: (String) -> Unit,
    onChangeMasterPassword: (current: String, next: String) -> String?,
    onRequireLock: () -> Unit,
    onUserActivity: () -> Unit,
    sessionRemainingMs: Long,
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
    var editing by remember { mutableStateOf<Credential?>(null) }
    var settingsOpen by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val secureClipboard = remember(context) { SecureClipboard(context) }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbar) },
        floatingActionButton = { FloatingActionButton(onClick = { onUserActivity(); adding = true }) { Text("+") } }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Vault", style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = if (sessionRemainingMs < 0) "Auto-lock: Not required" else "Auto-lock in ${formatRemainingTime(sessionRemainingMs)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (rooted) {
                        Text("Warning: rooted device detected", color = MaterialTheme.colorScheme.error)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { onUserActivity(); onRequireLock() }) { Text("Lock") }
                    IconButton(onClick = { onUserActivity(); settingsOpen = true }) { Text("⚙") }
                }
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

    editing?.let { item ->
        AddCredentialDialog(
            initialCredential = item,
            onDismiss = { editing = null },
            onSave = {
                onUserActivity()
                viewModel.save(it)
                editing = null
            }
        )
    }

    if (settingsOpen) {
        SettingsDialog(
            biometricEnabled = biometricEnabled,
            selectedTheme = selectedTheme,
            onBiometricToggle = {
                onUserActivity()
                onBiometricToggle(it)
            },
            onThemeSelected = {
                onUserActivity()
                onThemeChange(it)
            },
            selectedSessionLimit = selectedSessionLimit,
            onSessionLimitSelected = {
                onUserActivity()
                onSessionLimitChange(it)
            },
            onChangeMasterPassword = { current, next ->
                onUserActivity()
                onChangeMasterPassword(current, next)
            },
            onDismiss = { settingsOpen = false }
        )
    }


    selected?.let { item ->
        CredentialDetailDialog(
            credential = item,
            onDismiss = { selected = null },
            onEdit = {
                onUserActivity()
                editing = item
                selected = null
            },
            onDelete = {
                onUserActivity()
                viewModel.delete(item)
                selected = null
            },
            onCopyPassword = {
                onUserActivity()
                secureClipboard.copyPassword(item.password)
                scope.launch { snackbar.showSnackbar("Password copied and will clear in 60s") }
            },
            onCopyUsername = {
                onUserActivity()
                secureClipboard.copyUsernameOrEmail(item.username)
                scope.launch { snackbar.showSnackbar("Username/email copied and will clear in 60s") }
            }
        )
    }
}

@Composable
private fun AddCredentialDialog(
    initialCredential: Credential? = null,
    onDismiss: () -> Unit,
    onSave: (Credential) -> Unit
) {
    var title by remember { mutableStateOf(initialCredential?.title.orEmpty()) }
    var username by remember { mutableStateOf(initialCredential?.username.orEmpty()) }
    var password by remember { mutableStateOf(initialCredential?.password.orEmpty()) }
    var category by remember { mutableStateOf(initialCredential?.category ?: "General") }
    var url by remember { mutableStateOf(initialCredential?.url.orEmpty()) }
    var notes by remember { mutableStateOf(initialCredential?.notes.orEmpty()) }

    val canSave = title.isNotBlank() && username.isNotBlank() && password.isNotBlank()
    val isEditing = initialCredential != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEditing) "Edit Credential" else "Add Credential",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        confirmButton = {
            Button(
                enabled = canSave,
                onClick = {
                    onSave(
                        Credential(
                            id = initialCredential?.id ?: UUID.randomUUID().toString(),
                            createdAt = initialCredential?.createdAt ?: System.currentTimeMillis(),
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
            ) { Text(if (isEditing) "Update" else "Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (isEditing) "Update your login details." else "Store login details securely in your vault.",
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
                    label = { Text("Login / Email") },
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
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onCopyPassword: () -> Unit,
    onCopyUsername: () -> Unit
) {
    var showPassword by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = credential.title,
                style = MaterialTheme.typography.headlineSmall
            )
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { confirmDelete = true },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onEdit) { Text("Edit") }
                    Button(onClick = onDismiss, shape = RoundedCornerShape(12.dp)) { Text("Close") }
                }
            }
        },
        dismissButton = {},
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DetailLineWithCopy(
                    label = "Login",
                    value = credential.username,
                    onCopy = onCopyUsername
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Password", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(if (showPassword) credential.password else "*******", style = MaterialTheme.typography.bodyLarge)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = { showPassword = !showPassword }) { Text(if (showPassword) "Hide" else "View") }
                        TextButton(onClick = onCopyPassword) { Text("Copy") }
                    }
                }

                credential.url?.takeIf { it.isNotBlank() }?.let {
                    Text("URL: $it", style = MaterialTheme.typography.bodyMedium)
                }
                credential.notes?.takeIf { it.isNotBlank() }?.let {
                    Text("Notes: $it", style = MaterialTheme.typography.bodyMedium)
                }
                Text("Type: ${credential.category}", style = MaterialTheme.typography.bodyMedium)
            }
        }
    )

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete Credential") },
            text = { Text("Are you sure you want to delete this record?") },
            confirmButton = {
                Button(onClick = {
                    confirmDelete = false
                    onDelete()
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsDialog(
    biometricEnabled: Boolean,
    selectedTheme: String,
    onBiometricToggle: (Boolean) -> Unit,
    onThemeSelected: (String) -> Unit,
    selectedSessionLimit: String,
    onSessionLimitSelected: (String) -> Unit,
    onChangeMasterPassword: (current: String, next: String) -> String?,
    onDismiss: () -> Unit
) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var sessionMenuExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settings", style = MaterialTheme.typography.headlineSmall) },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("Security", style = MaterialTheme.typography.titleMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Biometric Authentication")
                    Switch(checked = biometricEnabled, onCheckedChange = onBiometricToggle)
                }

                HorizontalDivider()

                Text("Theme", style = MaterialTheme.typography.titleMedium)
                appThemeOptions.forEach { option ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onThemeSelected(option.key) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selectedTheme == option.key, onClick = { onThemeSelected(option.key) })
                        Text(option.label)
                    }
                }

                HorizontalDivider()

                Text("Auto-lock Session", style = MaterialTheme.typography.titleMedium)
                ExposedDropdownMenuBox(
                    expanded = sessionMenuExpanded,
                    onExpandedChange = { sessionMenuExpanded = !sessionMenuExpanded }
                ) {
                    OutlinedTextField(
                        value = sessionLimitOptions.firstOrNull { it.key == selectedSessionLimit }?.label ?: "5m",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Session Timeout") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sessionMenuExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    DropdownMenu(
                        expanded = sessionMenuExpanded,
                        onDismissRequest = { sessionMenuExpanded = false }
                    ) {
                        sessionLimitOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.label) },
                                onClick = {
                                    onSessionLimitSelected(option.key)
                                    sessionMenuExpanded = false
                                }
                            )
                        }
                    }
                }


                HorizontalDivider()

                Text("Change Master Password", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = { currentPassword = it; passwordError = null },
                    label = { Text("Current Master Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it; passwordError = null },
                    label = { Text("New Master Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it; passwordError = null },
                    label = { Text("Confirm New Master Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Button(
                    onClick = {
                        passwordError = when {
                            currentPassword.isBlank() -> "Current master password is required"
                            newPassword != confirmPassword -> "New password and confirm password do not match"
                            else -> onChangeMasterPassword(currentPassword, newPassword)
                        }
                        if (passwordError == null) {
                            currentPassword = ""
                            newPassword = ""
                            confirmPassword = ""
                            passwordError = "Successfully updated"
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Update Master Password") }

                passwordError?.let {
                    Text(
                        text = it,
                        color = if (it.contains("success", ignoreCase = true)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    )
}

private data class ThemeOption(val key: String, val label: String)

private val appThemeOptions = listOf(
    ThemeOption("MIDNIGHT", "Midnight Blue"),
    ThemeOption("SLATE", "Slate Gray"),
    ThemeOption("GRAPHITE", "Graphite"),
    ThemeOption("FOREST", "Forest Night"),
    ThemeOption("INDIGO", "Indigo")
)

private data class SessionLimitOption(val key: String, val label: String)

private val sessionLimitOptions = listOf(
    SessionLimitOption("1m", "1m"),
    SessionLimitOption("2m", "2m"),
    SessionLimitOption("5m", "5m"),
    SessionLimitOption("10m", "10m"),
    SessionLimitOption("none", "Not required")
)

private fun formatRemainingTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}


@Composable
private fun DetailLineWithCopy(label: String, value: String, onCopy: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyLarge)
        }
        TextButton(onClick = onCopy) { Text("Copy") }
    }
}
