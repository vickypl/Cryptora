package com.yourapp.vault.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.yourapp.vault.R
import com.yourapp.vault.domain.model.Credential
import com.yourapp.vault.domain.model.CredentialCategory
import com.yourapp.vault.ui.components.CategoryDropdown
import com.yourapp.vault.ui.components.CategoryFields
import com.yourapp.vault.ui.components.CryptoraCard
import com.yourapp.vault.ui.components.CryptoraTextField
import com.yourapp.vault.security.PasswordGenerator
import com.yourapp.vault.util.SecureClipboard
import com.yourapp.vault.viewmodel.VaultViewModel
import com.yourapp.vault.ui.theme.extraColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun VaultApp(
    setupDone: Boolean,
    rooted: Boolean,
    unlocked: Boolean,
    biometricEnabled: Boolean,
    onSetup: (master: String, vaultDirectory: Uri, restoreExisting: Boolean) -> Result<Unit>,
    onHasExistingVault: (vaultDirectory: Uri) -> Boolean,
    onUnlock: (password: String?) -> String?,
    onBiometricUnlock: (onResult: (String?) -> Unit) -> Unit,
    onBiometricToggle: (Boolean) -> Unit,
    selectedTheme: String,
    onThemeChange: (String) -> Unit,
    selectedSessionLimit: String,
    onSessionLimitChange: (String) -> Unit,
    onChangeMasterPassword: (next: String) -> String?,
    onRequireLock: () -> Unit,
    onUserActivity: () -> Unit,
    lockoutMs: Long,
    sessionRemainingMs: Long,
    onImportBackup: suspend (Uri, String, String?) -> Result<Int>,
    onActiveBackupUriRequest: () -> Uri?,
    vaultViewModel: VaultViewModel?
) {
    AnimatedContent(
        targetState = Triple(setupDone, unlocked, vaultViewModel),
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "vault-navigation"
    ) { (isSetupDone, isUnlocked, vm) ->
        when {
            !isSetupDone -> SetupScreen(onSetup, onHasExistingVault, onUserActivity)
            !isUnlocked -> UnlockScreen(
                onUnlock = onUnlock,
                onBiometricUnlock = onBiometricUnlock,
                lockoutMs = lockoutMs,
                onUserActivity = onUserActivity,
                biometricEnabled = biometricEnabled,
                onImportBackup = onImportBackup,
                onActiveBackupUriRequest = onActiveBackupUriRequest
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
                onImportBackup = onImportBackup,
                onActiveBackupUriRequest = onActiveBackupUriRequest,
                viewModel = vm
            )
        }
    }
}

@Composable
private fun SetupScreen(
    onSetup: (String, Uri, Boolean) -> Result<Unit>,
    onHasExistingVault: (Uri) -> Boolean,
    onUserActivity: () -> Unit
) {
    var selectedDirectory by remember { mutableStateOf<Uri?>(null) }
    var restoreMode by remember { mutableStateOf(false) }
    var master by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var creatingVault by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val directoryPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        selectedDirectory = uri
        error = null
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .imePadding()
            .padding(horizontal = 24.dp)
    ) {
        CryptoraCard(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .widthIn(max = 460.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (restoreMode) "Restore Existing Vault" else "Create Your Vault",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = if (restoreMode) {
                        "Restore credentials from your existing encrypted backup file."
                    } else {
                        "Select a vault directory, then use a strong master password to protect your offline vault."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Restore from backup")
                    Switch(
                        checked = restoreMode,
                        onCheckedChange = {
                            onUserActivity()
                            restoreMode = it
                            error = null
                        },
                        enabled = !creatingVault,
                        colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.extraColors.switchChecked, uncheckedThumbColor = MaterialTheme.extraColors.switchUnchecked)
                    )
                }

                Button(
                    onClick = {
                        onUserActivity()
                        directoryPicker.launch(selectedDirectory)
                    },
                    enabled = !creatingVault,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (selectedDirectory == null) "Select Vault Directory" else "Change Vault Directory")
                }

                selectedDirectory?.let {
                    Text(
                        text = "Selected: $it",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                CryptoraTextField(
                    value = master,
                    onValueChange = {
                        onUserActivity()
                        master = it
                        if (error != null) error = null
                    },
                    label = "Master Password",
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
                        val selected = selectedDirectory
                        if (selected == null) {
                            error = "Select a vault directory first"
                            return@Button
                        }
                        val validationError = validateSetupInput(master)
                        if (validationError != null) {
                            error = validationError
                            return@Button
                        }
                        if (restoreMode && !onHasExistingVault(selected)) {
                            error = "Backup file not found in selected directory"
                            return@Button
                        }

                        creatingVault = true
                        scope.launch {
                            val result = withContext(Dispatchers.Default) { onSetup(master, selected, restoreMode) }
                            creatingVault = false
                            if (result.isFailure) {
                                error = result.exceptionOrNull()?.message ?: "Unable to process vault. Please try again."
                            }
                        }
                    },
                    enabled = !creatingVault,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (creatingVault) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            modifier = Modifier.padding(vertical = 4.dp).size(20.dp)
                        )
                    } else {
                        Text(if (restoreMode) "Restore Vault" else "Create Vault", modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
            }
        }
    }
}

private fun validateSetupInput(master: String): String? = null

@Composable
private fun UnlockScreen(
    onUnlock: (String?) -> String?,
    onBiometricUnlock: (onResult: (String?) -> Unit) -> Unit,
    lockoutMs: Long,
    onUserActivity: () -> Unit,
    biometricEnabled: Boolean,
    onImportBackup: suspend (Uri, String, String?) -> Result<Int>,
    onActiveBackupUriRequest: () -> Uri?
) {
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var attemptedBiometric by remember { mutableStateOf(false) }
    var importUri by remember { mutableStateOf<Uri?>(null) }
    var showImportDialog by remember { mutableStateOf(false) }
    var importError by remember { mutableStateOf<String?>(null) }
    var importLoading by remember { mutableStateOf(false) }
    var requireCurrentMasterPassword by remember { mutableStateOf(false) }
    var currentMasterPasswordOverride by remember { mutableStateOf("") }
    var importSuccessMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val activeBackupUri = onActiveBackupUriRequest()
    val filePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }
            importUri = uri
            importError = null
            requireCurrentMasterPassword = false
            currentMasterPasswordOverride = ""
            showImportDialog = true
        }
    }

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

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .widthIn(max = 460.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "App by: Vicky",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
                letterSpacing = 1.5.sp,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Mail: vicky542011@gmail.com",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
                textAlign = TextAlign.Center
            )

            CryptoraCard(modifier = Modifier.fillMaxWidth()) {
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

                    CryptoraTextField(
                        value = password,
                        onValueChange = { onUserActivity(); password = it },
                        label = "Master Password",
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        enabled = lockoutMs == 0L,
                        modifier = Modifier.fillMaxWidth()
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
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Unlock with Biometrics") }
                    }

                    Button(
                        enabled = lockoutMs == 0L,
                        onClick = {
                            onUserActivity()
                            error = onUnlock(password.ifBlank { null })
                        },
                        modifier = Modifier.fillMaxWidth()
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

            TextButton(
                onClick = { filePickerLauncher.launch(arrayOf("application/octet-stream", "application/enc", "*/*")) },
                enabled = !importLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.size(6.dp))
                Text(
                    text = "Restore from backup file",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }

            activeBackupUri?.let {
                Text(
                    text = "Active backup: ${it.lastPathSegment ?: "Unknown"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            importSuccessMessage?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    if (showImportDialog) {
        ImportPasswordDialog(
            isLoading = importLoading,
            errorMessage = importError,
            requireCurrentMasterPassword = requireCurrentMasterPassword,
            currentMasterPassword = currentMasterPasswordOverride,
            onCurrentMasterPasswordChange = { currentMasterPasswordOverride = it },
            onDismiss = {
                if (!importLoading) {
                    showImportDialog = false
                    importUri = null
                    importError = null
                    requireCurrentMasterPassword = false
                    currentMasterPasswordOverride = ""
                }
            },
            onConfirm = { backupPassword ->
                val uri = importUri
                val enteredPassword = backupPassword.trim()
                if (uri == null) {
                    importError = "No file selected"
                } else if (enteredPassword.isBlank()) {
                    importError = "Password is required"
                } else if (requireCurrentMasterPassword && currentMasterPasswordOverride.trim().isBlank()) {
                    importError = "Enter your current app master password"
                } else {
                    importLoading = true
                    importError = null
                    scope.launch {
                        val override = currentMasterPasswordOverride.trim().takeIf { it.isNotBlank() }
                        val result = withContext(Dispatchers.IO) { onImportBackup(uri, enteredPassword, override) }
                        importLoading = false
                        result.fold(
                            onSuccess = { count ->
                                showImportDialog = false
                                importUri = null
                                requireCurrentMasterPassword = false
                                currentMasterPasswordOverride = ""
                                importError = null
                                importSuccessMessage = "Imported $count credentials successfully"
                            },
                            onFailure = { err ->
                                val msg = err.message ?: "Wrong password or corrupted file"
                                importError = msg
                                if (msg.contains("unavailable", ignoreCase = true)) {
                                    requireCurrentMasterPassword = true
                                    importError = "You unlocked via biometric previously. Enter your master password to authorize backup re-encryption."
                                }
                            }
                        )
                    }
                }
            }
        )
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
    onChangeMasterPassword: (next: String) -> String?,
    onRequireLock: () -> Unit,
    onUserActivity: () -> Unit,
    sessionRemainingMs: Long,
    onImportBackup: suspend (Uri, String, String?) -> Result<Int>,
    onActiveBackupUriRequest: () -> Uri?,
    viewModel: VaultViewModel?
) {
    if (viewModel == null) {
        Text("Vault unavailable")
        return
    }

    val credentials = viewModel.credentials.collectAsLazyPagingItems()
    val query by viewModel.query.collectAsStateWithLifecycleCompat()
    val credentialCount by viewModel.credentialCount.collectAsStateWithLifecycleCompat()
    var adding by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Credential?>(null) }
    var editing by remember { mutableStateOf<Credential?>(null) }
    var settingsOpen by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val secureClipboard = remember(context) { SecureClipboard(context) }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbar) { data -> Snackbar(snackbarData = data, containerColor = MaterialTheme.extraColors.snackbarBackground, contentColor = MaterialTheme.extraColors.snackbarText) } },
        floatingActionButton = { FloatingActionButton(onClick = { onUserActivity(); adding = true }, containerColor = MaterialTheme.extraColors.fabBackground, contentColor = MaterialTheme.extraColors.fabIcon) { Text("+") } }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Vault ($credentialCount)", style = MaterialTheme.typography.titleLarge)
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
                supportingText = { Text("Search by description") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            LazyColumn(contentPadding = PaddingValues(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(
                    count = credentials.itemCount,
                    key = credentials.itemKey { it.id }
                ) { index ->
                    val item = credentials[index]
                    if (item != null) {
                        CryptoraCard(onClick = { onUserActivity(); selected = item }) {
                            Column(Modifier.padding(12.dp)) {
                                Text(item.displayTitle())
                                item.displaySubtitle()?.let { Text(it) }
                                Text(CredentialCategory.fromStored(item.category).name)
                            }
                        }
                    }
                }

                when (credentials.loadState.append) {
                    is LoadState.Loading -> item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    is LoadState.Error -> item {
                        Text(
                            text = "Failed to load more",
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    else -> Unit
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
            onChangeMasterPassword = { next ->
                onUserActivity()
                onChangeMasterPassword(next)
            },
            onImportBackup = onImportBackup,
            activeBackupUri = onActiveBackupUriRequest(),
            onImportSuccess = { count ->
                scope.launch { snackbar.showSnackbar("Imported $count credentials successfully") }
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
            onCopyField = { label, value ->
                onUserActivity()
                secureClipboard.copyField(label, value)
                scope.launch { snackbar.showSnackbar("$label copied and will clear in 60s") }
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
    var formState by remember(initialCredential) { mutableStateOf(CredentialFormState.fromCredential(initialCredential)) }
    val isEditing = initialCredential != null
    val context = LocalContext.current
    val secureClipboard = remember(context) { SecureClipboard(context) }

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
                enabled = formState.isValid(),
                onClick = { onSave(formState.toCredential(initialCredential)) }
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
                CategoryDropdown(
                    selected = formState.category,
                    onCategorySelected = { next ->
                        formState = CredentialFormState(category = next)
                    }
                )
                CategoryFields(
                    category = formState.category,
                    state = formState,
                    onStateChange = { formState = it },
                    onCopy = { label, value ->
                        if (value.isNotBlank()) secureClipboard.copyField(label, value)
                    }
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
    onCopyField: (String, String) -> Unit
) {
    var confirmDelete by remember { mutableStateOf(false) }
    val category = CredentialCategory.fromStored(credential.category)
    val state = CredentialFormState.fromCredential(credential)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Credential Details", style = MaterialTheme.typography.headlineSmall) },
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
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val createdText = remember(credential.createdAt) {
                    java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault())
                        .format(java.util.Date(credential.createdAt))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = category.toLabel(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = createdText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                HorizontalDivider()
                CategoryFields(
                    category = category,
                    state = state,
                    onStateChange = {},
                    onCopy = { label, value -> onCopyField(label, value) }
                )
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
    onChangeMasterPassword: (next: String) -> String?,
    onImportBackup: suspend (Uri, String, String?) -> Result<Int>,
    activeBackupUri: Uri?,
    onImportSuccess: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var showPasswordChangedPopup by remember { mutableStateOf(false) }
    var sessionMenuExpanded by remember { mutableStateOf(false) }
    var importUri by remember { mutableStateOf<Uri?>(null) }
    var showImportDialog by remember { mutableStateOf(false) }
    var importError by remember { mutableStateOf<String?>(null) }
    var importLoading by remember { mutableStateOf(false) }
    var requireCurrentMasterPassword by remember { mutableStateOf(false) }
    var currentMasterPasswordOverride by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val filePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }
            importUri = uri
            importError = null
            requireCurrentMasterPassword = false
            currentMasterPasswordOverride = ""
            showImportDialog = true
        }
    }

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
                    Switch(checked = biometricEnabled, onCheckedChange = onBiometricToggle, colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.extraColors.switchChecked, uncheckedThumbColor = MaterialTheme.extraColors.switchUnchecked))
                }

                HorizontalDivider()

                Text("Theme", style = MaterialTheme.typography.titleMedium)
                ThemePickerSection(
                    currentTheme = selectedTheme,
                    onThemeSelected = onThemeSelected
                )

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
                            .menuAnchor()
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

                Text("Backup", style = MaterialTheme.typography.titleMedium)
                Button(
                    onClick = { filePickerLauncher.launch(arrayOf("application/octet-stream", "application/enc", "*/*")) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !importLoading
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Import Backup")
                }
                activeBackupUri?.let {
                    Text(
                        text = "Active backup: ${it.lastPathSegment ?: "Unknown"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                HorizontalDivider()

                Text("Change Master Password", style = MaterialTheme.typography.titleMedium)
                CryptoraTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it; passwordError = null },
                    label = "New Master Password",
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                CryptoraTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it; passwordError = null },
                    label = "Confirm New Master Password",
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        passwordError = when {
                            newPassword != confirmPassword -> "New password and confirm password do not match"
                            else -> onChangeMasterPassword(newPassword)
                        }
                        if (passwordError == null) {
                            newPassword = ""
                            confirmPassword = ""
                            showPasswordChangedPopup = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
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

    if (showImportDialog) {
        ImportPasswordDialog(
            isLoading = importLoading,
            errorMessage = importError,
            requireCurrentMasterPassword = requireCurrentMasterPassword,
            currentMasterPassword = currentMasterPasswordOverride,
            onCurrentMasterPasswordChange = { currentMasterPasswordOverride = it },
            onDismiss = {
                if (!importLoading) {
                    showImportDialog = false
                    importUri = null
                    importError = null
                    requireCurrentMasterPassword = false
                    currentMasterPasswordOverride = ""
                }
            },
            onConfirm = { password ->
                val uri = importUri
                val enteredPassword = password.trim()
                if (uri == null) {
                    importError = "No backup file selected"
                } else if (enteredPassword.isBlank()) {
                    importError = "Password is required"
                } else if (requireCurrentMasterPassword && currentMasterPasswordOverride.trim().isBlank()) {
                    importError = "Enter current app master password"
                } else {
                    importLoading = true
                    importError = null
                    scope.launch {
                        val overridePassword = currentMasterPasswordOverride.trim().takeIf { it.isNotBlank() }
                        val result = withContext(Dispatchers.IO) { onImportBackup(uri, enteredPassword, overridePassword) }
                        importLoading = false
                        result.fold(
                            onSuccess = { count ->
                                showImportDialog = false
                                importUri = null
                                importError = null
                                requireCurrentMasterPassword = false
                                currentMasterPasswordOverride = ""
                                onImportSuccess(count)
                            },
                            onFailure = { error ->
                                val message = error.message ?: "Wrong password or corrupted file"
                                importError = message
                                if (message.contains("Current app master password unavailable", ignoreCase = true)) {
                                    requireCurrentMasterPassword = true
                                    importError = "You unlocked via biometric. Enter your current app master password below to re-encrypt imported backup for future sync."
                                }
                            }
                        )
                    }
                }
            }
        )
    }

    if (showPasswordChangedPopup) {
        AlertDialog(
            onDismissRequest = { showPasswordChangedPopup = false },
            title = { Text("Success") },
            text = { Text("Master password updated successfully") },
            confirmButton = {
                TextButton(onClick = { showPasswordChangedPopup = false }) { Text("OK") }
            }
        )
    }
}


@Composable
private fun ImportPasswordDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    requireCurrentMasterPassword: Boolean = false,
    currentMasterPassword: String = "",
    onCurrentMasterPasswordChange: (String) -> Unit = {}
) {
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Lock, contentDescription = null)
                Text("Decrypt Backup File")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Enter the master password used to encrypt this backup file.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Backup Password") },
                    singleLine = true,
                    enabled = !isLoading,
                    isError = errorMessage != null,
                    supportingText = {
                        if (errorMessage != null) {
                            Text(errorMessage, color = MaterialTheme.colorScheme.error)
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { if (password.isNotBlank()) onConfirm(password) }),
                    modifier = Modifier.fillMaxWidth()
                )
                if (requireCurrentMasterPassword) {
                    OutlinedTextField(
                        value = currentMasterPassword,
                        onValueChange = onCurrentMasterPasswordChange,
                        label = { Text("Current App Master Password") },
                        singleLine = true,
                        enabled = !isLoading,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !isLoading) { Text("Cancel") } },
        confirmButton = {
            Button(
                onClick = { onConfirm(password) },
                enabled = password.isNotBlank() && !isLoading && (!requireCurrentMasterPassword || currentMasterPassword.isNotBlank())
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.size(8.dp))
                }
                Text("Import")
            }
        }
    )
}

private data class ThemeOption(
    val key: String,
    val label: String,
    val accent: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemePickerSection(
    currentTheme: String,
    onThemeSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val themes = listOf(
        ThemeOption("BLOOD_VAULT", "🩸 Blood Vault", Color(0xFFCC0000)),
        ThemeOption("MATRIX", "👾 Matrix", Color(0xFF00FF41)),
        ThemeOption("DEEP_OCEAN", "🌊 Deep Ocean", Color(0xFF00E5FF)),
        ThemeOption("NUCLEAR", "☢️ Nuclear", Color(0xFFCCFF00)),
        ThemeOption("INFERNO", "🔥 Inferno", Color(0xFFFF6600)),
        ThemeOption("CHROME_PUNK", "🤖 Chrome Punk", Color(0xFFB0BEC5)),
        ThemeOption("SAKURA_NOIR", "🌸 Sakura Noir", Color(0xFFFF80AB))
    )

    val selected = themes.firstOrNull { it.key == currentTheme } ?: themes.first { it.key == "MATRIX" }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selected.label,
            onValueChange = {},
            readOnly = true,
            label = { Text("Theme") },
            leadingIcon = {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .background(selected.accent, CircleShape)
                )
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            themes.forEach { theme ->
                val isSelected = theme.key == currentTheme
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(theme.accent, CircleShape)
                            )
                            Text(
                                text = theme.label,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    },
                    trailingIcon = {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = theme.accent,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    },
                    onClick = {
                        onThemeSelected(theme.key)
                        expanded = false
                    }
                )
            }
        }
    }
}

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


private fun Credential.displayTitle(): String = description.ifBlank {
    when (CredentialCategory.fromStored(category)) {
        CredentialCategory.EMAIL -> emailLogin.orEmpty().ifBlank { "Email" }
        CredentialCategory.BANK -> bankAccountNo.orEmpty().ifBlank { "Bank" }
        CredentialCategory.CARD -> "Card"
        CredentialCategory.IDENTITY -> "Identity"
        CredentialCategory.OTHER -> title.ifBlank { "Credential" }
    }
}

private fun Credential.displaySubtitle(): String? = when (CredentialCategory.fromStored(category)) {
    CredentialCategory.EMAIL -> emailLogin
    CredentialCategory.BANK -> bankCustomerId ?: bankAccountNo
    CredentialCategory.CARD -> cardNumber?.chunked(4)?.joinToString("-")
    CredentialCategory.IDENTITY -> identityId?.chunked(4)?.joinToString("-")
    CredentialCategory.OTHER -> username
}?.takeIf { it.isNotBlank() }


private fun CredentialCategory.toLabel(): String = when (this) {
    CredentialCategory.EMAIL -> "📧 Email"
    CredentialCategory.BANK -> "🏦 Bank"
    CredentialCategory.CARD -> "💳 Card"
    CredentialCategory.IDENTITY -> "🪪 Identity"
    CredentialCategory.OTHER -> "📁 Other"
}
