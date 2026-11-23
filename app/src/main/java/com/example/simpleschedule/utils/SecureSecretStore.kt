package com.example.simpleschedule.utils

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.example.simpleschedule.data.local.datastore.dataStore
import com.example.simpleschedule.data.local.datastore.dataStore
import java.nio.ByteBuffer
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Keystore-backed storage for credentials; plaintext legacy values are migrated on first read. */
object SecureSecretStore {
    private const val KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "SimpleScheduleSecrets"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"

    private fun key(): SecretKey {
        val store = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (store.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE).apply {
            init(KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build())
        }.generateKey()
    }

    private fun encrypt(value: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val payload = ByteBuffer.allocate(4 + cipher.iv.size + cipher.doFinal(value.toByteArray(Charsets.UTF_8)).size)
        val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        payload.putInt(cipher.iv.size).put(cipher.iv).put(encrypted)
        return Base64.encodeToString(payload.array(), Base64.NO_WRAP)
    }

    private fun decrypt(value: String): String {
        val bytes = Base64.decode(value, Base64.DEFAULT)
        val buffer = ByteBuffer.wrap(bytes)
        val iv = ByteArray(buffer.int).also(buffer::get)
        val encrypted = ByteArray(buffer.remaining()).also(buffer::get)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
        return String(cipher.doFinal(encrypted), Charsets.UTF_8)
    }

    fun readSync(prefs: androidx.datastore.preferences.core.Preferences, encryptedKey: androidx.datastore.preferences.core.Preferences.Key<String>, legacyKey: androidx.datastore.preferences.core.Preferences.Key<String>): String {
        val encrypted = prefs[encryptedKey]
        if (!encrypted.isNullOrBlank()) return runCatching { decrypt(encrypted) }.getOrDefault("")
        return prefs[legacyKey].orEmpty()
    }

    suspend fun read(context: Context, encryptedKey: androidx.datastore.preferences.core.Preferences.Key<String>, legacyKey: androidx.datastore.preferences.core.Preferences.Key<String>): String {
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

    suspend fun write(context: Context, encryptedKey: androidx.datastore.preferences.core.Preferences.Key<String>, legacyKey: androidx.datastore.preferences.core.Preferences.Key<String>, value: String) {
        context.dataStore.edit { prefs ->
            if (value.isBlank()) prefs.remove(encryptedKey) else prefs[encryptedKey] = encrypt(value)
            prefs.remove(legacyKey)
        }
    }
}
