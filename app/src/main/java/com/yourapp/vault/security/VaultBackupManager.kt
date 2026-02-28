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
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class VaultBackupManager(private val context: Context) {
    fun vaultExists(directoryUri: Uri): Boolean {
        val dir = DocumentFile.fromTreeUri(context, directoryUri) ?: return false
        return findVaultFile(dir)?.exists() == true
    }

    fun writeVault(directoryUri: Uri, credentials: List<Credential>, masterPassword: CharArray): Result<Unit> {
        return runCatching {
            val dir = DocumentFile.fromTreeUri(context, directoryUri) ?: error("Unable to access selected directory")
            val payload = encryptPayload(credentialsToJson(credentials), masterPassword)

            val file = findVaultFile(dir)
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
            val file = findVaultFile(dir) ?: error("Vault file not found")
            val content = context.contentResolver.openInputStream(file.uri)?.use { stream ->
                stream.readBytes().toString(StandardCharsets.UTF_8)
            } ?: error("Unable to read vault file")
            val decryptedJson = decryptPayload(content, masterPassword)
            jsonToCredentials(decryptedJson)
        }
    }

    private fun encryptPayload(plainJson: String, masterPassword: CharArray): String {
        val salt = KeyDerivation.randomSalt(32)
        val keyMaterial = deriveWithIterations(masterPassword, salt, CURRENT_ITERATIONS)
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
            .put("kdfIterations", CURRENT_ITERATIONS)
            .put("iv", iv.toB64())
            .put("ciphertext", ciphertext.toB64())
            .put("hmac", hmac.toB64())
            .toString()
    }

    private fun decryptPayload(payload: String, masterPassword: CharArray): String {
        val json = JSONObject(payload.trim())
        val salt = json.getString("salt").fromB64()
        val iv = json.getString("iv").fromB64()
        val ciphertext = json.getString("ciphertext").fromB64()
        val expectedHmac = json.getString("hmac").fromB64()

        val configuredIterations = json.optInt("kdfIterations", CURRENT_ITERATIONS)
        val attempts = linkedSetOf(configuredIterations, CURRENT_ITERATIONS).apply {
            LEGACY_ITERATIONS.forEach { add(it) }
        }

        attempts.forEach { iterations ->
            val keyMaterial = deriveWithIterations(masterPassword.copyOf(), salt, iterations)
            val encryptionKey = keyMaterial.copyOfRange(0, 16) + keyMaterial.copyOfRange(16, 32)
            val hmacKey = MessageDigest.getInstance("SHA-256").digest(keyMaterial + "hmac".toByteArray(StandardCharsets.UTF_8))
            keyMaterial.fill(0)

            val mac = Mac.getInstance("HmacSHA256")
            mac.init(SecretKeySpec(hmacKey, "HmacSHA256"))
            val actualHmac = mac.doFinal(iv + ciphertext)
            if (!MessageDigest.isEqual(expectedHmac, actualHmac)) {
                return@forEach
            }

            try {
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(encryptionKey, "AES"), GCMParameterSpec(128, iv))
                return String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8)
            } catch (_: Exception) {
                return@forEach
            }
        }

        error("Invalid Master Password or Corrupted Vault File.")
    }

    private fun deriveWithIterations(password: CharArray, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(password, salt, iterations, PBKDF2_KEY_LENGTH)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
            password.fill('\u0000')
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
        val trimmed = value.trim()
        val array = if (trimmed.startsWith("[")) {
            JSONArray(trimmed)
        } else {
            JSONObject(trimmed).optJSONArray("credentials") ?: JSONArray()
        }

        return buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(
                    Credential(
                        id = item.optString("id").ifBlank { UUID.randomUUID().toString() },
                        title = item.firstNonBlank("title", "site", "name").ifBlank { "Untitled" },
                        username = item.firstNonBlank("username", "email", "user"),
                        password = item.firstNonBlank("password", "pass"),
                        url = item.optString("url", null),
                        notes = item.optString("notes", null),
                        category = item.optString("category").ifBlank { "General" },
                        createdAt = item.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = item.optLong("updatedAt", System.currentTimeMillis())
                    )
                )
            }
        }
    }

    private fun JSONObject.firstNonBlank(vararg keys: String): String {
        keys.forEach { key ->
            val value = optString(key)
            if (value.isNotBlank()) {
                return value
            }
        }
        return ""
    }

    private fun findVaultFile(directory: DocumentFile): DocumentFile? {
        directory.findFile(VAULT_FILE_NAME)?.let { return it }
        return directory.listFiles().firstOrNull { file ->
            val name = file.name ?: return@firstOrNull false
            name.equals(VAULT_FILE_NAME, ignoreCase = true) ||
                name.startsWith("vault", ignoreCase = true) && name.endsWith(".enc", ignoreCase = true)
        }
    }

    private fun ByteArray.toB64(): String = Base64.encodeToString(this, Base64.NO_WRAP)
    private fun String.fromB64(): ByteArray = Base64.decode(this, Base64.NO_WRAP)

    companion object {
        const val VAULT_FILE_NAME = "vault.enc"
        private const val PBKDF2_KEY_LENGTH = 256
        private const val CURRENT_ITERATIONS = 300_000
        private val LEGACY_ITERATIONS = intArrayOf(210_000)
    }
}
