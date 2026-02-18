package com.yourapp.vault.security

import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object KeyDerivation {
    private const val KEY_LENGTH = 256
    private const val ITERATIONS = 210_000

    fun randomSalt(size: Int = 32): ByteArray = ByteArray(size).also {
        SecureRandom().nextBytes(it)
    }

    fun derive(password: CharArray, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
    }
}
