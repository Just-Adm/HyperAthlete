package com.hyperathlete.app.domain.progress

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

enum class MetricUnit { KG, CM, SECONDS }

/**
 * Everything measured by hand (brief §12, §17): body weight, the test-week tests and tape measurements.
 * [higherIsBetter] decides the arrow colour in the tests table (runs: lower time is better).
 */
enum class Metric(val unit: MetricUnit, val higherIsBetter: Boolean, val test: Boolean, val baseline: Double? = null) {
    WEIGHT(MetricUnit.KG, true, true, 67.0),
    VERTICAL_JUMP(MetricUnit.CM, true, true, 15.0),
    KNEE_TO_WALL_L(MetricUnit.CM, true, true),
    KNEE_TO_WALL_R(MetricUnit.CM, true, true),
    DEEP_SQUAT(MetricUnit.SECONDS, true, true, 0.0),
    BALANCE_L(MetricUnit.SECONDS, true, true),
    BALANCE_R(MetricUnit.SECONDS, true, true),
    PLANK(MetricUnit.SECONDS, true, true, 60.0),
    RUN_5K(MetricUnit.SECONDS, false, true, 1200.0),
    RUN_10K(MetricUnit.SECONDS, false, true, 3180.0),
    THIGH(MetricUnit.CM, true, true),
    ARM(MetricUnit.CM, true, true),
    ;

    companion object {
        /** Order of the test form and the tests table (brief §17). */
        val Tests = listOf(VERTICAL_JUMP, KNEE_TO_WALL_L, KNEE_TO_WALL_R, DEEP_SQUAT, BALANCE_L, BALANCE_R, PLANK, RUN_5K, RUN_10K, WEIGHT, THIGH, ARM)
        val Measurements = listOf(THIGH, ARM)
        /** YouTube search for "how to do this test" (English gives the best videos). */
        fun videoQuery(m: Metric): String = when (m) {
            VERTICAL_JUMP -> "how to measure vertical jump at home wall test"
            KNEE_TO_WALL_L, KNEE_TO_WALL_R -> "knee to wall test ankle dorsiflexion how to"
            DEEP_SQUAT -> "deep squat hold test heels down ankle mobility"
            BALANCE_L, BALANCE_R -> "single leg balance test eyes closed how to"
            PLANK -> "how to do a plank correctly test"
            RUN_5K, RUN_10K -> "how to pace a 5k run test"
            WEIGHT -> "how to weigh yourself accurately morning"
            THIGH -> "how to measure thigh circumference tape"
            ARM -> "how to measure arm circumference flexed bicep tape"
        }

        /** Knee-to-wall goal from the brief. */
        const val KNEE_TO_WALL_GOAL = 10.0
    }
}

data class MetricEntry(
    val id: Long = 0,
    val metric: Metric,
    val date: LocalDate,
    val value: Double,
    /** Program week / cycle when it was a test-week entry. */
    val week: Int? = null,
    val cycle: Int? = null,
    val test: Boolean = false,
)

/** Reminders for the progress side (brief §18): morning weigh-in and test weeks. */
data class ProgressPrefs(
    val weightReminder: Boolean = true,
    val weightDays: Set<DayOfWeek> = DayOfWeek.entries.toSet(),
    val weightTime: LocalTime = LocalTime.of(7, 30),
    val testReminder: Boolean = true,
    /** Two-way body-weight sync with Health Connect. */
    val healthSync: Boolean = false,
)
