package com.hyperathlete.app.domain.model

import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Kilograms are what the database keeps. Pounds are only a way of showing and typing them,
 * so switching units never changes a single logged number.
 */
object Weights {
    const val LB_PER_KG = 2.20462262185

    fun label(u: WeightUnit) = if (u == WeightUnit.LB) "lb" else "kg"

    /** kg → the number shown in [u]. */
    fun show(kg: Double, u: WeightUnit): Double = if (u == WeightUnit.LB) kg * LB_PER_KG else kg

    /** A number typed in [u] → kg. */
    fun toKg(v: Double, u: WeightUnit): Double = if (u == WeightUnit.LB) v / LB_PER_KG else v

    /** "67.5", "148.8": at most one decimal, no trailing ".0". */
    fun num(v: Double, decimals: Int = 1): String {
        if (decimals == 0) return String.format(Locale.US, "%,d", v.roundToLong())
        val r = String.format(Locale.US, "%.${decimals}f", v)
        return r.trimEnd('0').trimEnd('.').ifEmpty { "0" }
    }

    /** "67.5 kg" / "148.8 lb". */
    fun text(kg: Double, u: WeightUnit, decimals: Int = 1) = num(show(kg, u), decimals) + " " + label(u)

    /** "+1.5 kg" / "−3.3 lb" (true minus sign). */
    fun signed(kg: Double, u: WeightUnit): String {
        val v = show(kg, u)
        val sign = if (v > 0.049) "+" else if (v < -0.049) "−" else ""
        return sign + num(abs(v)) + " " + label(u)
    }

    /** Stepper increment for the workout logger: 2.5 kg plates become 5 lb. */
    fun step(kgStep: Double, u: WeightUnit): Double = if (u == WeightUnit.LB) (if (kgStep >= 2.5) 5.0 else 2.5) else kgStep
}
