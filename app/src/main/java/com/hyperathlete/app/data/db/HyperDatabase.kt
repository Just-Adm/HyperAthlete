package com.hyperathlete.app.data.db

import androidx.room.AutoMigration
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import com.hyperathlete.app.data.nutrition.CustomFoodEntity
import com.hyperathlete.app.data.nutrition.FavoriteFoodEntity
import com.hyperathlete.app.data.nutrition.MealEntryEntity
import com.hyperathlete.app.data.nutrition.NutritionDao
import com.hyperathlete.app.data.progress.MetricEntryEntity
import com.hyperathlete.app.data.progress.ProgressDao
import com.hyperathlete.app.data.workout.ActiveSessionEntity
import com.hyperathlete.app.data.workout.CalendarEventEntity
import com.hyperathlete.app.data.workout.ExerciseNoteEntity
import com.hyperathlete.app.data.workout.KneePainEntity
import com.hyperathlete.app.data.workout.SetLogEntity
import com.hyperathlete.app.data.workout.WorkoutDao
import com.hyperathlete.app.data.workout.WorkoutSessionEntity
import kotlinx.coroutines.flow.Flow

/** Single-row table: the person using the app. Body-weight history gets its own table in Phase 4. */
@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Int = 1,
    val name: String,
    val birthEpochDay: Long?,
    val weightKg: Double,
    val heightCm: Double,
    val photoUri: String? = null,
    val updatedAt: Long = System.currentTimeMillis(),
)

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile WHERE id = 1")
    fun observe(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profile WHERE id = 1")
    suspend fun get(): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: UserProfileEntity)
}

/**
 * Offline-first local store. Later phases add tables (sessions, sets, foods, meals, weights, tests…)
 * through Room auto-migrations so nothing recorded is ever lost on update.
 */
@Database(
    entities = [
        UserProfileEntity::class,
        WorkoutSessionEntity::class, SetLogEntity::class, KneePainEntity::class, ExerciseNoteEntity::class,
        ActiveSessionEntity::class, CalendarEventEntity::class,
        MealEntryEntity::class, CustomFoodEntity::class, FavoriteFoodEntity::class,
        MetricEntryEntity::class,
    ],
    version = 4,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2), AutoMigration(from = 2, to = 3), AutoMigration(from = 3, to = 4)],
)
abstract class HyperDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun nutritionDao(): NutritionDao
    abstract fun progressDao(): ProgressDao
}
