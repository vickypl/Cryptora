package com.yourapp.vault.security

import android.os.Debug
import java.net.Socket

object TamperDetection {
    fun isDebuggerAttached(): Boolean = Debug.isDebuggerConnected()

    fun isHookingDetected(): Boolean {
        return detectFridaProcess() || detectXposed() || detectFridaPort()
    }

    private fun detectFridaProcess(): Boolean {
        val suspicious = listOf("frida-server", "frida", "gum-js-loop")
        val process = Runtime.getRuntime().exec("ps")
        val output = process.inputStream.bufferedReader().use { it.readText().lowercase() }
        return suspicious.any { output.contains(it) }
    }

    private fun detectXposed(): Boolean {
        val xposedClasses = listOf(
            "de.robv.android.xposed.XposedBridge",
            "de.robv.android.xposed.XC_MethodHook"
        )
        return xposedClasses.any {
            runCatching {
                Class.forName(it)
                true
            }.getOrDefault(false)
        }
    }

    private fun detectFridaPort(): Boolean {
        val ports = listOf(27042, 27043)
        return ports.any { port ->
            runCatching {
                Socket("127.0.0.1", port).use { true }
            }.getOrDefault(false)
        }
    }
}
