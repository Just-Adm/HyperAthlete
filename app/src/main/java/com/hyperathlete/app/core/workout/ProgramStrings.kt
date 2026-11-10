package com.hyperathlete.app.core.workout

import com.hyperathlete.app.R
import com.hyperathlete.app.core.locale.AppLocales
import com.hyperathlete.app.core.locale.Bidi
import com.hyperathlete.app.core.locale.Localizer
import com.hyperathlete.app.domain.program.DayTemplate
import com.hyperathlete.app.domain.program.Measure
import com.hyperathlete.app.domain.program.PlannedSlot
import com.hyperathlete.app.domain.training.SetValues

/**
 * Program text for places without Compose (notification, calendar, reminders), following the
 * same language rules as the UI ("صدر + Biceps" in the mixed mode).
 */
class ProgramStrings(val l: Localizer, val unit: com.hyperathlete.app.domain.model.WeightUnit = com.hyperathlete.app.domain.model.WeightUnit.KG) {
    private val W = com.hyperathlete.app.domain.model.Weights
    private val rtl = AppLocales.isRtl(l.language)

    fun sessionTitle(t: DayTemplate): String =
        Bidi.join(t.focus.map { if (it.isTerm) l.term(it.label) else l.string(it.label) }, " + ", rtl)

    fun exercise(slot: PlannedSlot): String = slot.exercise.name.term(l.language)

    fun range(min: Int, max: Int): String = Bidi.ltr(if (min == max) "$min" else "$min-$max")

    /** "4 × 6-8", "3 × 15-30 s per side", "3 × 40 m" — the numbers stay one left-to-right block. */
    fun target(slot: PlannedSlot, sets: Int = slot.sets): String {
        val r = if (slot.min == slot.max) "${slot.min}" else "${slot.min}-${slot.max}"
        val base = when (slot.exercise.measure) {
            Measure.REPS -> Bidi.ltr("$sets × $r")
            Measure.TIME -> l.string(R.string.unit_seconds_value, Bidi.ltr("$sets × $r"))
            Measure.DISTANCE -> Bidi.ltr("$sets × $r m")
            Measure.LENGTH -> l.string(R.string.unit_lengths_value, Bidi.ltr("$sets × $r"))
        }
        return if (slot.exercise.perSide) l.string(R.string.per_side_value, base) else base
    }

    fun weight(kg: Double?): String? = kg?.takeIf { it > 0 }?.let { Bidi.ltr(W.text(it, unit)) }

    /** "60 kg × 6-8" for the notification and "up next" line, as one left-to-right block. */
    fun planned(slot: PlannedSlot, v: SetValues): String {
        val w = v.weightKg?.takeIf { it > 0 }?.let { W.text(it, unit) + " × " } ?: ""
        val r = if (slot.min == slot.max) "${slot.min}" else "${slot.min}-${slot.max}"
        return when (slot.exercise.measure) {
            Measure.REPS -> Bidi.ltr(w + r)
            Measure.TIME -> l.string(R.string.unit_seconds_value, Bidi.ltr("${v.seconds ?: slot.min}"))
            Measure.DISTANCE -> Bidi.ltr(w + "${v.meters ?: slot.max} m")
            Measure.LENGTH -> l.string(R.string.unit_lengths_value, Bidi.ltr("${slot.max}"))
        }
    }

    companion object {
        fun fmt(x: Double): String = if (x % 1.0 == 0.0) x.toLong().toString() else String.format(java.util.Locale.US, "%.1f", x).trimEnd('0').trimEnd('.')
    }
}
