package com.yourapp.vault

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.lifecycle.viewmodel.CreationExtras
import com.yourapp.vault.security.RootDetection
import com.yourapp.vault.ui.VaultApp
import com.yourapp.vault.viewmodel.SessionViewModel
import com.yourapp.vault.viewmodel.VaultViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
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
                        onSetup = { master, pin ->
                            runCatching {
                                val dbKey = appContainer.authManager.createVault(master.toCharArray(), pin?.toCharArray())
                                vaultViewModel = VaultViewModel(appContainer.createRepository(dbKey))
                                setupDone = true
                                sessionVm.unlock()
                            }
                        },
                        onUnlock = { password, pin ->
                            val success = when {
                                !password.isNullOrBlank() -> appContainer.authManager.verifyMasterPassword(password.toCharArray())
                                !pin.isNullOrBlank() -> appContainer.authManager.verifyPin(pin.toCharArray())
                                else -> false
                            }
                            if (success) {
                                val dbKey = appContainer.authManager.openDbKey() ?: return@VaultApp false
                                vaultViewModel = VaultViewModel(appContainer.createRepository(dbKey))
                                sessionVm.unlock()
                            }
                            success
                        },
                        onBiometricToggle = appContainer::setBiometricEnabled,
                        onRequireLock = { sessionVm.lock() },
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

    override fun onPause() {
        super.onPause()
        ViewModelProvider(this)[SessionViewModel::class.java].lock()
    }
}
