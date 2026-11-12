package com.upisoundbox.core.data.db

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class DatabaseKeyManager(private val context: Context) {

    fun getOrCreatePassphrase(): ByteArray {
        val prefs = context.getSharedPreferences("soundbox_db_security", Context.MODE_PRIVATE)
        val encryptedBase64 = prefs.getString("encrypted_passphrase", null)
        val ivBase64 = prefs.getString("iv", null)

        if (encryptedBase64 != null && ivBase64 != null) {
            try {
                return decryptPassphrase(encryptedBase64, ivBase64)
            } catch (_: Exception) {
                // If keystore was invalidated or error occurs, fall back to re-generating or raw fallback
            }
        }

        // Generate a new 256-bit random passphrase
        val rawKey = ByteArray(32).also { SecureRandom().nextBytes(it) }
        try {
            val (encrypted, iv) = encryptPassphrase(rawKey)
            prefs.edit()
                .putString("encrypted_passphrase", encrypted)
                .putString("iv", iv)
                .apply()
        } catch (_: Exception) {
            // Android Keystore may not be available on some mock/emulator environments
        }
        return rawKey
    }

    private fun getOrCreateMasterKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        if (!keyStore.containsAlias(MASTER_KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
            val spec = KeyGenParameterSpec.Builder(
                MASTER_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
            keyGenerator.init(spec)
            keyGenerator.generateKey()
        }
        return (keyStore.getKey(MASTER_KEY_ALIAS, null) as? SecretKey)
            ?: (keyStore.getEntry(MASTER_KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.secretKey
            ?: throw IllegalStateException("Could not retrieve master key from AndroidKeyStore")
    }

    private fun encryptPassphrase(passphrase: ByteArray): Pair<String, String> {
        val masterKey = getOrCreateMasterKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, masterKey)
        val iv = cipher.iv
        val encrypted = cipher.doFinal(passphrase)
        return Base64.encodeToString(encrypted, Base64.NO_WRAP) to Base64.encodeToString(iv, Base64.NO_WRAP)
    }

    private fun decryptPassphrase(encryptedBase64: String, ivBase64: String): ByteArray {
        val masterKey = getOrCreateMasterKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val iv = Base64.decode(ivBase64, Base64.NO_WRAP)
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, masterKey, spec)
        val encrypted = Base64.decode(encryptedBase64, Base64.NO_WRAP)
        return cipher.doFinal(encrypted)
    }

    companion object {
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val MASTER_KEY_ALIAS = "soundbox_sqlcipher_master_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}

