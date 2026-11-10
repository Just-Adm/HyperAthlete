package com.hyperathlete.app.core.auth

import android.accounts.Account
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import com.hyperathlete.app.BuildConfig
import com.hyperathlete.app.core.security.Secret
import com.hyperathlete.app.core.security.SecretStore
import com.hyperathlete.app.data.account.AccountStore
import com.hyperathlete.app.data.profile.ProfileRepository
import com.hyperathlete.app.domain.account.AccountState
import com.hyperathlete.app.domain.account.LinkedAccount
import com.hyperathlete.app.domain.account.Provider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Outcome messages the account / onboarding screens show as a snackbar-like line. */
enum class AuthMessage { GOOGLE_LINKED, DISCORD_LINKED, GOOGLE_SETUP, DISCORD_SETUP, CANCELLED, OFFLINE, FAILED }

/** Google's answer to "may I?": either done, or a consent screen to show first. */
sealed interface GoogleStep {
    data class NeedsConsent(val intent: PendingIntent) : GoogleStep
    data object Done : GoogleStep
    data object Failed : GoogleStep
}

class NeedsSignInException : Exception("Google sign-in needed")

/**
 * Google + Discord accounts linked to the one local profile.
 *
 * Google uses the Identity AuthorizationClient: one consent for name / e-mail / picture and the hidden
 * Drive app folder (drive.appdata). The app never sees the Google password, and the access token is
 * fetched fresh (silently) each time it's needed, so no Google token is stored at all.
 *
 * Discord uses OAuth2 Authorization Code + PKCE in a Custom Tab (public client, no secret).
 */
@Singleton
class AccountRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val store: AccountStore,
    private val secrets: SecretStore,
    private val profiles: ProfileRepository,
    private val http: OkHttpClient,
) {
    val state: Flow<AccountState> = store.state

    private val _messages = MutableSharedFlow<AuthMessage>(extraBufferCapacity = 4)
    val messages: SharedFlow<AuthMessage> = _messages

    private val _busy = MutableStateFlow<Provider?>(null)
    /** Which sign-in is in progress (button shows a spinner). */
    val busy: StateFlow<Provider?> = _busy

    private val json = Json { ignoreUnknownKeys = true }

    /* ------------------------------------------------------------ Google */

    private fun googleRequest(email: String?): AuthorizationRequest = AuthorizationRequest.builder()
        .setRequestedScopes(GOOGLE_SCOPES.map(::Scope))
        .apply { if (email != null) setAccount(Account(email, "com.google")) }
        .build()

    /** Step 1 of linking Google (from a screen). Shows the account chooser / consent via [GoogleStep.NeedsConsent]. */
    suspend fun startGoogle(): GoogleStep {
        _busy.value = Provider.GOOGLE
        return try {
            val result = Identity.getAuthorizationClient(context).authorize(googleRequest(null)).await()
            if (result.hasResolution()) GoogleStep.NeedsConsent(result.pendingIntent!!)
            else { finishGoogle(result); GoogleStep.Done }
        } catch (e: Exception) {
            _busy.value = null
            _messages.tryEmit(e.toMessage(Provider.GOOGLE)); GoogleStep.Failed
        }
    }

    /** Step 2: the consent screen came back. */
    suspend fun completeGoogle(data: Intent?) {
        try {
            // Google returns RESULT_CANCELED both when you back out and when it refuses (e.g. the
            // Cloud project isn't set up); the real reason is inside the intent, so always read it.
            if (data == null) { _messages.tryEmit(AuthMessage.CANCELLED); return }
            val result = Identity.getAuthorizationClient(context).getAuthorizationResultFromIntent(data)
            finishGoogle(result)
        } catch (e: Exception) {
            _messages.tryEmit(e.toMessage(Provider.GOOGLE))
        } finally {
            _busy.value = null
        }
    }

    fun cancelGoogle() { _busy.value = null; _messages.tryEmit(AuthMessage.CANCELLED) }

    private suspend fun finishGoogle(result: AuthorizationResult) {
        val token = result.accessToken ?: throw IllegalStateException("no token")
        val info = withContext(Dispatchers.IO) { getJson("https://openidconnect.googleapis.com/v1/userinfo", token) }
        val email = info.str("email") ?: result.toGoogleSignInAccount()?.email ?: ""
        val account = LinkedAccount(
            Provider.GOOGLE, id = info.str("sub") ?: email, name = info.str("name") ?: email.substringBefore('@'),
            handle = email, avatarUrl = info.str("picture")?.replace(Regex("=s\\d+-c$"), "=s256-c"), linkedAt = Instant.now(),
        )
        store.link(account)
        refreshPhoto()
        _busy.value = null
        _messages.tryEmit(AuthMessage.GOOGLE_LINKED)
    }

    /**
     * A fresh Drive token without any UI (works once the user has agreed). Used by the backup worker.
     * Throws [NeedsSignInException] when Google wants the user to agree again.
     */
    suspend fun googleToken(): String {
        val email = store.current().google?.handle?.takeIf { it.contains('@') } ?: throw NeedsSignInException()
        val result = Identity.getAuthorizationClient(context).authorize(googleRequest(email)).await()
        if (result.hasResolution()) throw NeedsSignInException()
        return result.accessToken ?: throw NeedsSignInException()
    }

    /* ------------------------------------------------------------ Discord */

    private val pkcePrefs by lazy { context.getSharedPreferences("discord_pkce", Context.MODE_PRIVATE) }

    /** Opens Discord's sign-in page in a Custom Tab; the answer arrives in [DiscordCallbackActivity]. */
    fun startDiscord(from: Context, toolbarColor: Int) {
        val verifier = Pkce.verifier()
        val state = Pkce.state()
        pkcePrefs.edit().putString("verifier", verifier).putString("state", state).putLong("at", System.currentTimeMillis()).apply()
        val url = DiscordOAuth.authorizeUrl(BuildConfig.DISCORD_CLIENT_ID, Pkce.challenge(verifier), state)
        _busy.value = Provider.DISCORD
        // Discord app installed → it shows its own "Authorize" screen with the account you're already
        // signed in to, and sends the answer back through the same redirect. No browser login needed.
        val inApp = Intent(Intent.ACTION_VIEW, Uri.parse(url)).setPackage(DISCORD_APP)
            .apply { if (from !is android.app.Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        if (runCatching { from.startActivity(inApp) }.isSuccess) return
        try {
            val tab = CustomTabsIntent.Builder()
                .setDefaultColorSchemeParams(CustomTabColorSchemeParams.Builder().setToolbarColor(toolbarColor).build())
                .setShowTitle(true)
                .build()
            if (from !is android.app.Activity) tab.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            tab.launchUrl(from, Uri.parse(url))
        } catch (e: Exception) {
            // No browser with Custom Tabs: plain browser.
            runCatching { from.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                .onFailure { _busy.value = null; _messages.tryEmit(AuthMessage.FAILED) }
        }
    }

    /** User came back to the app without finishing (closed the tab). */
    fun discordAbandoned() { if (_busy.value == Provider.DISCORD) _busy.value = null }

    suspend fun completeDiscord(uri: Uri) {
        try {
            val error = uri.getQueryParameter("error")
            val code = uri.getQueryParameter("code")
            val state = uri.getQueryParameter("state")
            val verifier = pkcePrefs.getString("verifier", null)
            val expected = pkcePrefs.getString("state", null)
            pkcePrefs.edit().clear().apply()
            when {
                error == "access_denied" -> { _messages.tryEmit(AuthMessage.CANCELLED); return }
                error != null || code == null -> { _messages.tryEmit(AuthMessage.DISCORD_SETUP); return }
                verifier == null || state != expected -> { _messages.tryEmit(AuthMessage.FAILED); return }
            }
            val tokens = withContext(Dispatchers.IO) {
                val body = FormBody.Builder()
                    .add("client_id", BuildConfig.DISCORD_CLIENT_ID)
                    .add("grant_type", "authorization_code")
                    .add("code", code!!)
                    .add("redirect_uri", DiscordOAuth.redirectUri(BuildConfig.DISCORD_CLIENT_ID))
                    .add("code_verifier", verifier!!)
                    .build()
                http.newCall(Request.Builder().url(DiscordOAuth.TOKEN).post(body).build()).execute().use { r ->
                    val text = r.body.string()
                    // 401 invalid_client = "Public Client" isn't switched on in the developer portal.
                    if (!r.isSuccessful) throw DiscordSetupException(text)
                    json.parseToJsonElement(text).jsonObject
                }
            }
            val access = tokens.str("access_token") ?: throw DiscordSetupException("no token")
            secrets.set(Secret.DISCORD_ACCESS_TOKEN, access)
            secrets.set(Secret.DISCORD_REFRESH_TOKEN, tokens.str("refresh_token"))
            val me = withContext(Dispatchers.IO) { getJson(DiscordOAuth.ME, access) }
            val id = me.str("id") ?: throw IllegalStateException("no id")
            val username = me.str("username").orEmpty()
            store.link(
                LinkedAccount(
                    Provider.DISCORD, id, name = me.str("global_name") ?: username, handle = "@$username",
                    avatarUrl = DiscordOAuth.avatarUrl(id, me.str("avatar"), me.str("discriminator")), linkedAt = Instant.now(),
                ),
            )
            refreshPhoto()
            _messages.tryEmit(AuthMessage.DISCORD_LINKED)
        } catch (e: Exception) {
            _messages.tryEmit(e.toMessage(Provider.DISCORD))
        } finally {
            _busy.value = null
        }
    }

    /* ------------------------------------------------------------ Shared */

    suspend fun unlink(provider: Provider) {
        if (provider == Provider.DISCORD) {
            secrets.set(Secret.DISCORD_ACCESS_TOKEN, null)
            secrets.set(Secret.DISCORD_REFRESH_TOKEN, null)
        } else {
            // Ask Google to forget the Drive permission too (best effort; unlinking works offline).
            runCatching {
                val email = store.current().google?.handle
                if (email != null) {
                    Identity.getAuthorizationClient(context).revokeAccess(
                        com.google.android.gms.auth.api.identity.RevokeAccessRequest.builder()
                            .setAccount(Account(email, "com.google"))
                            .setScopes(GOOGLE_SCOPES.map(::Scope))
                            .build(),
                    ).await()
                }
            }
        }
        store.unlink(provider)
        refreshPhoto()
    }

    suspend fun setPhotoFrom(provider: Provider?) {
        store.setPhotoFrom(provider)
        refreshPhoto()
    }

    /** Puts the chosen account's picture into the profile (kept as a local file so it works offline). */
    suspend fun refreshPhoto() {
        val s = store.current()
        val account = s.photoFrom?.let { s.of(it) }
        val file = File(context.filesDir, "avatar.png")
        val path = account?.avatarUrl?.let { url ->
            withContext(Dispatchers.IO) {
                runCatching {
                    http.newCall(Request.Builder().url(url).build()).execute().use { r ->
                        if (!r.isSuccessful) return@runCatching null
                        val bytes = r.body.bytes()
                        if (BitmapFactory.decodeByteArray(bytes, 0, bytes.size) == null) return@runCatching null
                        file.writeBytes(bytes)
                        file.absolutePath + "?v=" + System.currentTimeMillis()
                    }
                }.getOrNull()
            }
        }
        if (account == null) runCatching { file.delete() }
        // Offline: keep the picture we already have.
        if (path != null || account == null) profiles.update { it.copy(photoUri = path) }
    }

    private fun getJson(url: String, bearer: String): JsonObject =
        http.newCall(Request.Builder().url(url).header("Authorization", "Bearer $bearer").build()).execute().use { r ->
            if (!r.isSuccessful) throw java.io.IOException("HTTP ${r.code}")
            json.parseToJsonElement(r.body.string()).jsonObject
        }

    private fun JsonObject.str(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull

    /** Google's status code behind the last failure (shown with the message so problems can be told apart). */
    @Volatile var lastGoogleCode: Int? = null
        private set

    private fun Exception.toMessage(p: Provider): AuthMessage = when {
        this is java.io.IOException -> AuthMessage.OFFLINE
        this is DiscordSetupException -> AuthMessage.DISCORD_SETUP
        // 16 = CANCELED, 12501 = SIGN_IN_CANCELLED: the user backed out.
        this is ApiException && (statusCode == CommonStatusCodes.CANCELED || statusCode == 12501) -> AuthMessage.CANCELLED.also { lastGoogleCode = statusCode }
        // DEVELOPER_ERROR (10) / SIGN_IN_REQUIRED etc.: the Google Cloud project isn't set up for this app yet.
        this is ApiException -> AuthMessage.GOOGLE_SETUP.also { lastGoogleCode = statusCode }
        p == Provider.DISCORD -> AuthMessage.FAILED
        else -> AuthMessage.GOOGLE_SETUP
    }

    private class DiscordSetupException(msg: String) : Exception(msg)

    companion object {
        const val DISCORD_APP = "com.discord"
        const val DRIVE_APPDATA = "https://www.googleapis.com/auth/drive.appdata"
        val GOOGLE_SCOPES = listOf("openid", "email", "profile", DRIVE_APPDATA)
    }
}

/** Play services Task → coroutine, without an extra library. */
suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { cont.resume(it) }
    addOnFailureListener { cont.resumeWithException(it) }
    addOnCanceledListener { cont.cancel() }
}
