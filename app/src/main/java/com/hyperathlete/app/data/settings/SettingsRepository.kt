package com.hyperathlete.app.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.hyperathlete.app.domain.model.AppLanguage
import com.hyperathlete.app.domain.model.AppSettings
import com.hyperathlete.app.domain.model.CalendarPrefs
import com.hyperathlete.app.domain.model.ReminderPrefs
import com.hyperathlete.app.domain.model.WorkoutPrefs
import com.hyperathlete.app.domain.nutrition.NutritionPrefs
import com.hyperathlete.app.domain.model.TextSize
import com.hyperathlete.app.domain.model.ThemeMode
import com.hyperathlete.app.domain.layout.PageId
import com.hyperathlete.app.domain.layout.PageLayout
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** Reads/writes [AppSettings]. Every read degrades to defaults on bad data instead of crashing. */
@Singleton
class SettingsRepository @Inject constructor(@ApplicationContext private val context: Context) {

    private object K {
        val onboardingDone = booleanPreferencesKey("onboarding_done")
        val onboardingStep = intPreferencesKey("onboarding_step")
        val language = stringPreferencesKey("language")
        val themeMode = stringPreferencesKey("theme_mode")
        val accent = longPreferencesKey("accent_argb")
        val secondary = longPreferencesKey("secondary_argb")
        val textSize = stringPreferencesKey("text_size")
        val haptics = booleanPreferencesKey("haptics")
        val keepScreenOn = booleanPreferencesKey("keep_screen_on")
        val programStart = longPreferencesKey("program_start_epoch_day")
        val trainingTimes = stringPreferencesKey("training_times")
        val weightUnit = stringPreferencesKey("weight_unit")
        val autostartAck = booleanPreferencesKey("autostart_ack")
        val weekOffset = intPreferencesKey("week_offset")
        val streakSince = longPreferencesKey("streak_since_epoch_day")
        val wSounds = booleanPreferencesKey("w_sounds")
        val wVolume = floatPreferencesKey("w_volume")
        val wTicks = booleanPreferencesKey("w_ticks")
        val wVibration = booleanPreferencesKey("w_vibration")
        val wLive = booleanPreferencesKey("w_live_updates")
        val rWorkout = booleanPreferencesKey("r_workout")
        val rMinutes = intPreferencesKey("r_minutes_before")
        val rKnee = booleanPreferencesKey("r_knee_morning")
        val rKneeTime = stringPreferencesKey("r_knee_time")
        val rMobility = booleanPreferencesKey("r_mobility")
        val cSync = booleanPreferencesKey("c_sync")
        val cId = longPreferencesKey("c_calendar_id")
        val cReminder = intPreferencesKey("c_reminder_minutes")
        val kneeCheck = longPreferencesKey("knee_check_epoch_day")
        val streakSeen = intPreferencesKey("streak_seen")
        val nMale = booleanPreferencesKey("n_male")
        val nActivity = floatPreferencesKey("n_activity")
        val nSurplus = intPreferencesKey("n_surplus")
        val nProtein = floatPreferencesKey("n_protein_per_kg")
        val nFat = intPreferencesKey("n_fat_percent")
        val nCarb = booleanPreferencesKey("n_carb_cycling")
        val nCarbBoost = intPreferencesKey("n_carb_boost")
        val nWorkoutMeals = booleanPreferencesKey("n_workout_meals")
        val nReminder = booleanPreferencesKey("n_reminder")
        val nReminderTime = stringPreferencesKey("n_reminder_time")
        val pWeight = booleanPreferencesKey("p_weight_reminder")
        val pWeightDays = stringPreferencesKey("p_weight_days")
        val pWeightTime = stringPreferencesKey("p_weight_time")
        val pHealth = booleanPreferencesKey("p_health_sync")
        val pTests = booleanPreferencesKey("p_test_reminder")
        val pSuggestDismissed = longPreferencesKey("p_suggest_dismissed_epoch_day")
        fun layout(page: PageId) = stringPreferencesKey("layout_${page.name}")
    }

    val settings: Flow<AppSettings> = context.settingsStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it.toSettings() }

    /** True once the user confirmed the Honor background steps (no API can verify them). */
    val autostartAcknowledged: Flow<Boolean> = context.settingsStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[K.autostartAck] ?: false }

    suspend fun current(): AppSettings = settings.first()

    /** Box order / visibility per page (page customisation mode). */
    val layouts: Flow<Map<PageId, PageLayout>> = context.settingsStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs -> PageId.entries.associateWith { PageLayout.decode(it, prefs[K.layout(it)]) } }

    suspend fun setLayout(layout: PageLayout) = edit { it[K.layout(layout.page)] = layout.encode() }
    suspend fun resetLayout(page: PageId) = edit { it.remove(K.layout(page)) }

    private fun Preferences.toSettings(): AppSettings {
        val d = AppSettings()
        return AppSettings(
            onboardingDone = this[K.onboardingDone] ?: d.onboardingDone,
            onboardingStep = this[K.onboardingStep] ?: d.onboardingStep,
            language = enumOr(this[K.language], d.language),
            themeMode = enumOr(this[K.themeMode], d.themeMode),
            accentArgb = this[K.accent] ?: d.accentArgb,
            secondaryArgb = this[K.secondary] ?: d.secondaryArgb,
            textSize = enumOr(this[K.textSize], d.textSize),
            hapticsEnabled = this[K.haptics] ?: d.hapticsEnabled,
            keepScreenOnDuringWorkout = this[K.keepScreenOn] ?: d.keepScreenOnDuringWorkout,
            programStart = this[K.programStart]?.let { LocalDate.ofEpochDay(it) },
            weekOffset = this[K.weekOffset] ?: 0,
            streakSince = this[K.streakSince]?.let { LocalDate.ofEpochDay(it) },
            workout = WorkoutPrefs(
                sounds = this[K.wSounds] ?: d.workout.sounds,
                volume = this[K.wVolume] ?: d.workout.volume,
                countdownTicks = this[K.wTicks] ?: d.workout.countdownTicks,
                vibration = this[K.wVibration] ?: d.workout.vibration,
                liveUpdates = this[K.wLive] ?: d.workout.liveUpdates,
            ),
            reminders = ReminderPrefs(
                workout = this[K.rWorkout] ?: d.reminders.workout,
                minutesBefore = this[K.rMinutes] ?: d.reminders.minutesBefore,
                kneeMorning = this[K.rKnee] ?: d.reminders.kneeMorning,
                kneeMorningTime = this[K.rKneeTime]?.let { runCatching { LocalTime.parse(it) }.getOrNull() } ?: d.reminders.kneeMorningTime,
                mobility = this[K.rMobility] ?: d.reminders.mobility,
            ),
            calendar = CalendarPrefs(
                sync = this[K.cSync] ?: d.calendar.sync,
                calendarId = this[K.cId]?.takeIf { it > 0 },
                reminderMinutes = this[K.cReminder] ?: d.calendar.reminderMinutes,
            ),
            trainingTimes = decodeTimes(this[K.trainingTimes]) ?: d.trainingTimes,
            weightUnit = enumOr(this[K.weightUnit], d.weightUnit),
            progress = com.hyperathlete.app.domain.progress.ProgressPrefs(
                weightReminder = this[K.pWeight] ?: d.progress.weightReminder,
                weightDays = this[K.pWeightDays]?.split(',')?.mapNotNull { n -> runCatching { DayOfWeek.valueOf(n) }.getOrNull() }?.toSet() ?: d.progress.weightDays,
                weightTime = this[K.pWeightTime]?.let { runCatching { LocalTime.parse(it) }.getOrNull() } ?: d.progress.weightTime,
                testReminder = this[K.pTests] ?: d.progress.testReminder,
                healthSync = this[K.pHealth] ?: d.progress.healthSync,
            ),
            nutrition = NutritionPrefs(
                male = this[K.nMale] ?: d.nutrition.male,
                activity = this[K.nActivity]?.toDouble()?.let { a -> NutritionPrefs.ActivityLevels.minBy { kotlin.math.abs(it - a) } } ?: d.nutrition.activity,
                surplus = (this[K.nSurplus] ?: d.nutrition.surplus).coerceIn(0, 1000),
                proteinPerKg = ((this[K.nProtein]?.toDouble() ?: d.nutrition.proteinPerKg) * 10).let { kotlin.math.round(it) / 10 }.coerceIn(1.2, 3.0),
                fatPercent = (this[K.nFat] ?: d.nutrition.fatPercent).coerceIn(15, 40),
                carbCycling = this[K.nCarb] ?: d.nutrition.carbCycling,
                carbBoostKcal = (this[K.nCarbBoost] ?: d.nutrition.carbBoostKcal).coerceIn(0, 500),
                workoutMeals = this[K.nWorkoutMeals] ?: d.nutrition.workoutMeals,
                reminder = this[K.nReminder] ?: d.nutrition.reminder,
                reminderTime = this[K.nReminderTime]?.let { runCatching { LocalTime.parse(it) }.getOrNull() } ?: d.nutrition.reminderTime,
            ),
        )
    }

    suspend fun setOnboardingStep(step: Int) = edit { it[K.onboardingStep] = step }
    suspend fun setOnboardingDone(done: Boolean) = edit {
        it[K.onboardingDone] = done
        if (!done) it[K.onboardingStep] = 0
    }
    suspend fun setLanguage(v: AppLanguage) = edit { it[K.language] = v.name }
    suspend fun setThemeMode(v: ThemeMode) = edit { it[K.themeMode] = v.name }
    suspend fun setAccent(argb: Long) = edit { it[K.accent] = argb }
    suspend fun setSecondary(argb: Long) = edit { it[K.secondary] = argb }
    suspend fun setTextSize(v: TextSize) = edit { it[K.textSize] = v.name }
    suspend fun setHaptics(v: Boolean) = edit { it[K.haptics] = v }
    suspend fun setKeepScreenOn(v: Boolean) = edit { it[K.keepScreenOn] = v }
    suspend fun setProgramStart(date: LocalDate) = edit { it[K.programStart] = date.toEpochDay() }
    suspend fun setTrainingTimes(times: Map<DayOfWeek, LocalTime>) = edit { it[K.trainingTimes] = encodeTimes(times) }
    suspend fun setAutostartAcknowledged(v: Boolean) = edit { it[K.autostartAck] = v }
    suspend fun setWeekOffset(v: Int) = edit { it[K.weekOffset] = v }
    suspend fun ensureStreakSince(today: LocalDate) = edit { if (it[K.streakSince] == null) it[K.streakSince] = today.toEpochDay() }

    /** Morning the "how's your knee?" question is due, or null. */
    val kneeCheck: Flow<LocalDate?> = context.settingsStore.data
        .catch { emit(emptyPreferences()) }
        .map { p -> p[K.kneeCheck]?.let { LocalDate.ofEpochDay(it) } }

    suspend fun kneeCheckDate(): LocalDate? = kneeCheck.first()

    /** Last streak value the Home box celebrated, so a new day is celebrated exactly once. */
    val streakSeen: Flow<Int> = context.settingsStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[K.streakSeen] ?: 0 }

    suspend fun setStreakSeen(v: Int) = edit { it[K.streakSeen] = v }

    suspend fun setKneeCheckDate(date: LocalDate?) = edit {
        if (date == null) it.remove(K.kneeCheck) else it[K.kneeCheck] = date.toEpochDay()
    }

    suspend fun setWorkoutPrefs(w: WorkoutPrefs) = edit {
        it[K.wSounds] = w.sounds; it[K.wVolume] = w.volume; it[K.wTicks] = w.countdownTicks
        it[K.wVibration] = w.vibration; it[K.wLive] = w.liveUpdates
    }

    suspend fun setWeightUnit(u: com.hyperathlete.app.domain.model.WeightUnit) = edit { it[K.weightUnit] = u.name }

    suspend fun setReminderPrefs(r: ReminderPrefs) = edit {
        it[K.rWorkout] = r.workout; it[K.rMinutes] = r.minutesBefore; it[K.rKnee] = r.kneeMorning
        it[K.rKneeTime] = r.kneeMorningTime.toString(); it[K.rMobility] = r.mobility
    }

    suspend fun setCalendarPrefs(c: CalendarPrefs) = edit {
        it[K.cSync] = c.sync; it[K.cReminder] = c.reminderMinutes
        if (c.calendarId != null) it[K.cId] = c.calendarId else it.remove(K.cId)
    }

    suspend fun setNutritionPrefs(n: NutritionPrefs) = edit {
        it[K.nMale] = n.male; it[K.nActivity] = n.activity.toFloat(); it[K.nSurplus] = n.surplus
        it[K.nProtein] = n.proteinPerKg.toFloat(); it[K.nFat] = n.fatPercent; it[K.nCarb] = n.carbCycling
        it[K.nCarbBoost] = n.carbBoostKcal; it[K.nWorkoutMeals] = n.workoutMeals; it[K.nReminder] = n.reminder
        it[K.nReminderTime] = n.reminderTime.toString()
    }

    suspend fun setProgressPrefs(p: com.hyperathlete.app.domain.progress.ProgressPrefs) = edit {
        it[K.pWeight] = p.weightReminder
        it[K.pWeightDays] = p.weightDays.sortedBy { d -> d.value }.joinToString(",") { d -> d.name }
        it[K.pWeightTime] = p.weightTime.toString()
        it[K.pTests] = p.testReminder
        it[K.pHealth] = p.healthSync
    }

    /** Day the "+200 kcal" suggestion was answered (applied or dismissed); it stays quiet for two weeks after. */
    val suggestionAnswered: Flow<LocalDate?> = context.settingsStore.data
        .catch { emit(emptyPreferences()) }
        .map { p -> p[K.pSuggestDismissed]?.let { LocalDate.ofEpochDay(it) } }

    suspend fun setSuggestionAnswered(d: LocalDate) = edit { it[K.pSuggestDismissed] = d.toEpochDay() }

    /**
     * 3.3.0 removed the voice assistant, the voice coach and every AI feature. Their saved values are
     * dropped once so they are not carried into backups; everything else is untouched.
     */
    suspend fun dropRemovedFeatures() = edit { prefs ->
        prefs.asMap().keys.filter { isRemoved(it.name) }.forEach { prefs.remove(it) }
    }

    suspend fun resetAppearance() = edit {
        it.remove(K.themeMode); it.remove(K.accent); it.remove(K.secondary); it.remove(K.textSize)
    }

    /* ---- backup / export (Phase 5) ---- */

    /** Every stored setting by key name, minus the ones that only make sense on this phone. */
    suspend fun rawSnapshot(): Map<String, Any> = context.settingsStore.data.first().asMap()
        .mapKeys { it.key.name }.filterKeys { it !in DeviceOnlyKeys && !isRemoved(it) }

    /** Replaces all settings with [values], keeping this phone's own [DeviceOnlyKeys]. */
    suspend fun restoreRaw(values: Map<String, Any>) = context.settingsStore.edit { p ->
        val keep = p.asMap().filterKeys { it.name in DeviceOnlyKeys }
        p.clear()
        @Suppress("UNCHECKED_CAST")
        fun put(name: String, v: Any) {
            when (v) {
                is Boolean -> p[booleanPreferencesKey(name)] = v
                is Int -> p[intPreferencesKey(name)] = v
                is Long -> p[longPreferencesKey(name)] = v
                is Float -> p[floatPreferencesKey(name)] = v
                is Double -> p[androidx.datastore.preferences.core.doublePreferencesKey(name)] = v
                is String -> p[stringPreferencesKey(name)] = v
                is Set<*> -> p[androidx.datastore.preferences.core.stringSetPreferencesKey(name)] = v as Set<String>
            }
        }
        // Backups from 3.2.0 and older still carry the removed assistant settings: skip them.
        values.filterKeys { it !in DeviceOnlyKeys && !isRemoved(it) }.forEach { (k, v) -> put(k, v) }
        keep.forEach { (k, v) -> put(k.name, v) }
    }

    /** "Clear data": back to a fresh install (the setup runs again). */
    suspend fun clearAll() = edit { it.clear() }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.settingsStore.edit { block(it) }
    }

    companion object {
        /** Tied to this phone (Honor background steps confirmed here, this phone's calendar id). */
        val DeviceOnlyKeys = setOf("autostart_ack", "c_calendar_id")

        /** Keys of features removed in 3.3.0 (besides every "ai_" key). */
        val RemovedKeys = setOf("w_voice_coach", "w_coach_gemini")

        fun isRemoved(key: String): Boolean = key.startsWith("ai_") || key in RemovedKeys

        inline fun <reified E : Enum<E>> enumOr(raw: String?, default: E): E =
            raw?.let { r -> enumValues<E>().firstOrNull { it.name == r } } ?: default

        /** "SUNDAY=18:00;MONDAY=18:30" */
        fun encodeTimes(times: Map<DayOfWeek, LocalTime>): String =
            times.entries.sortedBy { it.key.value }.joinToString(";") { "${it.key.name}=${it.value}" }

        fun decodeTimes(raw: String?): Map<DayOfWeek, LocalTime>? {
            if (raw.isNullOrBlank()) return null
            val parsed = raw.split(';').mapNotNull { part ->
                val (d, t) = part.split('=').takeIf { it.size == 2 } ?: return@mapNotNull null
                val day = runCatching { DayOfWeek.valueOf(d) }.getOrNull() ?: return@mapNotNull null
                val time = runCatching { LocalTime.parse(t) }.getOrNull() ?: return@mapNotNull null
                day to time
            }.toMap()
            return if (parsed.isEmpty()) null else AppSettings.DefaultTrainingTimes + parsed
        }
    }
}
