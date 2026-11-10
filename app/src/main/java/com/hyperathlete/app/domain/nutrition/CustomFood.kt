package com.hyperathlete.app.domain.nutrition

import kotlinx.serialization.Serializable

/** A food the user typed in, a scanned product, or a recipe built from other foods. */
data class CustomFood(
    val id: Long = 0,
    val name: String,
    val servingName: String,
    val servingGrams: Double,
    val per100: Macros,
    val recipe: Boolean = false,
    val ingredients: List<RecipeIngredient> = emptyList(),
    val servings: Double? = null,
    val barcode: String? = null,
) {
    val foodId: String get() = Food.CUSTOM_PREFIX + id

    /** Shown and logged exactly like a built-in food. */
    fun toFood(): Food = Food(
        id = foodId,
        cat = if (recipe) CAT_RECIPE else CAT_CUSTOM,
        ar = name, en = name, art = null,
        per100 = listOf(per100.kcal, per100.protein, per100.carbs, per100.fat),
        units = listOf(ServingUnit(SERVING, servingName, servingName, servingGrams)),
        defUnit = SERVING, defQty = 1.0,
        aka = barcode,
    )

    companion object {
        const val SERVING = "serving"
        const val CAT_CUSTOM = "custom"
        const val CAT_RECIPE = "recipe"

        /** From what is printed on a label: values for one serving of [grams] g. */
        fun fromServing(name: String, servingName: String, grams: Double, perServing: Macros, barcode: String? = null): CustomFood {
            val g = grams.coerceAtLeast(1.0)
            return CustomFood(name = name.trim(), servingName = servingName.trim(), servingGrams = g, per100 = perServing * (100.0 / g), barcode = barcode)
        }

        /** A recipe: the whole pot divided into [servings] portions. */
        fun recipe(name: String, ingredients: List<RecipeIngredient>, servings: Double, servingName: String): CustomFood =
            fromIngredients(name, ingredients, servings, servingName).copy(recipe = true)

        /**
         * A food built from what goes into it (a sandwich, a bowl, a shake): the values are the sum of the
         * ingredients, split into [servings] portions (1 = the whole thing is one serving).
         */
        fun fromIngredients(name: String, ingredients: List<RecipeIngredient>, servings: Double, servingName: String): CustomFood {
            val grams = ingredients.sumOf { it.grams }.coerceAtLeast(1.0)
            val total = ingredients.map { it.macros }.sum()
            val n = servings.coerceAtLeast(1.0)
            return CustomFood(
                name = name.trim(), servingName = servingName.trim(), servingGrams = grams / n,
                per100 = total * (100.0 / grams), ingredients = ingredients, servings = n,
            )
        }
    }
}

@Serializable
data class RecipeIngredient(
    val foodId: String,
    val ar: String,
    val en: String,
    val art: String? = null,
    val unitId: String,
    val unitAr: String,
    val unitEn: String,
    val unitGrams: Double,
    val qty: Double,
    val per100: Macros,
) {
    val grams: Double get() = unitGrams * qty
    val macros: Macros get() = per100.forGrams(grams)

    val isFree: Boolean get() = foodId.startsWith(FREE_PREFIX)

    companion object {
        const val FREE_PREFIX = "free:"

        fun of(food: Food, unit: ServingUnit, qty: Double) =
            RecipeIngredient(food.id, food.ar, food.en, food.art, unit.id, unit.ar, unit.en, unit.g, qty, food.macros100)

        /** An ingredient that is not in the list: the user types its name, weight and the values for that weight. */
        fun free(name: String, grams: Double, values: Macros): RecipeIngredient {
            val g = grams.coerceAtLeast(1.0)
            val n = name.trim()
            val u = ServingUnit.GRAM
            return RecipeIngredient(FREE_PREFIX + n, n, n, null, u.id, u.ar, u.en, u.g, g, values * (100.0 / g))
        }
    }
}
