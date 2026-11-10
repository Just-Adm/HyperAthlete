package com.hyperathlete.app.core.reminders

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.hyperathlete.app.R
import com.hyperathlete.app.core.locale.Localizer
import com.hyperathlete.app.core.workout.ProgramStrings
import com.hyperathlete.app.core.workout.WorkoutAlarms
import com.hyperathlete.app.core.workout.WorkoutNotifier
import com.hyperathlete.app.data.settings.SettingsRepository
import com.hyperathlete.app.data.workout.WorkoutRepository
import com.hyperathlete.app.domain.model.AppSettings
import com.hyperathlete.app.domain.program.SessionKind
import com.hyperathlete.app.domain.program.WeekPlan
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

enum class ReminderKind { WORKOUT, MOBILITY, KNEE, FOOD, WEIGHT, TESTS }

data class PlannedReminder(val kind: ReminderKind, val at: LocalDateTime, val date: LocalDate)

/** Which reminder comes next (pure, so the rules are testable). */
object ReminderPlanner {
    val TEST_TIME: java.time.LocalTime = java.time.LocalTime.of(10, 0)

    fun next(now: LocalDateTime, s: AppSettings, kneeCheckDate: LocalDate?, doneToday: Boolean): List<PlannedReminder> {
        val out = mutableListOf<PlannedReminder>()
        val r = s.reminders
        for (offset in 0L..7L) {
            val date = now.toLocalDate().plusDays(offset)
            if (offset == 0L && doneToday) continue
            val t = WeekPlan.of(date.dayOfWeek)
            val time = s.trainingTimes[date.dayOfWeek] ?: continue
            if (t.kind == SessionKind.TRAINING && r.workout) {
                out += PlannedReminder(ReminderKind.WORKOUT, date.atTime(time).minusMinutes(r.minutesBefore.toLong()), date)
            } else if (t.kind == SessionKind.REST && r.mobility) {
                out += PlannedReminder(ReminderKind.MOBILITY, date.atTime(time), date)
            }
        }
        if (r.kneeMorning && kneeCheckDate != null) {
            out += PlannedReminder(ReminderKind.KNEE, kneeCheckDate.atTime(r.kneeMorningTime), kneeCheckDate)
        }
        val pr = s.progress
        if (pr.weightReminder && pr.weightDays.isNotEmpty()) {
            (0L..7L).map { now.toLocalDate().plusDays(it) }
                .firstOrNull { d -> d.dayOfWeek in pr.weightDays && d.atTime(pr.weightTime).isAfter(now) }
                ?.let { d -> out += PlannedReminder(ReminderKind.WEIGHT, d.atTime(pr.weightTime), d) }
        }
        if (pr.testReminder && s.programStart != null) {
            // Sunday and Thursday of a test week (weeks 1 and 9), mid-morning.
            (0L..70L).map { now.toLocalDate().plusDays(it) }
                .firstOrNull { d ->
                    (d.dayOfWeek == java.time.DayOfWeek.SUNDAY || d.dayOfWeek == java.time.DayOfWeek.THURSDAY) &&
                        s.position(d).let { it.started && it.isTestWeek } && d.atTime(TEST_TIME).isAfter(now)
                }
                ?.let { d -> out += PlannedReminder(ReminderKind.TESTS, d.atTime(TEST_TIME), d) }
        }
        val n = s.nutrition
        if (n.reminder) {
            val today = now.toLocalDate()
            val date = if (today.atTime(n.reminderTime).isAfter(now)) today else today.plusDays(1)
            out += PlannedReminder(ReminderKind.FOOD, date.atTime(n.reminderTime), date)
        }
        return out.filter { it.at.isAfter(now) }.sortedBy { it.at }
    }
}

/**
 * Workout / mobility reminders and the morning knee check (brief §7, §10.7, §18). One exact alarm per
 * kind, always pointing at the next occurrence; rescheduled after each firing, at boot and on changes.
 */
@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: SettingsRepository,
    private val workouts: WorkoutRepository,
    private val nutrition: com.hyperathlete.app.data.nutrition.NutritionRepository,
    private val profiles: com.hyperathlete.app.data.profile.ProfileRepository,
    private val progress: com.hyperathlete.app.data.progress.ProgressRepository,
    private val clock: Clock,
) {
    private val am = context.getSystemService(AlarmManager::class.java)

    suspend fun reschedule() {
        val s = settings.current()
        val now = LocalDateTime.now(clock)
        val doneToday = workouts.isDone(now.toLocalDate())
        val upcoming = ReminderPlanner.next(now, s, settings.kneeCheckDate(), doneToday)
        for (kind in ReminderKind.entries) {
            val next = upcoming.firstOrNull { it.kind == kind }
            val pi = pending(kind, next?.date)
            if (next == null) am.cancel(pi) else set(next.at, pi)
        }
    }

    suspend fun requestKneeCheck(trainingDay: LocalDate) {
        settings.setKneeCheckDate(trainingDay.plusDays(1))
        reschedule()
    }

    private fun set(at: LocalDateTime, pi: PendingIntent) {
        val millis = at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        runCatching {
            if (WorkoutAlarms.canExact(am)) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
            else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
        }
    }

    private fun pending(kind: ReminderKind, date: LocalDate?): PendingIntent = PendingIntent.getBroadcast(
        context, 500 + kind.ordinal,
        Intent(context, ReminderReceiver::class.java).setAction(ACTION_FIRE).putExtra(EXTRA_KIND, kind.name)
            .putExtra(EXTRA_DATE, date?.toEpochDay() ?: -1L),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    /** Posts the reminder notification for [kind]. */
    suspend fun show(kind: ReminderKind, date: LocalDate) {
        val s = settings.current()
        val l = Localizer(context, s.language)
        WorkoutNotifier.ensureChannels(context, l)
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED &&
            android.os.Build.VERSION.SDK_INT >= 33
        ) return
        val p = ProgramStrings(l)
        val t = WeekPlan.of(date.dayOfWeek)
        val b = NotificationCompat.Builder(context, WorkoutNotifier.CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_fitness_center)
            .setColor(s.accentArgb.toInt())
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        when (kind) {
            ReminderKind.WORKOUT -> {
                val time = s.trainingTimes[date.dayOfWeek] ?: return
                val startAt = date.atTime(time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                b.setContentTitle(p.sessionTitle(t))
                    .setContentText(l.string(R.string.reminder_workout_in, s.reminders.minutesBefore))
                    .setShortCriticalText(l.string(R.string.reminder_short_in, s.reminders.minutesBefore))
                    .setWhen(startAt).setUsesChronometer(true).setChronometerCountDown(true).setShowWhen(true)
                    .setOngoing(true)
                    .setRequestPromotedOngoing(s.workout.liveUpdates)
                    .setTimeoutAfter((startAt - clock.millis() + 60 * 60_000L).coerceAtLeast(60_000L))
                    .setContentIntent(WorkoutNotifier.openApp(context, WorkoutNotifier.OPEN_TODAY))
                    .addAction(R.drawable.ic_play_arrow_fill, l.string(R.string.reminder_start), WorkoutNotifier.openApp(context, WorkoutNotifier.OPEN_START))
            }
            ReminderKind.MOBILITY -> {
                b.setContentTitle(l.string(R.string.reminder_mobility_title))
                    .setContentText(l.string(R.string.reminder_mobility_text))
                    .setContentIntent(WorkoutNotifier.openApp(context, WorkoutNotifier.OPEN_TODAY))
                    .addAction(R.drawable.ic_play_arrow_fill, l.string(R.string.reminder_start), WorkoutNotifier.openApp(context, WorkoutNotifier.OPEN_START))
            }
            ReminderKind.FOOD -> {
                val entries = nutrition.dayOnce(date)
                val text = if (entries.isEmpty()) l.string(R.string.reminder_food_empty) else {
                    val eaten = entries.map { it.total }.fold(com.hyperathlete.app.domain.nutrition.Macros.ZERO) { a, m -> a + m }
                    val target = com.hyperathlete.app.domain.nutrition.TargetCalculator.targets(profiles.current(), s.nutrition, date)
                    when (val st = com.hyperathlete.app.domain.nutrition.DayStatus.of(eaten, target)) {
                        is com.hyperathlete.app.domain.nutrition.DayStatus.Remaining -> l.string(
                            R.string.nutri_left, l.string(R.string.unit_kcal_value, st.kcal), l.string(R.string.unit_g_value, st.protein.toString()),
                        )
                        is com.hyperathlete.app.domain.nutrition.DayStatus.ProteinOnly -> l.string(R.string.nutri_protein_left, l.string(R.string.unit_g_value, st.protein.toString()))
                        else -> return // goal reached: nothing to remind
                    }
                }
                b.setSmallIcon(R.drawable.ic_restaurant)
                    .setContentTitle(l.string(R.string.reminder_food_title))
                    .setContentText(text)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                    .setContentIntent(WorkoutNotifier.openApp(context, WorkoutNotifier.OPEN_NUTRITION))
            }
            ReminderKind.WEIGHT -> {
                b.setSmallIcon(R.drawable.ic_monitor_weight)
                    .setContentTitle(l.string(R.string.reminder_weight_title))
                    .setContentText(l.string(R.string.reminder_weight_text))
                    .setContentIntent(WorkoutNotifier.openApp(context, WorkoutNotifier.OPEN_WEIGHT))
            }
            ReminderKind.TESTS -> {
                val weekStart = com.hyperathlete.app.domain.program.ProgramClock.weekStart(date)
                if (progress.testsRecorded(weekStart, weekStart.plusDays(6))) return
                b.setSmallIcon(R.drawable.ic_flag)
                    .setContentTitle(l.string(R.string.reminder_tests_title))
                    .setContentText(l.string(R.string.reminder_tests_text))
                    .setStyle(NotificationCompat.BigTextStyle().bigText(l.string(R.string.reminder_tests_text)))
                    .setContentIntent(WorkoutNotifier.openApp(context, WorkoutNotifier.OPEN_TESTS))
            }
            ReminderKind.KNEE -> {
                b.setContentTitle(l.string(R.string.knee_morning_title))
                    .setContentText(l.string(R.string.knee_morning_text))
                    .setContentIntent(WorkoutNotifier.openApp(context, WorkoutNotifier.OPEN_KNEE))
                    .addAction(
                        R.drawable.ic_check_fill, l.string(R.string.knee_same_or_better),
                        PendingIntent.getBroadcast(
                            context, 610, Intent(context, ReminderReceiver::class.java).setAction(ACTION_KNEE_SAME),
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                        ),
                    )
            }
        }
        runCatching { NotificationManagerCompat.from(context).notify(NOTIF_BASE + kind.ordinal, b.build()) }
    }

    /** Morning answer to "how's your knee?"; clears the pending check. */
    suspend fun answerKneeMorning(pain: Int?, sharp: Boolean) {
        val last = workouts.lastKnee()
        if (last != null) {
            val morning = pain ?: workouts.maxKneeDuring(last.sessionId ?: -1) ?: last.during
            workouts.updateKnee(last.copy(morning = morning, sharp = last.sharp || sharp))
        }
        settings.setKneeCheckDate(null)
        NotificationManagerCompat.from(context).cancel(NOTIF_BASE + ReminderKind.KNEE.ordinal)
        reschedule()
    }

    companion object {
        const val ACTION_FIRE = "com.hyperathlete.app.action.REMINDER"
        const val ACTION_KNEE_SAME = "com.hyperathlete.app.action.KNEE_SAME"
        const val EXTRA_KIND = "kind"
        const val EXTRA_DATE = "date"
        const val NOTIF_BASE = 7100
    }
}

@AndroidEntryPoint
class ReminderReceiver : BroadcastReceiver() {
    @Inject lateinit var reminders: ReminderScheduler

    override fun onReceive(context: Context, intent: Intent) {
        val result = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                when (intent.action) {
                    ReminderScheduler.ACTION_FIRE -> {
                        val kind = runCatching { ReminderKind.valueOf(intent.getStringExtra(ReminderScheduler.EXTRA_KIND)!!) }.getOrNull()
                        val day = intent.getLongExtra(ReminderScheduler.EXTRA_DATE, -1)
                        if (kind != null && day >= 0) reminders.show(kind, LocalDate.ofEpochDay(day))
                        reminders.reschedule()
                    }
                    ReminderScheduler.ACTION_KNEE_SAME -> reminders.answerKneeMorning(null, false)
                    Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED, Intent.ACTION_TIMEZONE_CHANGED,
                    Intent.ACTION_TIME_CHANGED -> reminders.reschedule()
                }
            } finally {
                result.finish()
            }
        }
    }
}
