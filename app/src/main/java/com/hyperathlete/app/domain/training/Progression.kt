package com.hyperathlete.app.domain.training

import com.hyperathlete.app.domain.program.Gear
import com.hyperathlete.app.domain.program.LiftClass
import com.hyperathlete.app.domain.program.Load
import com.hyperathlete.app.domain.program.Measure
import com.hyperathlete.app.domain.program.PlannedSlot
import kotlinx.serialization.Serializable
import kotlin.math.floor
import kotlin.math.roundToInt

/** What was done in one set. Only the fields that fit the exercise type are filled. */
@Serializable
data class SetValues(
    val weightKg: Double? = null,
    val reps: Int? = null,
    val seconds: Int? = null,
    val meters: Int? = null,
    val rir: Int? = null,
)

/** One earlier session of an exercise, sets in order. */
data class PastExercise(val epochDay: Long, val sets: List<SetValues>, val deload: Boolean)

sealed interface Suggestion {
    /** Double progression earned: go up and restart at the bottom of the rep range. */
    data class Increase(val fromKg: Double, val toKg: Double, val targetReps: Int) : Suggestion
    /** Body-weight lift maxed out: start adding weight on the dip belt. */
    data class AddBelt(val kg: Double) : Suggestion
    /** Deload week: same exercise, 10% lighter. */
    data class Deload(val kg: Double) : Suggestion
}

/** RIR target for a set (brief §15.1). */
enum class RirTarget { STOP_SHORT, FAILURE, NONE }

/**
 * Brief §15.2 (double progression) and §15.7 (deload).
 *  - Hit the top of the rep range on every set at one weight → next time +2.5 kg (bar) / +2 kg (dumbbell) and
 *    back to the bottom of the range.
 *  - Deload week: 10% lighter, rounded to the plate step.
 */
object Progression {

    fun suggest(slot: PlannedSlot, last: PastExercise?, deload: Boolean): Suggestion? {
        if (slot.exercise.measure != Measure.REPS) return null
        val work = last?.sets?.filter { (it.reps ?: 0) > 0 } ?: return null
        if (work.isEmpty()) return null
        val gear = slot.exercise.gear
        val top = work.maxOf { it.weightKg ?: 0.0 }
        if (deload) {
            if (slot.load != Load.WEIGHTED || top <= 0.0) return null
            return Suggestion.Deload(roundDown(top * 0.9, stepOf(gear)))
        }
        if (last.deload) return null
        val enoughSets = work.size >= slot.sets
        val allTop = work.all { (it.reps ?: 0) >= slot.max && (it.weightKg ?: 0.0) >= top - 0.01 }
        if (!enoughSets || !allTop || !slot.isRange && slot.exercise.cls == LiftClass.OTHER) return null
        return when (slot.load) {
            Load.WEIGHTED -> {
                val step = stepOf(gear)
                if (step <= 0.0) null else Suggestion.Increase(top, top + step, slot.min)
            }
            Load.BODYWEIGHT -> if (gear == Gear.BELT) {
                if (top <= 0.0) Suggestion.AddBelt(Gear.BELT.stepKg) else Suggestion.Increase(top, top + Gear.BELT.stepKg, slot.min)
            } else null
            Load.NONE -> null
        }
    }

    /** Values shown in the logger before the user touches anything: last time's numbers, or a sane start. */
    fun prefill(slot: PlannedSlot, set: Int, last: PastExercise?, suggestion: Suggestion?, current: List<SetValues>): SetValues {
        val ex = slot.exercise
        val previousThisSession = current.lastOrNull()
        val lastSame = last?.sets?.getOrNull(set - 1) ?: last?.sets?.lastOrNull()
        val weight: Double? = when {
            slot.load == Load.NONE && ex.measure != Measure.DISTANCE -> null
            suggestion is Suggestion.Increase -> previousThisSession?.weightKg ?: suggestion.toKg
            suggestion is Suggestion.AddBelt -> previousThisSession?.weightKg ?: suggestion.kg
            suggestion is Suggestion.Deload -> previousThisSession?.weightKg ?: suggestion.kg
            previousThisSession?.weightKg != null -> previousThisSession.weightKg
            lastSame?.weightKg != null -> lastSame.weightKg
            slot.load == Load.BODYWEIGHT -> 0.0
            slot.load == Load.NONE -> null
            else -> startWeight(ex.gear)
        }
        return when (ex.measure) {
            Measure.REPS, Measure.LENGTH -> {
                val reps = when (suggestion) {
                    is Suggestion.Increase -> suggestion.targetReps
                    else -> lastSame?.reps?.coerceIn(slot.min, slot.max) ?: slot.max
                }
                SetValues(weightKg = weight, reps = reps, rir = null)
            }
            Measure.TIME -> SetValues(weightKg = weight, seconds = lastSame?.seconds?.coerceIn(slot.min, slot.max) ?: slot.min)
            Measure.DISTANCE -> SetValues(weightKg = weight ?: startWeight(ex.gear), meters = slot.max)
        }
    }

    fun rirTarget(slot: PlannedSlot, set: Int, of: Int): RirTarget = when (slot.exercise.cls) {
        LiftClass.COMPOUND -> RirTarget.STOP_SHORT
        LiftClass.ISOLATION -> if (set == of) RirTarget.FAILURE else RirTarget.STOP_SHORT
        LiftClass.OTHER -> RirTarget.NONE
    }

    fun asksRir(slot: PlannedSlot): Boolean = slot.exercise.cls != LiftClass.OTHER && slot.exercise.measure == Measure.REPS

    fun stepOf(gear: Gear): Double = gear.stepKg

    /** Where an empty logger starts: the empty bar, a light pair of dumbbells, one plate on the stack… */
    fun startWeight(gear: Gear): Double = when (gear) {
        Gear.BARBELL -> 20.0
        Gear.DUMBBELL -> 10.0
        Gear.CABLE, Gear.MACHINE -> 20.0
        Gear.PLATE -> 10.0
        Gear.KETTLEBELL -> 16.0
        Gear.SLED -> 40.0
        Gear.SANDBAG -> 20.0
        Gear.MEDBALL -> 6.0
        Gear.BELT, Gear.BODY -> 0.0
    }

    fun roundTo(kg: Double, step: Double): Double {
        if (step <= 0.0) return kg
        val r = (kg / step).roundToInt() * step
        return (r * 100).roundToInt() / 100.0
    }

    /** Deload weights round down so the week really is lighter. */
    fun roundDown(kg: Double, step: Double): Double {
        if (step <= 0.0) return kg
        val r = floor(kg / step + 1e-9) * step
        return (r * 100).roundToInt() / 100.0
    }

    /** Epley estimate, used to spot new records across rep ranges. */
    fun e1rm(kg: Double, reps: Int): Double = if (reps <= 1) kg else kg * (1 + reps / 30.0)

    /** Total kilos moved (weight × reps); body-weight-only sets count zero. */
    fun volume(sets: List<SetValues>): Double = sets.sumOf { (it.weightKg ?: 0.0) * (it.reps ?: 0) }

    /**
     * New personal record for this session compared with everything before it:
     * the heaviest weight, or a better estimated 1RM.
     */
    fun isRecord(today: List<SetValues>, before: List<SetValues>): Boolean {
        val t = today.filter { (it.weightKg ?: 0.0) > 0 && (it.reps ?: 0) > 0 }
        if (t.isEmpty()) return false
        val b = before.filter { (it.weightKg ?: 0.0) > 0 && (it.reps ?: 0) > 0 }
        if (b.isEmpty()) return false // first time is a baseline, not a record
        val bestBefore = b.maxOf { e1rm(it.weightKg!!, it.reps!!) }
        val bestToday = t.maxOf { e1rm(it.weightKg!!, it.reps!!) }
        return t.maxOf { it.weightKg!! } > b.maxOf { it.weightKg!! } + 0.01 || bestToday > bestBefore + 0.01
    }

    /** "60 kg × 8, 8, 7" — weight once when it didn't change, per set otherwise. */
    fun summarize(
        sets: List<SetValues>, kg: String, sep: String,
        unit: com.hyperathlete.app.domain.model.WeightUnit = com.hyperathlete.app.domain.model.WeightUnit.KG,
    ): String? {
        if (sets.isEmpty()) return null
        val weights = sets.map { it.weightKg }.distinct()
        fun w(x: Double) = com.hyperathlete.app.domain.model.Weights.num(com.hyperathlete.app.domain.model.Weights.show(x, unit))
        val reps = sets.map { it.reps ?: it.seconds ?: it.meters ?: 0 }
        return if (weights.size == 1 && weights[0] != null && weights[0]!! > 0) {
            "${w(weights[0]!!)} $kg × " + reps.joinToString(sep)
        } else if (weights.all { it == null || it == 0.0 }) {
            reps.joinToString(sep)
        } else {
            sets.joinToString(sep) { s -> if ((s.weightKg ?: 0.0) > 0) "${w(s.weightKg!!)}×${s.reps ?: s.seconds ?: s.meters ?: 0}" else "${s.reps ?: s.seconds ?: s.meters ?: 0}" }
        }
    }

    fun clampWeight(kg: Double): Double = floor(kg.coerceIn(0.0, 500.0) * 4) / 4
}
