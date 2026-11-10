package com.hyperathlete.app.domain.weather

import kotlinx.serialization.Serializable
import java.time.DayOfWeek
import java.time.LocalDate

/** WMO weather codes grouped into what the UI can show. */
enum class Sky { CLEAR, PARTLY_CLOUDY, CLOUDY, FOG, DRIZZLE, RAIN, SNOW, THUNDER }

fun skyOf(wmo: Int): Sky = when (wmo) {
    0 -> Sky.CLEAR
    1, 2 -> Sky.PARTLY_CLOUDY
    3 -> Sky.CLOUDY
    45, 48 -> Sky.FOG
    51, 53, 55, 56, 57 -> Sky.DRIZZLE
    61, 63, 65, 66, 67, 80, 81, 82 -> Sky.RAIN
    71, 73, 75, 77, 85, 86 -> Sky.SNOW
    95, 96, 99 -> Sky.THUNDER
    else -> Sky.CLOUDY
}

@Serializable
data class DailyForecast(
    val date: String, // ISO yyyy-MM-dd (local)
    val code: Int,
    val maxC: Double,
    val minC: Double,
    val rainChance: Int,
    val windMaxKmh: Double,
) {
    val localDate: LocalDate get() = LocalDate.parse(date)
}

/** One hour of today's forecast (local time of the forecast spot). */
@Serializable
data class HourlyForecast(
    val time: String, // ISO yyyy-MM-ddTHH:mm (local)
    val tempC: Double,
    val code: Int,
    val rainChance: Int = 0,
    val isDay: Boolean = true,
) {
    val localTime: java.time.LocalDateTime get() = java.time.LocalDateTime.parse(time)
}

@Serializable
data class WeatherSnapshot(
    val lat: Double,
    val lon: Double,
    val city: String?,
    val fetchedAtMillis: Long,
    val tempC: Double,
    val feelsLikeC: Double,
    val code: Int,
    val isDay: Boolean,
    val windKmh: Double,
    val daily: List<DailyForecast>,
    /** Hourly forecast for the first two days (48 hours from midnight). */
    val hourly: List<HourlyForecast> = emptyList(),
) {
    val today: DailyForecast? get() = daily.firstOrNull()

    /** 12 AM to 12 AM of [date]: 25 points, or fewer when the forecast doesn't cover it. */
    fun dayHours(date: LocalDate): List<HourlyForecast> {
        val start = date.atStartOfDay()
        val end = start.plusDays(1)
        return hourly.filter { h -> runCatching { h.localTime }.getOrNull()?.let { !it.isBefore(start) && !it.isAfter(end) } == true }
    }
}

/** Heads-up shown the day before the Outdoor session (Thursday) when the forecast is bad. */
data class OutdoorAlert(val rainChance: Int?, val hotMaxC: Double?)

object OutdoorRules {
    const val RAIN_THRESHOLD = 50
    const val HEAT_THRESHOLD_C = 35.0

    /** On the day before [outdoorDay], look at that day's forecast. */
    fun alertFor(snapshot: WeatherSnapshot, today: LocalDate, outdoorDay: DayOfWeek = DayOfWeek.THURSDAY): OutdoorAlert? {
        if (today.plusDays(1).dayOfWeek != outdoorDay) return null
        val f = snapshot.daily.firstOrNull { it.localDate == today.plusDays(1) } ?: return null
        val rainy = f.rainChance >= RAIN_THRESHOLD || skyOf(f.code) in setOf(Sky.RAIN, Sky.THUNDER, Sky.SNOW)
        val hot = f.maxC >= HEAT_THRESHOLD_C
        if (!rainy && !hot) return null
        return OutdoorAlert(rainChance = f.rainChance.takeIf { rainy }, hotMaxC = f.maxC.takeIf { hot })
    }
}
