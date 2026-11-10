package com.hyperathlete.app.domain.nutrition

import com.hyperathlete.app.domain.model.UserProfile
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.roundToInt

/** Nutrition settings (brief §16). Defaults follow the brief for a lean bulk. */
data class NutritionPrefs(
    val male: Boolean = true,
    /** Mifflin-St Jeor multiplier: 1.2 desk … 1.9 very hard. 1.55 = training 5 days a week. */
    val activity: Double = 1.55,
    /** Daily surplus on top of maintenance (brief: +300…400). */
    val surplus: Int = 350,
    /** Protein in g per kg of body weight (brief: 1.6…2.2, default 2). */
    val proteinPerKg: Double = 2.0,
    /** Share of the calories from fat. */
    val fatPercent: Int = 25,
    /** More carbs on Thursday and Friday (outdoor + legs), around the session. */
    val carbCycling: Boolean = true,
    val carbBoostKcal: Int = 150,
    /** Show the "before / after training" meal sections. */
    val workoutMeals: Boolean = true,
    /** Evening "did you log your food?" reminder (brief §18). */
    val reminder: Boolean = true,
    val reminderTime: LocalTime = LocalTime.of(21, 0),
) {
    companion object {
        val ActivityLevels = listOf(1.2, 1.375, 1.55, 1.725, 1.9)
        val CarbDays = setOf(DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)
        const val DEFAULT_AGE = 25
    }
}

data class NutritionTargets(
    val kcal: Int,
    val protein: Int,
    val carbs: Int,
    val fat: Int,
    val bmr: Int,
    val maintenance: Int,
    /** Today is a high-carb day. */
    val carbDay: Boolean,
) {
    val macros: Macros get() = Macros(kcal.toDouble(), protein.toDouble(), carbs.toDouble(), fat.toDouble())
}

object TargetCalculator {
    /** Mifflin-St Jeor resting energy. */
    fun bmr(weightKg: Double, heightCm: Double, age: Int, male: Boolean): Double =
        10 * weightKg + 6.25 * heightCm - 5 * age + if (male) 5 else -161

    fun targets(profile: UserProfile, prefs: NutritionPrefs, date: LocalDate): NutritionTargets {
        val age = profile.ageOn(date) ?: NutritionPrefs.DEFAULT_AGE
        val bmr = bmr(profile.weightKg, profile.heightCm, age, prefs.male)
        val maintenance = bmr * prefs.activity
        val carbDay = prefs.carbCycling && date.dayOfWeek in NutritionPrefs.CarbDays
        val kcal = roundTo10(maintenance + prefs.surplus + if (carbDay) prefs.carbBoostKcal else 0)
        val protein = (prefs.proteinPerKg * profile.weightKg).roundToInt()
        // Fat comes from the base day, so the carb boost lands entirely on carbs.
        val baseKcal = roundTo10(maintenance + prefs.surplus)
        val fat = (baseKcal * prefs.fatPercent / 100.0 / 9.0).roundToInt()
        val carbs = ((kcal - protein * 4 - fat * 9) / 4.0).roundToInt().coerceAtLeast(0)
        return NutritionTargets(kcal, protein, carbs, fat, bmr.roundToInt(), maintenance.roundToInt(), carbDay)
    }

    private fun roundTo10(v: Double) = ((v / 10.0).roundToInt() * 10)
}
