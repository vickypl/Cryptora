package com.yourapp.vault

import android.content.Context
import android.net.Uri
import com.yourapp.vault.data.local.VaultDatabase
import com.yourapp.vault.data.repository.VaultRepository
import com.yourapp.vault.domain.model.Credential
import com.yourapp.vault.security.AuthManager
import com.yourapp.vault.security.KeystoreManager
import com.yourapp.vault.security.SecureStorage
import com.yourapp.vault.security.VaultBackupManager

class AppContainer(private val context: Context) {
    private val storage = SecureStorage(context)
    private val keystoreManager = KeystoreManager()
    private val backupManager = VaultBackupManager(context)
    val authManager = AuthManager(storage, keystoreManager)

    fun isSetupDone(): Boolean = storage.isSetupDone()
    fun biometricEnabled(): Boolean = storage.biometricEnabled()
    fun setBiometricEnabled(enabled: Boolean) = storage.setBiometricEnabled(enabled)
    fun selectedTheme(): String = storage.getTheme()
    fun setSelectedTheme(theme: String) = storage.setTheme(theme)
    fun selectedSessionLimit(): String = storage.getSessionLimit()
    fun setSelectedSessionLimit(limit: String) = storage.setSessionLimit(limit)

    fun changeMasterPassword(newPassword: String): String? {
        return authManager.changeMasterPassword(newPassword.toCharArray())
    }

    fun createRepository(dbKey: ByteArray): VaultRepository {
        val db = VaultDatabase.build(context, dbKey)
        return VaultRepository(db.credentialDao())
    }

    fun persistVaultDirectory(uri: Uri) = storage.setVaultDirectoryUri(uri.toString())

    fun selectedVaultDirectory(): Uri? = storage.getVaultDirectoryUri()?.let(Uri::parse)

    fun hasExternalVault(uri: Uri): Boolean = backupManager.vaultExists(uri)

    fun restoreVaultFromExternal(uri: Uri, masterPassword: CharArray) = backupManager.restoreVault(uri, masterPassword)

    fun backupVaultToExternal(uri: Uri, credentials: List<Credential>, masterPassword: CharArray) =
        backupManager.writeVault(uri, credentials, masterPassword)

    fun backupManager(): VaultBackupManager = backupManager
}
