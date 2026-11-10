package com.hyperathlete.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hyperathlete.app.data.profile.ProfileRepository
import com.hyperathlete.app.data.settings.SettingsRepository
import com.hyperathlete.app.domain.model.AppLanguage
import com.hyperathlete.app.domain.model.AppSettings
import com.hyperathlete.app.domain.model.TextSize
import com.hyperathlete.app.domain.model.ThemeMode
import com.hyperathlete.app.domain.model.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

data class AppState(val settings: AppSettings, val profile: UserProfile, val today: LocalDate)

/** App-wide state (theme, language, profile) plus the settings actions used across screens. */
@HiltViewModel
class AppViewModel @Inject constructor(
    private val settingsRepo: SettingsRepository,
    private val profiles: ProfileRepository,
    private val clock: Clock,
    private val progressRepo: com.hyperathlete.app.data.progress.ProgressRepository,
) : ViewModel() {

    /** Today's date, re-checked every minute so pages roll over at midnight (and soon after the phone wakes). */
    private val today = flow {
        while (true) {
            emit(LocalDate.now(clock))
            delay(60_000)
        }
    }.distinctUntilChanged()

    val state: StateFlow<AppState?> = combine(settingsRepo.settings, profiles.profile, today) { s, p, d ->
        AppState(s, p, d)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Deep link from a notification ("workout", "start", "today", "knee"). */
    private val _pendingOpen = MutableStateFlow<String?>(null)
    val pendingOpen: StateFlow<String?> = _pendingOpen
    fun open(what: String?) { if (what != null) _pendingOpen.value = what }
    fun consumeOpen() { _pendingOpen.value = null }

    val autostartAcknowledged: StateFlow<Boolean> =
        settingsRepo.autostartAcknowledged.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun setLanguage(v: AppLanguage) = launch { settingsRepo.setLanguage(v) }
    fun setThemeMode(v: ThemeMode) = launch { settingsRepo.setThemeMode(v) }
    fun setAccent(argb: Long) = launch { settingsRepo.setAccent(argb) }
    fun setSecondary(argb: Long) = launch { settingsRepo.setSecondary(argb) }
    fun setTextSize(v: TextSize) = launch { settingsRepo.setTextSize(v) }
    fun setHaptics(v: Boolean) = launch { settingsRepo.setHaptics(v) }
    fun setWeightUnit(u: com.hyperathlete.app.domain.model.WeightUnit) = launch { settingsRepo.setWeightUnit(u) }
    fun setKeepScreenOn(v: Boolean) = launch { settingsRepo.setKeepScreenOn(v) }
    /** Theme, colours, text size and every page's box order back to the defaults. */
    fun resetAppearance() = launch {
        settingsRepo.resetAppearance()
        com.hyperathlete.app.domain.layout.PageId.entries.forEach { settingsRepo.resetLayout(it) }
    }
    fun applyThemePreset(p: com.hyperathlete.app.ui.theme.ThemePreset) = launch {
        settingsRepo.setThemeMode(p.mode); settingsRepo.setAccent(p.accentArgb); settingsRepo.setSecondary(p.secondaryArgb)
    }
    fun setAutostartAcknowledged(v: Boolean) = launch { settingsRepo.setAutostartAcknowledged(v) }
    fun rerunOnboarding() = launch { settingsRepo.setOnboardingDone(false) }
    fun saveProfile(p: UserProfile) = launch { profiles.save(p) }

    /* ---- program, workout, reminders, calendar (Phase 2) ---- */
    fun setWeekOffset(v: Int) = launch { settingsRepo.setWeekOffset(v) }
    fun setProgramStart(d: java.time.LocalDate) = launch { settingsRepo.setProgramStart(d); settingsRepo.setWeekOffset(0) }
    fun setTrainingTime(day: java.time.DayOfWeek, t: java.time.LocalTime) = launch {
        settingsRepo.setTrainingTimes(settingsRepo.current().trainingTimes + (day to t))
    }
    fun setWorkoutPrefs(w: com.hyperathlete.app.domain.model.WorkoutPrefs) = launch { settingsRepo.setWorkoutPrefs(w) }
    fun setReminderPrefs(r: com.hyperathlete.app.domain.model.ReminderPrefs) = launch { settingsRepo.setReminderPrefs(r) }
    fun setCalendarPrefs(c: com.hyperathlete.app.domain.model.CalendarPrefs) = launch { settingsRepo.setCalendarPrefs(c) }

    /* ---- nutrition (Phase 3) ---- */
    private var nutritionSave: kotlinx.coroutines.Job? = null
    /** Sliders fire on every pixel: write once the finger settles. */
    fun setNutritionPrefs(n: com.hyperathlete.app.domain.nutrition.NutritionPrefs) {
        nutritionSave?.cancel()
        nutritionSave = viewModelScope.launch { kotlinx.coroutines.delay(150); settingsRepo.setNutritionPrefs(n) }
    }

    /* ---- progress (Phase 4) ---- */
    fun setProgressPrefs(p: com.hyperathlete.app.domain.progress.ProgressPrefs) = launch {
        val turnedOn = p.healthSync && !settingsRepo.current().progress.healthSync
        settingsRepo.setProgressPrefs(p)
        if (turnedOn) runCatching { progressRepo.syncHealth(pushMine = true) }
    }

    /* ---- page customisation ---- */
    val layouts = settingsRepo.layouts.stateIn(
        viewModelScope, SharingStarted.Eagerly,
        com.hyperathlete.app.domain.layout.PageId.entries.associateWith { com.hyperathlete.app.domain.layout.PageLayout.default(it) },
    )
    fun saveLayout(l: com.hyperathlete.app.domain.layout.PageLayout) = launch { settingsRepo.setLayout(l) }
    fun resetLayout(page: com.hyperathlete.app.domain.layout.PageId) = launch { settingsRepo.resetLayout(page) }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}
