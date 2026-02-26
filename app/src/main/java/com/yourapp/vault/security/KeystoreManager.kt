package com.yourapp.vault.security

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyInfo
import android.security.keystore.KeyProperties
import android.security.keystore.StrongBoxUnavailableException
import android.util.Log
import java.security.KeyFactory
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class KeystoreManager {
    private val alias = "vault_master_alias"

    private fun getOrCreateKey(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(alias, null) as? SecretKey)?.let {
            logHardwareBacking(it)
            return it
        }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        try {
            keyGenerator.init(buildSpec(useStrongBox = true))
        } catch (e: StrongBoxUnavailableException) {
            Log.w(TAG, "StrongBox unavailable; falling back to hardware-backed AndroidKeyStore", e)
            keyGenerator.init(buildSpec(useStrongBox = false))
        } catch (e: Exception) {
            Log.w(TAG, "StrongBox initialization failed; falling back to hardware-backed AndroidKeyStore", e)
            keyGenerator.init(buildSpec(useStrongBox = false))
        }

        return keyGenerator.generateKey().also(::logHardwareBacking)
    }

    private fun buildSpec(useStrongBox: Boolean): KeyGenParameterSpec {
        val builder = KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .setUserAuthenticationRequired(false)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            builder.setIsStrongBoxBacked(useStrongBox)
        }
        return builder.build()
    }

    private fun logHardwareBacking(secretKey: SecretKey) {
        runCatching {
            val factory = KeyFactory.getInstance(secretKey.algorithm, "AndroidKeyStore")
            val info = factory.getKeySpec(secretKey, KeyInfo::class.java) as KeyInfo
            if (!info.isInsideSecureHardware) {
                Log.w(TAG, "Vault key is not inside secure hardware")
            }
        }.onFailure {
            Log.w(TAG, "Unable to verify key hardware security", it)
        }
    }

    fun wrap(data: ByteArray): Pair<ByteArray, ByteArray> {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        return cipher.doFinal(data) to cipher.iv
    }

    fun unwrap(wrapped: ByteArray, iv: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
        return cipher.doFinal(wrapped)
    }

    fun encryptWithDerivedKey(plain: ByteArray, derivedKey: ByteArray): Pair<ByteArray, ByteArray> {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val keySpec = SecretKeySpec(derivedKey, "AES")
        cipher.init(Cipher.ENCRYPT_MODE, keySpec)
        return cipher.doFinal(plain) to cipher.iv
    }

    fun decryptWithDerivedKey(ciphertext: ByteArray, iv: ByteArray, derivedKey: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val keySpec = SecretKeySpec(derivedKey, "AES")
        cipher.init(Cipher.DECRYPT_MODE, keySpec, GCMParameterSpec(128, iv))
        return cipher.doFinal(ciphertext)
    }

    companion object {
        private const val TAG = "KeystoreManager"
    }
}
