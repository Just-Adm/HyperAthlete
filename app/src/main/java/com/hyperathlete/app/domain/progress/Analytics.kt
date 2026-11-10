package com.hyperathlete.app.domain.progress

import com.hyperathlete.app.domain.nutrition.Adherence
import com.hyperathlete.app.domain.nutrition.AdherenceRule
import com.hyperathlete.app.domain.nutrition.Macros
import com.hyperathlete.app.domain.nutrition.NutritionTargets
import com.hyperathlete.app.domain.program.ProgramClock
import com.hyperathlete.app.domain.program.SessionKind
import com.hyperathlete.app.domain.program.WeekPlan
import com.hyperathlete.app.domain.streak.DayMark
import com.hyperathlete.app.domain.streak.DayRecord
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

/* ------------------------------------------------------------------ inputs */

data class LoggedSet(
    val sessionId: Long,
    val date: LocalDate,
    val exerciseId: String,
    val weightKg: Double?,
    val reps: Int?,
    val seconds: Int?,
    val rir: Int?,
    val deload: Boolean,
)

data class SessionInfo(val id: Long, val date: LocalDate, val mobility: Boolean, val volumeKg: Double, val sets: Int, val avgRir: Double?)

data class KneePoint(val date: LocalDate, val during: Int?, val morning: Int?)

/** Date window of the time filter at the top of the page (brief §12). */
enum class TimeRange(val weeks: Long?) {
    W4(4), W8(8), M6(26), ALL(null);

    fun start(today: LocalDate, earliest: LocalDate?): LocalDate =
        weeks?.let { today.minusWeeks(it).plusDays(1) } ?: (earliest ?: today.minusWeeks(4)).coerceAtMost(today)
}

data class Point(val date: LocalDate, val value: Double)

/* ------------------------------------------------------------------ results */

data class WeekOverview(val sessionsDone: Int, val sessionsPlanned: Int, val volumeKg: Double, val avgRir: Double?)

enum class WeightPace { GAINING_RIGHT, TOO_SLOW, TOO_FAST, LOSING, UNKNOWN }

data class WeightSummary(
    val current: Double?,
    val start: Double?,
    val points: List<Point>,
    /** 7-day moving average at every weigh-in. */
    val average: List<Point>,
    /** kg per month over the last four weeks of the average. */
    val monthlyRate: Double?,
    val pace: WeightPace,
    /** Brief §16: no movement for two weeks → suggest +200 kcal. */
    val suggestMore: Boolean,
) {
    val change: Double? get() = if (current != null && start != null) current - start else null
}

data class NutritionAverages(
    val days: Int,
    val loggedDays: Int,
    val avgKcal: Double,
    val avgProtein: Double,
    val targetKcal: Double,
    val targetProtein: Double,
    /** Share of the window's days that were on target. */
    val adherence: Double,
    val marks: List<Pair<LocalDate, Adherence>>,
)

data class BestSet(val date: LocalDate, val weightKg: Double, val reps: Int, val e1rm: Double)

data class LiftProgress(val lift: KeyLift, val exerciseId: String?, val series: List<Point>, val best: BestSet?, val first: Double?, val latest: Double?)

data class PersonalRecord(val date: LocalDate, val exerciseId: String, val weightKg: Double, val reps: Int, val e1rm: Double)

data class MuscleBalance(
    /** Average hard sets per week, per muscle. */
    val weeklySets: Map<Muscle, Double>,
    /** Upper ÷ lower sets, one point per week. */
    val ratio: List<Point>,
    /** Ratio is moving toward 1 (or below) — the lower body is catching up. */
    val closing: Boolean?,
)

data class TestRow(val metric: Metric, val first: Double?, val firstIsBaseline: Boolean, val latest: Double?, val latestDate: LocalDate?) {
    /** +1 better, −1 worse, 0 same / unknown. */
    val trend: Int get() {
        if (first == null || latest == null || abs(latest - first) < 1e-6) return 0
        val up = latest > first
        return if (up == metric.higherIsBetter) 1 else -1
    }
}

data class WeeklyRecap(
    val liftGain: Pair<KeyLift, Double>?,
    val proteinPct: Int?,
    val sessions: Int,
    val planned: Int,
    val weightChange: Double?,
)

/* ------------------------------------------------------------------ calculations */

object Progress {

    /** Epley one-rep max; a single is its own max. */
    fun e1rm(weightKg: Double, reps: Int): Double = if (reps <= 1) weightKg else weightKg * (1 + reps / 30.0)

    private fun LoggedSet.e1rmOrNull(): Double? {
        val w = weightKg ?: return null
        val r = reps ?: return null
        if (w <= 0 || r <= 0) return null
        return e1rm(w, r)
    }

    fun weekOverview(sessions: List<SessionInfo>, sets: List<LoggedSet>, today: LocalDate): WeekOverview {
        val start = ProgramClock.weekStart(today)
        val end = start.plusDays(6)
        val week = sessions.filter { !it.mobility && it.date in start..end }
        val weekSets = sets.filter { it.date in start..end && !it.deload }
        val volume = weekSets.sumOf { (it.weightKg ?: 0.0) * (it.reps ?: 0) }
        val rirs = weekSets.mapNotNull { it.rir }
        return WeekOverview(
            sessionsDone = week.map { it.date }.distinct().size,
            sessionsPlanned = WeekPlan.days.count { it.kind == SessionKind.TRAINING },
            volumeKg = volume,
            avgRir = if (rirs.isEmpty()) null else rirs.average(),
        )
    }

    /** Weight trend, 7-day average, pace against +0.5…1 kg a month, and the two-week stall rule. */
    fun weight(entries: List<MetricEntry>, today: LocalDate, from: LocalDate): WeightSummary {
        val all = entries.filter { it.metric == Metric.WEIGHT }.sortedBy { it.date }
        if (all.isEmpty()) return WeightSummary(null, null, emptyList(), emptyList(), null, WeightPace.UNKNOWN, false)
        fun avgAt(d: LocalDate): Double? = all.filter { !it.date.isAfter(d) && it.date.isAfter(d.minusDays(7)) }.map { it.value }.takeIf { it.isNotEmpty() }?.average()
        val avg = all.map { Point(it.date, avgAt(it.date)!!) }
        val inRange = all.filter { !it.date.isBefore(from) }
        val now = avgAt(today) ?: all.last().value
        val spanDays = ChronoUnit.DAYS.between(all.first().date, today)
        val monthAgo = avgAt(today.minusDays(28)) ?: if (spanDays >= 14) all.first().value else null
        val rate = monthAgo?.let { m ->
            val days = minOf(28L, spanDays).coerceAtLeast(1)
            (now - m) * 30.0 / days
        }
        val pace = when {
            rate == null || spanDays < 14 -> WeightPace.UNKNOWN
            rate < -0.2 -> WeightPace.LOSING
            rate < 0.4 -> WeightPace.TOO_SLOW
            rate > 1.3 -> WeightPace.TOO_FAST
            else -> WeightPace.GAINING_RIGHT
        }
        val twoWeeksAgo = avgAt(today.minusDays(14))
        val recent = all.any { !it.date.isBefore(today.minusDays(4)) }
        val stalled = spanDays >= 14 && recent && twoWeeksAgo != null && now - twoWeeksAgo < 0.2
        return WeightSummary(
            current = all.last().value, start = all.first().value,
            points = inRange.map { Point(it.date, it.value) },
            average = avg.filter { !it.date.isBefore(from) },
            monthlyRate = rate, pace = pace, suggestMore = stalled,
        )
    }

    fun nutrition(totals: Map<LocalDate, Macros>, targetOn: (LocalDate) -> NutritionTargets, today: LocalDate, days: Int): NutritionAverages {
        val window = (0 until days).map { today.minusDays(it.toLong()) }.reversed()
        val logged = window.filter { d -> totals[d]?.let { it.kcal > 0 || it.protein > 0 } == true }
        val marks = window.map { d -> d to AdherenceRule.of(totals[d] ?: Macros.ZERO, targetOn(d)) }
        val targets = window.map(targetOn)
        return NutritionAverages(
            days = days,
            loggedDays = logged.size,
            avgKcal = if (logged.isEmpty()) 0.0 else logged.sumOf { totals[it]!!.kcal } / logged.size,
            avgProtein = if (logged.isEmpty()) 0.0 else logged.sumOf { totals[it]!!.protein } / logged.size,
            targetKcal = targets.map { it.kcal }.average(),
            targetProtein = targets.map { it.protein }.average(),
            adherence = marks.count { it.second == Adherence.GOOD } / days.toDouble(),
            marks = marks,
        )
    }

    /** Best estimated 1RM per session for a key lift (deload sets left out). */
    fun lift(lift: KeyLift, sets: List<LoggedSet>, from: LocalDate): LiftProgress {
        // The variant trained most recently is the one shown (goblet → back squat).
        val ids = lift.exerciseIds
        val used = sets.filter { it.exerciseId in ids && !it.deload && it.e1rmOrNull() != null }
        val id = used.maxByOrNull { it.date }?.exerciseId
        val mine = used.filter { it.exerciseId == id }
        val perSession = mine.groupBy { it.sessionId }.map { (_, list) ->
            val best = list.maxBy { it.e1rmOrNull()!! }
            Point(best.date, best.e1rmOrNull()!!)
        }.sortedBy { it.date }
        val bestSet = mine.maxByOrNull { it.e1rmOrNull()!! }?.let { BestSet(it.date, it.weightKg!!, it.reps!!, it.e1rmOrNull()!!) }
        val shown = perSession.filter { !it.date.isBefore(from) }
        return LiftProgress(lift, id, shown, bestSet, perSession.firstOrNull()?.value, perSession.lastOrNull()?.value)
    }

    /** Every time an exercise's estimated 1RM beat its previous best (first session = baseline, not a record). */
    fun records(sets: List<LoggedSet>): List<PersonalRecord> {
        val out = mutableListOf<PersonalRecord>()
        sets.filter { !it.deload && it.e1rmOrNull() != null }.groupBy { it.exerciseId }.forEach { (id, list) ->
            var best: Double? = null
            list.groupBy { it.sessionId }.values.sortedBy { it.first().date }.forEach { session ->
                val top = session.maxBy { it.e1rmOrNull()!! }
                val v = top.e1rmOrNull()!!
                val prev = best
                if (prev != null && v > prev + 1e-6) out += PersonalRecord(top.date, id, top.weightKg!!, top.reps!!, v)
                best = max(prev ?: 0.0, v)
            }
        }
        return out.sortedByDescending { it.date }
    }

    fun balance(sets: List<LoggedSet>, from: LocalDate, today: LocalDate): MuscleBalance {
        val window = sets.filter { !it.date.isBefore(from) && !it.date.isAfter(today) }
        val weeks = (ChronoUnit.DAYS.between(from, today) + 1).coerceAtLeast(7) / 7.0
        val totals = mutableMapOf<Muscle, Double>()
        window.forEach { s -> MuscleMap.byExercise[s.exerciseId]?.forEach { (m, w) -> totals[m] = (totals[m] ?: 0.0) + w } }
        // A detail (forearm flexors, soleus…) shows its group's sets, plus any set aimed at it directly.
        val weekly = Muscle.entries.associateWith { m ->
            ((totals[m] ?: 0.0) + (m.parent?.let { totals[it] } ?: 0.0)) / weeks
        }
        // Only finished weeks: a half-done week (legs come on Friday) would swing the ratio.
        val thisWeek = ProgramClock.weekStart(today)
        val byWeek = window.filter { it.date.isBefore(thisWeek) }.groupBy { ProgramClock.weekStart(it.date) }.toSortedMap()
        val ratio = byWeek.mapNotNull { (start, list) ->
            var up = 0.0
            var low = 0.0
            list.forEach { s ->
                MuscleMap.byExercise[s.exerciseId]?.forEach { (m, w) ->
                    when (m.region) {
                        Muscle.Region.UPPER -> up += w
                        Muscle.Region.LOWER -> low += w
                        Muscle.Region.CORE, Muscle.Region.ACCESSORY -> Unit
                    }
                }
            }
            if (low <= 0) null else Point(start, up / low)
        }
        val closing = if (ratio.size < 3) null else {
            val half = ratio.size / 2
            val early = ratio.take(half).map { abs(it.value - 1) }.average()
            val late = ratio.takeLast(half).map { abs(it.value - 1) }.average()
            late < early
        }
        return MuscleBalance(weekly, ratio, closing)
    }

    fun knee(points: List<KneePoint>, from: LocalDate): List<Point> =
        points.filter { !it.date.isBefore(from) && it.during != null }
            .groupBy { it.date }.map { (d, l) -> Point(d, l.maxOf { it.during!! }.toDouble()) }.sortedBy { it.date }

    fun series(entries: List<MetricEntry>, metric: Metric, from: LocalDate? = null): List<Point> =
        entries.filter { it.metric == metric && (from == null || !it.date.isBefore(from)) }.sortedBy { it.date }.map { Point(it.date, it.value) }

    /** Deep-squat holds logged in the mobility routine, best per day (seconds). */
    fun deepSquatHolds(sets: List<LoggedSet>, from: LocalDate): List<Point> =
        sets.filter { it.exerciseId == "deep_squat_hold" && it.seconds != null && !it.date.isBefore(from) }
            .groupBy { it.date }.map { (d, l) -> Point(d, l.maxOf { it.seconds!! }.toDouble()) }.sortedBy { it.date }

    /** Tests table: first test (or the starting value from the brief) against the latest one. */
    fun tests(entries: List<MetricEntry>): List<TestRow> = Metric.Tests.map { m ->
        val tests = entries.filter { it.metric == m && it.test }.sortedBy { it.date }
        val all = entries.filter { it.metric == m }.sortedBy { it.date }
        val first = tests.firstOrNull()?.value
        val latest = (tests.lastOrNull() ?: all.lastOrNull())
        val useBaseline = first == null || (tests.size == 1 && m.baseline != null && latest == tests.first())
        TestRow(
            metric = m,
            first = if (useBaseline) m.baseline ?: first else first,
            firstIsBaseline = useBaseline && m.baseline != null,
            latest = latest?.value,
            latestDate = latest?.date,
        )
    }

    /** Heatmap marks from the first tracked day to today. */
    fun heatmap(records: Map<LocalDate, DayRecord>, start: LocalDate, today: LocalDate): List<Pair<LocalDate, DayMark>> {
        val out = mutableListOf<Pair<LocalDate, DayMark>>()
        var d = start
        while (!d.isAfter(today)) {
            val rec = records[d]
            val rest = WeekPlan.of(d.dayOfWeek).kind == SessionKind.REST
            out += d to when {
                rec?.sessionDone == true -> DayMark.DONE
                rest -> DayMark.REST
                d == today -> DayMark.TODAY
                else -> DayMark.MISSED
            }
            d = d.plusDays(1)
        }
        return out
    }

    /** The weekly card (brief §12.11): a short summary built from these numbers. */
    fun recap(sets: List<LoggedSet>, sessions: List<SessionInfo>, totals: Map<LocalDate, Macros>, targetOn: (LocalDate) -> NutritionTargets, weights: List<MetricEntry>, today: LocalDate): WeeklyRecap {
        val weekAgo = today.minusDays(7)
        val gains = KeyLift.entries.mapNotNull { lift ->
            val l = lift(lift, sets, LocalDate.MIN)
            val before = l.series.filter { !it.date.isAfter(weekAgo) }.maxOfOrNull { it.value } ?: return@mapNotNull null
            val now = l.series.filter { it.date.isAfter(weekAgo) }.maxOfOrNull { it.value } ?: return@mapNotNull null
            (lift to (now - before)).takeIf { it.second >= 0.5 }
        }
        val n = nutrition(totals, targetOn, today, 7)
        val w = weights.filter { it.metric == Metric.WEIGHT }.sortedBy { it.date }
        val wNow = w.lastOrNull()
        val wThen = w.lastOrNull { !it.date.isAfter(weekAgo) }
        val ov = weekOverview(sessions, sets, today)
        return WeeklyRecap(
            liftGain = gains.maxByOrNull { it.second },
            proteinPct = if (n.loggedDays == 0) null else (n.avgProtein / n.targetProtein * 100).roundToInt(),
            sessions = ov.sessionsDone, planned = ov.sessionsPlanned,
            weightChange = if (wNow != null && wThen != null && wNow != wThen) wNow.value - wThen.value else null,
        )
    }
}
