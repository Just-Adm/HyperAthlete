package com.hyperathlete.app.data.di

import android.content.Context
import androidx.room.Room
import com.hyperathlete.app.data.db.HyperDatabase
import com.hyperathlete.app.data.db.UserProfileDao
import com.hyperathlete.app.data.nutrition.NutritionDao
import com.hyperathlete.app.data.workout.WorkoutDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): HyperDatabase =
        Room.databaseBuilder(context, HyperDatabase::class.java, "hyper.db").build()

    @Provides
    fun userProfileDao(db: HyperDatabase): UserProfileDao = db.userProfileDao()

    @Provides
    fun workoutDao(db: HyperDatabase): WorkoutDao = db.workoutDao()

    @Provides
    fun nutritionDao(db: HyperDatabase): NutritionDao = db.nutritionDao()

    @Provides
    fun progressDao(db: HyperDatabase): com.hyperathlete.app.data.progress.ProgressDao = db.progressDao()

    /** Injected so date logic is testable. */
    @Provides
    @Singleton
    fun clock(): Clock = Clock.systemDefaultZone()
}
