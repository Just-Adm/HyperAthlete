package com.hyperathlete.app.domain

import com.hyperathlete.app.data.settings.SettingsRepository
import com.hyperathlete.app.domain.model.Age
import com.hyperathlete.app.domain.model.AppLanguage
import com.hyperathlete.app.domain.model.AppSettings
import com.hyperathlete.app.domain.model.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

class AgeAndSettingsTest {

    @Test fun `age counts whole years and handles the birthday itself`() {
        val birth = LocalDate.of(2009, 3, 20)
        assertEquals(17, Age.years(birth, LocalDate.of(2026, 10, 15)))
        assertEquals(16, Age.years(birth, LocalDate.of(2026, 3, 19)))
        assertEquals(17, Age.years(birth, LocalDate.of(2026, 3, 20)))
        assertEquals(0, Age.years(birth, LocalDate.of(2008, 1, 1))) // never negative
    }

    @Test fun `leap-day birthdays turn a year older on the 1st of March in common years`() {
        val birth = LocalDate.of(2008, 2, 29)
        assertEquals(16, Age.years(birth, LocalDate.of(2025, 2, 28)))
        assertEquals(17, Age.years(birth, LocalDate.of(2025, 3, 1)))
        assertEquals(16, Age.years(birth, LocalDate.of(2024, 2, 29)))
    }

    @Test fun `profile without a birth date has no age`() {
        assertNull(UserProfile(name = "A").ageOn(LocalDate.of(2026, 1, 1)))
    }

    @Test fun `training times round-trip through storage`() {
        val times = AppSettings.DefaultTrainingTimes + (DayOfWeek.THURSDAY to LocalTime.of(7, 30))
        val decoded = SettingsRepository.decodeTimes(SettingsRepository.encodeTimes(times))
        assertEquals(times, decoded)
    }

    @Test fun `corrupt training times fall back to defaults per day`() {
        assertNull(SettingsRepository.decodeTimes(""))
        assertNull(SettingsRepository.decodeTimes("garbage"))
        val partial = SettingsRepository.decodeTimes("FRIDAY=06:15;NOPE=1:00;MONDAY=xx")!!
        assertEquals(LocalTime.of(6, 15), partial[DayOfWeek.FRIDAY])
        assertEquals(LocalTime.of(18, 0), partial[DayOfWeek.MONDAY])
        assertEquals(7, partial.size)
    }

    @Test fun `unknown enum values fall back to defaults`() {
        assertEquals(AppLanguage.MIXED, SettingsRepository.enumOr("KLINGON", AppLanguage.MIXED))
        assertEquals(AppLanguage.ENGLISH, SettingsRepository.enumOr("ENGLISH", AppLanguage.MIXED))
    }
}

class BidiTest {
    @Test fun `join wraps every part and the whole run in the reading direction`() {
        val rtl = com.hyperathlete.app.core.locale.Bidi.join(listOf("صدر", "Biceps"), " + ", rtl = true)
        assertEquals("⁧⁨صدر⁩ + ⁨Biceps⁩⁩", rtl)
        assertEquals("صدر + Biceps", com.hyperathlete.app.core.locale.Bidi.strip(rtl))
        val ltr = com.hyperathlete.app.core.locale.Bidi.join(listOf("Chest", "Biceps"), " + ", rtl = false)
        assertEquals('⁦', ltr.first())
    }
}
