package com.hyperathlete.app.domain

import com.hyperathlete.app.domain.program.Phase
import com.hyperathlete.app.domain.program.ProgramClock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class ProgramClockTest {

    private val sunday = LocalDate.of(2026, 10, 11) // a Sunday

    @Test fun `week starts on the Sunday on or before a date`() {
        assertEquals(sunday, ProgramClock.weekStart(sunday))
        assertEquals(sunday, ProgramClock.weekStart(LocalDate.of(2026, 10, 15))) // Thursday
        assertEquals(sunday, ProgramClock.weekStart(LocalDate.of(2026, 10, 17))) // Saturday
        assertEquals(DayOfWeek.SUNDAY, ProgramClock.weekStart(LocalDate.of(2026, 1, 1)).dayOfWeek)
    }

    @Test fun `first day is week 1 foundation and a test week`() {
        val p = ProgramClock.position(sunday, sunday)
        assertTrue(p.started)
        assertEquals(1, p.week); assertEquals(1, p.cycle); assertEquals(Phase.FOUNDATION, p.phase)
        assertTrue(p.isTestWeek)
        assertEquals(8, p.weeksUntilDeload)
    }

    @Test fun `start date mid-week snaps to that week's Sunday`() {
        val thursday = sunday.plusDays(4)
        assertEquals(1, ProgramClock.position(thursday, sunday.plusDays(6)).week)
        assertEquals(2, ProgramClock.position(thursday, sunday.plusDays(7)).week)
    }

    @Test fun `phases follow weeks 1-4, 5-8, 9`() {
        fun phaseAtWeek(w: Int) = ProgramClock.position(sunday, sunday.plusWeeks((w - 1).toLong())).phase
        (1..4).forEach { assertEquals(Phase.FOUNDATION, phaseAtWeek(it)) }
        (5..8).forEach { assertEquals(Phase.BUILD, phaseAtWeek(it)) }
        assertEquals(Phase.DELOAD, phaseAtWeek(9))
    }

    @Test fun `after week 9 the program returns to week 5 and counts cycles`() {
        assertEquals(9 to 1, ProgramClock.weekInCycle(9))
        assertEquals(5 to 2, ProgramClock.weekInCycle(10))
        assertEquals(9 to 2, ProgramClock.weekInCycle(14))
        assertEquals(5 to 3, ProgramClock.weekInCycle(15))
        val p = ProgramClock.position(sunday, sunday.plusWeeks(9)) // absolute week 10
        assertEquals(5, p.week); assertEquals(2, p.cycle); assertEquals(Phase.BUILD, p.phase)
        assertFalse(p.isTestWeek)
        assertTrue(ProgramClock.position(sunday, sunday.plusWeeks(13)).isTestWeek) // week 9 of cycle 2
    }

    @Test fun `before the start date reports days remaining`() {
        val p = ProgramClock.position(sunday, sunday.minusDays(3))
        assertFalse(p.started)
        assertEquals(3, p.daysUntilStart)
        assertEquals(1, p.week)
    }

    @Test fun `manual week offset moves the position`() {
        assertEquals(4, ProgramClock.position(sunday, sunday, weekOffset = 3).week)
        assertEquals(1, ProgramClock.position(sunday, sunday, weekOffset = -5).week) // never below week 1
    }
}
