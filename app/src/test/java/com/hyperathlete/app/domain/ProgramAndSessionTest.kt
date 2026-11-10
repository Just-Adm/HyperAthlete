package com.hyperathlete.app.domain

import com.hyperathlete.app.domain.program.BlockType
import com.hyperathlete.app.domain.program.Planner
import com.hyperathlete.app.domain.program.Program
import com.hyperathlete.app.domain.program.SessionKind
import com.hyperathlete.app.domain.program.WeekPlan
import com.hyperathlete.app.domain.session.SessionEngine
import com.hyperathlete.app.domain.session.SessionState
import com.hyperathlete.app.domain.session.Stage
import com.hyperathlete.app.domain.training.KneeAdvice
import com.hyperathlete.app.domain.training.KneeReport
import com.hyperathlete.app.domain.training.KneeRule
import com.hyperathlete.app.domain.training.Motivation
import com.hyperathlete.app.domain.training.PastExercise
import com.hyperathlete.app.domain.training.Progression
import com.hyperathlete.app.domain.training.RirTarget
import com.hyperathlete.app.domain.training.SetValues
import com.hyperathlete.app.domain.training.Suggestion
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate

object Seed {
    val program: Program by lazy { Program.parse(File("src/main/assets/program/program.json").readText()) }
}

class ProgramSeedTest {
    private val p = Seed.program

    @Test fun `every day of the brief's table is there with the right exercise count`() {
        for (t in WeekPlan.days) {
            val plan = Planner.plan(p, t.day, week = 1)
            val count = plan.slots.size
            assertEquals("${t.day}", t.exerciseCount, count)
        }
    }

    @Test fun `Thursday has four blocks A-D with a superset and a circuit`() {
        val thu = Planner.plan(p, DayOfWeek.THURSDAY, 1)
        assertEquals(listOf("jumps", "sled", "circuit", "core"), thu.blocks.map { it.id })
        assertEquals(BlockType.SUPERSET, thu.blocks[1].type)
        assertEquals(BlockType.CIRCUIT, thu.blocks[2].type)
        assertEquals(4, thu.blocks[2].rounds)
    }

    @Test fun `phase variants follow the week`() {
        fun fri(week: Int) = Planner.plan(p, DayOfWeek.FRIDAY, week).slots.first().exercise.id
        assertEquals("goblet_squat", fri(4))
        assertEquals("back_squat", fri(5))
        fun circuitFirst(week: Int) = Planner.plan(p, DayOfWeek.THURSDAY, week).blocks[2].slots.first().exercise.id
        assertEquals("kb_deadlift", circuitFirst(2))
        assertEquals("tire_flip", circuitFirst(3))
        val jumps = Planner.plan(p, DayOfWeek.THURSDAY, 6).blocks[0].slots.map { it.exercise.id }
        assertEquals(listOf("box_jump", "broad_jump"), jumps)
    }

    @Test fun `back extensions are body weight in the foundation and loaded in the build phase`() {
        fun load(week: Int) = Planner.plan(p, DayOfWeek.MONDAY, week).slots.first { it.exercise.id == "back_extension" }.load
        assertEquals(com.hyperathlete.app.domain.program.Load.BODYWEIGHT, load(2))
        assertEquals(com.hyperathlete.app.domain.program.Load.WEIGHTED, load(6))
    }

    @Test fun `deload week halves sets and rounds but keeps the exercises`() {
        val normal = Planner.plan(p, DayOfWeek.SUNDAY, 8)
        val deload = Planner.plan(p, DayOfWeek.SUNDAY, 9)
        assertTrue(deload.deload)
        assertEquals(normal.slots.map { it.exercise.id }, deload.slots.map { it.exercise.id })
        assertEquals(2, deload.slots.first().sets) // bench 4 → 2
        assertEquals(2, deload.slots[1].sets)      // incline 3 → 2 (rounded up)
        assertEquals(2, Planner.plan(p, DayOfWeek.THURSDAY, 9).blocks[2].rounds)
        assertFalse(Planner.plan(p, DayOfWeek.TUESDAY, 9).deload) // mobility is never "deloaded"
    }

    @Test fun `tempo cue only in the foundation phase and only for lower-body lifts`() {
        assertTrue(Planner.plan(p, DayOfWeek.FRIDAY, 2).slots.first().tempo)
        assertFalse(Planner.plan(p, DayOfWeek.FRIDAY, 6).slots.first().tempo)
        assertFalse(Planner.plan(p, DayOfWeek.SUNDAY, 2).slots.first().tempo)
    }

    @Test fun `swaps only accept the alternatives written in the program`() {
        val swapped = Planner.plan(p, DayOfWeek.SUNDAY, 1, mapOf("main:cable_fly" to "pec_deck", "main:bench_press" to "pec_deck"))
        assertEquals("pec_deck", swapped.slot("main:cable_fly")!!.exercise.id)
        assertEquals("bench_press", swapped.slot("main:bench_press")!!.exercise.id)
        assertEquals(listOf("cable_fly"), swapped.slot("main:cable_fly")!!.alternatives.map { it.id })
    }

    @Test fun `steps interleave supersets and circuits by round, with rest after the round`() {
        val thu = Planner.plan(p, DayOfWeek.THURSDAY, 1)
        val sled = thu.steps.filter { it.block == 1 }
        assertEquals(8, sled.size)
        assertEquals(listOf(0, 1, 0, 1), sled.take(4).map { it.slot })
        assertEquals(0, sled[0].restMax)
        assertEquals(90, sled[1].restMax)
        assertEquals(0, thu.steps.last().restMax)
    }

    @Test fun `every pictogram the program uses exists`() {
        val art = Json.parseToJsonElement(File("src/main/assets/program/art.json").readText()).jsonObject
        for (e in p.exercises) assertTrue(e.art, e.art in art)
        assertTrue("STRETCH" in art)
    }

    @Test fun `rest days are the mobility routine`() {
        assertEquals(SessionKind.REST, WeekPlan.of(DayOfWeek.SATURDAY).kind)
        assertTrue(Planner.plan(p, DayOfWeek.SATURDAY, 3).isMobility)
    }
}

class ProgressionTest {
    private val p = Seed.program
    private fun slot(day: DayOfWeek, id: String, week: Int = 2) = Planner.plan(p, day, week).slots.first { it.exercise.id == id }
    private fun past(vararg sets: Pair<Double, Int>, deload: Boolean = false) =
        PastExercise(0, sets.map { SetValues(weightKg = it.first, reps = it.second) }, deload)

    @Test fun `top of the range on every set earns the jump, back to the bottom of the range`() {
        val incline = slot(DayOfWeek.SUNDAY, "incline_db_press") // 3 × 8-10, dumbbells
        val s = Progression.suggest(incline, past(24.0 to 10, 24.0 to 10, 24.0 to 10), deload = false)
        assertEquals(Suggestion.Increase(24.0, 26.0, 8), s)
        val bench = slot(DayOfWeek.SUNDAY, "bench_press") // 4 × 6-8, barbell
        assertEquals(Suggestion.Increase(60.0, 62.5, 6), Progression.suggest(bench, past(60.0 to 8, 60.0 to 8, 60.0 to 8, 60.0 to 8), false))
    }

    @Test fun `one short set or a missing set means stay`() {
        val incline = slot(DayOfWeek.SUNDAY, "incline_db_press")
        assertNull(Progression.suggest(incline, past(24.0 to 10, 24.0 to 10, 24.0 to 9), false))
        assertNull(Progression.suggest(incline, past(24.0 to 10, 24.0 to 10), false))
    }

    @Test fun `dips go on the belt only after 3 x 10 with body weight`() {
        val dips = slot(DayOfWeek.MONDAY, "weighted_dips")
        assertNull(Progression.suggest(dips, past(0.0 to 10, 0.0 to 9, 0.0 to 8), false))
        assertEquals(Suggestion.AddBelt(2.5), Progression.suggest(dips, past(0.0 to 10, 0.0 to 10, 0.0 to 10), false))
    }

    @Test fun `deload is 10 percent lighter on the plate step`() {
        val bench = slot(DayOfWeek.SUNDAY, "bench_press", 9)
        assertEquals(Suggestion.Deload(55.0), Progression.suggest(bench, past(60.0 to 8, 62.5 to 6), deload = true))
        // a deload session is never used as proof for the next jump
        assertNull(Progression.suggest(bench, past(55.0 to 8, 55.0 to 8, deload = true), false))
    }

    @Test fun `prefill repeats last time and applies an earned jump`() {
        val incline = slot(DayOfWeek.SUNDAY, "incline_db_press")
        val last = past(24.0 to 10, 24.0 to 9, 24.0 to 8)
        val v = Progression.prefill(incline, 2, last, null, emptyList())
        assertEquals(24.0, v.weightKg)
        assertEquals(9, v.reps)
        val up = Progression.prefill(incline, 1, last, Suggestion.Increase(24.0, 26.0, 8), emptyList())
        assertEquals(26.0, up.weightKg); assertEquals(8, up.reps)
        val fresh = Progression.prefill(slot(DayOfWeek.SUNDAY, "bench_press"), 1, null, null, emptyList())
        assertEquals(20.0, fresh.weightKg) // empty bar
    }

    @Test fun `RIR targets - compound stops short, isolation goes to failure on the last set only`() {
        val bench = slot(DayOfWeek.SUNDAY, "bench_press")
        val fly = slot(DayOfWeek.SUNDAY, "cable_fly")
        assertEquals(RirTarget.STOP_SHORT, Progression.rirTarget(bench, 4, 4))
        assertEquals(RirTarget.STOP_SHORT, Progression.rirTarget(fly, 2, 3))
        assertEquals(RirTarget.FAILURE, Progression.rirTarget(fly, 3, 3))
    }

    @Test fun `records need history and a heavier weight or better estimate`() {
        val before = listOf(SetValues(60.0, 8), SetValues(60.0, 7))
        assertTrue(Progression.isRecord(listOf(SetValues(62.5, 6)), before))
        assertTrue(Progression.isRecord(listOf(SetValues(60.0, 10)), before))
        assertFalse(Progression.isRecord(listOf(SetValues(60.0, 8)), before))
        assertFalse(Progression.isRecord(listOf(SetValues(60.0, 8)), emptyList()))
    }

    @Test fun `last-time summary`() {
        assertEquals("60 kg × 8، 8، 7", Progression.summarize(listOf(SetValues(60.0, 8), SetValues(60.0, 8), SetValues(60.0, 7)), "kg", "، "))
        assertEquals("60×8, 62.5×6", Progression.summarize(listOf(SetValues(60.0, 8), SetValues(62.5, 6)), "kg", ", "))
    }
}

class KneeAndMotivationTest {
    @Test fun `knee rule`() {
        assertEquals(KneeAdvice.NORMAL, KneeRule.evaluate(KneeReport(3, 2)))
        assertEquals(KneeAdvice.EASE_OFF, KneeRule.evaluate(KneeReport(4, null)))
        assertEquals(KneeAdvice.EASE_OFF, KneeRule.evaluate(KneeReport(2, 3))) // worse next morning
        assertEquals(KneeAdvice.SEE_PHYSIO, KneeRule.evaluate(KneeReport(1, null, sharpOrSwollen = true)))
    }

    @Test fun `motivation is stable for a date and changes across days`() {
        val d = LocalDate.of(2026, 10, 16)
        assertEquals(Motivation.lineFor(d, false), Motivation.lineFor(d, false))
        val week = (0L until 14L).map { Motivation.lineFor(d.plusDays(it), false) }.toSet()
        assertTrue(week.size > 7)
        assertEquals("Today we build.", Motivation.render("%s, today we build.", ""))
        assertEquals("آدم، يلا", Motivation.render("%s، يلا", "آدم"))
    }
}

class SessionEngineTest {
    private val p = Seed.program
    private fun state() = SessionState(sessionId = 1, epochDay = 0, dayValue = DayOfWeek.SUNDAY.value, week = 2, cycle = 1, startedAt = 0)

    @Test fun `warm-up, sets, rest and finish`() {
        val plan = Planner.plan(p, DayOfWeek.SUNDAY, 2)
        var s = SessionEngine.toggleWarmup(state(), 0)
        s = SessionEngine.startAt(plan, s, 300_000)
        assertEquals(Stage.WORK, s.stage)
        assertEquals(300_000, s.warmupMs)
        s = SessionEngine.log(plan, s, SetValues(60.0, 8), 11, 400_000)
        assertEquals(1, s.stepIndex)
        val rest = s.rest!!
        assertEquals(400_000 + 120_000, rest.readyAt)
        assertEquals(400_000 + 180_000, rest.endsAt)
        s = SessionEngine.extendRest(s, 30)
        assertEquals(400_000 + 210_000, s.rest!!.endsAt)
        // finish every set
        var t = 1_000_000L
        while (!SessionEngine.allDone(plan, s)) { s = SessionEngine.log(plan, s, SetValues(10.0, 10), 0, t); t += 1000 }
        assertNull(s.rest) // nothing after the last set
        assertNull(SessionEngine.current(plan, s))
        assertEquals(plan.totalSets to plan.totalSets, SessionEngine.progress(plan, s))
    }

    @Test fun `skip, jump back and undo`() {
        val plan = Planner.plan(p, DayOfWeek.SUNDAY, 2)
        var s = SessionEngine.startAt(plan, state(), 0)
        s = SessionEngine.skipSlot(plan, s, "main:bench_press", 0)
        assertEquals("main:incline_db_press", SessionEngine.stepKey(plan, s.stepIndex))
        s = SessionEngine.log(plan, s, SetValues(24.0, 10), 1, 10)
        s = SessionEngine.jumpTo(plan, s, "main:bench_press")
        assertEquals(0, s.stepIndex)
        assertFalse("main:bench_press" in s.skipped)
        val (undone, removed) = SessionEngine.undo(s)
        assertEquals(1, removed!!.dbId)
        assertTrue(undone.logs.isEmpty())
    }

    @Test fun `knee check after the squat and after the whole sled block`() {
        val fri = Planner.plan(p, DayOfWeek.FRIDAY, 2)
        var s = SessionEngine.startAt(fri, state().copy(dayValue = 5), 0)
        repeat(3) { s = SessionEngine.log(fri, s, SetValues(30.0, 12), 0, it.toLong()) }
        assertNull(s.kneeAsk)
        s = SessionEngine.log(fri, s, SetValues(30.0, 12), 0, 5)
        assertEquals("goblet_squat", s.kneeAsk)
        s = SessionEngine.answerKnee(s)

        val thu = Planner.plan(p, DayOfWeek.THURSDAY, 2)
        var t = SessionEngine.startAt(thu, state().copy(dayValue = 4), 0)
        t = SessionEngine.skipSlot(thu, t, "jumps:snap_down", 0)
        t = SessionEngine.skipSlot(thu, t, "jumps:box_jump", 0)
        repeat(7) { t = SessionEngine.log(thu, t, SetValues(40.0, meters = 20), 0, it.toLong()) }
        assertNull(t.kneeAsk)
        t = SessionEngine.log(thu, t, SetValues(40.0, meters = 20), 0, 9)
        assertNotNull(t.kneeAsk)
    }

    @Test fun `circuit round timer restarts after the round rest`() {
        val thu = Planner.plan(p, DayOfWeek.THURSDAY, 3)
        val first = thu.steps.indexOfFirst { it.block == 2 }
        var s = SessionEngine.startAt(thu, state().copy(dayValue = 4), 0).copy(stepIndex = first)
        repeat(thu.blocks[2].slots.size) { s = SessionEngine.log(thu, s, SetValues(reps = 5), 0, 1000L * it) }
        assertNotNull(s.rest)
        s = SessionEngine.endRest(thu, s, 200_000)
        assertEquals(200_000L, s.roundStartedAt)
    }

    @Test fun `timed holds run both sides with a switch`() {
        val sat = Planner.plan(p, DayOfWeek.SATURDAY, 2)
        val calf = sat.steps.indexOfFirst { sat.slotOf(it).exercise.id == "calf_stretch" }
        var s = SessionEngine.startAt(sat, state().copy(dayValue = 6), 0).copy(stepIndex = calf)
        assertTrue(SessionEngine.isHold(sat, s))
        s = SessionEngine.startHold(sat, s, 45, 0)
        var (s2, done) = SessionEngine.tickHold(s, 45_000)
        assertFalse(done)
        assertEquals(2, s2.hold!!.side)
        assertEquals(45_000 + 5_000 + 45_000L, s2.hold!!.endsAt)
        val (_, done2) = SessionEngine.tickHold(s2, 95_000)
        assertTrue(done2)
    }

    @Test fun `pause shifts the timers`() {
        val plan = Planner.plan(p, DayOfWeek.SUNDAY, 2)
        var s = SessionEngine.startAt(plan, state(), 0)
        s = SessionEngine.log(plan, s, SetValues(60.0, 8), 1, 1000)
        val end = s.rest!!.endsAt
        s = SessionEngine.pause(s, 2000)
        s = SessionEngine.resume(s, 12_000)
        assertEquals(end + 10_000, s.rest!!.endsAt)
        assertEquals(10_000L, s.pausedMs)
    }

    @Test fun `snapshot survives a round trip`() {
        val plan = Planner.plan(p, DayOfWeek.SUNDAY, 2)
        val s = SessionEngine.log(plan, SessionEngine.startAt(plan, state(), 0), SetValues(60.0, 8, rir = 2), 7, 10)
        assertEquals(s, SessionState.decode(s.encode()))
    }
}
