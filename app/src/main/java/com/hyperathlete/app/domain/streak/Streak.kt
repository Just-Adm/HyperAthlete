package com.hyperathlete.app.domain.streak

import com.hyperathlete.app.domain.program.SessionKind
import java.time.DayOfWeek
import java.time.LocalDate

/** What happened on a day, as recorded by the workout player (Phase 2). */
data class DayRecord(val sessionDone: Boolean = false, val mobilityDone: Boolean = false)

enum class DayMark {
    /** Training session finished. */
    DONE,
    /** Planned rest day (with [DayStatus.bonus] when the mobility routine was done). */
    REST,
    /** Training day that passed without the session. */
    MISSED,
    /** Today, not finished yet. */
    TODAY,
    UPCOMING,
    /** Before tracking started: neutral, never counts as missed. */
    NONE,
}

data class DayStatus(val date: LocalDate, val mark: DayMark, val bonus: Boolean = false)

data class StreakResult(val current: Int, val best: Int, val week: List<DayStatus>) {
    /** Milestones that get a special celebration. */
    val milestone: Int? get() = MILESTONES.firstOrNull { it == current }

    companion object {
        val MILESTONES = listOf(7, 14, 30, 60, 100)
    }
}

/**
 * Streak rules from the brief:
 *  - a training day counts once its session is finished;
 *  - a planned rest day never breaks the streak (it counts as a streak day; mobility adds a bonus mark);
 *  - a training day that passes unfinished breaks it;
 *  - today never breaks it until it is over.
 * Days before [trackingStart] are neutral.
 */
object StreakCalculator {

    fun compute(
        records: Map<LocalDate, DayRecord>,
        kindOf: (DayOfWeek) -> SessionKind,
        trackingStart: LocalDate?,
        today: LocalDate,
    ): StreakResult {
        var run = 0
        var best = 0
        if (trackingStart != null && !trackingStart.isAfter(today)) {
            var d: LocalDate = trackingStart
            while (!d.isAfter(today)) {
                val rec = records[d]
                val rest = kindOf(d.dayOfWeek) == SessionKind.REST
                when {
                    rec?.sessionDone == true || rest -> run++ // finished session, or a planned rest day
                    d == today -> Unit // today's session is still pending
                    else -> run = 0 // missed training day
                }
                best = maxOf(best, run)
                d = d.plusDays(1)
            }
        }
        return StreakResult(run, best, week(records, kindOf, trackingStart, today))
    }

    /** Sunday → Saturday marks for the week containing [today]. */
    fun week(
        records: Map<LocalDate, DayRecord>,
        kindOf: (DayOfWeek) -> SessionKind,
        trackingStart: LocalDate?,
        today: LocalDate,
    ): List<DayStatus> {
        val sunday = today.minusDays((today.dayOfWeek.value % 7).toLong())
        return (0L until 7L).map { i ->
            val d = sunday.plusDays(i)
            val rec = records[d]
            val rest = kindOf(d.dayOfWeek) == SessionKind.REST
            val tracked = trackingStart != null && !d.isBefore(trackingStart)
            val mark = when {
                rec?.sessionDone == true -> DayMark.DONE
                rest && !d.isAfter(today) -> DayMark.REST
                d == today -> DayMark.TODAY
                d.isAfter(today) -> if (rest) DayMark.REST else DayMark.UPCOMING
                !tracked -> DayMark.NONE
                else -> DayMark.MISSED
            }
            DayStatus(d, mark, bonus = rest && rec?.mobilityDone == true)
        }
    }
}
