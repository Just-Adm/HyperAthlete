package com.hyperathlete.app.domain

import com.hyperathlete.app.domain.nutrition.Adherence
import com.hyperathlete.app.domain.nutrition.Macros
import com.hyperathlete.app.domain.nutrition.NutritionTargets
import com.hyperathlete.app.domain.progress.KeyLift
import com.hyperathlete.app.domain.progress.LoggedSet
import com.hyperathlete.app.domain.progress.Metric
import com.hyperathlete.app.domain.progress.MetricEntry
import com.hyperathlete.app.domain.progress.Muscle
import com.hyperathlete.app.domain.progress.MuscleMap
import com.hyperathlete.app.domain.progress.Progress
import com.hyperathlete.app.domain.progress.SessionInfo
import com.hyperathlete.app.domain.progress.TimeRange
import com.hyperathlete.app.domain.progress.WeightPace
import com.hyperathlete.app.domain.program.Program
import com.hyperathlete.app.domain.streak.DayMark
import com.hyperathlete.app.domain.streak.DayRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate

class ProgressAnalyticsTest {
    private val today = LocalDate.of(2026, 10, 15) // Thursday
    private var sid = 0L

    private fun session(date: LocalDate, vararg sets: Triple<String, Double, Int>, deload: Boolean = false): List<LoggedSet> {
        val id = ++sid
        return sets.map { (ex, w, r) -> LoggedSet(id, date, ex, w, r, null, 2, deload) }
    }

    @Test fun `every strength exercise in the program has muscles, stretches don't`() {
        val program = Program.parse(File("src/main/assets/program/program.json").readText())
        val stretches = setOf("knee_to_wall", "calf_stretch", "couch_stretch", "hamstring_stretch", "deep_squat_hold", "wall_slides", "single_leg_balance")
        for (e in program.exercises) {
            if (e.id in stretches) assertNull(e.id, MuscleMap.byExercise[e.id]) else assertNotNull(e.id, MuscleMap.byExercise[e.id])
        }
        KeyLift.entries.flatMap { it.exerciseIds }.forEach { id -> assertTrue(id, program.exercises.any { it.id == id }) }
    }

    @Test fun `epley estimate, best set and records`() {
        assertEquals(80.0, Progress.e1rm(60.0, 10), 1e-9)
        assertEquals(100.0, Progress.e1rm(100.0, 1), 1e-9)
        val sets = session(today.minusDays(14), Triple("bench_press", 60.0, 8), Triple("bench_press", 60.0, 7)) +
            session(today.minusDays(7), Triple("bench_press", 62.5, 6)) +
            session(today, Triple("bench_press", 62.5, 8)) +
            session(today.plusDays(0), Triple("bench_press", 70.0, 3), deload = true)
        val lift = Progress.lift(KeyLift.BENCH, sets, today.minusDays(30))
        assertEquals(3, lift.series.size)
        assertEquals(62.5 * (1 + 8 / 30.0), lift.best!!.e1rm, 1e-9)
        assertEquals(today, lift.best!!.date)
        val prs = Progress.records(sets)
        // 60×8 = 76.0 → 62.5×6 = 75.0 (no) → 62.5×8 = 79.2 (yes); the deload set never counts
        assertEquals(1, prs.size)
        assertEquals(today, prs.first().date)
    }

    @Test fun `squat follows the variant trained last`() {
        val sets = session(today.minusDays(30), Triple("goblet_squat", 24.0, 10)) + session(today, Triple("back_squat", 60.0, 6))
        assertEquals("back_squat", Progress.lift(KeyLift.SQUAT, sets, today.minusDays(60)).exerciseId)
        assertNull(Progress.lift(KeyLift.DIPS, sets, today.minusDays(60)).exerciseId)
    }

    @Test fun `muscle volume per week and the upper-lower ratio`() {
        val sets = session(today.minusDays(21), *Array(8) { Triple("bench_press", 60.0, 8) }, Triple("back_squat", 60.0, 6)) +
            session(today.minusDays(14), *Array(8) { Triple("bench_press", 60.0, 8) }, *Array(4) { Triple("back_squat", 60.0, 6) }) +
            session(today.minusDays(7), *Array(4) { Triple("bench_press", 60.0, 8) }, *Array(6) { Triple("back_squat", 60.0, 6) }) +
            session(today, *Array(9) { Triple("bench_press", 60.0, 8) })
        val b = Progress.balance(sets, today.minusDays(27), today)
        assertEquals(29.0 / 4.0, b.weeklySets[Muscle.CHEST]!!, 1e-9)
        // the running week is left out of the ratio
        assertEquals(3, b.ratio.size)
        assertTrue(b.ratio.first().value > b.ratio.last().value)
        assertEquals(true, b.closing)
    }

    @Test fun `muscle ids from before 3_3_0 keep their names and order`() {
        val old = listOf(
            "CHEST", "FRONT_DELTS", "SIDE_DELTS", "BICEPS", "FOREARMS", "ABS", "OBLIQUES", "QUADS", "ADDUCTORS", "TIBIALIS",
            "TRAPS", "REAR_DELTS", "LATS", "MID_BACK", "TRICEPS", "LOWER_BACK", "GLUTES", "HAMSTRINGS", "CALVES",
        )
        assertEquals(old, Muscle.entries.take(old.size).map { it.name })
        old.forEach { assertEquals(it, null, Muscle.valueOf(it).parent) }
    }

    @Test fun `small muscle groups show their group's sets and stay out of the ratio`() {
        val sets = session(today.minusDays(7), *Array(4) { Triple("hammer_curl", 12.0, 10) }, *Array(3) { Triple("face_pull", 20.0, 15) }) +
            session(today.minusDays(6), *Array(3) { Triple("box_jump", 0.0, 5) })
        val b = Progress.balance(sets, today.minusDays(13), today)
        val forearms = b.weeklySets.getValue(Muscle.FOREARMS)
        assertEquals(2.0, forearms, 1e-9)
        assertEquals(forearms, b.weeklySets.getValue(Muscle.FOREARM_FLEXORS), 1e-9)
        assertEquals(forearms, b.weeklySets.getValue(Muscle.FOREARM_EXTENSORS), 1e-9)
        assertEquals(b.weeklySets.getValue(Muscle.CALVES), b.weeklySets.getValue(Muscle.SOLEUS), 1e-9)
        assertEquals(b.weeklySets.getValue(Muscle.CALVES), b.weeklySets.getValue(Muscle.GASTROCNEMIUS), 1e-9)
        assertEquals(0.75, b.weeklySets.getValue(Muscle.ROTATOR_CUFF), 1e-9)
        // Accessory sets don't move the upper-to-lower ratio: same ratio without the rotator-cuff credit.
        val upper = 4 * (1.0 + 0.5) + 3 * (1.0 + 0.5)
        val lower = 3 * (0.5 + 0.5 + 0.5)
        assertEquals(upper / lower, b.ratio.single().value, 1e-9)
        // "Least trained" only lists big groups.
        assertTrue(Muscle.entries.filter { it.major }.none { it.parent != null || it.region == Muscle.Region.ACCESSORY })
    }

    @Test fun `every muscle is drawn, mirrored left and right`() {
        val drawn = (com.hyperathlete.app.ui.progress.BodyArt.front + com.hyperathlete.app.ui.progress.BodyArt.back).map { it.first }
        val groups = Muscle.entries.filter { Muscle.detailsOf(it).isEmpty() }
        groups.forEach { assertTrue("$it has no shape", it in drawn) }
        // Shapes come in left/right pairs.
        (com.hyperathlete.app.ui.progress.BodyArt.front + com.hyperathlete.app.ui.progress.BodyArt.back)
            .groupingBy { it.first }.eachCount().forEach { (m, n) -> assertEquals("$m", 0, n % 2) }
    }

    @Test fun `weight trend, pace and the two-week stall`() {
        val gaining = (0..30).map { MetricEntry(metric = Metric.WEIGHT, date = today.minusDays(30L - it), value = 67.0 + it * 0.025) }
        val g = Progress.weight(gaining, today, TimeRange.W4.start(today, null))
        assertEquals(WeightPace.GAINING_RIGHT, g.pace)
        assertFalse(g.suggestMore)
        assertEquals(0.75, g.change!!, 1e-9)
        val flat = (0..20).map { MetricEntry(metric = Metric.WEIGHT, date = today.minusDays(20L - it), value = 67.0 + (it % 2) * 0.1) }
        val f = Progress.weight(flat, today, today.minusDays(27))
        assertTrue(f.suggestMore)
        assertEquals(WeightPace.TOO_SLOW, f.pace)
        assertEquals(WeightPace.UNKNOWN, Progress.weight(gaining.takeLast(5), today, today.minusDays(27)).pace)
    }

    @Test fun `nutrition averages count logged days and adherence over the window`() {
        val t = NutritionTargets(3000, 134, 400, 80, 1650, 2550, false)
        val totals = mapOf(
            today to Macros(2950.0, 130.0),
            today.minusDays(1) to Macros(2000.0, 80.0),
            today.minusDays(2) to Macros(3100.0, 140.0),
        )
        val n = Progress.nutrition(totals, { t }, today, 7)
        assertEquals(3, n.loggedDays)
        assertEquals((2950 + 2000 + 3100) / 3.0, n.avgKcal, 1e-9)
        assertEquals(2 / 7.0, n.adherence, 1e-9)
        assertEquals(Adherence.NONE, n.marks.first().second)
        assertEquals(7, n.marks.size)
    }

    @Test fun `tests table uses the brief's starting values until a second test`() {
        val first = listOf(
            MetricEntry(metric = Metric.PLANK, date = today.minusDays(56), value = 75.0, test = true),
            MetricEntry(metric = Metric.RUN_5K, date = today.minusDays(56), value = 1260.0, test = true),
        )
        val rows = Progress.tests(first)
        val plank = rows.first { it.metric == Metric.PLANK }
        assertTrue(plank.firstIsBaseline)
        assertEquals(60.0, plank.first!!, 0.0)
        assertEquals(1, plank.trend)
        assertEquals(-1, rows.first { it.metric == Metric.RUN_5K }.trend) // slower than 20:00
        val later = first + MetricEntry(metric = Metric.PLANK, date = today, value = 95.0, test = true)
        val p2 = Progress.tests(later).first { it.metric == Metric.PLANK }
        assertFalse(p2.firstIsBaseline)
        assertEquals(75.0, p2.first!!, 0.0)
        assertEquals(95.0, p2.latest!!, 0.0)
        assertNull(Progress.tests(emptyList()).first { it.metric == Metric.KNEE_TO_WALL_L }.latest)
    }

    @Test fun `week overview and heatmap`() {
        val sunday = LocalDate.of(2026, 10, 11)
        val sessions = listOf(
            SessionInfo(1, sunday, false, 4000.0, 16, 1.5), SessionInfo(2, sunday.plusDays(1), false, 3000.0, 14, 2.0),
            SessionInfo(3, sunday.plusDays(2), true, 0.0, 9, null), SessionInfo(4, sunday.minusDays(1), false, 1.0, 1, null),
        )
        val sets = session(sunday, Triple("bench_press", 60.0, 8)) + session(sunday.plusDays(1), Triple("lat_pulldown", 50.0, 10))
        val o = Progress.weekOverview(sessions, sets, today)
        assertEquals(2, o.sessionsDone)
        assertEquals(5, o.sessionsPlanned)
        assertEquals(60.0 * 8 + 50.0 * 10, o.volumeKg, 1e-9)
        val marks = Progress.heatmap(mapOf(sunday to DayRecord(sessionDone = true)), sunday, today).toMap()
        assertEquals(DayMark.DONE, marks[sunday])
        assertEquals(DayMark.MISSED, marks[sunday.plusDays(1)])
        assertEquals(DayMark.REST, marks[sunday.plusDays(2)])
        assertEquals(DayMark.TODAY, marks[today])
    }

    @Test fun everyTestHasAnEnglishVideoQuery() {
        for (m in com.hyperathlete.app.domain.progress.Metric.entries) {
            val q = com.hyperathlete.app.domain.progress.Metric.videoQuery(m)
            org.junit.Assert.assertTrue(q, q.isNotBlank() && q.all { it.code < 128 })
        }
    }
}
