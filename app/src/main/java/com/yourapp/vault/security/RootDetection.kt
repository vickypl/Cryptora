package com.yourapp.vault.security

import android.os.Build
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

    fun isRooted(): Boolean {
        val checks = listOf(
            ::hasSuBinary,
            ::canExecuteWhichSu,
            ::hasTestKeys,
            ::hasDangerousSystemProperties,
            ::hasWritableSystemPartition
        )
        return checks.any { runCatching { it() }.getOrDefault(false) }
    }

    private fun hasSuBinary(): Boolean = suspiciousPaths.any { File(it).exists() }

    private fun canExecuteWhichSu(): Boolean {
        val process = Runtime.getRuntime().exec(arrayOf("/system/xbin/which", "su"))
        return process.inputStream.bufferedReader().use { it.readLine() != null }
    }

    private fun hasTestKeys(): Boolean = Build.TAGS?.contains("test-keys") == true

    private fun hasDangerousSystemProperties(): Boolean {
        val dangerousProps = mapOf(
            "ro.debuggable" to "1",
            "ro.secure" to "0"
        )

        val process = Runtime.getRuntime().exec("getprop")
        val output = process.inputStream.bufferedReader().use { it.readText() }
        return dangerousProps.any { (prop, value) ->
            output.contains("[$prop]") && output.contains("[$value]")
        }
    }

    private fun hasWritableSystemPartition(): Boolean {
        val paths = listOf("/system", "/system/bin", "/system/sbin", "/system/xbin", "/vendor/bin")
        return paths.any { path -> File(path).canWrite() }
    }
}
