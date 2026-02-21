package com.yourapp.vault

import android.content.Context
import com.yourapp.vault.data.local.VaultDatabase
import com.yourapp.vault.data.repository.VaultRepository
import com.yourapp.vault.security.AuthManager
import com.yourapp.vault.security.KeystoreManager
import com.yourapp.vault.security.SecureStorage

class AppContainer(private val context: Context) {
    private val storage = SecureStorage(context)
    private val keystoreManager = KeystoreManager()
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
}
