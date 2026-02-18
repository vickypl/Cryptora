package com.yourapp.vault.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordGeneratorTest {
    @Test
    fun generatesExpectedLength() {
        val pwd = PasswordGenerator.generate(16, upper = true, lower = true, digits = true, symbols = false)
        assertEquals(16, pwd.length)
        assertTrue(pwd.any { it.isUpperCase() || it.isLowerCase() || it.isDigit() })
    }
}
