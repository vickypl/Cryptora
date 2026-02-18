package com.yourapp.vault.security

import java.security.SecureRandom

object PasswordGenerator {
    fun generate(length: Int, upper: Boolean, lower: Boolean, digits: Boolean, symbols: Boolean): String {
        require(length in 8..32)
        val charset = buildString {
            if (upper) append("ABCDEFGHIJKLMNOPQRSTUVWXYZ")
            if (lower) append("abcdefghijklmnopqrstuvwxyz")
            if (digits) append("0123456789")
            if (symbols) append("!@#$%^&*()-_=+[]{};:,.<>?")
        }
        require(charset.isNotEmpty())

        val random = SecureRandom()
        return (1..length)
            .map { charset[random.nextInt(charset.length)] }
            .joinToString("")
    }
}
