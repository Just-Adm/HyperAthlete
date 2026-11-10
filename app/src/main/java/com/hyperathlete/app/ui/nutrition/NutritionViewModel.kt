package com.hyperathlete.app.ui.nutrition

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hyperathlete.app.data.nutrition.NutritionRepository
import com.hyperathlete.app.data.profile.ProfileRepository
import com.hyperathlete.app.data.settings.SettingsRepository
import com.hyperathlete.app.domain.model.UserProfile
import com.hyperathlete.app.domain.nutrition.Adherence
import com.hyperathlete.app.domain.nutrition.AdherenceRule
import com.hyperathlete.app.domain.nutrition.DayStatus
import com.hyperathlete.app.domain.nutrition.Macros
import com.hyperathlete.app.domain.nutrition.MealEntry
import com.hyperathlete.app.domain.nutrition.MealType
import com.hyperathlete.app.domain.nutrition.NutritionPrefs
import com.hyperathlete.app.domain.nutrition.NutritionTargets
import com.hyperathlete.app.domain.nutrition.TargetCalculator
import com.hyperathlete.app.domain.nutrition.sum
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

/** One day of the food diary, ready to draw. Built by [of] so screenshot tests can make one too. */
data class NutritionDay(
    val date: LocalDate,
    val today: LocalDate,
    val entries: List<MealEntry>,
    val targets: NutritionTargets,
    val weightKg: Double,
    val meals: List<MealType>,
) {
    val eaten: Macros = entries.map { it.total }.sum()
    val status: DayStatus = DayStatus.of(eaten, targets)
    fun inMeal(m: MealType) = entries.filter { it.meal == m }

    companion object {
        fun of(date: LocalDate, today: LocalDate, entries: List<MealEntry>, profile: UserProfile, prefs: NutritionPrefs) = NutritionDay(
            date, today, entries, TargetCalculator.targets(profile, prefs, date), profile.weightKg,
            // A hidden section still shows if something was logged in it.
            (MealType.visible(prefs.workoutMeals) + MealType.entries.filter { m -> m.workout && !prefs.workoutMeals && entries.any { it.meal == m } })
                .sortedBy { it.ordinal },
        )
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class NutritionViewModel @Inject constructor(
    private val repo: NutritionRepository,
    private val settings: SettingsRepository,
    private val profiles: ProfileRepository,
    private val clock: Clock,
) : ViewModel() {

    private val today = MutableStateFlow(LocalDate.now(clock))
    /** null = follow today (rolls over at midnight). */
    private val picked = MutableStateFlow<LocalDate?>(null)

    fun refreshDay() { today.value = LocalDate.now(clock) }

    private val selected: Flow<LocalDate> = combine(today, picked) { t, p -> p ?: t }

    val day: StateFlow<NutritionDay?> = selected.flatMapLatest { d -> dayFlow(d) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Today's numbers for the small Home box. */
    val todaySummary: StateFlow<NutritionDay?> = today.flatMapLatest { d -> dayFlow(d) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private fun dayFlow(d: LocalDate): Flow<NutritionDay> =
        combine(repo.day(d), settings.settings, profiles.profile, today) { entries, s, p, t -> NutritionDay.of(d, t, entries, p, s.nutrition) }

    fun select(d: LocalDate) { picked.value = if (d == today.value) null else d }
    fun shift(days: Long) {
        val base = picked.value ?: today.value
        select(base.plusDays(days))
    }

    /** Adherence colour for every day of [month] (brief §11: month calendar coloured by how the day went). */
    fun month(month: YearMonth): Flow<Map<LocalDate, Adherence>> =
        combine(repo.totals(month.atDay(1), month.atEndOfMonth()), settings.settings, profiles.profile) { totals, s, p ->
            totals.mapValues { (d, m) -> AdherenceRule.of(m, TargetCalculator.targets(p, s.nutrition, d)) }
        }

    /* ---- delete with undo ---- */
    private val _undo = MutableStateFlow<MealEntry?>(null)
    val undo: StateFlow<MealEntry?> = _undo
    private var undoJob: Job? = null

    fun delete(e: MealEntry) {
        viewModelScope.launch {
            repo.delete(e.id)
            _undo.value = e
            undoJob?.cancel()
            undoJob = launch { delay(5_000); _undo.value = null }
        }
    }

    fun undoDelete() {
        val e = _undo.value ?: return
        _undo.value = null
        undoJob?.cancel()
        viewModelScope.launch { repo.save(e.copy(id = 0)) }
    }

    fun dismissUndo() { _undo.value = null }

    /** Same food again in the same meal (long-press "repeat"). */
    fun duplicate(e: MealEntry, to: LocalDate) {
        viewModelScope.launch { repo.save(e.copy(id = 0, date = to, createdAt = clock.millis())) }
    }

    val todayDate: StateFlow<LocalDate> = today
    val selectedDate: StateFlow<LocalDate> = combine(today, picked) { t, p -> p ?: t }
        .stateIn(viewModelScope, SharingStarted.Eagerly, today.value)
}
