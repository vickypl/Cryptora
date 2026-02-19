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
        primary = Color(0xFF8FA8FF),
        secondary = Color(0xFF9DB5C9),
        tertiary = Color(0xFFA9C4A0),
        surface = Color(0xFF10131A),
        background = Color(0xFF0C0F15)
    )

    "SLATE" -> darkColorScheme(
        primary = Color(0xFF8BA1B8),
        secondary = Color(0xFF9AA8B8),
        tertiary = Color(0xFF8EB0A0),
        surface = Color(0xFF14171C),
        background = Color(0xFF101317)
    )

    "GRAPHITE" -> darkColorScheme(
        primary = Color(0xFFA7AFC0),
        secondary = Color(0xFFB0B6C2),
        tertiary = Color(0xFF95A79E),
        surface = Color(0xFF151515),
        background = Color(0xFF0F0F10)
    )

    "FOREST" -> darkColorScheme(
        primary = Color(0xFF8FB2A0),
        secondary = Color(0xFF97AA9D),
        tertiary = Color(0xFFB2C19A),
        surface = Color(0xFF111814),
        background = Color(0xFF0D120F)
    )

    "INDIGO" -> darkColorScheme(
        primary = Color(0xFF9E9DDA),
        secondary = Color(0xFF9FA8DA),
        tertiary = Color(0xFF90A4AE),
        surface = Color(0xFF14142A),
        background = Color(0xFF0E0E1C)
    )

    else -> darkColorScheme()
}
