package com.hyperathlete.app.domain.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/** Three language modes. MIXED = Arabic interface with English exercise names and sports terms. */
enum class AppLanguage { MIXED, ARABIC, ENGLISH }

enum class ThemeMode { LIGHT, DARK, SYSTEM }

/** Kilograms or pounds for every weight shown and typed (stored in kg). */
enum class WeightUnit { KG, LB }

/** Text size presets for the customisation screen. */
enum class TextSize(val scale: Float) { SMALL(0.92f), DEFAULT(1f), LARGE(1.1f), XLARGE(1.2f) }

/** Workout player feedback and the lock-screen capsule. */
data class WorkoutPrefs(
    val sounds: Boolean = true,
    /** 0..1, applied on top of the alarm stream volume. */
    val volume: Float = 0.8f,
    val countdownTicks: Boolean = true,
    val vibration: Boolean = true,
    /** Android 16 Live Updates (the capsule next to the camera and on the lock screen). */
    val liveUpdates: Boolean = true,
)

data class ReminderPrefs(
    val workout: Boolean = true,
    val minutesBefore: Int = 30,
    /** "How's your knee?" the morning after a leg / jump / sled day. */
    val kneeMorning: Boolean = true,
    val kneeMorningTime: LocalTime = LocalTime.of(9, 0),
    val mobility: Boolean = true,
)

data class CalendarPrefs(
    val sync: Boolean = false,
    /** null = the app's own "Hyper Athlete" calendar. */
    val calendarId: Long? = null,
    val reminderMinutes: Int = 30,
)

/**
 * Everything the user can change that is not part of their training/food history.
 * Persisted in DataStore; the profile itself (name, birth date, body numbers) lives in Room.
 */
data class AppSettings(
    val onboardingDone: Boolean = false,
    val onboardingStep: Int = 0,
    val language: AppLanguage = AppLanguage.MIXED,
    val themeMode: ThemeMode = ThemeMode.LIGHT,
    val accentArgb: Long = 0xFFFF6B00,
    val secondaryArgb: Long = 0xFF2F6BFF,
    val textSize: TextSize = TextSize.DEFAULT,
    val hapticsEnabled: Boolean = true,
    val keepScreenOnDuringWorkout: Boolean = true,
    val programStart: LocalDate? = null,
    /** Manual "edit current week" / "deload now" adjustment in weeks. */
    val weekOffset: Int = 0,
    /** Day the streak started counting (first launch with session logging). */
    val streakSince: LocalDate? = null,
    val trainingTimes: Map<DayOfWeek, LocalTime> = DefaultTrainingTimes,
    val workout: WorkoutPrefs = WorkoutPrefs(),
    val reminders: ReminderPrefs = ReminderPrefs(),
    val calendar: CalendarPrefs = CalendarPrefs(),
    val nutrition: com.hyperathlete.app.domain.nutrition.NutritionPrefs = com.hyperathlete.app.domain.nutrition.NutritionPrefs(),
    val progress: com.hyperathlete.app.domain.progress.ProgressPrefs = com.hyperathlete.app.domain.progress.ProgressPrefs(),
    val weightUnit: WeightUnit = WeightUnit.KG,
) {
    /** Where the program is today, with the manual week adjustment applied. */
    fun position(today: LocalDate): com.hyperathlete.app.domain.program.ProgramPosition =
        com.hyperathlete.app.domain.program.ProgramClock.position(
            programStart ?: com.hyperathlete.app.domain.program.ProgramClock.weekStart(today), today, weekOffset,
        )

    companion object {
        val DefaultTrainingTimes: Map<DayOfWeek, LocalTime> =
            DayOfWeek.entries.associateWith { LocalTime.of(18, 0) }
    }
}
