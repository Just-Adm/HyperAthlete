package com.hyperathlete.app.core.auth

import java.net.URLEncoder
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

/**
 * OAuth 2.0 PKCE (RFC 7636) pieces. With PKCE + Discord's "Public Client" switch the app signs in
 * without any client secret, so nothing secret is shipped inside the APK.
 */
object Pkce {
    private val random = SecureRandom()
    private val b64 = Base64.getUrlEncoder().withoutPadding()

    /** 64 random bytes → 86 URL-safe characters (allowed range is 43–128). */
    fun verifier(): String = ByteArray(64).also(random::nextBytes).let(b64::encodeToString)

    fun state(): String = ByteArray(16).also(random::nextBytes).let(b64::encodeToString)

    /** S256 challenge = BASE64URL(SHA-256(ASCII(verifier))). */
    fun challenge(verifier: String): String =
        b64.encodeToString(MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII)))

    fun query(params: List<Pair<String, String>>): String =
        params.joinToString("&") { (k, v) -> "$k=" + URLEncoder.encode(v, "UTF-8").replace("+", "%20") }
}

/** Discord endpoints and URLs (pure, unit-tested). */
object DiscordOAuth {
    const val AUTHORIZE = "https://discord.com/oauth2/authorize"
    const val TOKEN = "https://discord.com/api/v10/oauth2/token"
    const val ME = "https://discord.com/api/v10/users/@me"
    const val SCOPE = "identify"

    /** Registered in the Discord developer portal → OAuth2 → Redirects. */
    fun redirectUri(clientId: String) = "discord-$clientId:/authorize/callback"

    fun authorizeUrl(clientId: String, challenge: String, state: String): String = AUTHORIZE + "?" + Pkce.query(
        listOf(
            "response_type" to "code",
            "client_id" to clientId,
            "scope" to SCOPE,
            "redirect_uri" to redirectUri(clientId),
            "code_challenge" to challenge,
            "code_challenge_method" to "S256",
            "state" to state,
            "prompt" to "none",
        ),
    )

    fun avatarUrl(id: String, avatar: String?, discriminator: String?): String =
        if (avatar != null) {
            // PNG even for animated avatars: a still picture is all the profile needs.
            "https://cdn.discordapp.com/avatars/$id/$avatar.png?size=256"
        } else {
            // Default avatar: new usernames use (id >> 22) % 6, legacy ones discriminator % 5.
            val index = if (discriminator.isNullOrBlank() || discriminator == "0") ((id.toLongOrNull() ?: 0L) shr 22) % 6 else (discriminator.toIntOrNull() ?: 0) % 5L
            "https://cdn.discordapp.com/embed/avatars/$index.png"
        }
}
