package com.hyperathlete.app.data.nutrition

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * One food in the diary. Name, unit and per-100 g values are copied in when logged, and the totals
 * (food + add-ons) are stored too, so later edits to the food list never rewrite history and month
 * views can sum days in SQL.
 */
@Entity(tableName = "meal_entry", indices = [Index("epochDay"), Index("foodId")])
data class MealEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val epochDay: Long,
    val meal: String,
    val foodId: String,
    val nameAr: String,
    val nameEn: String,
    val art: String? = null,
    val unitId: String,
    val unitAr: String,
    val unitEn: String,
    val unitGrams: Double,
    val qty: Double,
    val kcal100: Double,
    val protein100: Double,
    val carbs100: Double,
    val fat100: Double,
    /** JSON list of LoggedAddon. */
    val addonsJson: String = "[]",
    val kcal: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val createdAt: Long,
)

/** The user's own foods and recipes (brief §11: "إضافة أكلة أو وصفة خاصة فيني"), and scanned products. */
@Entity(tableName = "custom_food", indices = [Index("barcode")])
data class CustomFoodEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val servingName: String,
    val servingGrams: Double,
    val kcal100: Double,
    val protein100: Double,
    val carbs100: Double,
    val fat100: Double,
    val recipe: Boolean = false,
    /** JSON list of RecipeIngredient for recipes. */
    val ingredientsJson: String? = null,
    val servings: Double? = null,
    val barcode: String? = null,
    val createdAt: Long,
)

@Entity(tableName = "food_favorite")
data class FavoriteFoodEntity(
    @PrimaryKey val foodId: String,
    val addedAt: Long,
)

data class DayTotalRow(val epochDay: Long, val kcal: Double, val protein: Double, val carbs: Double, val fat: Double)

data class RecentRow(val foodId: String, val lastAt: Long)

@Dao
interface NutritionDao {
    @Query("SELECT * FROM meal_entry WHERE epochDay = :day ORDER BY createdAt, id")
    fun day(day: Long): Flow<List<MealEntryEntity>>

    @Query("SELECT * FROM meal_entry WHERE epochDay = :day ORDER BY createdAt, id")
    suspend fun dayOnce(day: Long): List<MealEntryEntity>

    @Query(
        "SELECT epochDay, SUM(kcal) AS kcal, SUM(protein) AS protein, SUM(carbs) AS carbs, SUM(fat) AS fat " +
            "FROM meal_entry WHERE epochDay BETWEEN :from AND :to GROUP BY epochDay",
    )
    fun totals(from: Long, to: Long): Flow<List<DayTotalRow>>

    @Query("SELECT foodId, MAX(createdAt) AS lastAt FROM meal_entry GROUP BY foodId ORDER BY lastAt DESC LIMIT :limit")
    fun recents(limit: Int): Flow<List<RecentRow>>

    @Query("SELECT * FROM meal_entry WHERE foodId = :foodId ORDER BY createdAt DESC LIMIT 1")
    suspend fun lastOf(foodId: String): MealEntryEntity?

    @Query("SELECT * FROM meal_entry WHERE id = :id")
    suspend fun entry(id: Long): MealEntryEntity?

    @Insert
    suspend fun insert(e: MealEntryEntity): Long

    @Update
    suspend fun update(e: MealEntryEntity)

    @Query("DELETE FROM meal_entry WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT COUNT(*) FROM meal_entry WHERE epochDay = :day")
    suspend fun countOn(day: Long): Int

    @Query("SELECT foodId FROM food_favorite ORDER BY addedAt DESC")
    fun favorites(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(f: FavoriteFoodEntity)

    @Query("DELETE FROM food_favorite WHERE foodId = :foodId")
    suspend fun removeFavorite(foodId: String)

    @Query("SELECT * FROM custom_food ORDER BY createdAt DESC")
    fun customFoods(): Flow<List<CustomFoodEntity>>

    @Query("SELECT * FROM custom_food WHERE id = :id")
    suspend fun customFood(id: Long): CustomFoodEntity?

    @Query("SELECT * FROM custom_food WHERE barcode = :barcode LIMIT 1")
    suspend fun byBarcode(barcode: String): CustomFoodEntity?

    @Insert
    suspend fun insertCustom(f: CustomFoodEntity): Long

    @Update
    suspend fun updateCustom(f: CustomFoodEntity)

    @Query("DELETE FROM custom_food WHERE id = :id")
    suspend fun deleteCustom(id: Long)
}
