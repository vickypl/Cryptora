package com.yourapp.vault

import android.os.Bundle
import android.view.WindowManager
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.yourapp.vault.security.RootDetection
import com.yourapp.vault.ui.VaultApp
import com.yourapp.vault.viewmodel.SessionViewModel
import com.yourapp.vault.viewmodel.VaultViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        enableEdgeToEdge()

        val appContainer = AppContainer(applicationContext)
        val sessionVm = ViewModelProvider(this)[SessionViewModel::class.java]
        var currentSessionLimitMs: Long? = sessionLimitMsFor(appContainer.selectedSessionLimit())

        setContent {
            var selectedTheme by remember { mutableStateOf(appContainer.selectedTheme()) }
            var selectedSessionLimit by remember { mutableStateOf(appContainer.selectedSessionLimit()) }

            MaterialTheme(colorScheme = colorSchemeFor(selectedTheme)) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var vaultViewModel by remember { mutableStateOf<VaultViewModel?>(null) }
                    var setupDone by remember { mutableStateOf(appContainer.isSetupDone()) }
                    val unlocked by sessionVm.isUnlocked.collectAsState()
                    var sessionRemainingMs by remember { mutableStateOf(sessionLimitMsFor(selectedSessionLimit) ?: -1L) }

                    LaunchedEffect(unlocked) {
                        if (unlocked) {
                            sessionVm.markActive()
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
                        rooted = RootDetection.isRooted(),
                        unlocked = unlocked,
                        biometricEnabled = appContainer.biometricEnabled(),
                        onSetup = { master ->
                            runCatching {
                                val dbKey = appContainer.authManager.createVault(master.toCharArray(), null)
                                vaultViewModel = VaultViewModel(appContainer.createRepository(dbKey))
                                setupDone = true
                                sessionVm.unlock()
                            }
                        },
                        onUnlock = { password ->
                            runCatching {
                                val success = !password.isNullOrBlank() &&
                                    appContainer.authManager.verifyMasterPassword(password.toCharArray())
                                if (!success) {
                                    return@runCatching "Authentication failed"
                                }

                                val dbKey = appContainer.authManager.openDbKey()
                                    ?: return@runCatching "Unable to access vault key"
                                vaultViewModel = VaultViewModel(appContainer.createRepository(dbKey))
                                sessionVm.unlock()
                                null
                            }.getOrElse {
                                "Unable to unlock vault. Please try again."
                            }
                        },
                        onBiometricUnlock = { onResult ->
                            val biometricManager = BiometricManager.from(this@MainActivity)
                            val allowedAuthenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
                                BiometricManager.Authenticators.BIOMETRIC_WEAK
                            when (biometricManager.canAuthenticate(allowedAuthenticators)) {
                                BiometricManager.BIOMETRIC_SUCCESS -> {
                                val executor = ContextCompat.getMainExecutor(this@MainActivity)
                                val promptInfo = BiometricPrompt.PromptInfo.Builder()
                                    .setTitle("Unlock Cryptora")
                                    .setSubtitle("Authenticate to access your vault")
                                    .setNegativeButtonText("Cancel")
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
                                                vaultViewModel = VaultViewModel(appContainer.createRepository(dbKey))
                                                sessionVm.unlock()
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
                        onChangeMasterPassword = appContainer::changeMasterPassword,
                        onRequireLock = { sessionVm.lock() },
                        onUserActivity = { sessionVm.markActive() },
                        lockoutMs = appContainer.authManager.lockoutRemainingMs(),
                        sessionRemainingMs = sessionRemainingMs,
                        vaultViewModel = vaultViewModel
                    )
                }
            }
        }

        lifecycleScope.launch {
            while (true) {
                delay(1_000)
                sessionVm.lockIfInactive()
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

private fun colorSchemeFor(theme: String) = when (theme) {
    "MIDNIGHT" -> darkColorScheme(
        primary = Color(0xFF4F7DFF),
        onPrimary = Color(0xFFFFFFFF),
        secondary = Color(0xFF6C88B7),
        onSecondary = Color(0xFFFFFFFF),
        tertiary = Color(0xFF6A8C7A),
        background = Color(0xFF0C1118),
        onBackground = Color(0xFFE8EEF8),
        surface = Color(0xFF131A24),
        onSurface = Color(0xFFE8EEF8),
        surfaceVariant = Color(0xFF1C2531),
        primaryContainer = Color(0xFF203764),
        secondaryContainer = Color(0xFF25384B)
    )

    "SLATE" -> darkColorScheme(
        primary = Color(0xFF3E6A8E),
        onPrimary = Color(0xFFFFFFFF),
        secondary = Color(0xFF5F7D95),
        onSecondary = Color(0xFFFFFFFF),
        tertiary = Color(0xFF6A8F89),
        background = Color(0xFF11161B),
        onBackground = Color(0xFFE7ECF0),
        surface = Color(0xFF171C22),
        onSurface = Color(0xFFE7ECF0),
        surfaceVariant = Color(0xFF222A33),
        primaryContainer = Color(0xFF24435C),
        secondaryContainer = Color(0xFF2C3D4A)
    )

    "GRAPHITE" -> darkColorScheme(
        primary = Color(0xFF5D6672),
        onPrimary = Color(0xFFFFFFFF),
        secondary = Color(0xFF7A838F),
        onSecondary = Color(0xFFFFFFFF),
        tertiary = Color(0xFF6E7F76),
        background = Color(0xFF121418),
        onBackground = Color(0xFFE6E8EC),
        surface = Color(0xFF191B1F),
        onSurface = Color(0xFFE6E8EC),
        surfaceVariant = Color(0xFF25282E),
        primaryContainer = Color(0xFF363C45),
        secondaryContainer = Color(0xFF3F454E)
    )

    "FOREST" -> darkColorScheme(
        primary = Color(0xFF2F7D5A),
        onPrimary = Color(0xFFFFFFFF),
        secondary = Color(0xFF4C8A72),
        onSecondary = Color(0xFFFFFFFF),
        tertiary = Color(0xFF72946A),
        background = Color(0xFF0B140F),
        onBackground = Color(0xFFE5F1EA),
        surface = Color(0xFF111C17),
        onSurface = Color(0xFFE5F1EA),
        surfaceVariant = Color(0xFF1D2A24),
        primaryContainer = Color(0xFF1F4B38),
        secondaryContainer = Color(0xFF2A5546)
    )

    "INDIGO" -> darkColorScheme(
        primary = Color(0xFF5B57D9),
        onPrimary = Color(0xFFFFFFFF),
        secondary = Color(0xFF7671C2),
        onSecondary = Color(0xFFFFFFFF),
        tertiary = Color(0xFF5B7A9A),
        background = Color(0xFF0F0E1D),
        onBackground = Color(0xFFE8E7FA),
        surface = Color(0xFF17162A),
        onSurface = Color(0xFFE8E7FA),
        surfaceVariant = Color(0xFF23223A),
        primaryContainer = Color(0xFF302F73),
        secondaryContainer = Color(0xFF3A3961)
    )

    else -> darkColorScheme()
}
