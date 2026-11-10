package com.hyperathlete.app.core.update

import com.hyperathlete.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** `downloads/version.json`: what the newest published APK is and where to get it. */
@Serializable
data class UpdateManifest(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val sha256: String = "",
    val releaseNotes: ReleaseNotes = ReleaseNotes(),
    val minSdk: Int = 26,
)

@Serializable
data class ReleaseNotes(
    @SerialName("en") val en: String = "",
    @SerialName("ar") val ar: String = "",
)

sealed interface UpdateResult {
    data class Available(val manifest: UpdateManifest) : UpdateResult
    data object UpToDate : UpdateResult
    /** A newer version exists but needs a newer Android than this phone has. */
    data class Incompatible(val manifest: UpdateManifest) : UpdateResult
    /** Offline, timed out, server error or a bad file: never shown as an error, only as "couldn't check". */
    data object Unavailable : UpdateResult
}

/**
 * Checks [BuildConfig.UPDATE_MANIFEST_URL] for a newer version. Short timeouts, no retries, never throws,
 * and runs off the main thread, so a slow or missing connection can't block or crash anything. It only
 * reports; opening the download is the user's choice (the APK is never installed by the app).
 */
@Singleton
class UpdateChecker @Inject constructor(baseClient: OkHttpClient) {

    private val client = baseClient.newBuilder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .callTimeout(10, TimeUnit.SECONDS)
        .retryOnConnectionFailure(false)
        .build()

    suspend fun check(
        url: String = BuildConfig.UPDATE_MANIFEST_URL,
        currentCode: Int = BuildConfig.VERSION_CODE,
        sdk: Int = android.os.Build.VERSION.SDK_INT,
    ): UpdateResult = withContext(Dispatchers.IO) {
        withTimeoutOrNull(12_000) {
            runCatching {
                if (!url.startsWith("https://")) return@runCatching UpdateResult.Unavailable
                val request = Request.Builder().url(url).header("Cache-Control", "no-cache").build()
                client.newCall(request).execute().use { r ->
                    if (!r.isSuccessful) return@use UpdateResult.Unavailable
                    val body = r.body.string()
                    decide(parse(body) ?: return@use UpdateResult.Unavailable, currentCode, sdk)
                }
            }.getOrDefault(UpdateResult.Unavailable)
        } ?: UpdateResult.Unavailable
    }

    companion object {
        private val json = Json { ignoreUnknownKeys = true; isLenient = true }

        fun parse(body: String): UpdateManifest? = runCatching { json.decodeFromString(UpdateManifest.serializer(), body) }
            .getOrNull()?.takeIf { it.versionCode > 0 && it.apkUrl.startsWith("https://") }

        /** Pure rule, unit-tested. */
        fun decide(m: UpdateManifest, currentCode: Int, sdk: Int): UpdateResult = when {
            m.versionCode <= currentCode -> UpdateResult.UpToDate
            m.minSdk > sdk -> UpdateResult.Incompatible(m)
            else -> UpdateResult.Available(m)
        }
    }
}
