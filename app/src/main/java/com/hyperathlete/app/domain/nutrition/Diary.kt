package com.hyperathlete.app.domain.nutrition

import com.hyperathlete.app.domain.model.AppLanguage
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.roundToInt

/**
 * Meal sections of the day (brief §11). The two workout ones can be hidden in the settings.
 * SUPPLEMENTS holds vitamins, omega-3, medicines and the like: always last, never hidden.
 */
enum class MealType(val workout: Boolean = false) {
    BREAKFAST, LUNCH, DINNER, SNACKS, PRE_WORKOUT(true), POST_WORKOUT(true), SUPPLEMENTS;

    companion object {
        fun visible(workoutMeals: Boolean) = entries.filter { workoutMeals || !it.workout }

        /** Vitamins, minerals and medicines go to their own section whatever "+" opened the list. */
        fun forFood(cat: String, fallback: MealType): MealType = if (cat == "vitamins") SUPPLEMENTS else fallback

        /** Section the "+" on the summary opens by default, from the time of day. */
        fun forTime(t: LocalTime): MealType = when (t.hour) {
            in 4..10 -> BREAKFAST
            in 11..15 -> LUNCH
            in 16..17 -> SNACKS
            in 18..22 -> DINNER
            else -> SNACKS
        }
    }
}

/** An add-on as it was logged (snapshot, so editing the food list later never changes history). */
@Serializable
data class LoggedAddon(
    val id: String,
    val ar: String,
    val en: String,
    val art: String? = null,
    val qty: Double,
    val unitAr: String,
    val unitEn: String,
    val unitGrams: Double,
    val per100: Macros,
) {
    val macros: Macros get() = per100.forGrams(unitGrams * qty)
    fun name(language: AppLanguage) = if (language == AppLanguage.ENGLISH) en else ar

    companion object {
        fun of(addon: Addon, qty: Double) =
            LoggedAddon(addon.id, addon.ar, addon.en, addon.art, qty, addon.unit.ar, addon.unit.en, addon.unit.g, addon.macros100)
    }
}

/** One line in the food diary. */
data class MealEntry(
    val id: Long = 0,
    val date: LocalDate,
    val meal: MealType,
    val foodId: String,
    val ar: String,
    val en: String,
    val art: String?,
    val unitId: String,
    val unitAr: String,
    val unitEn: String,
    val unitGrams: Double,
    val qty: Double,
    val per100: Macros,
    val addons: List<LoggedAddon> = emptyList(),
    val createdAt: Long = 0,
) {
    val grams: Double get() = unitGrams * qty
    val base: Macros get() = per100.forGrams(grams)
    val total: Macros get() = base + addons.map { it.macros }.sum()
    fun name(language: AppLanguage) = if (language == AppLanguage.ENGLISH) en else ar
    fun unitName(language: AppLanguage) = if (language == AppLanguage.ENGLISH) unitEn else unitAr

    companion object {
        fun of(date: LocalDate, meal: MealType, food: Food, unit: ServingUnit, qty: Double, addons: List<LoggedAddon>, now: Long) = MealEntry(
            date = date, meal = meal, foodId = food.id, ar = food.ar, en = food.en, art = food.art,
            unitId = unit.id, unitAr = unit.ar, unitEn = unit.en, unitGrams = unit.g, qty = qty,
            per100 = food.macros100, addons = addons, createdAt = now,
        )
    }
}

/** How a day went against the targets: colours the month calendar. */
enum class Adherence { NONE, GOOD, CLOSE, MISSED }

object AdherenceRule {
    /** Bulking: eating too little is the usual miss, so calories and protein both count. */
    fun of(eaten: Macros, target: NutritionTargets): Adherence {
        if (eaten.kcal < 1 && eaten.protein < 0.5) return Adherence.NONE
        val k = eaten.kcal / target.kcal.coerceAtLeast(1)
        val p = eaten.protein / target.protein.coerceAtLeast(1)
        return when {
            k in 0.9..1.15 && p >= 0.9 -> Adherence.GOOD
            k in 0.75..1.3 && p >= 0.7 -> Adherence.CLOSE
            else -> Adherence.MISSED
        }
    }
}

/** The sentence under the ring: what is left, or how far over. */
sealed interface DayStatus {
    data class Remaining(val kcal: Int, val protein: Int) : DayStatus
    data class ProteinOnly(val protein: Int) : DayStatus
    data class Over(val kcal: Int) : DayStatus
    data object Done : DayStatus

    companion object {
        /** Within 5 % of the calorie goal counts as reached. */
        fun of(eaten: Macros, target: NutritionTargets): DayStatus {
            val leftKcal = (target.kcal - eaten.kcal).roundToInt()
            val leftProtein = (target.protein - eaten.protein).roundToInt().coerceAtLeast(0)
            val tolerance = (target.kcal * 0.05).roundToInt()
            return when {
                leftKcal < -tolerance -> Over(-leftKcal)
                leftKcal > tolerance -> Remaining(leftKcal, leftProtein)
                leftProtein > 2 -> ProteinOnly(leftProtein)
                else -> Done
            }
        }
    }
}

/** Per-meal protein spread (brief §16: about four protein meals a day). */
object ProteinSpread {
    val Main = listOf(MealType.BREAKFAST, MealType.LUNCH, MealType.DINNER, MealType.SNACKS)

    fun perMeal(entries: List<MealEntry>): Map<MealType, Double> {
        val by = entries.groupBy { if (it.meal.workout || it.meal == MealType.SUPPLEMENTS) MealType.SNACKS else it.meal }
        return Main.associateWith { m -> by[m].orEmpty().sumOf { it.total.protein } }
    }

    fun mealTarget(target: NutritionTargets): Int = (target.protein / 4.0).roundToInt()
}
