package com.yourapp.vault.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SecureClipboard(context: Context) {
    private val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    fun copyPassword(password: String) = copySensitive(label = "password", value = password)

    fun copyUsernameOrEmail(usernameOrEmail: String) = copySensitive(label = "username_or_email", value = usernameOrEmail)

    private fun copySensitive(label: String, value: String) {
        clipboardManager.setPrimaryClip(ClipData.newPlainText(label, value))
        CoroutineScope(Dispatchers.Main).launch {
            delay(15_000)
            clipboardManager.clearPrimaryClip()
        }
    }
}
