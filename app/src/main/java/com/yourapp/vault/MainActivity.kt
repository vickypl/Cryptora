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
            MaterialTheme {
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
                            if (biometricManager.canAuthenticate(allowedAuthenticators) != BiometricManager.BIOMETRIC_SUCCESS) {
                                onResult("Biometric authentication is not available")
                            } else {
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
                                            onResult(errString.toString())
                                        }

                                        override fun onAuthenticationFailed() {
                                            onResult("Authentication failed")
                                        }
                                    }
                                )

                                biometricPrompt.authenticate(promptInfo)
                            }
                        },
                        onBiometricToggle = appContainer::setBiometricEnabled,
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