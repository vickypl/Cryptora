package com.yourapp.vault.security

import android.os.SystemClock
import android.util.Log
import java.security.MessageDigest

class AuthManager(
    private val secureStorage: SecureStorage,
    private val keystoreManager: KeystoreManager
) {
    private var failedAttempts = 0
    private var lockoutStartMs = 0L

    fun createVault(masterPassword: CharArray, pin: CharArray?): ByteArray {
        val salt = KeyDerivation.randomSalt(32)
        val derivedMasterKey = KeyDerivation.derive(masterPassword, salt)
        val dbKey = KeyDerivation.randomSalt(32)

        val (passwordWrappedDbKey, passwordWrappedDbIv) = keystoreManager.encryptWithDerivedKey(dbKey, derivedMasterKey)
        val (wrappedByKeystore, wrappedByKeystoreIv) = runCatching { keystoreManager.wrap(dbKey) }
            .getOrElse {
                Log.w(TAG, "Keystore wrap unavailable during setup; password unlock will still work", it)
                byteArrayOf<Byte>() to byteArrayOf<Byte>()
            }

        secureStorage.saveSetup(
            salt = salt,
            wrappedDbKey = wrappedByKeystore,
            wrappedDbIv = wrappedByKeystoreIv,
            passwordWrappedDbKey = passwordWrappedDbKey,
            passwordWrappedDbIv = passwordWrappedDbIv
        )
        pin?.let { secureStorage.setPinHash(KeyDerivation.derive(it, salt)) }
        derivedMasterKey.fill(0)
        return dbKey
    }

    fun verifyMasterPassword(password: CharArray): Boolean {
        if (isLockedOut()) return false
        val decryptedDbKey = runCatching { openDbKey(password) }.getOrNull()
        val success = decryptedDbKey != null
        decryptedDbKey?.fill(0)
        if (success) resetAttempts() else registerFailure()
        return success
    }

    fun verifyPin(pin: CharArray): Boolean {
        if (isLockedOut()) return false
        val pinHash = secureStorage.getPinHash() ?: return false
        val salt = secureStorage.getSalt() ?: return false
        val success = MessageDigest.isEqual(KeyDerivation.derive(pin, salt), pinHash)
        if (success) resetAttempts() else registerFailure()
        return success
    }

    fun changeMasterPassword(currentPassword: CharArray, newPassword: CharArray): String? {
        val dbKey = openDbKey(currentPassword) ?: return "Current master password is incorrect"

        val validationError = validatePasswordStrength(newPassword)
        if (validationError != null) {
            dbKey.fill(0)
            return validationError
        }

        val salt = secureStorage.getSalt() ?: run {
            dbKey.fill(0)
            return "Vault configuration missing"
        }
        val newDerivedKey = KeyDerivation.derive(newPassword, salt)
        val (passwordWrappedDbKey, passwordWrappedDbIv) = keystoreManager.encryptWithDerivedKey(dbKey, newDerivedKey)
        secureStorage.setWrappedPayload(passwordWrappedDbKey, passwordWrappedDbIv)
        newDerivedKey.fill(0)
        dbKey.fill(0)
        return null
    }

    fun openDbKey(masterPassword: CharArray): ByteArray? {
        val salt = secureStorage.getSalt() ?: return null
        val passwordWrappedDbKey = secureStorage.getPasswordWrappedDbKey() ?: return null
        val passwordWrappedDbIv = secureStorage.getPasswordWrappedDbIv() ?: return null

        val derivedKey = KeyDerivation.derive(masterPassword, salt)
        return try {
            keystoreManager.decryptWithDerivedKey(passwordWrappedDbKey, passwordWrappedDbIv, derivedKey)
        } catch (e: Exception) {
            Log.w(TAG, "Master key unwrap/decrypt failed", e)
            null
        } finally {
            derivedKey.fill(0)
        }
    }

    fun openDbKey(): ByteArray? {
        val wrappedByKeystore = secureStorage.getWrappedDbKey() ?: return null
        val wrappedByKeystoreIv = secureStorage.getWrappedDbIv() ?: return null
        if (wrappedByKeystore.isEmpty() || wrappedByKeystoreIv.isEmpty()) return null
        return runCatching { keystoreManager.unwrap(wrappedByKeystore, wrappedByKeystoreIv) }.getOrNull()
    }

    fun isLockedOut(): Boolean {
        if (failedAttempts < 5) return false
        val elapsed = SystemClock.elapsedRealtime() - lockoutStartMs
        if (elapsed >= 30_000) {
            resetAttempts()
            return false
        }
        return true
    }

    fun lockoutRemainingMs(): Long {
        if (!isLockedOut()) return 0
        return (30_000 - (SystemClock.elapsedRealtime() - lockoutStartMs)).coerceAtLeast(0)
    }

    private fun validatePasswordStrength(password: CharArray): String? {
        if (password.size < 12) return "New password must be at least 12 characters"
        val value = String(password)
        if (value.none(Char::isUpperCase) || value.none(Char::isLowerCase) || value.none(Char::isDigit) || !value.any { !it.isLetterOrDigit() }) {
            return "Use uppercase, lowercase, number, and symbol in new password"
        }
        return null
    }

    private fun registerFailure() {
        failedAttempts += 1
        if (failedAttempts >= 5 && lockoutStartMs == 0L) {
            lockoutStartMs = SystemClock.elapsedRealtime()
        }
    }

    private fun resetAttempts() {
        failedAttempts = 0
        lockoutStartMs = 0L
    }

    companion object {
        private const val TAG = "AuthManager"
    }
}
