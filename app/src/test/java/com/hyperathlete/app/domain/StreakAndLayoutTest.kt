package com.hyperathlete.app.domain

import com.hyperathlete.app.domain.layout.BoxId
import com.hyperathlete.app.domain.layout.PageId
import com.hyperathlete.app.domain.layout.PageLayout
import com.hyperathlete.app.domain.program.WeekPlan
import com.hyperathlete.app.domain.streak.DayMark
import com.hyperathlete.app.domain.streak.DayRecord
import com.hyperathlete.app.domain.streak.StreakCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class StreakTest {
    private val kind = { d: java.time.DayOfWeek -> WeekPlan.of(d).kind }
    private val sun = LocalDate.of(2026, 10, 11) // Sunday (training); Tue + Sat are rest days

    private fun done(vararg days: LocalDate) = days.associateWith { DayRecord(sessionDone = true) }

    @Test fun `finished sessions and planned rest days build the streak`() {
        // Sun ✓, Mon ✓, Tue rest, Wed ✓ (today)
        val r = StreakCalculator.compute(done(sun, sun.plusDays(1), sun.plusDays(3)), kind, sun, sun.plusDays(3))
        assertEquals(4, r.current)
        assertEquals(4, r.best)
    }

    @Test fun `a missed training day breaks it, today's pending session does not`() {
        // Sun ✓, Mon missed, Tue rest, Wed today pending
        val r = StreakCalculator.compute(done(sun), kind, sun, sun.plusDays(3))
        assertEquals(1, r.current) // only Tuesday's rest after the break
        assertEquals(1, r.best)
        assertEquals(DayMark.MISSED, r.week[1].mark)
        assertEquals(DayMark.TODAY, r.week[3].mark)
    }

    @Test fun `rest days never break and mobility adds a bonus mark`() {
        val records = done(sun, sun.plusDays(1)) + (sun.plusDays(2) to DayRecord(mobilityDone = true))
        val r = StreakCalculator.compute(records, kind, sun, sun.plusDays(2))
        assertEquals(3, r.current)
        assertEquals(DayMark.REST, r.week[2].mark)
        assertTrue(r.week[2].bonus)
    }

    @Test fun `days before tracking starts are neutral, never missed`() {
        val r = StreakCalculator.compute(emptyMap(), kind, trackingStart = null, today = sun.plusDays(4))
        assertEquals(0, r.current)
        assertEquals(DayMark.NONE, r.week[0].mark)
        assertEquals(DayMark.REST, r.week[2].mark) // planned rest still shows
        assertEquals(DayMark.TODAY, r.week[4].mark)
        assertEquals(DayMark.UPCOMING, r.week[5].mark)
    }

    @Test fun `week strip runs Sunday to Saturday`() {
        val r = StreakCalculator.compute(emptyMap(), kind, null, sun.plusDays(4))
        assertEquals(sun, r.week.first().date)
        assertEquals(sun.plusDays(6), r.week.last().date)
    }

    @Test fun `milestones are detected exactly`() {
        val many = (0L until 7L).map { sun.plusDays(it) }.toTypedArray()
        val r = StreakCalculator.compute(done(*many), kind, sun, sun.plusDays(6))
        assertEquals(7, r.current)
        assertEquals(7, r.milestone)
        assertNull(StreakCalculator.compute(done(sun), kind, sun, sun).milestone)
    }
}

class PageLayoutTest {
    @Test fun `default order follows the brief`() {
        assertEquals(listOf(BoxId.HOME_STREAK, BoxId.HOME_TODAY, BoxId.HOME_SCHEDULE, BoxId.HOME_WEEK, BoxId.HOME_NUTRITION), PageLayout.default(PageId.HOME).order)
        assertEquals(listOf(BoxId.NUTRITION_SUMMARY, BoxId.NUTRITION_MEALS, BoxId.NUTRITION_PROTEIN), PageLayout.default(PageId.NUTRITION).order)
    }

    @Test fun `move and hide survive encode and decode`() {
        val l = PageLayout.default(PageId.HOME).move(BoxId.HOME_WEEK, BoxId.HOME_STREAK).toggle(BoxId.HOME_TODAY)
        assertEquals(listOf(BoxId.HOME_WEEK, BoxId.HOME_STREAK, BoxId.HOME_TODAY, BoxId.HOME_SCHEDULE, BoxId.HOME_NUTRITION), l.order)
        assertEquals(listOf(BoxId.HOME_WEEK, BoxId.HOME_STREAK, BoxId.HOME_SCHEDULE, BoxId.HOME_NUTRITION), l.visible)
        assertEquals(l, PageLayout.decode(PageId.HOME, l.encode()))
    }

    @Test fun `unknown boxes are dropped and missing ones come back at their default spot`() {
        val decoded = PageLayout.decode(PageId.HOME, "HOME_WEEK,NOT_A_BOX|HOME_GONE")
        assertEquals(BoxId.defaults(PageId.HOME).toSet(), decoded.order.toSet())
        assertEquals(5, decoded.order.size)
        assertTrue(decoded.hidden.isEmpty())
    }

    @Test fun `a layout saved before the schedule box existed gets it back at its default spot`() {
        val old = PageLayout.decode(PageId.HOME, "HOME_WEEK,HOME_STREAK,HOME_TODAY|")
        assertEquals(5, old.order.size)
        assertEquals(BoxId.HOME_NUTRITION, old.order.last())
        assertTrue(BoxId.HOME_SCHEDULE in old.visible)
    }

    @Test fun `garbage falls back to the default layout`() {
        assertEquals(PageLayout.default(PageId.HOME), PageLayout.decode(PageId.HOME, null))
        assertEquals(PageLayout.default(PageId.HOME).order.toSet(), PageLayout.decode(PageId.HOME, "|||").order.toSet())
    }
}
