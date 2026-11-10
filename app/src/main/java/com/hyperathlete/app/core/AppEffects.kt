package com.hyperathlete.app.core

import android.content.Context
import com.hyperathlete.app.core.calendar.CalendarSyncWorker
import com.hyperathlete.app.core.reminders.ReminderScheduler
import com.hyperathlete.app.core.workout.SessionController
import com.hyperathlete.app.data.settings.SettingsRepository
import com.hyperathlete.app.domain.model.CalendarPrefs
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App-wide reactions to settings: reminders follow the training times and switches, the calendar
 * follows the program, and a killed workout comes back. Started once from [com.hyperathlete.app.HyperApp].
 */
@Singleton
class AppEffects @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: SettingsRepository,
    private val reminders: ReminderScheduler,
    private val controller: SessionController,
    private val accounts: com.hyperathlete.app.data.account.AccountStore,
    private val workouts: com.hyperathlete.app.data.workout.WorkoutRepository,
    private val nutrition: com.hyperathlete.app.data.nutrition.NutritionRepository,
    private val progress: com.hyperathlete.app.data.progress.ProgressRepository,
    private val weather: com.hyperathlete.app.data.weather.WeatherRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var started = false

    private data class Key(val schedule: List<Any?>, val calendar: CalendarPrefs, val accent: Long)

    @OptIn(FlowPreview::class)
    fun start() {
        if (started) return
        started = true
        scope.launch { runCatching { controller.restore() } }
        scope.launch { runCatching { dropRemovedFeatures() } }
        // Home / lock screen widgets follow workouts, meals, weigh-ins, weather and settings.
        scope.launch {
            val today = java.time.LocalDate.now()
            kotlinx.coroutines.flow.combine(
                settings.settings.map { listOf(it.language, it.accentArgb, it.secondaryArgb, it.nutrition, it.programStart, it.weekOffset, it.streakSince, it.weightUnit) },
                workouts.days(today.minusDays(14)),
                nutrition.totals(today.minusDays(1), today.plusDays(1)),
                progress.metrics.map { list -> list.filter { it.metric == com.hyperathlete.app.domain.progress.Metric.WEIGHT }.maxByOrNull { it.date } },
                weather.state.map { (it as? com.hyperathlete.app.data.weather.WeatherState.Ready)?.snapshot?.fetchedAtMillis },
            ) { a, b, c, d, e -> listOf(a, b, c, d, e) }
                .distinctUntilChanged()
                .debounce(1500)
                .collect { com.hyperathlete.app.ui.widgets.Widgets.refresh(context) }
        }
        runCatching { CalendarSyncWorker.scheduleDaily(context) }
        // Nightly Drive backup follows the Google link and the backup switch / time.
        scope.launch {
            accounts.state.map { a -> Triple(a.google != null, a.backup.auto, a.backup.time) }
                .distinctUntilChanged()
                .collect { (google, auto, time) ->
                    runCatching {
                        if (google && auto) com.hyperathlete.app.core.backup.BackupWorker.schedule(context, time)
                        else com.hyperathlete.app.core.backup.BackupWorker.cancel(context)
                    }
                }
        }
        scope.launch {
            settings.settings
                .map { s -> Key(listOf(s.trainingTimes, s.reminders, s.programStart, s.weekOffset, s.language, s.workout.liveUpdates, s.nutrition.reminder, s.nutrition.reminderTime, s.progress), s.calendar, s.accentArgb) }
                .distinctUntilChanged()
                .debounce(600)
                .collectIndexed { i, k ->
                    runCatching { reminders.reschedule() }
                    if (i > 0 || k.calendar.sync) runCatching { CalendarSyncWorker.syncNow(context, clear = !k.calendar.sync) }
                }
        }
    }

    /** 3.3.0 removed the assistant, the voice coach and the AI recap: forget what they saved on this phone. */
    private suspend fun dropRemovedFeatures() {
        settings.dropRemovedFeatures()
        listOf("coach_recap", "launch_router", "brain_trouble")
            .forEach { runCatching { context.deleteSharedPreferences(it) } }
        runCatching { java.io.File(context.cacheDir, "assistant_tts").deleteRecursively() }
    }
}
