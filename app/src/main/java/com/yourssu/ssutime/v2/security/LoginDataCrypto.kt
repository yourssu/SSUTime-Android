package com.yourssu.ssutime.v2.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private const val ANDROID_KEY_STORE = "AndroidKeyStore"
private const val KEY_ALIAS = "ssutime_login_data_aes_gcm"
private const val TRANSFORMATION = "AES/GCM/NoPadding"
private const val GCM_TAG_LENGTH_BITS = 128
private const val IV_LENGTH_BYTES = 12
private const val ENCRYPTED_PREFIX = "SSUTIME_LOGIN_V1:"

internal object LoginDataCrypto {
    fun isEncrypted(value: String): Boolean =
        value.startsWith(ENCRYPTED_PREFIX)

    fun encrypt(plainText: ByteArray): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())

        val iv = cipher.iv
        val cipherText = cipher.doFinal(plainText)
        val payload = ByteArray(iv.size + cipherText.size)
        iv.copyInto(payload, destinationOffset = 0)
        cipherText.copyInto(payload, destinationOffset = iv.size)

        return ENCRYPTED_PREFIX + Base64.getEncoder().encodeToString(payload)
    }

    fun decrypt(encryptedValue: String): ByteArray {
        val payload = Base64.getDecoder().decode(encryptedValue.removePrefix(ENCRYPTED_PREFIX))
        require(payload.size > IV_LENGTH_BYTES) { "암호화된 계정 정보 형식이 올바르지 않습니다." }

        val iv = payload.copyOfRange(0, IV_LENGTH_BYTES)
        val cipherText = payload.copyOfRange(IV_LENGTH_BYTES, payload.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            getOrCreateSecretKey(),
            GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv),
        )

        return cipher.doFinal(cipherText)
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply {
            load(null)
        }
        val existingKey = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existingKey != null) {
            return existingKey
        }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEY_STORE,
        )
        val keySpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(true)
            .build()

        keyGenerator.init(keySpec)
        return keyGenerator.generateKey()
    }
}
