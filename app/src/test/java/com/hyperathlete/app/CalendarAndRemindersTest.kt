package com.hyperathlete.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hyperathlete.app.core.AppEntryPoint
import com.hyperathlete.app.core.reminders.ReminderKind
import com.hyperathlete.app.core.reminders.ReminderPlanner
import com.hyperathlete.app.domain.model.AppLanguage
import com.hyperathlete.app.domain.model.AppSettings
import com.hyperathlete.app.domain.model.ReminderPrefs
import dagger.hilt.android.EntryPointAccessors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class CalendarAndRemindersTest {

    private val settings = AppSettings(programStart = LocalDate.of(2026, 10, 4), language = AppLanguage.MIXED)

    @Test fun `calendar plan - one event per day, phase-aware lists, deload and test week markers`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val cal = EntryPointAccessors.fromApplication(context, AppEntryPoint::class.java).calendar()
        val events = cal.plan(settings, LocalDate.of(2026, 10, 15), weeks = 9)
        val days = events.filter { it.key.startsWith("d:") }
        assertEquals(63, days.size)
        // Thursday 15 Oct: week 2, the Outdoor day with its four blocks
        val thu = days.first { it.key == "d:${LocalDate.of(2026, 10, 15).toEpochDay()}" }
        assertTrue(thu.title.contains("Outdoor Full Body"))
        assertTrue(thu.description.contains("Snap Down"))
        // Friday of week 5 lists the back squat instead of the goblet squat
        val friW5 = days.first { it.key == "d:${LocalDate.of(2026, 11, 6).toEpochDay()}" }
        assertTrue(friW5.description.contains("Back Squat"))
        assertFalse(friW5.description.contains("Goblet"))
        // Rest days, the deload week marker (week 9 starts Sun 29 Nov) and the deload tag in titles
        assertTrue(days.any { it.title.contains("راحة") })
        val markers = events.filter { it.key.startsWith("w:") }
        assertTrue(markers.any { it.key == "w:${LocalDate.of(2026, 11, 29).toEpochDay()}" && it.allDay })
        assertTrue(days.first { it.key == "d:${LocalDate.of(2026, 11, 29).toEpochDay()}" }.title.contains("Deload"))
        // Training time + reminder
        assertEquals(30, thu.reminderMinutes)
    }

    @Test fun `reminders - next workout before the session, mobility on rest days, knee check next morning`() {
        val wed = LocalDateTime.of(2026, 10, 14, 12, 0)
        val next = ReminderPlanner.next(wed, settings, kneeCheckDate = null, doneToday = false)
        assertEquals(ReminderKind.WORKOUT, next.first().kind)
        assertEquals(LocalDateTime.of(2026, 10, 14, 17, 30), next.first().at)
        // already trained today → tomorrow's reminder comes first
        val afterTraining = ReminderPlanner.next(wed, settings, null, doneToday = true)
        assertEquals(LocalDate.of(2026, 10, 15), afterTraining.first { it.kind == ReminderKind.WORKOUT }.date)
        // Saturday is a rest day: mobility reminder at the usual time
        val sat = ReminderPlanner.next(LocalDateTime.of(2026, 10, 17, 9, 0), settings, null, false)
        assertEquals(ReminderKind.MOBILITY, sat.first().kind)
        // knee check the morning after legs
        val fri = ReminderPlanner.next(LocalDateTime.of(2026, 10, 16, 20, 0), settings, LocalDate.of(2026, 10, 17), true)
        assertTrue(fri.any { it.kind == ReminderKind.KNEE && it.at == LocalDate.of(2026, 10, 17).atTime(LocalTime.of(9, 0)) })
        // morning weigh-in: tomorrow at 7:30 once today's has passed
        assertEquals(LocalDate.of(2026, 10, 15).atTime(LocalTime.of(7, 30)), next.first { it.kind == ReminderKind.WEIGHT }.at)
        // test week 1 (program started Sunday 11th): Thursday 10:00
        val testWeek = ReminderPlanner.next(wed, settings.copy(programStart = LocalDate.of(2026, 10, 11)), null, false)
        assertEquals(LocalDate.of(2026, 10, 15).atTime(LocalTime.of(10, 0)), testWeek.first { it.kind == ReminderKind.TESTS }.at)
        // switches off → nothing
        // evening food reminder: today if still ahead, else tomorrow
        assertTrue(next.any { it.kind == ReminderKind.FOOD && it.at == LocalDate.of(2026, 10, 14).atTime(LocalTime.of(21, 0)) })
        val late = ReminderPlanner.next(LocalDateTime.of(2026, 10, 14, 22, 0), settings, null, true)
        assertEquals(LocalDate.of(2026, 10, 15), late.first { it.kind == ReminderKind.FOOD }.date)
        // switches off → nothing
        val off = settings.copy(
            reminders = ReminderPrefs(workout = false, mobility = false, kneeMorning = false),
            nutrition = settings.nutrition.copy(reminder = false),
            progress = settings.progress.copy(weightReminder = false, testReminder = false),
        )
        assertTrue(ReminderPlanner.next(wed, off, LocalDate.of(2026, 10, 15), false).isEmpty())
    }
}
