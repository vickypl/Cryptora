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

    fun copyPassword(password: String) {
        clipboardManager.setPrimaryClip(ClipData.newPlainText("password", password))
        CoroutineScope(Dispatchers.Main).launch {
            delay(15_000)
            clipboardManager.clearPrimaryClip()
        }
    }
}
