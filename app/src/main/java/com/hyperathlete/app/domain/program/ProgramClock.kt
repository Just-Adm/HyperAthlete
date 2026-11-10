package com.hyperathlete.app.domain.program

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

enum class Phase { FOUNDATION, BUILD, DELOAD }

/**
 * Where the user is in the program on a given day.
 * @param week 1..9 inside the current cycle
 * @param cycle 1 for the first pass; after week 9 the program returns to week 5 and the cycle increments
 * @param absoluteWeek weeks since the start (1-based), counting every cycle
 */
data class ProgramPosition(
    val started: Boolean,
    val daysUntilStart: Int,
    val absoluteWeek: Int,
    val week: Int,
    val cycle: Int,
    val phase: Phase,
    val dayOfWeek: DayOfWeek,
) {
    val isTestWeek: Boolean get() = (week == 1 && cycle == 1) || week == 9
    val weeksUntilDeload: Int get() = ProgramClock.WEEKS - week
}

/**
 * Program calendar: 9-week blocks (1–4 foundation, 5–8 build, 9 deload + tests),
 * then back to week 5 with the new numbers. Weeks run Sunday → Saturday.
 */
object ProgramClock {
    const val WEEKS = 9
    const val FOUNDATION_END = 4
    const val BUILD_END = 8
    const val REPEAT_FROM = 5

    fun weekStart(date: LocalDate): LocalDate = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))

    fun phaseOf(week: Int): Phase = when {
        week <= FOUNDATION_END -> Phase.FOUNDATION
        week <= BUILD_END -> Phase.BUILD
        else -> Phase.DELOAD
    }

    /** Maps an absolute week (1, 2, 3 …) onto (week-in-cycle, cycle). */
    fun weekInCycle(absoluteWeek: Int): Pair<Int, Int> {
        require(absoluteWeek >= 1)
        if (absoluteWeek <= WEEKS) return absoluteWeek to 1
        val loopLength = WEEKS - REPEAT_FROM + 1 // weeks 5..9 = 5 weeks
        val k = absoluteWeek - WEEKS - 1
        return (REPEAT_FROM + k % loopLength) to (2 + k / loopLength)
    }

    /**
     * @param start the chosen program start date (snapped to its week's Sunday)
     * @param weekOffset manual adjustment from settings ("edit current week")
     */
    fun position(start: LocalDate, today: LocalDate, weekOffset: Int = 0): ProgramPosition {
        val anchor = weekStart(start)
        val days = ChronoUnit.DAYS.between(anchor, today).toInt()
        if (days < 0) {
            return ProgramPosition(
                started = false, daysUntilStart = -days, absoluteWeek = 1, week = 1, cycle = 1,
                phase = Phase.FOUNDATION, dayOfWeek = today.dayOfWeek,
            )
        }
        val absolute = (days / 7 + 1 + weekOffset).coerceAtLeast(1)
        val (week, cycle) = weekInCycle(absolute)
        return ProgramPosition(
            started = true, daysUntilStart = 0, absoluteWeek = absolute, week = week, cycle = cycle,
            phase = phaseOf(week), dayOfWeek = today.dayOfWeek,
        )
    }
}
