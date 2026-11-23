package com.example.simpleschedule.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.example.simpleschedule.data.local.datastore.dataStore
import java.nio.ByteBuffer
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Keystore-backed AES-GCM storage for credentials with legacy plaintext migration. */
object EncryptedSecretStore {
    private const val PROVIDER = "AndroidKeyStore"
    private const val KEY_ALIAS = "SimpleScheduleSecrets"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"

    private fun key(): SecretKey {
        val store = KeyStore.getInstance(PROVIDER).apply { load(null) }
        (store.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVIDER).apply {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
            )
        }.generateKey()
    }

    private fun encrypt(value: String): String {
        if (value.isEmpty()) return ""
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        return ByteBuffer.allocate(4 + cipher.iv.size + encrypted.size).apply {
            putInt(cipher.iv.size)
            put(cipher.iv)
            put(encrypted)
        }.array().let { Base64.encodeToString(it, Base64.NO_WRAP) }
    }

    private fun decrypt(value: String): String {
        if (value.isEmpty()) return ""
        val buffer = ByteBuffer.wrap(Base64.decode(value, Base64.NO_WRAP))
        require(buffer.remaining() >= Int.SIZE_BYTES) { "Invalid encrypted secret" }
        val ivSize = buffer.int
        require(ivSize > 0 && buffer.remaining() >= ivSize) { "Invalid encrypted secret" }
        val iv = ByteArray(ivSize).also(buffer::get)
        val encrypted = ByteArray(buffer.remaining()).also(buffer::get)
        return Cipher.getInstance(TRANSFORMATION).run {
            init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
            String(doFinal(encrypted), Charsets.UTF_8)
        }
    }

    fun readSync(
        prefs: Preferences,
        encryptedKey: Preferences.Key<String>,
        legacyKey: Preferences.Key<String>
    ): String {
        val encrypted = prefs[encryptedKey]
        if (!encrypted.isNullOrBlank()) return runCatching { decrypt(encrypted) }.getOrDefault("")
        return prefs[legacyKey].orEmpty()
    }

    suspend fun read(
        context: Context,
        encryptedKey: Preferences.Key<String>,
        legacyKey: Preferences.Key<String>
    ): String {
        var result = ""
        context.dataStore.edit { prefs ->
            val encrypted = prefs[encryptedKey]
            if (!encrypted.isNullOrBlank()) {
                result = runCatching { decrypt(encrypted) }.getOrDefault("")
            } else {
                val legacy = prefs[legacyKey].orEmpty()
                if (legacy.isNotEmpty()) {
                    prefs[encryptedKey] = encrypt(legacy)
                    prefs.remove(legacyKey)
                    result = legacy
                }
            }
        }
        return result
    }

    suspend fun write(
        context: Context,
        encryptedKey: Preferences.Key<String>,
        legacyKey: Preferences.Key<String>,
        value: String
    ) {
        context.dataStore.edit { prefs ->
            if (value.isBlank()) prefs.remove(encryptedKey) else prefs[encryptedKey] = encrypt(value)
            prefs.remove(legacyKey)
        }
    }
}
