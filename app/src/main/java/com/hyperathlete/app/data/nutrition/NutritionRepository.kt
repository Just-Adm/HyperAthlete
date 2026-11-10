package com.hyperathlete.app.data.nutrition

import android.content.Context
import com.hyperathlete.app.domain.nutrition.CustomFood
import com.hyperathlete.app.domain.nutrition.Food
import com.hyperathlete.app.domain.nutrition.FoodDb
import com.hyperathlete.app.domain.nutrition.FoodIndex
import com.hyperathlete.app.domain.nutrition.LoggedAddon
import com.hyperathlete.app.domain.nutrition.Macros
import com.hyperathlete.app.domain.nutrition.MealEntry
import com.hyperathlete.app.domain.nutrition.MealType
import com.hyperathlete.app.domain.nutrition.RecipeIngredient
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** Built-in foods plus the user's own, with one search index over both. */
class FoodCatalog(val db: FoodDb, val custom: List<CustomFood>) {
    val customFoods: List<Food> = custom.map { it.toFood() }
    private val byId: Map<String, Food> = db.foods.associateBy { it.id } + customFoods.associateBy { it.id }
    val index: FoodIndex by lazy { FoodIndex(customFoods + db.foods) }

    fun food(id: String): Food? = byId[id]
    fun customById(id: String): CustomFood? = custom.firstOrNull { it.foodId == id }
}

@Singleton
class NutritionRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: NutritionDao,
    private val clock: Clock,
) {
    val foodDb: FoodDb by lazy { load(context) }

    val catalog: Flow<FoodCatalog> = dao.customFoods()
        .map { list -> FoodCatalog(foodDb, list.map { it.toDomain() }) }
        .flowOn(Dispatchers.Default)

    fun day(date: LocalDate): Flow<List<MealEntry>> = dao.day(date.toEpochDay()).map { list -> list.map { it.toDomain() } }

    suspend fun dayOnce(date: LocalDate): List<MealEntry> = dao.dayOnce(date.toEpochDay()).map { it.toDomain() }

    /** Eaten totals per day in [from]..[to] (days with nothing logged are absent). */
    fun totals(from: LocalDate, to: LocalDate): Flow<Map<LocalDate, Macros>> =
        dao.totals(from.toEpochDay(), to.toEpochDay()).map { rows ->
            rows.associate { LocalDate.ofEpochDay(it.epochDay) to Macros(it.kcal, it.protein, it.carbs, it.fat) }
        }

    val favorites: Flow<List<String>> = dao.favorites()

    fun recents(limit: Int = 24): Flow<List<String>> = dao.recents(limit).map { rows -> rows.map { it.foodId } }

    /** Recent foods resolved against the catalog (deleted custom foods drop out). */
    fun recentFoods(limit: Int = 24): Flow<List<Food>> =
        combine(recents(limit), catalog) { ids, cat -> ids.mapNotNull(cat::food) }

    suspend fun lastPortion(foodId: String): MealEntry? = dao.lastOf(foodId)?.toDomain()

    suspend fun entry(id: Long): MealEntry? = dao.entry(id)?.toDomain()

    suspend fun save(entry: MealEntry): Long {
        val row = entry.toEntity()
        return if (entry.id == 0L) dao.insert(row.copy(createdAt = if (entry.createdAt > 0) entry.createdAt else clock.millis()))
        else { dao.update(row); entry.id }
    }

    suspend fun delete(id: Long) = dao.delete(id)

    suspend fun countOn(date: LocalDate): Int = dao.countOn(date.toEpochDay())

    suspend fun setFavorite(foodId: String, on: Boolean) {
        if (on) dao.addFavorite(FavoriteFoodEntity(foodId, clock.millis())) else dao.removeFavorite(foodId)
    }

    suspend fun saveCustom(food: CustomFood): Long {
        val row = food.toEntity(clock.millis())
        return if (food.id == 0L) dao.insertCustom(row) else { dao.updateCustom(row); food.id }
    }

    suspend fun deleteCustom(id: Long) {
        dao.deleteCustom(id)
        dao.removeFavorite(Food.CUSTOM_PREFIX + id)
    }

    suspend fun byBarcode(barcode: String): CustomFood? = dao.byBarcode(barcode)?.toDomain()

    companion object {
        private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }
        private val addonList = ListSerializer(LoggedAddon.serializer())
        private val ingredientList = ListSerializer(RecipeIngredient.serializer())

        @Volatile private var cached: FoodDb? = null

        fun load(context: Context): FoodDb = cached ?: synchronized(this) {
            cached ?: context.applicationContext.assets.open("food/foods.json").bufferedReader().use {
                FoodDb.parse(it.readText())
            }.also { cached = it }
        }

        fun MealEntryEntity.toDomain() = MealEntry(
            id = id,
            date = LocalDate.ofEpochDay(epochDay),
            meal = MealType.entries.firstOrNull { it.name == meal } ?: MealType.SNACKS,
            foodId = foodId, ar = nameAr, en = nameEn, art = art,
            unitId = unitId, unitAr = unitAr, unitEn = unitEn, unitGrams = unitGrams, qty = qty,
            per100 = Macros(kcal100, protein100, carbs100, fat100),
            addons = runCatching { json.decodeFromString(addonList, addonsJson) }.getOrDefault(emptyList()),
            createdAt = createdAt,
        )

        fun MealEntry.toEntity(): MealEntryEntity {
            val t = total
            return MealEntryEntity(
                id = id, epochDay = date.toEpochDay(), meal = meal.name, foodId = foodId,
                nameAr = ar, nameEn = en, art = art,
                unitId = unitId, unitAr = unitAr, unitEn = unitEn, unitGrams = unitGrams, qty = qty,
                kcal100 = per100.kcal, protein100 = per100.protein, carbs100 = per100.carbs, fat100 = per100.fat,
                addonsJson = json.encodeToString(addonList, addons),
                kcal = t.kcal, protein = t.protein, carbs = t.carbs, fat = t.fat,
                createdAt = createdAt,
            )
        }

        fun CustomFoodEntity.toDomain() = CustomFood(
            id = id, name = name, servingName = servingName, servingGrams = servingGrams,
            per100 = Macros(kcal100, protein100, carbs100, fat100), recipe = recipe,
            ingredients = ingredientsJson?.let { runCatching { json.decodeFromString(ingredientList, it) }.getOrNull() }.orEmpty(),
            servings = servings, barcode = barcode,
        )

        fun CustomFood.toEntity(now: Long) = CustomFoodEntity(
            id = id, name = name, servingName = servingName, servingGrams = servingGrams,
            kcal100 = per100.kcal, protein100 = per100.protein, carbs100 = per100.carbs, fat100 = per100.fat,
            recipe = recipe, ingredientsJson = if (ingredients.isNotEmpty()) json.encodeToString(ingredientList, ingredients) else null,
            servings = servings, barcode = barcode, createdAt = now,
        )
    }
}
