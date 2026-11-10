package com.hyperathlete.app.core.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/** Names of the secrets the app keeps (the Discord sign-in tokens). */
enum class Secret(val prefKey: String) {
    DISCORD_ACCESS_TOKEN("discord_access"), DISCORD_REFRESH_TOKEN("discord_refresh"),
}

/**
 * Sign-in tokens encrypted with an AES-256-GCM key that never leaves the Android Keystore.
 * Only the ciphertext is written to disk; it is useless on another device or after a reinstall.
 */
@Singleton
class SecretStore @Inject constructor(@ApplicationContext private val context: Context) {

    private val prefs by lazy { context.getSharedPreferences("secrets", Context.MODE_PRIVATE) }
    private val _present = MutableStateFlow<Set<Secret>>(emptySet())
    /** Which secrets are set (never the values themselves) — for UI status. */
    val present: StateFlow<Set<Secret>> = _present.asStateFlow()

    init {
        // Keys of features that were removed (Azure voices, Claude, Gemini): don't keep them on the phone.
        runCatching { prefs.edit().remove("azure_speech_key").remove("claude_api_key").remove("gemini_api_key").apply() }
        _present.value = runCatching { Secret.entries.filter { prefs.contains(it.prefKey) }.toSet() }.getOrDefault(emptySet())
    }

    fun get(secret: Secret): String? = runCatching {
        val stored = prefs.getString(secret.prefKey, null) ?: return null
        val bytes = Base64.decode(stored, Base64.NO_WRAP)
        val iv = bytes.copyOfRange(0, IV_SIZE)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
        String(cipher.doFinal(bytes, IV_SIZE, bytes.size - IV_SIZE), Charsets.UTF_8)
    }.getOrNull()

    fun set(secret: Secret, value: String?) {
        val clean = value?.trim().orEmpty()
        if (clean.isEmpty()) {
            prefs.edit().remove(secret.prefKey).apply()
        } else {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key())
            val out = cipher.iv + cipher.doFinal(clean.toByteArray(Charsets.UTF_8))
            prefs.edit().putString(secret.prefKey, Base64.encodeToString(out, Base64.NO_WRAP)).apply()
        }
        _present.value = Secret.entries.filter { prefs.contains(it.prefKey) }.toSet()
    }

    private fun key(): SecretKey {
        val ks = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (ks.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        gen.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return gen.generateKey()
    }

    private companion object {
        const val KEYSTORE = "AndroidKeyStore"
        const val ALIAS = "hyper_secrets_v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_SIZE = 12
    }
}
