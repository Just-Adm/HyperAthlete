package com.hyperathlete.app.data

import com.hyperathlete.app.data.weather.WeatherRepository
import com.hyperathlete.app.domain.weather.OutdoorRules
import com.hyperathlete.app.domain.weather.Sky
import com.hyperathlete.app.domain.weather.skyOf
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class WeatherTest {
    private fun sample(thuRain: Int, thuMax: Double, thuCode: Int = 1) = """
        {"current":{"time":"2026-10-14T17:00","temperature_2m":24.6,"apparent_temperature":25.9,"weather_code":2,"wind_speed_10m":11.3,"is_day":1},
         "daily":{"time":["2026-10-14","2026-10-15","2026-10-16"],
                  "weather_code":[2,$thuCode,0],
                  "temperature_2m_max":[28.1,$thuMax,27.0],
                  "temperature_2m_min":[16.2,15.0,14.8],
                  "precipitation_probability_max":[0,$thuRain,null],
                  "wind_speed_10m_max":[18.0,20.5,12.0]}}
    """.trimIndent()

    private fun parse(json: String) = WeatherRepository.parse(Json.parseToJsonElement(json).jsonObject, 31.95, 35.93, now = 0)

    @Test fun `parses Open-Meteo current weather and the daily forecast`() {
        val s = parse(sample(10, 29.0))
        assertEquals(24.6, s.tempC, 0.01)
        assertEquals(25.9, s.feelsLikeC, 0.01)
        assertEquals(Sky.PARTLY_CLOUDY, skyOf(s.code))
        assertTrue(s.isDay)
        assertEquals(3, s.daily.size)
        assertEquals(0, s.daily[2].rainChance) // null in the feed becomes 0
        assertEquals(LocalDate.of(2026, 10, 15), s.daily[1].localDate)
    }

    @Test fun `hourly temperatures cover midnight to midnight`() {
        val times = (0..48).joinToString(",") { i -> "\"2026-10-%02dT%02d:00\"".format(14 + i / 24, i % 24) }
        val temps = (0..48).joinToString(",") { "${10 + it % 24}.5" }
        val codes = (0..48).joinToString(",") { "0" }
        val rain = (0..48).joinToString(",") { if (it == 20) "null" else "${it % 24 * 2}" }
        val json = sample(10, 29.0).trimEnd().removeSuffix("}") +
            ""","hourly":{"time":[$times],"temperature_2m":[$temps],"weather_code":[$codes],"precipitation_probability":[$rain],"is_day":[$codes]}}"""
        val s = parse(json)
        assertEquals(49, s.hourly.size)
        val day = s.dayHours(LocalDate.of(2026, 10, 14))
        assertEquals(25, day.size) // 12 AM … 11 PM and the next 12 AM
        assertEquals(10.5, day.first().tempC, 0.01)
        assertEquals("2026-10-15T00:00", day.last().time)
        assertEquals(0, day[20].rainChance) // null becomes 0
        // An old cache without hourly data still reads.
        assertTrue(parse(sample(10, 29.0)).hourly.isEmpty())
    }

    @Test fun `Wednesday warns about rain or heat on Thursday's Outdoor session`() {
        val wednesday = LocalDate.of(2026, 10, 14)
        assertNotNull(OutdoorRules.alertFor(parse(sample(70, 26.0)), wednesday)?.rainChance)
        assertEquals(37.0, OutdoorRules.alertFor(parse(sample(0, 37.0)), wednesday)?.hotMaxC)
        assertNull(OutdoorRules.alertFor(parse(sample(10, 29.0)), wednesday))
        // Thunderstorm code counts as rain even with a low probability number.
        assertNotNull(OutdoorRules.alertFor(parse(sample(20, 25.0, thuCode = 95)), wednesday))
    }

    @Test fun `no alert on other days`() {
        assertNull(OutdoorRules.alertFor(parse(sample(90, 40.0)), LocalDate.of(2026, 10, 15)))
    }
}
