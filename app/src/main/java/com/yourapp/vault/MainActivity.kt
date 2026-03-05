package com.yourapp.vault

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.fragment.app.FragmentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.yourapp.vault.security.TamperDetection
import com.yourapp.vault.ui.VaultApp
import com.yourapp.vault.ui.theme.CryptoraTheme
import com.yourapp.vault.viewmodel.SessionViewModel
import com.yourapp.vault.viewmodel.VaultViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        enableEdgeToEdge()

        val appContainer = AppContainer(applicationContext)
        val debuggerAttached = TamperDetection.isDebuggerAttached()
        val hookingDetected = TamperDetection.isHookingDetected()
        if (debuggerAttached) {
            Log.w("MainActivity", "Debugger detected. Sensitive operations should be restricted.")
        }
        if (hookingDetected) {
            Log.w("MainActivity", "Hooking framework indicators detected.")
        }
        val sessionVm = ViewModelProvider(this)[SessionViewModel::class.java]
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (sessionVm.isUnlocked.value) {
                    sessionVm.lock()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                    isEnabled = true
                }
            }
        })
        var currentSessionLimitMs: Long? = sessionLimitMsFor(appContainer.selectedSessionLimit())

        setContent {
            var selectedTheme by remember { mutableStateOf(appContainer.selectedTheme()) }
            var selectedSessionLimit by remember { mutableStateOf(appContainer.selectedSessionLimit()) }

            CryptoraTheme(selectedTheme = selectedTheme) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    var vaultViewModel by remember { mutableStateOf<VaultViewModel?>(null) }
                    var setupDone by remember { mutableStateOf(appContainer.isSetupDone()) }
                    val unlocked by sessionVm.isUnlocked.collectAsState()
                    var sessionRemainingMs by remember { mutableStateOf(sessionLimitMsFor(selectedSessionLimit) ?: -1L) }

                    LaunchedEffect(unlocked) {
                        if (unlocked) {
                            sessionVm.resetInactivityTimer()
                        }
                    }


                    LaunchedEffect(unlocked, selectedSessionLimit) {
                        val limitMs = sessionLimitMsFor(selectedSessionLimit)
                        while (unlocked) {
                            sessionRemainingMs = limitMs?.let { sessionVm.sessionRemainingMs(it) } ?: -1L
                            delay(1_000)
                        }
                        sessionRemainingMs = limitMs ?: -1L
                    }
                    VaultApp(
                        setupDone = setupDone,
                        rooted = false,
                        unlocked = unlocked,
                        biometricEnabled = appContainer.biometricEnabled(),
                        onSetup = { master, vaultDirectory, restoreExisting ->
                            runCatching {
                                contentResolver.takePersistableUriPermission(
                                    vaultDirectory,
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                                )

                                val dbKey = appContainer.authManager.createVault(master.toCharArray(), null)
                                val repository = appContainer.createRepository(dbKey)

                                if (restoreExisting) {
                                    val restoredCredentials = appContainer.restoreVaultFromExternal(vaultDirectory, master.toCharArray())
                                        .getOrElse { throw IllegalStateException("Invalid Master Password or Corrupted Vault File.") }
                                    runBlocking { repository.upsertAll(restoredCredentials) }
                                    appContainer.backupVaultToExternal(vaultDirectory, restoredCredentials, master.toCharArray())
                                        .getOrElse { throw IllegalStateException("Restore completed, but backup target could not be activated") }
                                } else {
                                    appContainer.backupVaultToExternal(vaultDirectory, emptyList(), master.toCharArray())
                                        .getOrElse { throw IllegalStateException("Unable to initialize vault backup file") }
                                }

                                appContainer.persistVaultDirectory(vaultDirectory)
                                vaultViewModel = VaultViewModel(
                                    repository = repository,
                                    backupManager = appContainer.backupManager(),
                                    backupDirectoryProvider = appContainer::selectedVaultDirectory,
                                    masterPasswordProvider = sessionVm::getMasterPassword
                                )
                                setupDone = true
                                sessionVm.unlockWithPassword(master)
                                sessionVm.resetInactivityTimer()
                                if (restoreExisting) {
                                    vaultViewModel?.registerBackupTarget(vaultDirectory, master)
                                }
                            }
                        },
                        onHasExistingVault = { vaultDirectory ->
                            runCatching {
                                contentResolver.takePersistableUriPermission(
                                    vaultDirectory,
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                                )
                            }
                            appContainer.hasExternalVault(vaultDirectory)
                        },
                        onUnlock = { password ->
                            runCatching {
                                if (password.isNullOrBlank()) {
                                    return@runCatching "Authentication failed"
                                }
                                val dbKey = appContainer.authManager.openDbKey(password.toCharArray())
                                    ?: return@runCatching "Authentication failed"
                                val repository = appContainer.createRepository(dbKey)
                                appContainer.selectedVaultDirectory()?.let { uri ->
                                    val snapshot = runBlocking { repository.listAllCredentials() }
                                    appContainer.backupVaultToExternal(uri, snapshot, password.toCharArray())
                                }
                                vaultViewModel = VaultViewModel(
                                    repository = repository,
                                    backupManager = appContainer.backupManager(),
                                    backupDirectoryProvider = appContainer::selectedVaultDirectory,
                                    masterPasswordProvider = sessionVm::getMasterPassword
                                )
                                sessionVm.unlockWithPassword(password)
                                sessionVm.resetInactivityTimer()
                                null
                            }.getOrElse {
                                "Unable to unlock vault. Please try again."
                            }
                        },
                        onBiometricUnlock = { onResult ->
                            if (hookingDetected || debuggerAttached) {
                                onResult("Sensitive operations disabled due to security risk detected")
                            } else {
                                val biometricManager = BiometricManager.from(this@MainActivity)
                                val allowedAuthenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
                                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
                                when (biometricManager.canAuthenticate(allowedAuthenticators)) {
                                BiometricManager.BIOMETRIC_SUCCESS -> {
                                val executor = ContextCompat.getMainExecutor(this@MainActivity)
                                val promptInfo = BiometricPrompt.PromptInfo.Builder()
                                    .setTitle("Unlock Cryptora")
                                    .setSubtitle("Authenticate to access your vault")
                                    .setAllowedAuthenticators(allowedAuthenticators)
                                    .build()

                                val biometricPrompt = BiometricPrompt(
                                    this@MainActivity,
                                    executor,
                                    object : BiometricPrompt.AuthenticationCallback() {
                                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                            val dbKey = appContainer.authManager.openDbKey()
                                            if (dbKey == null) {
                                                onResult("Unable to access vault key")
                                                return
                                            }
                                            runCatching {
                                                vaultViewModel = VaultViewModel(
                                                    repository = appContainer.createRepository(dbKey),
                                                    backupManager = appContainer.backupManager(),
                                                    backupDirectoryProvider = appContainer::selectedVaultDirectory,
                                                    masterPasswordProvider = sessionVm::getMasterPassword
                                                )
                                                sessionVm.unlock()
                                                sessionVm.resetInactivityTimer()
                                                onResult(null)
                                            }.getOrElse {
                                                onResult("Unable to unlock vault. Please try again.")
                                            }
                                        }

                                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                                            if (errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                                                errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                                                errorCode == BiometricPrompt.ERROR_CANCELED
                                            ) {
                                                onResult("Biometric canceled. Use master password.")
                                            } else {
                                                onResult(errString.toString())
                                            }
                                        }

                                        override fun onAuthenticationFailed() {
                                            onResult("Authentication failed")
                                        }
                                    }
                                )

                                biometricPrompt.authenticate(promptInfo)
                                }
                                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> onResult("No biometric enrolled. Use master password.")
                                BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE,
                                BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> onResult("Biometric authentication is not available")
                                else -> onResult("Biometric authentication is currently unavailable")
                                }
                            }
                        },
                        onBiometricToggle = appContainer::setBiometricEnabled,
                        selectedTheme = selectedTheme,
                        onThemeChange = { theme ->
                            selectedTheme = theme
                            appContainer.setSelectedTheme(theme)
                        },
                        selectedSessionLimit = selectedSessionLimit,
                        onSessionLimitChange = { limit ->
                            selectedSessionLimit = limit
                            appContainer.setSelectedSessionLimit(limit)
                            currentSessionLimitMs = sessionLimitMsFor(limit)
                        },
                        onChangeMasterPassword = { next ->
                            val updateError = appContainer.changeMasterPassword(next)
                            if (updateError != null) {
                                updateError
                            } else {
                                sessionVm.updateMasterPassword(next)
                                val reEncryptResult = runBlocking {
                                    vaultViewModel?.reEncryptBackupWithNewMasterPassword(next) ?: Result.success(Unit)
                                }
                                reEncryptResult.exceptionOrNull()?.let {
                                    "Master password changed, but backup could not be re-encrypted. Please trigger a backup sync."
                                }
                            }
                        },
                        onRequireLock = { sessionVm.lock() },
                        onUserActivity = { sessionVm.resetInactivityTimer() },
                        lockoutMs = appContainer.authManager.lockoutRemainingMs(),
                        sessionRemainingMs = sessionRemainingMs,
                        onImportBackup = { backupUri, importPassword ->
                            val currentPassword = sessionVm.getMasterPassword()?.concatToString()
                            vaultViewModel?.importBackup(
                                backupUri = backupUri,
                                importPassword = importPassword,
                                currentMasterPassword = currentPassword,
                                onBackupTargetActivated = { appContainer.persistVaultDirectory(it) }
                            ) ?: Result.failure(IllegalStateException("Vault unavailable"))
                        },
                        onActiveBackupUriRequest = appContainer::selectedVaultDirectory,
                        vaultViewModel = vaultViewModel
                    )
                }
            }
        }

        lifecycleScope.launch {
            while (true) {
                delay(1_000)
                currentSessionLimitMs?.let { sessionVm.lockIfSessionExpired(it) }
            }
        }
    }

}

private fun sessionLimitMsFor(limit: String): Long? = when (limit) {
    "1m" -> 60_000L
    "2m" -> 120_000L
    "5m" -> 300_000L
    "10m" -> 600_000L
    "none" -> null
    else -> 300_000L
}
