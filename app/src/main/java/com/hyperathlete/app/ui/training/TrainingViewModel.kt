package com.hyperathlete.app.ui.training

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hyperathlete.app.core.reminders.ReminderScheduler
import com.hyperathlete.app.core.workout.ActiveSession
import com.hyperathlete.app.core.workout.SessionController
import com.hyperathlete.app.core.workout.SessionEvent
import com.hyperathlete.app.core.workout.WorkoutService
import com.hyperathlete.app.data.program.ProgramRepository
import com.hyperathlete.app.data.settings.SettingsRepository
import com.hyperathlete.app.data.workout.WorkoutRepository
import com.hyperathlete.app.domain.program.Program
import com.hyperathlete.app.domain.program.WeekPlan
import com.hyperathlete.app.domain.streak.DayRecord
import com.hyperathlete.app.domain.streak.StreakCalculator
import com.hyperathlete.app.domain.streak.StreakResult
import com.hyperathlete.app.domain.training.KneeAdvice
import com.hyperathlete.app.domain.training.KneeReport
import com.hyperathlete.app.domain.training.KneeRule
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import javax.inject.Inject

/** What Home needs about training: streak, this week, today's checklist, a running session, knee check. */
data class TrainingState(
    val program: Program,
    val streak: StreakResult,
    val days: Map<LocalDate, DayRecord>,
    val todayLogged: Set<String>,
    val active: ActiveSession?,
    /** "How's your knee?" is due this morning. */
    val kneeDue: Boolean,
    val knee: KneeAdvice,
    /** Streak value already celebrated on Home. */
    val streakSeen: Int = 0,
) {
    fun doneOn(date: LocalDate): Boolean = days[date]?.let { it.sessionDone || it.mobilityDone } == true

    companion object {
        /** Before the database answers (first frame) or in previews: nothing logged yet. */
        fun empty(context: Context, today: LocalDate) = TrainingState(
            ProgramRepository.load(context),
            StreakCalculator.compute(emptyMap(), { WeekPlan.of(it).kind }, null, today),
            emptyMap(), emptySet(), null, false, KneeAdvice.NORMAL,
        )
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TrainingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val controller: SessionController,
    private val repo: WorkoutRepository,
    private val programs: ProgramRepository,
    private val settings: SettingsRepository,
    private val reminders: ReminderScheduler,
    private val clock: Clock,
) : ViewModel() {

    private val today = MutableStateFlow(LocalDate.now(clock))

    /** Call on resume so the page follows midnight. */
    fun refreshDay() {
        today.value = LocalDate.now(clock)
        viewModelScope.launch { controller.restore() }
    }

    private val history = today.flatMapLatest { d ->
        combine(repo.days(d.minusDays(400)), repo.slotsLoggedOn(d), repo.firstCompletedDay(), repo.lastKneeFlow()) { days, logged, first, knee ->
            History(d, days, logged, first, knee?.let { KneeRule.evaluate(KneeReport(it.during, it.morning, it.sharp)) } ?: KneeAdvice.NORMAL)
        }
    }

    private data class History(
        val today: LocalDate, val days: Map<LocalDate, DayRecord>, val logged: Set<String>, val first: LocalDate?, val knee: KneeAdvice,
    )

    val state: StateFlow<TrainingState?> = combine(history, settings.settings, controller.active, settings.kneeCheck, settings.streakSeen) { h, s, active, kneeCheck, seen ->
        val since = listOfNotNull(s.streakSince, h.first).minOrNull()
        TrainingState(
            program = programs.program,
            streak = StreakCalculator.compute(h.days, { WeekPlan.of(it).kind }, since, h.today),
            days = h.days,
            todayLogged = h.logged,
            active = active,
            kneeDue = kneeCheck != null && !kneeCheck.isAfter(h.today),
            knee = h.knee,
            streakSeen = seen,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val events: SharedFlow<SessionEvent> = controller.events

    /** Starts the session for [day] (or brings back the running one) and calls [then] when ready. */
    fun start(day: DayOfWeek, then: () -> Unit) {
        viewModelScope.launch {
            controller.start(day, today.value)
            WorkoutService.start(context)
            then()
        }
    }

    fun markStreakSeen(v: Int) {
        viewModelScope.launch { settings.setStreakSeen(v) }
    }

    fun answerKneeMorning(pain: Int?, sharp: Boolean) {
        viewModelScope.launch { reminders.answerKneeMorning(pain, sharp) }
    }
}
