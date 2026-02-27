package com.yourapp.vault.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyDerivationTest {
    @Test
    fun deriveIsDeterministicForSameInput() {
        val salt = ByteArray(32) { 1 }
        val first = KeyDerivation.derive("master-pass".toCharArray(), salt)
        val second = KeyDerivation.derive("master-pass".toCharArray(), salt)
        assertEquals(first.toList(), second.toList())
    }

    @Test
    fun deriveDiffersForDifferentSalt() {
        val first = KeyDerivation.derive("master-pass".toCharArray(), ByteArray(32) { 1 })
        val second = KeyDerivation.derive("master-pass".toCharArray(), ByteArray(32) { 2 })
        assertFalse(first.contentEquals(second))
    }

    @Test
    fun deriveClearsPasswordInMemory() {
        val password = "master-pass".toCharArray()
        KeyDerivation.derive(password, ByteArray(32) { 1 })
        assertTrue(password.all { it == '\u0000' })
    }
}
