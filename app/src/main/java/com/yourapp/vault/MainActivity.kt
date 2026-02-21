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

        setContent {
            var selectedTheme by remember { mutableStateOf(appContainer.selectedTheme()) }

            MaterialTheme(colorScheme = colorSchemeFor(selectedTheme)) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var vaultViewModel by remember { mutableStateOf<VaultViewModel?>(null) }
                    var setupDone by remember { mutableStateOf(appContainer.isSetupDone()) }
                    val unlocked by sessionVm.isUnlocked.collectAsState()

                    LaunchedEffect(unlocked) {
                        if (unlocked) {
                            sessionVm.markActive()
                        }
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
                        onChangeMasterPassword = appContainer::changeMasterPassword,
                        onRequireLock = { sessionVm.lock() },
                        onUserActivity = { sessionVm.markActive() },
                        lockoutMs = appContainer.authManager.lockoutRemainingMs(),
                        vaultViewModel = vaultViewModel
                    )
                }
            }
        }

        lifecycleScope.launch {
            while (true) {
                delay(5_000)
                sessionVm.lockIfInactive()
            }
        }
    }

}

private fun colorSchemeFor(theme: String) = when (theme) {
    "MIDNIGHT" -> darkColorScheme(
        primary = Color(0xFF4F7DFF),
        secondary = Color(0xFF6C88B7),
        tertiary = Color(0xFF6A8C7A),
        surface = Color(0xFF131A24),
        background = Color(0xFF0C1118)
    )

    "SLATE" -> darkColorScheme(
        primary = Color(0xFF3E6A8E),
        secondary = Color(0xFF5F7D95),
        tertiary = Color(0xFF6A8F89),
        surface = Color(0xFF171C22),
        background = Color(0xFF11161B)
    )

    "GRAPHITE" -> darkColorScheme(
        primary = Color(0xFF5D6672),
        secondary = Color(0xFF7A838F),
        tertiary = Color(0xFF6E7F76),
        surface = Color(0xFF191B1F),
        background = Color(0xFF121418)
    )

    "FOREST" -> darkColorScheme(
        primary = Color(0xFF2F7D5A),
        secondary = Color(0xFF4C8A72),
        tertiary = Color(0xFF72946A),
        surface = Color(0xFF111C17),
        background = Color(0xFF0B140F)
    )

    "INDIGO" -> darkColorScheme(
        primary = Color(0xFF5B57D9),
        secondary = Color(0xFF7671C2),
        tertiary = Color(0xFF5B7A9A),
        surface = Color(0xFF17162A),
        background = Color(0xFF0F0E1D)
    )

    else -> darkColorScheme()
}
