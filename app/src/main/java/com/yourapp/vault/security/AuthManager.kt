package com.yourapp.vault.security

import android.os.SystemClock
import java.security.MessageDigest

class AuthManager(
    private val secureStorage: SecureStorage,
    private val keystoreManager: KeystoreManager
) {
    private var failedAttempts = 0
    private var lockoutStartMs = 0L

    fun createVault(masterPassword: CharArray, pin: CharArray?): ByteArray {
        val salt = KeyDerivation.randomSalt(32)
        val masterHash = KeyDerivation.derive(masterPassword, salt)
        val dbKey = KeyDerivation.randomSalt(32)
        val (wrapped, iv) = keystoreManager.wrap(dbKey)
        secureStorage.saveSetup(masterHash, salt, wrapped, iv)
        pin?.let { secureStorage.setPinHash(KeyDerivation.derive(it, salt)) }
        return dbKey
    }

    fun verifyMasterPassword(password: CharArray): Boolean {
        if (isLockedOut()) return false
        val salt = secureStorage.getSalt() ?: return false
        val expectedHash = secureStorage.getMasterHash() ?: return false
        val actual = KeyDerivation.derive(password, salt)
        val success = MessageDigest.isEqual(actual, expectedHash)
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
        if (!verifyMasterPassword(currentPassword)) {
            return "Current master password is incorrect"
        }

        val validationError = validatePasswordStrength(newPassword)
        if (validationError != null) {
            return validationError
        }

        val salt = secureStorage.getSalt() ?: return "Vault configuration missing"
        secureStorage.setMasterHash(KeyDerivation.derive(newPassword, salt))
        return null
    }

    fun openDbKey(): ByteArray? {
        val wrapped = secureStorage.getWrappedDbKey() ?: return null
        val iv = secureStorage.getWrappedDbIv() ?: return null
        return keystoreManager.unwrap(wrapped, iv)
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
}
