package com.yourapp.vault.security

import android.content.Context
import android.net.Uri
import android.util.Base64
import androidx.documentfile.provider.DocumentFile
import com.yourapp.vault.domain.model.Credential
import org.json.JSONArray
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class VaultBackupManager(private val context: Context) {
    fun vaultExists(directoryUri: Uri): Boolean {
        val dir = DocumentFile.fromTreeUri(context, directoryUri) ?: return false
        return dir.findFile(VAULT_FILE_NAME)?.exists() == true
    }

    fun writeVault(directoryUri: Uri, credentials: List<Credential>, masterPassword: CharArray): Result<Unit> {
        return runCatching {
            val dir = DocumentFile.fromTreeUri(context, directoryUri) ?: error("Unable to access selected directory")
            val payload = encryptPayload(credentialsToJson(credentials), masterPassword)

            val file = dir.findFile(VAULT_FILE_NAME)
                ?: dir.createFile("application/octet-stream", VAULT_FILE_NAME)
                ?: error("Unable to create vault file")

            context.contentResolver.openOutputStream(file.uri, "wt")?.use { stream ->
                stream.write(payload.toByteArray(StandardCharsets.UTF_8))
                stream.flush()
            } ?: error("Unable to write vault file")
        }
    }

    fun restoreVault(directoryUri: Uri, masterPassword: CharArray): Result<List<Credential>> {
        return runCatching {
            val dir = DocumentFile.fromTreeUri(context, directoryUri) ?: error("Unable to access selected directory")
            val file = dir.findFile(VAULT_FILE_NAME) ?: error("Vault file not found")
            val content = context.contentResolver.openInputStream(file.uri)?.use { stream ->
                stream.readBytes().toString(StandardCharsets.UTF_8)
            } ?: error("Unable to read vault file")
            val decryptedJson = decryptPayload(content, masterPassword)
            jsonToCredentials(decryptedJson)
        }
    }

    private fun encryptPayload(plainJson: String, masterPassword: CharArray): String {
        val salt = KeyDerivation.randomSalt(32)
        val keyMaterial = KeyDerivation.derive(masterPassword, salt)
        val encryptionKey = keyMaterial.copyOfRange(0, 16) + keyMaterial.copyOfRange(16, 32)
        val hmacKey = MessageDigest.getInstance("SHA-256").digest(keyMaterial + "hmac".toByteArray(StandardCharsets.UTF_8))
        keyMaterial.fill(0)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val iv = KeyDerivation.randomSalt(12)
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(encryptionKey, "AES"), GCMParameterSpec(128, iv))
        val ciphertext = cipher.doFinal(plainJson.toByteArray(StandardCharsets.UTF_8))

        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(hmacKey, "HmacSHA256"))
        val hmac = mac.doFinal(iv + ciphertext)

        return JSONObject()
            .put("version", 1)
            .put("salt", salt.toB64())
            .put("iv", iv.toB64())
            .put("ciphertext", ciphertext.toB64())
            .put("hmac", hmac.toB64())
            .toString()
    }

    private fun decryptPayload(payload: String, masterPassword: CharArray): String {
        val json = JSONObject(payload)
        val salt = json.getString("salt").fromB64()
        val iv = json.getString("iv").fromB64()
        val ciphertext = json.getString("ciphertext").fromB64()
        val expectedHmac = json.getString("hmac").fromB64()

        val keyMaterial = KeyDerivation.derive(masterPassword, salt)
        val encryptionKey = keyMaterial.copyOfRange(0, 16) + keyMaterial.copyOfRange(16, 32)
        val hmacKey = MessageDigest.getInstance("SHA-256").digest(keyMaterial + "hmac".toByteArray(StandardCharsets.UTF_8))
        keyMaterial.fill(0)

        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(hmacKey, "HmacSHA256"))
        val actualHmac = mac.doFinal(iv + ciphertext)
        if (!MessageDigest.isEqual(expectedHmac, actualHmac)) {
            error("Invalid Master Password or Corrupted Vault File.")
        }

        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(encryptionKey, "AES"), GCMParameterSpec(128, iv))
            String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8)
        } catch (_: Exception) {
            error("Invalid Master Password or Corrupted Vault File.")
        }
    }

    private fun credentialsToJson(credentials: List<Credential>): String {
        val jsonArray = JSONArray()
        credentials.forEach { credential ->
            jsonArray.put(
                JSONObject()
                    .put("id", credential.id)
                    .put("title", credential.title)
                    .put("username", credential.username)
                    .put("password", credential.password)
                    .put("url", credential.url)
                    .put("notes", credential.notes)
                    .put("category", credential.category)
                    .put("createdAt", credential.createdAt)
                    .put("updatedAt", credential.updatedAt)
            )
        }
        return JSONObject().put("credentials", jsonArray).toString()
    }

    private fun jsonToCredentials(value: String): List<Credential> {
        val root = JSONObject(value)
        val array = root.optJSONArray("credentials") ?: JSONArray()
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(
                    Credential(
                        id = item.getString("id"),
                        title = item.getString("title"),
                        username = item.getString("username"),
                        password = item.getString("password"),
                        url = item.optString("url", null),
                        notes = item.optString("notes", null),
                        category = item.getString("category"),
                        createdAt = item.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = item.optLong("updatedAt", System.currentTimeMillis())
                    )
                )
            }
        }
    }

    private fun ByteArray.toB64(): String = Base64.encodeToString(this, Base64.NO_WRAP)
    private fun String.fromB64(): ByteArray = Base64.decode(this, Base64.NO_WRAP)

    companion object {
        const val VAULT_FILE_NAME = "vault.enc"
    }
}
