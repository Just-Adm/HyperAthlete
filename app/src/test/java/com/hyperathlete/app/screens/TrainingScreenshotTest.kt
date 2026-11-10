package com.hyperathlete.app.screens

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.hyperathlete.app.core.locale.ProvideAppLanguage
import com.hyperathlete.app.core.workout.ActiveSession
import com.hyperathlete.app.core.workout.ExerciseContext
import com.hyperathlete.app.core.workout.SessionSummary
import com.hyperathlete.app.domain.model.AppLanguage
import com.hyperathlete.app.domain.model.AppSettings
import com.hyperathlete.app.domain.model.CalendarPrefs
import com.hyperathlete.app.domain.model.ThemeMode
import com.hyperathlete.app.domain.model.UserProfile
import com.hyperathlete.app.domain.program.Planner
import com.hyperathlete.app.domain.program.PlannedSession
import com.hyperathlete.app.domain.program.Program
import com.hyperathlete.app.domain.program.ProgramClock
import com.hyperathlete.app.domain.session.HoldTimer
import com.hyperathlete.app.domain.session.SessionEngine
import com.hyperathlete.app.domain.session.SessionState
import com.hyperathlete.app.domain.streak.DayRecord
import com.hyperathlete.app.domain.streak.StreakCalculator
import com.hyperathlete.app.domain.training.KneeAdvice
import com.hyperathlete.app.domain.training.PastExercise
import com.hyperathlete.app.domain.training.SetValues
import com.hyperathlete.app.domain.training.Suggestion
import com.hyperathlete.app.ui.AppState
import com.hyperathlete.app.ui.main.ChecklistDetails
import com.hyperathlete.app.ui.main.DayDetails
import com.hyperathlete.app.ui.main.HomeTraining
import com.hyperathlete.app.ui.main.ScheduleBox
import com.hyperathlete.app.ui.main.TodayBox
import com.hyperathlete.app.ui.settings.CalendarSettingsScreen
import com.hyperathlete.app.ui.settings.ProgramSettingsScreen
import com.hyperathlete.app.ui.settings.WorkoutSettingsScreen
import com.hyperathlete.app.ui.theme.Hyper
import com.hyperathlete.app.ui.theme.HyperTheme
import com.hyperathlete.app.ui.workout.SummaryScreen
import com.hyperathlete.app.ui.workout.WorkoutActions
import com.hyperathlete.app.ui.workout.WorkoutScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate

/** Phase 2 screens: Home training boxes, day sheet, workout player states, summary, settings. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w360dp-h802dp-xhdpi", application = Application::class)
class TrainingScreenshotTest {

    @get:Rule val compose = createComposeRule()

    private val today = LocalDate.of(2026, 10, 15) // Thursday, week 2 (program started Sun 4 Oct)
    private val program: Program by lazy { Program.parse(File("src/main/assets/program/program.json").readText()) }
    private val profile = UserProfile("آدم", LocalDate.of(2009, 3, 20), 67.0, 176.0)
    private val t0 = 1_000_000_000L

    private fun state(language: AppLanguage = AppLanguage.MIXED, theme: ThemeMode = ThemeMode.LIGHT, settings: AppSettings = AppSettings()) =
        AppState(settings.copy(onboardingDone = true, language = language, themeMode = theme, programStart = LocalDate.of(2026, 10, 4)), profile, today)

    private fun shot(name: String, language: AppLanguage = AppLanguage.MIXED, theme: ThemeMode = ThemeMode.LIGHT, content: @Composable () -> Unit) {
        compose.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                ProvideAppLanguage(language) {
                    HyperTheme(themeMode = theme) {
                        Box(Modifier.fillMaxSize().background(Hyper.colors.bg)) { content() }
                    }
                }
            }
        }
        compose.onRoot().captureRoboImage("screenshots/$name.png")
    }

    private fun training(st: AppState, logged: Set<String> = emptySet(), done: Set<LocalDate> = emptySet()) = HomeTraining(
        program = program, position = st.settings.position(today), doneDays = done, todayLogged = logged,
        active = null, kneeDue = false, knee = KneeAdvice.NORMAL, onOpenDay = {}, onChecklist = {}, onResume = {}, onKnee = {},
    )

    private fun active(day: DayOfWeek, week: Int = 2, build: (PlannedSession, SessionState) -> SessionState): ActiveSession {
        val plan = Planner.plan(program, day, week)
        val base = SessionState(sessionId = 1, epochDay = today.toEpochDay(), dayValue = day.value, week = week, cycle = 1, startedAt = t0)
        val s = build(plan, base)
        val ctx = plan.slots.associate { slot ->
            slot.key to when (slot.exercise.id) {
                "bench_press" -> ExerciseContext(
                    PastExercise(0, listOf(SetValues(60.0, 8), SetValues(60.0, 8), SetValues(60.0, 8), SetValues(60.0, 8)), false),
                    Suggestion.Increase(60.0, 62.5, 6), null,
                )
                "incline_db_press" -> ExerciseContext(PastExercise(0, listOf(SetValues(24.0, 10), SetValues(24.0, 9), SetValues(24.0, 8)), false), null, null)
                else -> ExerciseContext(null, null, null)
            }
        }
        return ActiveSession(s, plan, ctx, KneeAdvice.NORMAL)
    }

    private fun logAll(plan: PlannedSession, s: SessionState, n: Int, v: (Int) -> SetValues): SessionState {
        var x = s
        repeat(n) { i -> x = SessionEngine.log(plan, x, v(i), i.toLong(), t0 + 60_000L * (i + 1)) }
        return x
    }

    /* ---------------- Home ---------------- */

    @Test fun home_training_boxes() = shot("home_training_boxes") {
        val st = state()
        val t = training(st, logged = setOf("jumps:snap_down", "jumps:box_jump"), done = setOf(today.minusDays(4), today.minusDays(3)))
        Column(Modifier.verticalScroll(rememberScrollState()).statusBarsPadding().padding(20.dp)) {
            TodayBox(st, t)
            androidx.compose.foundation.layout.Spacer(Modifier.padding(8.dp))
            ScheduleBox(st, t)
        }
    }

    @Test fun day_sheet_thursday() = shot("day_sheet_thursday") {
        val st = state()
        DayDetails(Planner.plan(program, DayOfWeek.THURSDAY, 2), st.settings.position(today), false, false, {}, {}, Modifier.statusBarsPadding())
    }

    @Test fun day_sheet_friday_build() = shot("day_sheet_friday_build") {
        val st = state()
        DayDetails(Planner.plan(program, DayOfWeek.FRIDAY, 6), ProgramClock.position(LocalDate.of(2026, 9, 6), today), false, false, {}, {}, Modifier.statusBarsPadding())
    }

    @Test fun day_sheet_sunday_arabic() = shot("day_sheet_sunday_arabic", AppLanguage.ARABIC) {
        val st = state(AppLanguage.ARABIC)
        DayDetails(Planner.plan(program, DayOfWeek.SUNDAY, 2), st.settings.position(today), false, false, {}, {}, Modifier.statusBarsPadding())
    }

    @Test fun day_sheet_saturday_english() = shot("day_sheet_saturday_english", AppLanguage.ENGLISH) {
        val st = state(AppLanguage.ENGLISH)
        DayDetails(Planner.plan(program, DayOfWeek.SATURDAY, 2), st.settings.position(today), false, false, {}, {}, Modifier.statusBarsPadding())
    }

    @Test fun checklist() = shot("checklist") {
        ChecklistDetails(Planner.plan(program, DayOfWeek.THURSDAY, 2), setOf("jumps:snap_down", "jumps:box_jump", "sled:backward_sled_drag"), Modifier.statusBarsPadding())
    }

    /* ---------------- Workout ---------------- */

    @Test fun workout_overview() = shot("workout_overview") {
        val a = active(DayOfWeek.SUNDAY) { _, s -> s.copy(warmupStartedAt = t0, warmupMs = 0) }
        WorkoutScreen(state(), a, WorkoutActions.None, fixedNow = t0 + 312_000)
    }

    @Test fun workout_set() = shot("workout_set") {
        val a = active(DayOfWeek.SUNDAY) { plan, s -> SessionEngine.startAt(plan, s, t0) }
        WorkoutScreen(state(), a, WorkoutActions.None, fixedNow = t0 + 5_000)
    }

    @Test fun workout_tools() = shot("workout_tools") {
        val a = active(DayOfWeek.SUNDAY) { plan, s -> logAll(plan, SessionEngine.startAt(plan, s, t0), 1) { SetValues(60.0, 8, rir = 2) } }
        Column(Modifier.statusBarsPadding().padding(20.dp)) {
            com.hyperathlete.app.ui.workout.ToolRow(a, a.plan.slots.first(), {}, {}, WorkoutActions.None)
        }
    }

    @Test fun workout_set_incline() = shot("workout_set_incline") {
        val a = active(DayOfWeek.SUNDAY) { plan, s ->
            val x = logAll(plan, SessionEngine.startAt(plan, s, t0), 5) { SetValues(62.5, 6, rir = 2) }
            SessionEngine.endRest(plan, x, t0 + 900_000)
        }
        WorkoutScreen(state(), a, WorkoutActions.None, fixedNow = t0 + 905_000)
    }

    @Test fun workout_rest() = shot("workout_rest") {
        val a = active(DayOfWeek.SUNDAY) { plan, s -> logAll(plan, SessionEngine.startAt(plan, s, t0), 1) { SetValues(62.5, 6, rir = 2) } }
        // 50 s into a 2–3 min rest
        WorkoutScreen(state(), a, WorkoutActions.None, fixedNow = t0 + 60_000 + 50_000)
    }

    @Test fun workout_rest_ready_dark() = shot("workout_rest_ready_dark", theme = ThemeMode.DARK) {
        val a = active(DayOfWeek.SUNDAY) { plan, s -> logAll(plan, SessionEngine.startAt(plan, s, t0), 2) { SetValues(62.5, 6, rir = 1) } }
        WorkoutScreen(state(theme = ThemeMode.DARK), a, WorkoutActions.None, fixedNow = t0 + 120_000 + 140_000)
    }

    @Test fun workout_knee_check() = shot("workout_knee_check") {
        val a = active(DayOfWeek.FRIDAY) { plan, s -> logAll(plan, SessionEngine.startAt(plan, s, t0), 4) { SetValues(24.0, 12) } }
        WorkoutScreen(state(), a, WorkoutActions.None, fixedNow = t0 + 240_000 + 20_000)
    }

    @Test fun workout_circuit() = shot("workout_circuit") {
        val a = active(DayOfWeek.THURSDAY, week = 3) { plan, s ->
            val first = plan.steps.indexOfFirst { it.block == 2 }
            val x = SessionEngine.startAt(plan, s, t0).copy(stepIndex = first, roundStartedAt = t0)
            logAll(plan, x, 9) { SetValues(reps = 10) }.copy(rest = null, roundStartedAt = t0 + 400_000)
        }
        WorkoutScreen(state(), a, WorkoutActions.None, fixedNow = t0 + 400_000 + 95_000)
    }

    @Test fun workout_hold() = shot("workout_hold") {
        val a = active(DayOfWeek.SATURDAY) { plan, s ->
            val calf = plan.steps.indexOfFirst { plan.slotOf(it).exercise.id == "calf_stretch" }
            SessionEngine.startAt(plan, s, t0).copy(stepIndex = calf, hold = HoldTimer(calf, 45, 2, 2, t0 + 45_000))
        }
        WorkoutScreen(state(), a, WorkoutActions.None, fixedNow = t0 + 17_000)
    }

    @Test fun workout_all_done() = shot("workout_all_done") {
        val a = active(DayOfWeek.SATURDAY) { plan, s -> logAll(plan, SessionEngine.startAt(plan, s, t0), plan.steps.size) { SetValues(reps = 10) }.copy(rest = null) }
        WorkoutScreen(state(), a, WorkoutActions.None, fixedNow = t0)
    }

    @Test fun workout_rest_english() = shot("workout_rest_english", AppLanguage.ENGLISH) {
        val a = active(DayOfWeek.WEDNESDAY) { plan, s -> logAll(plan, SessionEngine.startAt(plan, s, t0), 1) { SetValues(16.0, 8, rir = 2) } }
        WorkoutScreen(state(AppLanguage.ENGLISH), a, WorkoutActions.None, fixedNow = t0 + 60_000 + 30_000)
    }

    @Test fun summary() = shot("summary") {
        val st = state()
        val days = (0L..4L).associate { today.minusDays(it) to DayRecord(sessionDone = true) }
        val streak = StreakCalculator.compute(days, { com.hyperathlete.app.domain.program.WeekPlan.of(it).kind }, today.minusDays(4), today)
        SummaryScreen(
            st,
            SessionSummary(1, today, DayOfWeek.SUNDAY, false, 54 * 60_000L + 12_000, 6240.0, 16, 1.4, listOf("bench_press", "incline_db_press"), 2),
            streak, WorkoutActions.None, onDone = {},
        )
    }

    /* ---------------- Settings ---------------- */

    @Test fun settings_program() = shot("settings_program") { ProgramSettingsScreen(state(), null, {}) }

    @Test fun settings_workout() = shot("settings_workout") { WorkoutSettingsScreen(state(), null, {}) }

    @Test fun settings_calendar() = shot("settings_calendar") {
        val st = state(settings = AppSettings(calendar = CalendarPrefs(sync = true)))
        CalendarSettingsScreen(
            st,
            listOf(
                com.hyperathlete.app.core.calendar.CalendarInfo(1, "Hyper Athlete", "Hyper Athlete", 0xFFFF6B00.toInt(), true),
                com.hyperathlete.app.core.calendar.CalendarInfo(2, "adam@gmail.com", "Google", 0xFF2F6BFF.toInt(), false),
            ),
            granted = true, status = null, onBack = {}, onToggle = {}, onChange = {}, onSyncNow = {},
        )
    }
}
