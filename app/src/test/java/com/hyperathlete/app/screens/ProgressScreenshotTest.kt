package com.hyperathlete.app.screens

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
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
import com.hyperathlete.app.domain.layout.BoxId
import com.hyperathlete.app.domain.layout.PageId
import com.hyperathlete.app.domain.layout.PageLayout
import com.hyperathlete.app.domain.model.AppLanguage
import com.hyperathlete.app.domain.model.AppSettings
import com.hyperathlete.app.domain.model.ThemeMode
import com.hyperathlete.app.domain.model.UserProfile
import com.hyperathlete.app.domain.nutrition.Macros
import com.hyperathlete.app.domain.program.Program
import com.hyperathlete.app.domain.program.SessionKind
import com.hyperathlete.app.domain.program.WeekPlan
import com.hyperathlete.app.domain.progress.KneePoint
import com.hyperathlete.app.domain.progress.LoggedSet
import com.hyperathlete.app.domain.progress.Metric
import com.hyperathlete.app.domain.progress.MetricEntry
import com.hyperathlete.app.domain.progress.SessionInfo
import com.hyperathlete.app.domain.progress.TimeRange
import com.hyperathlete.app.domain.streak.DayRecord
import com.hyperathlete.app.ui.AppState
import com.hyperathlete.app.ui.progress.AddMeasurementSheet
import com.hyperathlete.app.ui.progress.AddWeightSheet
import com.hyperathlete.app.ui.progress.ProgressDeps
import com.hyperathlete.app.ui.progress.ProgressInput
import com.hyperathlete.app.ui.progress.ProgressScreen
import com.hyperathlete.app.ui.progress.ProgressUi
import com.hyperathlete.app.ui.progress.TestEntryScreen
import com.hyperathlete.app.ui.settings.ProgressSettingsScreen
import com.hyperathlete.app.ui.theme.Hyper
import com.hyperathlete.app.ui.theme.HyperTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.LocalDate
import kotlin.random.Random

/** Phase 4 screens: the progress page with eight weeks of made-up history, entry sheets, test form, settings. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w360dp-h802dp-xhdpi", application = Application::class)
class ProgressScreenshotTest {

    @get:Rule val compose = createComposeRule()

    private val today = LocalDate.of(2026, 10, 15) // Thursday, week 8
    private val start = LocalDate.of(2026, 8, 23)
    private val program: Program by lazy { Program.parse(File("src/main/assets/program/program.json").readText()) }
    private val profile = UserProfile("آدم", LocalDate.of(2001, 3, 20), 68.4, 176.0)
    private val settings = AppSettings(onboardingDone = true, programStart = start, streakSince = start)

    private fun shot(
        name: String, language: AppLanguage = AppLanguage.MIXED, theme: ThemeMode = ThemeMode.LIGHT,
        unit: com.hyperathlete.app.domain.model.WeightUnit = com.hyperathlete.app.domain.model.WeightUnit.KG, content: @Composable () -> Unit,
    ) {
        compose.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                ProvideAppLanguage(language) {
                    HyperTheme(themeMode = theme, weightUnit = unit) {
                        Box(Modifier.fillMaxSize().background(Hyper.colors.bg)) { content() }
                    }
                }
            }
        }
        compose.onRoot().captureRoboImage("screenshots/$name.png")
    }

    private val input: ProgressInput by lazy {
        val r = Random(7)
        val sets = mutableListOf<LoggedSet>()
        val sessions = mutableListOf<SessionInfo>()
        val records = mutableMapOf<LocalDate, DayRecord>()
        val knee = mutableListOf<KneePoint>()
        var id = 0L
        var d = start
        var week = 0
        while (d.isBefore(today)) {
            val t = WeekPlan.of(d.dayOfWeek)
            week = ((d.toEpochDay() - start.toEpochDay()) / 7).toInt()
            val skip = d == today.minusDays(9) // one missed day
            if (t.kind == SessionKind.TRAINING && !skip) {
                id++
                val g = week * 1.25
                fun add(ex: String, w: Double, reps: Int, n: Int) = repeat(n) { sets += LoggedSet(id, d, ex, w, reps - (if (it == n - 1) 1 else 0), null, 2, false) }
                when (d.dayOfWeek.value) {
                    7 -> { add("bench_press", 55 + g, 8, 4); add("incline_db_press", 20 + week * 0.5, 10, 3); add("cable_fly", 12.5, 12, 3); add("seated_db_curl", 12 + week * 0.25, 10, 3); add("hammer_curl", 14.0, 10, 3) }
                    1 -> { add("lat_pulldown", 50 + g, 10, 4); add("seated_cable_row", 45 + g, 10, 3); add("weighted_dips", 5 + week * 1.0, 8, 3); add("overhead_cable_ext", 20.0, 12, 3); add("back_extension", 10.0, 12, 3) }
                    3 -> { add("seated_db_shoulder_press", 16 + week * 0.5, 10, 4); add("db_lateral_raise", 8.0, 15, 4); add("face_pull", 20.0, 15, 3); add("db_shrug", 26.0, 12, 3) }
                    4 -> { add("kb_swing", 16.0, 15, 3); add("box_jump", 0.0, 5, 3); add("sled_push", 60.0, 1, 3); add("hanging_leg_raise", 0.0, 10, 3); add("copenhagen", 0.0, 1, 2) }
                    5 -> { add(if (week < 4) "goblet_squat" else "back_squat", if (week < 4) 20 + week * 2.0 else 40 + g * 2, 8, 4); add("rdl", 50 + g * 1.6, 8, 3); add("bulgarian_split_squat", 10 + week * 1.0, 10, 3); add("hip_adduction", 35.0, 12, 3); add("seated_leg_curl", 30.0, 12, 3); add("wall_tib_raise", 0.0, 15, 2) }
                }
                sessions += SessionInfo(id, d, false, 4000.0 + week * 150, 16, 2.0)
                records[d] = DayRecord(sessionDone = true)
                if (d.dayOfWeek.value == 5 || d.dayOfWeek.value == 4) knee += KneePoint(d, (5 - week / 2).coerceAtLeast(1) + r.nextInt(0, 2), null)
            } else if (t.kind == SessionKind.REST) {
                id++
                sets += LoggedSet(id, d, "deep_squat_hold", null, null, 10 + week * 4, null, false)
                sessions += SessionInfo(id, d, true, 0.0, 9, null)
                records[d] = DayRecord(mobilityDone = true)
            }
            d = d.plusDays(1)
        }
        val metrics = mutableListOf<MetricEntry>()
        (0..52).forEach { i ->
            if (i % 2 == 0 || i > 40) metrics += MetricEntry(metric = Metric.WEIGHT, date = start.plusDays(i.toLong()), value = 67.0 + i * 0.026 + r.nextDouble(-0.25, 0.25))
        }
        val tests = mapOf(
            Metric.VERTICAL_JUMP to 16.0, Metric.KNEE_TO_WALL_L to 5.5, Metric.KNEE_TO_WALL_R to 6.0, Metric.DEEP_SQUAT to 0.0,
            Metric.BALANCE_L to 9.0, Metric.BALANCE_R to 12.0, Metric.PLANK to 62.0, Metric.RUN_5K to 1215.0, Metric.RUN_10K to 3190.0,
            Metric.THIGH to 52.0, Metric.ARM to 31.5,
        )
        tests.forEach { (m, v) -> metrics += MetricEntry(metric = m, date = start.plusDays(1), value = v, week = 1, cycle = 1, test = true) }
        metrics += MetricEntry(metric = Metric.KNEE_TO_WALL_L, date = start.plusDays(30), value = 7.5)
        metrics += MetricEntry(metric = Metric.KNEE_TO_WALL_R, date = start.plusDays(30), value = 8.0)
        metrics += MetricEntry(metric = Metric.THIGH, date = start.plusDays(28), value = 53.0)
        metrics += MetricEntry(metric = Metric.ARM, date = start.plusDays(28), value = 32.0)
        metrics += MetricEntry(metric = Metric.THIGH, date = today.minusDays(2), value = 53.8)
        metrics += MetricEntry(metric = Metric.ARM, date = today.minusDays(2), value = 32.4)
        val totals = (0 until 30).associate { i ->
            today.minusDays(i.toLong()) to if (i == 4 || i == 11) Macros.ZERO else Macros(2500.0 + r.nextInt(-400, 500), 100.0 + r.nextInt(-20, 40))
        }.filterValues { it.kcal > 0 }
        ProgressInput(sets, sessions, knee, metrics, totals, records, start, null)
    }

    private fun ui(range: TimeRange = TimeRange.W8, input: ProgressInput = this.input, today: LocalDate = this.today, s: AppSettings = settings) =
        ProgressUi.of(input, s, profile, program, today, range)

    private fun deps(vararg order: BoxId) = ProgressDeps(
        if (order.isEmpty()) PageLayout.default(PageId.PROGRESS) else PageLayout(PageId.PROGRESS, order.toList(), emptySet()),
    )

    @Test fun progress_top() = shot("progress_top") { ProgressScreen(ui(), deps()) }

    @Test fun progress_dark() = shot("progress_dark", theme = ThemeMode.DARK) { ProgressScreen(ui(), deps()) }

    @Test fun progress_english() = shot("progress_english", AppLanguage.ENGLISH) { ProgressScreen(ui(), deps()) }

    @Test fun progress_weight_nutrition() = shot("progress_weight_nutrition") { ProgressScreen(ui(), deps(BoxId.PROGRESS_WEIGHT, BoxId.PROGRESS_NUTRITION)) }

    @Test fun progress_stall_suggestion() = shot("progress_stall") {
        val flat = input.copy(metrics = input.metrics.filter { it.metric != Metric.WEIGHT } +
            (0..20).map { MetricEntry(metric = Metric.WEIGHT, date = today.minusDays(20L - it), value = 68.3 + (it % 2) * 0.1) })
        ProgressScreen(ui(input = flat), deps(BoxId.PROGRESS_WEIGHT))
    }

    @Test fun progress_strength() = shot("progress_strength") { ProgressScreen(ui(), deps(BoxId.PROGRESS_STRENGTH)) }

    @Test fun progress_balance() = shot("progress_balance") { ProgressScreen(ui(), deps(BoxId.PROGRESS_BALANCE)) }

    @Test fun progress_balance_dark() = shot("progress_balance_dark", theme = ThemeMode.DARK) {
        ProgressScreen(ui(), ProgressDeps(deps(BoxId.PROGRESS_BALANCE).layout, bodySelection = com.hyperathlete.app.domain.progress.Muscle.GASTROCNEMIUS))
    }

    @Test fun progress_balance_selected() = shot("progress_balance_selected") {
        ProgressScreen(ui(), ProgressDeps(deps(BoxId.PROGRESS_BALANCE).layout, bodySelection = com.hyperathlete.app.domain.progress.Muscle.FOREARM_FLEXORS))
    }

    @Test fun progress_knee_tests() = shot("progress_knee_tests") { ProgressScreen(ui(), deps(BoxId.PROGRESS_KNEE, BoxId.PROGRESS_TESTS)) }

    @Test fun progress_tests_table() = shot("progress_tests_table") { ProgressScreen(ui(), deps(BoxId.PROGRESS_TESTS, BoxId.PROGRESS_CONSISTENCY)) }

    @Test fun progress_recap() = shot("progress_recap") {
        ProgressScreen(ui(), deps(BoxId.PROGRESS_RECAP, BoxId.PROGRESS_WEIGHT))
    }

    @Test fun progress_pounds() = shot("progress_pounds", unit = com.hyperathlete.app.domain.model.WeightUnit.LB) {
        ProgressScreen(ui(), deps(BoxId.PROGRESS_WEIGHT, BoxId.PROGRESS_STRENGTH))
    }

    @Test fun progress_bottom() = shot("progress_bottom") { ProgressScreen(ui(), deps(BoxId.PROGRESS_CONSISTENCY, BoxId.PROGRESS_MEASUREMENTS, BoxId.PROGRESS_RECAP)) }

    @Test fun progress_empty() = shot("progress_empty") {
        ProgressScreen(ui(TimeRange.W4, ProgressInput(), LocalDate.of(2026, 10, 6), settings.copy(programStart = LocalDate.of(2026, 10, 4))), deps())
    }

    @Test fun progress_test_week() = shot("progress_test_week") {
        ProgressScreen(ui(s = settings.copy(programStart = LocalDate.of(2026, 10, 11))), deps(BoxId.PROGRESS_PHASE, BoxId.PROGRESS_TESTS))
    }

    @Test fun test_entry_form() = shot("test_entry_form") {
        val last = input.metrics.filter { it.test }.associateBy { it.metric }
        TestEntryScreen("الأسبوع 9 من 9 · تخفيف", last, {}, {})
    }

    @Test fun test_entry_how_open() = shot("test_entry_how_open") {
        val last = input.metrics.filter { it.test }.associateBy { it.metric }
        TestEntryScreen("الأسبوع 9 من 9 · تخفيف", last, {}, {}, initiallyOpen = com.hyperathlete.app.domain.progress.Metric.KNEE_TO_WALL_L)
    }

    @Test fun add_weight_and_measure() = shot("add_weight_measure") {
        Column(Modifier.statusBarsPadding().padding(top = 12.dp)) {
            AddWeightSheet(68.4) {}
            AddMeasurementSheet(53.8, 32.4) { _, _ -> }
        }
    }

    @Test fun progress_settings() = shot("progress_settings") {
        ProgressSettingsScreen(
            AppState(settings.copy(progress = settings.progress.copy(healthSync = true)), profile, today), {}, {},
            health = com.hyperathlete.app.ui.settings.HealthDeps(granted = true),
        )
    }
}
