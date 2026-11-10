package com.hyperathlete.app.core

import com.hyperathlete.app.core.calendar.CalendarSync
import com.hyperathlete.app.core.reminders.ReminderScheduler
import com.hyperathlete.app.core.workout.SessionController
import com.hyperathlete.app.data.nutrition.NutritionRepository
import com.hyperathlete.app.data.profile.ProfileRepository
import com.hyperathlete.app.data.settings.SettingsRepository
import com.hyperathlete.app.data.workout.WorkoutRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Singletons for code Hilt can't inject (widgets, tests). */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface AppEntryPoint {
    fun sessions(): SessionController
    fun workouts(): WorkoutRepository
    fun settings(): SettingsRepository
    fun reminders(): ReminderScheduler
    fun calendar(): CalendarSync
    fun nutrition(): NutritionRepository
    fun profiles(): ProfileRepository
    fun progress(): com.hyperathlete.app.data.progress.ProgressRepository
    fun accounts(): com.hyperathlete.app.core.auth.AccountRepository
    fun snapshots(): com.hyperathlete.app.core.backup.SnapshotStore
    fun healthSync(): com.hyperathlete.app.core.health.HealthSync
}
