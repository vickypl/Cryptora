package com.yourapp.vault.security

import java.io.File

object RootDetection {
    private val suspiciousPaths = listOf(
        "/system/app/Superuser.apk",
        "/sbin/su",
        "/system/bin/su",
        "/system/xbin/su",
        "/data/local/xbin/su",
        "/data/local/bin/su",
        "/system/sd/xbin/su"
    )

    fun isRooted(): Boolean = suspiciousPaths.any { File(it).exists() }
}
