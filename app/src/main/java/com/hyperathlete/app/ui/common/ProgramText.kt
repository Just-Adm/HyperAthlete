package com.hyperathlete.app.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import com.hyperathlete.app.R
import com.hyperathlete.app.core.locale.Bidi
import com.hyperathlete.app.core.locale.bidiJoin
import com.hyperathlete.app.core.locale.termResource
import com.hyperathlete.app.domain.program.DayTemplate
import com.hyperathlete.app.domain.program.Focus
import com.hyperathlete.app.domain.program.Phase
import com.hyperathlete.app.domain.program.ProgramClock
import com.hyperathlete.app.domain.program.ProgramPosition
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
@ReadOnlyComposable
fun dayName(day: DayOfWeek): String = stringResource(
    when (day) {
        DayOfWeek.SUNDAY -> R.string.day_sun
        DayOfWeek.MONDAY -> R.string.day_mon
        DayOfWeek.TUESDAY -> R.string.day_tue
        DayOfWeek.WEDNESDAY -> R.string.day_wed
        DayOfWeek.THURSDAY -> R.string.day_thu
        DayOfWeek.FRIDAY -> R.string.day_fri
        DayOfWeek.SATURDAY -> R.string.day_sat
    },
)

@Composable
@ReadOnlyComposable
fun dayShort(day: DayOfWeek): String = stringResource(
    when (day) {
        DayOfWeek.SUNDAY -> R.string.day_short_sun
        DayOfWeek.MONDAY -> R.string.day_short_mon
        DayOfWeek.TUESDAY -> R.string.day_short_tue
        DayOfWeek.WEDNESDAY -> R.string.day_short_wed
        DayOfWeek.THURSDAY -> R.string.day_short_thu
        DayOfWeek.FRIDAY -> R.string.day_short_fri
        DayOfWeek.SATURDAY -> R.string.day_short_sat
    },
)

/** Anatomy terms follow the term language, everyday body parts the UI language ("صدر + Biceps" in the mixed mode). */
@Composable
@ReadOnlyComposable
fun focusName(f: Focus): String = if (f.isTerm) termResource(f.label) else stringResource(f.label)

@Composable
@ReadOnlyComposable
fun sessionTitle(day: DayTemplate): String = bidiJoin(day.focus.map { focusName(it) }, " + ")

@Composable
@ReadOnlyComposable
fun phaseName(phase: Phase): String = when (phase) {
    Phase.FOUNDATION -> stringResource(R.string.phase_foundation)
    Phase.BUILD -> stringResource(R.string.phase_build)
    Phase.DELOAD -> termResource(R.string.term_deload)
}

@Composable
@ReadOnlyComposable
fun weekPhaseLabel(pos: ProgramPosition): String =
    stringResource(R.string.week_phase, pos.week, ProgramClock.WEEKS, phaseName(pos.phase))

@Composable
@ReadOnlyComposable
fun formatTime(time: LocalTime): String =
    time.format(DateTimeFormatter.ofPattern("h:mm a", LocalConfiguration.current.locales[0]))

@Composable
@ReadOnlyComposable
fun formatDate(date: LocalDate): String =
    date.format(DateTimeFormatter.ofPattern("d MMMM yyyy", LocalConfiguration.current.locales[0]))

@Composable
@ReadOnlyComposable
fun minutesLabel(range: IntRange): String =
    if (range.first == range.last) stringResource(R.string.minutes_single, range.first)
    else stringResource(R.string.minutes_range, range.first, range.last)

/** A weight stored in kg, shown in the user's unit ("67.5 kg" / "148.8 lb") as one left-to-right block. */
@Composable
@ReadOnlyComposable
fun wt(kg: Double, decimals: Int = 1): String =
    Bidi.ltr(com.hyperathlete.app.domain.model.Weights.text(kg, com.hyperathlete.app.ui.theme.LocalWeightUnit.current, decimals))

/** "+1.5 kg" / "−3.3 lb" in the user's unit. */
@Composable
@ReadOnlyComposable
fun wtSigned(kg: Double): String =
    Bidi.ltr(com.hyperathlete.app.domain.model.Weights.signed(kg, com.hyperathlete.app.ui.theme.LocalWeightUnit.current))

/** "kg" or "lb". */
@Composable
@ReadOnlyComposable
fun wtUnit(): String = com.hyperathlete.app.domain.model.Weights.label(com.hyperathlete.app.ui.theme.LocalWeightUnit.current)

/** "67.5 kg" kept left-to-right inside Arabic text. */
fun measure(value: Double, unit: String): String {
    val v = if (value % 1.0 == 0.0) value.toLong().toString() else String.format(java.util.Locale.US, "%.1f", value)
    return Bidi.ltr("$v $unit")
}

/* ---------------- program content (Phase 2) ---------------- */

/** Non-Compose formatter for program text, cached per language. */
@Composable
fun rememberProgramStrings(): com.hyperathlete.app.core.workout.ProgramStrings {
    val context = androidx.compose.ui.platform.LocalContext.current
    val lang = com.hyperathlete.app.core.locale.LocalAppLanguage.current
    val unit = com.hyperathlete.app.ui.theme.LocalWeightUnit.current
    return androidx.compose.runtime.remember(context, lang, unit) {
        com.hyperathlete.app.core.workout.ProgramStrings(com.hyperathlete.app.core.locale.Localizer(context, lang), unit)
    }
}

@Composable
@ReadOnlyComposable
fun exerciseName(e: com.hyperathlete.app.domain.program.Exercise): String =
    e.name.term(com.hyperathlete.app.core.locale.LocalAppLanguage.current)

@Composable
@ReadOnlyComposable
fun com.hyperathlete.app.domain.program.Txt.text(): String = ui(com.hyperathlete.app.core.locale.LocalAppLanguage.current)

/** "2-3 د" for minute-sized rests, "60-90 ث" for short ones. */
@Composable
@ReadOnlyComposable
fun restLabel(minSec: Int, maxSec: Int): String =
    if (minSec >= 120 && minSec % 60 == 0 && maxSec % 60 == 0) {
        stringResource(R.string.rest_minutes, Bidi.ltr(if (minSec == maxSec) "${minSec / 60}" else "${minSec / 60}-${maxSec / 60}"))
    } else {
        stringResource(R.string.rest_seconds, Bidi.ltr(if (minSec == maxSec) "$minSec" else "$minSec-$maxSec"))
    }

/** The picture on each day's card (brief §9 box 3). */
fun dayArt(day: DayOfWeek, week: Int): String = when (day) {
    DayOfWeek.SUNDAY -> "BENCH"
    DayOfWeek.MONDAY -> "PULLDOWN"
    DayOfWeek.WEDNESDAY -> "SHOULDER_PRESS"
    DayOfWeek.THURSDAY -> "SLED_PUSH"
    DayOfWeek.FRIDAY -> if (week in 5..9) "BACK_SQUAT" else "GOBLET"
    DayOfWeek.TUESDAY, DayOfWeek.SATURDAY -> "STRETCH"
}

@Composable
@ReadOnlyComposable
fun dayFocus(day: DayOfWeek): String = stringResource(
    when (day) {
        DayOfWeek.SUNDAY -> R.string.focus_line_sun
        DayOfWeek.MONDAY -> R.string.focus_line_mon
        DayOfWeek.WEDNESDAY -> R.string.focus_line_wed
        DayOfWeek.THURSDAY -> R.string.focus_line_thu
        DayOfWeek.FRIDAY -> R.string.focus_line_fri
        DayOfWeek.TUESDAY, DayOfWeek.SATURDAY -> R.string.focus_line_rest
    },
)
