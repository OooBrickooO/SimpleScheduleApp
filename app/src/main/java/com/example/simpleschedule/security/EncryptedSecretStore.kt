package com.example.simpleschedule.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.ByteBuffer
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.SecretKey

object EncryptedSecretStore {
    private const val provider = "AndroidKeyStore"
    private const val alias = "SimpleScheduleSecrets"
    private const val transformation = "AES/GCM/NoPadding"

    private fun key(): SecretKey {
        val store = KeyStore.getInstance(provider).apply { load(null) }
        (store.getKey(alias, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, provider).apply {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build())
        }.generateKey()
    }

    fun encrypt(value: String): String {
        if (value.isEmpty()) return ""
        val cipher = Cipher.getInstance(transformation)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        return ByteBuffer.allocate(4 + cipher.iv.size + encrypted.size).apply {
            putInt(cipher.iv.size).put(cipher.iv).put(encrypted)
        }.array().let { Base64.encodeToString(it, Base64.NO_WRAP) }
    }

    fun decrypt(value: String): String? {
        if (value.isEmpty()) return ""
        return try {
            val buffer = ByteBuffer.wrap(Base64.decode(value, Base64.NO_WRAP))
            val iv = ByteArray(buffer.int).also(buffer::get)
            val encrypted = ByteArray(buffer.remaining()).also(buffer::get)
            Cipher.getInstance(transformation).run {
                init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
                String(doFinal(encrypted), Charsets.UTF_8)
            }
        } catch (_: Exception) {
            null
        }
    }
}
