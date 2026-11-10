package com.hyperathlete.app.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hyperathlete.app.data.nutrition.NutritionRepository
import com.hyperathlete.app.data.profile.ProfileRepository
import com.hyperathlete.app.data.program.ProgramRepository
import com.hyperathlete.app.data.progress.ProgressRepository
import com.hyperathlete.app.data.settings.SettingsRepository
import com.hyperathlete.app.data.workout.WorkoutRepository
import com.hyperathlete.app.domain.model.AppSettings
import com.hyperathlete.app.domain.model.UserProfile
import com.hyperathlete.app.domain.nutrition.Macros
import com.hyperathlete.app.domain.nutrition.NutritionTargets
import com.hyperathlete.app.domain.nutrition.TargetCalculator
import com.hyperathlete.app.domain.program.Program
import com.hyperathlete.app.domain.program.ProgramClock
import com.hyperathlete.app.domain.program.ProgramPosition
import com.hyperathlete.app.domain.program.WeekPlan
import com.hyperathlete.app.domain.progress.KeyLift
import com.hyperathlete.app.domain.progress.KneePoint
import com.hyperathlete.app.domain.progress.LiftProgress
import com.hyperathlete.app.domain.progress.LoggedSet
import com.hyperathlete.app.domain.progress.Metric
import com.hyperathlete.app.domain.progress.MetricEntry
import com.hyperathlete.app.domain.progress.MuscleBalance
import com.hyperathlete.app.domain.progress.NutritionAverages
import com.hyperathlete.app.domain.progress.PersonalRecord
import com.hyperathlete.app.domain.progress.Point
import com.hyperathlete.app.domain.progress.Progress
import com.hyperathlete.app.domain.progress.SessionInfo
import com.hyperathlete.app.domain.progress.TestRow
import com.hyperathlete.app.domain.progress.TimeRange
import com.hyperathlete.app.domain.progress.WeekOverview
import com.hyperathlete.app.domain.progress.WeeklyRecap
import com.hyperathlete.app.domain.progress.WeightSummary
import com.hyperathlete.app.domain.streak.DayMark
import com.hyperathlete.app.domain.streak.DayRecord
import com.hyperathlete.app.domain.streak.StreakCalculator
import com.hyperathlete.app.domain.streak.StreakResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/** Raw history the page is computed from. */
data class ProgressInput(
    val sets: List<LoggedSet> = emptyList(),
    val sessions: List<SessionInfo> = emptyList(),
    val knee: List<KneePoint> = emptyList(),
    val metrics: List<MetricEntry> = emptyList(),
    val totals: Map<LocalDate, Macros> = emptyMap(),
    val records: Map<LocalDate, DayRecord> = emptyMap(),
    val firstDay: LocalDate? = null,
    val suggestionAnswered: LocalDate? = null,
)

/** Everything the progress page draws, for one time range. Built by [of] so screenshot tests can make one. */
data class ProgressUi(
    val today: LocalDate,
    val range: TimeRange,
    val from: LocalDate,
    val program: Program,
    val position: ProgramPosition,
    val overview: WeekOverview,
    val streak: StreakResult,
    val weight: WeightSummary,
    val showSuggestion: Boolean,
    val nutrition7: NutritionAverages,
    val nutrition30: NutritionAverages,
    val lifts: List<LiftProgress>,
    val records: List<PersonalRecord>,
    val balance: MuscleBalance,
    val knee: List<Point>,
    val kneeToWallL: List<Point>,
    val kneeToWallR: List<Point>,
    val deepSquat: List<Point>,
    val tests: List<TestRow>,
    val testWeek: Boolean,
    val testsDone: Boolean,
    val heatmap: List<Pair<LocalDate, DayMark>>,
    val thigh: List<Point>,
    val arm: List<Point>,
    val recap: WeeklyRecap,
    val hasTraining: Boolean,
) {
    companion object {
        fun of(input: ProgressInput, settings: AppSettings, profile: UserProfile, program: Program, today: LocalDate, range: TimeRange): ProgressUi {
            val earliest = listOfNotNull(input.firstDay, input.metrics.minOfOrNull { it.date }, settings.programStart).minOrNull()
            val from = range.start(today, earliest)
            val targetOn: (LocalDate) -> NutritionTargets = { d -> TargetCalculator.targets(profile, settings.nutrition, d) }
            val since = listOfNotNull(settings.streakSince, input.firstDay).minOrNull()
            val streak = StreakCalculator.compute(input.records, { WeekPlan.of(it).kind }, since, today)
            val weight = Progress.weight(input.metrics, today, from)
            val pos = settings.position(today)
            val weekStart = ProgramClock.weekStart(today)
            val heatStart = (since ?: today.minusWeeks(8)).let { s -> if (range.weeks != null) maxOf(s, from) else s }
            return ProgressUi(
                today = today, range = range, from = from, program = program, position = pos,
                overview = Progress.weekOverview(input.sessions, input.sets, today),
                streak = streak,
                weight = weight,
                showSuggestion = weight.suggestMore && input.suggestionAnswered?.let { it.plusDays(14).isBefore(today) } != false,
                nutrition7 = Progress.nutrition(input.totals, targetOn, today, 7),
                nutrition30 = Progress.nutrition(input.totals, targetOn, today, 30),
                lifts = KeyLift.entries.map { Progress.lift(it, input.sets, from) },
                records = Progress.records(input.sets).take(6),
                balance = Progress.balance(input.sets, from, today),
                knee = Progress.knee(input.knee, from),
                kneeToWallL = Progress.series(input.metrics, Metric.KNEE_TO_WALL_L),
                kneeToWallR = Progress.series(input.metrics, Metric.KNEE_TO_WALL_R),
                deepSquat = (Progress.series(input.metrics, Metric.DEEP_SQUAT) + Progress.deepSquatHolds(input.sets, LocalDate.MIN))
                    .groupBy { it.date }.map { (d, l) -> Point(d, l.maxOf { it.value }) }.sortedBy { it.date },
                tests = Progress.tests(input.metrics),
                testWeek = pos.started && pos.isTestWeek,
                testsDone = input.metrics.any { it.test && it.date in weekStart..weekStart.plusDays(6) },
                heatmap = Progress.heatmap(input.records, heatStart.coerceAtMost(today), today),
                thigh = Progress.series(input.metrics, Metric.THIGH),
                arm = Progress.series(input.metrics, Metric.ARM),
                recap = Progress.recap(input.sets, input.sessions, input.totals, targetOn, input.metrics, today),
                hasTraining = input.sets.isNotEmpty(),
            )
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ProgressViewModel @Inject constructor(
    private val workouts: WorkoutRepository,
    private val progress: ProgressRepository,
    private val nutrition: NutritionRepository,
    private val settings: SettingsRepository,
    private val profiles: ProfileRepository,
    private val programs: ProgramRepository,
    private val clock: Clock,
) : ViewModel() {

    private val today = MutableStateFlow(LocalDate.now(clock))
    val range = MutableStateFlow(TimeRange.W8)

    fun refreshDay() {
        today.value = LocalDate.now(clock)
        // Weigh-ins from a smart scale or another health app (when Health Connect sync is on).
        viewModelScope.launch { runCatching { progress.syncHealth() } }
    }

    private val training: Flow<Triple<List<LoggedSet>, List<SessionInfo>, List<KneePoint>>> =
        combine(workouts.allSets, workouts.completedSessions, workouts.kneeReports) { sets, sessions, knee ->
            Triple(
                sets.map { LoggedSet(it.sessionId, LocalDate.ofEpochDay(it.epochDay), it.exerciseId, it.weightKg, it.reps, it.seconds, it.rir, it.deload) },
                sessions.map { SessionInfo(it.id, LocalDate.ofEpochDay(it.epochDay), it.mobility, it.volumeKg, it.sets, it.avgRir) },
                knee.map { KneePoint(LocalDate.ofEpochDay(it.epochDay), it.during, it.morning) },
            )
        }

    private val input: Flow<ProgressInput> = today.flatMapLatest { d ->
        combine(
            training,
            progress.metrics,
            nutrition.totals(d.minusDays(400), d),
            workouts.days(d.minusDays(400)),
            combine(workouts.firstCompletedDay(), settings.suggestionAnswered) { a, b -> a to b },
        ) { t, metrics, totals, records, extra ->
            ProgressInput(t.first, t.second, t.third, metrics, totals, records, extra.first, extra.second)
        }
    }

    val ui: StateFlow<ProgressUi?> = combine(input, settings.settings, profiles.profile, range, today) { i, s, p, r, d ->
        ProgressUi.of(i, s, p, programs.program, d, r)
    }.flowOn(kotlinx.coroutines.Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setRange(r: TimeRange) { range.value = r }

    fun addWeight(kg: Double) = viewModelScope.launch { progress.saveWeight(today.value, kg) }

    fun saveMeasurements(thigh: Double?, arm: Double?) = viewModelScope.launch {
        thigh?.let { progress.save(MetricEntry(metric = Metric.THIGH, date = today.value, value = it)) }
        arm?.let { progress.save(MetricEntry(metric = Metric.ARM, date = today.value, value = it)) }
    }

    fun saveTests(values: Map<Metric, Double>) = viewModelScope.launch {
        val s = settings.current()
        val pos = s.position(today.value)
        progress.saveTests(today.value, pos.week, pos.cycle, values)
    }

    fun deleteMetric(e: MetricEntry) = viewModelScope.launch { progress.delete(e) }

    /** "+200 kcal" accepted: raise the surplus and stay quiet for two weeks. */
    fun applySuggestion() = viewModelScope.launch {
        val s = settings.current().nutrition
        settings.setNutritionPrefs(s.copy(surplus = (s.surplus + 200).coerceAtMost(1000)))
        settings.setSuggestionAnswered(today.value)
    }

    fun dismissSuggestion() = viewModelScope.launch { settings.setSuggestionAnswered(today.value) }
}
