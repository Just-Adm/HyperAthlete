package com.hyperathlete.app.data.weather

import android.content.Context
import com.hyperathlete.app.core.location.LocationSource
import com.hyperathlete.app.domain.weather.DailyForecast
import com.hyperathlete.app.domain.weather.HourlyForecast
import com.hyperathlete.app.domain.weather.WeatherSnapshot
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

sealed interface WeatherState {
    data object Loading : WeatherState
    /** No location permission and nothing cached yet. */
    data object NoPermission : WeatherState
    data class Ready(val snapshot: WeatherSnapshot, val refreshing: Boolean = false) : WeatherState
    data class Failed(val cached: WeatherSnapshot?) : WeatherState
}

/**
 * Current weather + 7-day forecast from Open-Meteo (free, no key) for the phone's coarse location.
 * Cached on disk for 30 minutes so the header never waits on the network.
 */
@Singleton
class WeatherRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val http: OkHttpClient,
    private val location: LocationSource,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val cacheFile get() = File(context.filesDir, "weather_cache.json")
    private val mutex = Mutex()
    private val _state = MutableStateFlow<WeatherState>(WeatherState.Loading)
    val state: StateFlow<WeatherState> = _state.asStateFlow()

    suspend fun refresh(locale: Locale, force: Boolean = false) = mutex.withLock {
        val cachedRecord = readCache()
        val cached = cachedRecord?.snapshot
        val now = System.currentTimeMillis()
        if (cached != null) _state.value = WeatherState.Ready(cached, refreshing = true)
        if (!force && cached != null && now - cached.fetchedAtMillis < MAX_AGE_MS && cachedRecord.cityLocale == locale.language && cached.hourly.isNotEmpty()) {
            _state.value = WeatherState.Ready(cached)
            return@withLock
        }
        val fix = location.current()
        val lat = fix?.latitude ?: cached?.lat
        val lon = fix?.longitude ?: cached?.lon
        if (lat == null || lon == null) {
            _state.value = if (!location.hasPermission()) WeatherState.NoPermission else WeatherState.Failed(null)
            return@withLock
        }
        val fresh = runCatching { fetch(lat, lon) }.getOrNull()
        if (fresh == null) {
            _state.value = if (cached != null) WeatherState.Ready(cached) else WeatherState.Failed(null)
            return@withLock
        }
        // Reverse-geocode only when we moved or the language changed.
        val sameSpot = cached != null && abs(cached.lat - lat) < 0.05 && abs(cached.lon - lon) < 0.05
        val city = if (sameSpot && cachedRecord?.cityLocale == locale.language && cached?.city != null) cached.city
        else location.cityName(lat, lon, locale) ?: cached?.city
        val snapshot = fresh.copy(city = city)
        writeCache(CachedWeather(snapshot, locale.language))
        _state.value = WeatherState.Ready(snapshot)
    }

    private suspend fun fetch(lat: Double, lon: Double): WeatherSnapshot = withContext(Dispatchers.IO) {
        val url = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
            "&current=temperature_2m,apparent_temperature,weather_code,wind_speed_10m,is_day" +
            "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max,wind_speed_10m_max" +
            "&hourly=temperature_2m,weather_code,precipitation_probability,is_day" +
            "&timezone=auto&forecast_days=7"
        http.newCall(Request.Builder().url(url).build()).execute().use { res ->
            check(res.isSuccessful) { "weather ${res.code}" }
            parse(json.parseToJsonElement(res.body.string()).jsonObject, lat, lon)
        }
    }

    /** The last saved forecast, without any network (widgets). */
    fun cached(): WeatherSnapshot? = (_state.value as? WeatherState.Ready)?.snapshot ?: readCache()?.snapshot

    private fun readCache(): CachedWeather? = runCatching {
        if (!cacheFile.exists()) null else json.decodeFromString<CachedWeather>(cacheFile.readText())
    }.getOrNull()

    private fun writeCache(record: CachedWeather) {
        runCatching { cacheFile.writeText(json.encodeToString(CachedWeather.serializer(), record)) }
    }

    companion object {
        const val MAX_AGE_MS = 30 * 60 * 1000L

        /** Parses Open-Meteo's forecast response. Public for tests. */
        fun parse(root: JsonObject, lat: Double, lon: Double, now: Long = System.currentTimeMillis()): WeatherSnapshot {
            val cur = root["current"]!!.jsonObject
            val daily = root["daily"]!!.jsonObject
            fun arr(k: String) = daily[k]!!.jsonArray
            val days = arr("time").indices.map { i ->
                DailyForecast(
                    date = arr("time")[i].jsonPrimitive.content,
                    code = arr("weather_code")[i].jsonPrimitive.int,
                    maxC = arr("temperature_2m_max")[i].jsonPrimitive.double,
                    minC = arr("temperature_2m_min")[i].jsonPrimitive.double,
                    rainChance = runCatching { arr("precipitation_probability_max")[i].jsonPrimitive.int }.getOrDefault(0),
                    windMaxKmh = runCatching { arr("wind_speed_10m_max")[i].jsonPrimitive.double }.getOrDefault(0.0),
                )
            }
            // Hourly: today and tomorrow from midnight (enough for "12 AM to 12 AM" even after midnight).
            val hours = runCatching {
                val h = root["hourly"]!!.jsonObject
                fun col(k: String) = h[k]!!.jsonArray
                val times = col("time")
                times.indices.take(49).map { i ->
                    HourlyForecast(
                        time = times[i].jsonPrimitive.content,
                        tempC = col("temperature_2m")[i].jsonPrimitive.double,
                        code = runCatching { col("weather_code")[i].jsonPrimitive.int }.getOrDefault(0),
                        rainChance = runCatching { col("precipitation_probability")[i].jsonPrimitive.int }.getOrDefault(0),
                        isDay = runCatching { col("is_day")[i].jsonPrimitive.int != 0 }.getOrDefault(true),
                    )
                }
            }.getOrDefault(emptyList())
            return WeatherSnapshot(
                lat = lat, lon = lon, city = null, fetchedAtMillis = now,
                tempC = cur["temperature_2m"]!!.jsonPrimitive.double,
                feelsLikeC = cur["apparent_temperature"]!!.jsonPrimitive.double,
                code = cur["weather_code"]!!.jsonPrimitive.int,
                isDay = cur["is_day"]?.jsonPrimitive?.int != 0,
                windKmh = cur["wind_speed_10m"]!!.jsonPrimitive.double,
                daily = days,
                hourly = hours,
            )
        }
    }
}

/** Disk cache record: the snapshot plus the language its city name is in. */
@kotlinx.serialization.Serializable
private data class CachedWeather(val snapshot: WeatherSnapshot, val cityLocale: String)
