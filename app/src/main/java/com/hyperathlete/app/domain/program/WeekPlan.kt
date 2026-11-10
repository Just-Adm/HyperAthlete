package com.hyperathlete.app.domain.program

import androidx.annotation.StringRes
import com.hyperathlete.app.R
import java.time.DayOfWeek

/**
 * Muscle / focus labels used in session titles.
 * [isTerm] = shown in English in the mixed mode ("صدر + Biceps"): anatomy terms stay English,
 * everyday body parts follow the interface language.
 */
enum class Focus(@StringRes val label: Int, val isTerm: Boolean) {
    CHEST(R.string.focus_chest, false),
    BACK(R.string.focus_back, false),
    SHOULDERS(R.string.focus_shoulders, false),
    LEGS(R.string.focus_legs, false),
    KNEES(R.string.focus_knees, false),
    LOWER_BACK(R.string.focus_lower_back, false),
    BICEPS(R.string.focus_biceps, true),
    TRICEPS(R.string.focus_triceps, true),
    TRAPS(R.string.focus_traps, true),
    CORE(R.string.focus_core, true),
    OUTDOOR_FULL_BODY(R.string.focus_outdoor_full_body, true),
    ENDURANCE(R.string.focus_endurance, false),
    REST(R.string.focus_rest, false),
    MOBILITY(R.string.focus_mobility_routine, false),
}

enum class SessionKind { TRAINING, REST }

/** One day of the weekly template. The full exercise prescriptions arrive with the program seed in Phase 2. */
data class DayTemplate(
    val day: DayOfWeek,
    val kind: SessionKind,
    val focus: List<Focus>,
    val exerciseCount: Int,
    val minutes: IntRange,
    val floor: Int?,
)

object WeekPlan {
    /** Weekly schedule from the brief (section 14). Sunday first. */
    val days: List<DayTemplate> = listOf(
        DayTemplate(DayOfWeek.SUNDAY, SessionKind.TRAINING, listOf(Focus.CHEST, Focus.BICEPS), 5, 60..60, 1),
        DayTemplate(DayOfWeek.MONDAY, SessionKind.TRAINING, listOf(Focus.BACK, Focus.TRICEPS), 5, 60..60, 2),
        DayTemplate(DayOfWeek.TUESDAY, SessionKind.REST, listOf(Focus.REST, Focus.MOBILITY), 9, 20..20, null),
        DayTemplate(DayOfWeek.WEDNESDAY, SessionKind.TRAINING, listOf(Focus.SHOULDERS, Focus.TRAPS), 5, 55..55, 2),
        DayTemplate(DayOfWeek.THURSDAY, SessionKind.TRAINING, listOf(Focus.OUTDOOR_FULL_BODY, Focus.ENDURANCE, Focus.CORE), 14, 85..90, 4),
        DayTemplate(DayOfWeek.FRIDAY, SessionKind.TRAINING, listOf(Focus.LEGS, Focus.KNEES, Focus.LOWER_BACK), 5, 60..70, 3),
        DayTemplate(DayOfWeek.SATURDAY, SessionKind.REST, listOf(Focus.REST, Focus.MOBILITY), 9, 20..20, null),
    )

    fun of(day: DayOfWeek): DayTemplate = days.first { it.day == day }

    /** Sunday-first ordering used by the week strip and schedule lists. */
    val sundayFirst: List<DayOfWeek> = days.map { it.day }
}
