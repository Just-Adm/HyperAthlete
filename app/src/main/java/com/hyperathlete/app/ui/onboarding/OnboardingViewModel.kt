package com.hyperathlete.app.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hyperathlete.app.data.profile.ProfileRepository
import com.hyperathlete.app.data.settings.SettingsRepository
import com.hyperathlete.app.domain.model.AppLanguage
import com.hyperathlete.app.domain.model.AppSettings
import com.hyperathlete.app.domain.model.UserProfile
import com.hyperathlete.app.domain.program.ProgramClock
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

enum class OnboardingStep {
    WELCOME, PROFILE, PROGRAM, PERMISSIONS, ACCOUNT, READY;

    /**
     * The saved step number. 3.2.0 had an assistant step at 4, so later steps keep their old numbers
     * and a setup that was interrupted before the update resumes on the same page.
     */
    val stored: Int get() = if (ordinal >= ACCOUNT.ordinal) ordinal + 1 else ordinal

    companion object {
        fun fromStored(n: Int): OnboardingStep = when {
            n == 4 -> ACCOUNT // the removed assistant step continues with the next one
            n > 4 -> entries.getOrElse(n - 1) { WELCOME }
            else -> entries.getOrElse(n) { WELCOME }
        }
    }
}

data class OnboardingState(
    val loaded: Boolean = false,
    val step: OnboardingStep = OnboardingStep.WELCOME,
    val forward: Boolean = true,
    val language: AppLanguage = AppLanguage.MIXED,
    val profile: UserProfile = UserProfile(),
    val programStart: LocalDate = LocalDate.now(),
    val trainingTimes: Map<DayOfWeek, LocalTime> = AppSettings.DefaultTrainingTimes,
    val autostartAcknowledged: Boolean = false,
    val today: LocalDate = LocalDate.now(),
) {
    val profileValid: Boolean get() = profile.name.isNotBlank() && profile.birthDate != null
    val stepIndex: Int get() = step.ordinal
    val totalSteps: Int get() = OnboardingStep.entries.size
}

/**
 * Drives the 6-step setup. Every answer is written straight to storage, so leaving halfway
 * (or the system killing the app) never loses what was entered, and the flow resumes on the same step.
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val profiles: ProfileRepository,
    private val clock: Clock,
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state.asStateFlow()
    private var profileSave: Job? = null

    init {
        viewModelScope.launch {
            val s = settings.current()
            val p = profiles.current()
            val today = LocalDate.now(clock)
            _state.value = OnboardingState(
                loaded = true,
                step = OnboardingStep.fromStored(s.onboardingStep),
                language = s.language,
                profile = p,
                programStart = s.programStart ?: ProgramClock.weekStart(today),
                trainingTimes = s.trainingTimes,
                today = today,
            )
        }
        viewModelScope.launch {
            settings.autostartAcknowledged.collect { ack -> _state.update { it.copy(autostartAcknowledged = ack) } }
        }
        viewModelScope.launch {
            settings.settings.collect { s -> _state.update { it.copy(language = s.language) } }
        }
    }

    fun next() = go(1)
    fun back() = go(-1)

    private fun go(delta: Int) {
        val cur = _state.value
        val target = (cur.stepIndex + delta).coerceIn(0, cur.totalSteps - 1)
        if (target == cur.stepIndex) return
        if (delta > 0 && cur.step == OnboardingStep.PROGRAM) persistProgram()
        _state.update { it.copy(step = OnboardingStep.entries[target], forward = delta > 0) }
        viewModelScope.launch { settings.setOnboardingStep(OnboardingStep.entries[target].stored) }
    }

    fun setLanguage(language: AppLanguage) = viewModelScope.launch { settings.setLanguage(language) }

    /* ---- profile (debounced so typing a name doesn't write on every key) ---- */

    private fun updateProfile(transform: (UserProfile) -> UserProfile) {
        _state.update { it.copy(profile = transform(it.profile)) }
        profileSave?.cancel()
        profileSave = viewModelScope.launch {
            delay(300)
            profiles.save(_state.value.profile)
        }
    }

    fun setName(v: String) = updateProfile { it.copy(name = v.take(30)) }
    fun setBirthDate(v: LocalDate) = updateProfile { it.copy(birthDate = v) }
    fun setWeight(v: Double) = updateProfile { it.copy(weightKg = v.coerceIn(30.0, 200.0)) }
    fun setHeight(v: Double) = updateProfile { it.copy(heightCm = v.coerceIn(120.0, 230.0)) }

    /* ---- program ---- */

    fun setProgramStart(date: LocalDate) {
        _state.update { it.copy(programStart = ProgramClock.weekStart(date)) }
        persistProgram()
    }

    fun setTrainingTime(day: DayOfWeek, time: LocalTime) {
        _state.update { it.copy(trainingTimes = it.trainingTimes + (day to time)) }
        persistProgram()
    }

    fun applyTimeToAll(time: LocalTime) {
        _state.update { it.copy(trainingTimes = DayOfWeek.entries.associateWith { time }) }
        persistProgram()
    }

    private fun persistProgram() {
        val s = _state.value
        viewModelScope.launch {
            settings.setProgramStart(s.programStart)
            settings.setTrainingTimes(s.trainingTimes)
        }
    }

    /* ---- permissions ---- */

    fun setAutostartAcknowledged(v: Boolean) = viewModelScope.launch { settings.setAutostartAcknowledged(v) }

    /* ---- finish ---- */

    fun finish(onDone: () -> Unit) {
        viewModelScope.launch {
            profileSave?.cancel()
            profiles.save(_state.value.profile)
            persistProgram()
            settings.setOnboardingDone(true)
            onDone()
        }
    }
}
