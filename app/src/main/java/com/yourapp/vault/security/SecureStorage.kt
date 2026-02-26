package com.yourapp.vault.security

import android.content.Context
import android.util.Base64
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SecureStorage(context: Context) {
    // EncryptedSharedPreferences is defense-in-depth only; primary protection is Keystore auth binding + SQLCipher key wrapping.
    private val prefs = EncryptedSharedPreferences.create(
        context,
        "vault_secure_prefs",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveSetup(
        salt: ByteArray,
        wrappedDbKey: ByteArray,
        wrappedDbIv: ByteArray,
        passwordWrappedDbKey: ByteArray,
        passwordWrappedDbIv: ByteArray
    ) {
        prefs.edit {
            putString("salt", salt.toB64())
            putString("wrapped_db_key", wrappedDbKey.toB64())
            putString("wrapped_db_key_iv", wrappedDbIv.toB64())
            putString("pwd_wrapped_db_key", passwordWrappedDbKey.toB64())
            putString("pwd_wrapped_db_key_iv", passwordWrappedDbIv.toB64())
            putBoolean("is_setup_done", true)
        }
    }

    fun isSetupDone(): Boolean = prefs.getBoolean("is_setup_done", false)

    fun getSalt(): ByteArray? = prefs.getString("salt", null)?.fromB64()
    fun getWrappedDbKey(): ByteArray? = prefs.getString("wrapped_db_key", null)?.fromB64()
    fun getWrappedDbIv(): ByteArray? = prefs.getString("wrapped_db_key_iv", null)?.fromB64()
    fun getPasswordWrappedDbKey(): ByteArray? = prefs.getString("pwd_wrapped_db_key", null)?.fromB64()
    fun getPasswordWrappedDbIv(): ByteArray? = prefs.getString("pwd_wrapped_db_key_iv", null)?.fromB64()

    fun setWrappedPayload(passwordWrappedDbKey: ByteArray, passwordWrappedDbIv: ByteArray) = prefs.edit {
        putString("pwd_wrapped_db_key", passwordWrappedDbKey.toB64())
        putString("pwd_wrapped_db_key_iv", passwordWrappedDbIv.toB64())
    }

    fun setKeystoreWrappedPayload(wrappedDbKey: ByteArray, wrappedDbIv: ByteArray) = prefs.edit {
        putString("wrapped_db_key", wrappedDbKey.toB64())
        putString("wrapped_db_key_iv", wrappedDbIv.toB64())
    }

    fun setBiometricEnabled(enabled: Boolean) = prefs.edit { putBoolean("biometric_enabled", enabled) }
    fun biometricEnabled(): Boolean = prefs.getBoolean("biometric_enabled", true)

    fun setTheme(theme: String) = prefs.edit { putString("app_theme", theme) }
    fun getTheme(): String = prefs.getString("app_theme", "MIDNIGHT") ?: "MIDNIGHT"

    fun setSessionLimit(limit: String) = prefs.edit { putString("session_limit", limit) }
    fun getSessionLimit(): String = prefs.getString("session_limit", "5m") ?: "5m"

    fun setPinHash(pinHash: ByteArray?) = prefs.edit {
        if (pinHash == null) remove("pin_hash") else putString("pin_hash", pinHash.toB64())
    }

    fun getPinHash(): ByteArray? = prefs.getString("pin_hash", null)?.fromB64()

    private fun ByteArray.toB64(): String = Base64.encodeToString(this, Base64.NO_WRAP)
    private fun String.fromB64(): ByteArray = Base64.decode(this, Base64.NO_WRAP)
}
