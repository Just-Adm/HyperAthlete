package com.hyperathlete.app.core.calendar

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.hyperathlete.app.R
import com.hyperathlete.app.core.locale.Localizer
import com.hyperathlete.app.core.workout.ProgramStrings
import com.hyperathlete.app.data.program.ProgramRepository
import com.hyperathlete.app.data.settings.SettingsRepository
import com.hyperathlete.app.data.workout.CalendarEventEntity
import com.hyperathlete.app.data.workout.WorkoutDao
import com.hyperathlete.app.domain.model.AppSettings
import com.hyperathlete.app.domain.program.Phase
import com.hyperathlete.app.domain.program.Planner
import com.hyperathlete.app.domain.program.ProgramClock
import com.hyperathlete.app.domain.program.SessionKind
import com.hyperathlete.app.domain.program.WeekPlan
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

data class CalendarInfo(val id: Long, val name: String, val account: String, val color: Int, val own: Boolean)

/** One event the app wants in the calendar. [key] identifies it across syncs. */
data class CalEvent(
    val key: String,
    val title: String,
    val description: String,
    val start: Long,
    val end: Long,
    val allDay: Boolean,
    val reminderMinutes: Int?,
)

/**
 * Calendar sync (brief §7): a "Hyper Athlete" calendar (or one the user picks) with an event per day
 * for the next weeks — title, exercise list with sets × reps, the training time and a reminder.
 * Rest days read "Rest + mobility"; the deload week and test weeks get an all-day marker. Only events
 * this app created are ever changed or removed.
 */
@Singleton
class CalendarSync @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: SettingsRepository,
    private val programs: ProgramRepository,
    private val dao: WorkoutDao,
    private val clock: Clock,
) {
    private val cr get() = context.contentResolver

    fun hasPermission(): Boolean = listOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)
        .all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }

    fun calendars(): List<CalendarInfo> {
        if (!hasPermission()) return emptyList()
        val out = mutableListOf<CalendarInfo>()
        runCatching {
            cr.query(
                CalendarContract.Calendars.CONTENT_URI,
                arrayOf(
                    CalendarContract.Calendars._ID, CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
                    CalendarContract.Calendars.ACCOUNT_NAME, CalendarContract.Calendars.CALENDAR_COLOR,
                    CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL, CalendarContract.Calendars.ACCOUNT_TYPE,
                ),
                null, null, null,
            )?.use { c ->
                while (c.moveToNext()) {
                    if (c.getInt(4) < CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR) continue
                    val own = c.getString(2) == ACCOUNT && c.getString(5) == CalendarContract.ACCOUNT_TYPE_LOCAL
                    out += CalendarInfo(c.getLong(0), c.getString(1) ?: "", c.getString(2) ?: "", c.getInt(3), own)
                }
            }
        }
        return out.sortedByDescending { it.own }
    }

    /** Brings the calendar in line with the program for the coming weeks. Returns the number of changes. */
    suspend fun sync(): Int {
        val s = settings.current()
        if (!s.calendar.sync || !hasPermission()) return 0
        val calendarId = s.calendar.calendarId?.takeIf { id -> calendars().any { it.id == id } } ?: ensureOwnCalendar(s) ?: return 0
        val wanted = plan(s, LocalDate.now(clock)).associateBy { it.key }
        val known = dao.calendarEvents().associateBy { it.key }
        var changes = 0
        // remove events for days that left the window or moved to another calendar
        for ((key, e) in known) {
            if (key !in wanted || e.calendarId != calendarId) {
                deleteEvent(e.eventId); dao.deleteCalendarEvent(key); changes++
            }
        }
        val current = dao.calendarEvents().associateBy { it.key }
        for ((key, ev) in wanted) {
            val hash = ev.hashCode()
            val existing = current[key]
            if (existing != null && existing.hash == hash && eventExists(existing.eventId)) continue
            if (existing != null) deleteEvent(existing.eventId)
            val id = insertEvent(calendarId, ev) ?: continue
            dao.saveCalendarEvent(CalendarEventEntity(key, id, calendarId, hash))
            changes++
        }
        return changes
    }

    /** "Stop syncing": removes only this app's events (and its own calendar if it made one). */
    suspend fun clear() {
        if (!hasPermission()) { dao.clearCalendarEvents(); return }
        dao.calendarEvents().forEach { deleteEvent(it.eventId) }
        dao.clearCalendarEvents()
        calendars().filter { it.own }.forEach { cal ->
            runCatching { cr.delete(asSyncAdapter(ContentUris.withAppendedId(CalendarContract.Calendars.CONTENT_URI, cal.id)), null, null) }
        }
    }

    /** The events for [weeks] weeks from the start of this week (pure apart from string lookups). */
    fun plan(s: AppSettings, today: LocalDate, weeks: Int = HORIZON_WEEKS): List<CalEvent> {
        val l = Localizer(context, s.language)
        val p = ProgramStrings(l)
        val zone = ZoneId.systemDefault()
        val from = ProgramClock.weekStart(today)
        val out = mutableListOf<CalEvent>()
        for (d in 0 until weeks * 7) {
            val date = from.plusDays(d.toLong())
            val pos = s.position(date)
            val t = WeekPlan.of(date.dayOfWeek)
            val time = s.trainingTimes[date.dayOfWeek] ?: continue
            val start = date.atTime(time).atZone(zone).toInstant().toEpochMilli()
            if (t.kind == SessionKind.REST) {
                out += CalEvent(
                    "d:${date.toEpochDay()}", l.string(R.string.cal_rest_title),
                    mobilityText(l, p, date, pos.week), start, start + 20 * 60_000L, false, s.calendar.reminderMinutes,
                )
            } else {
                val plan = Planner.plan(programs.program, date.dayOfWeek, pos.week)
                val title = p.sessionTitle(t) + if (plan.deload) " · " + l.term(R.string.term_deload) else ""
                val desc = buildString {
                    appendLine(l.string(R.string.cal_week_line, pos.week, ProgramClock.WEEKS, phaseName(l, pos.phase)))
                    t.floor?.let { appendLine(l.string(R.string.floor_value, it)) }
                    appendLine()
                    plan.blocks.forEach { b ->
                        b.title?.let { appendLine(it.ui(s.language)) }
                        b.slots.forEachIndexed { i, slot -> appendLine("${i + 1}. ${p.exercise(slot)} — ${p.target(slot)}") }
                    }
                    plan.notes.forEach { appendLine(); append("• ").appendLine(it.ui(s.language)) }
                }.trim()
                out += CalEvent("d:${date.toEpochDay()}", title, desc, start, start + t.minutes.last * 60_000L, false, s.calendar.reminderMinutes)
            }
            if (date.dayOfWeek == java.time.DayOfWeek.SUNDAY && (pos.week == ProgramClock.WEEKS || pos.isTestWeek)) {
                val allDayStart = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
                val label = if (pos.week == ProgramClock.WEEKS) R.string.cal_deload_week else R.string.cal_test_week
                out += CalEvent("w:${date.toEpochDay()}", l.string(label), l.string(R.string.cal_tests_hint), allDayStart, allDayStart + 7 * 86_400_000L, true, null)
            }
        }
        return out
    }

    private fun mobilityText(l: Localizer, p: ProgramStrings, date: LocalDate, week: Int): String {
        val plan = Planner.plan(programs.program, date.dayOfWeek, week)
        return plan.slots.mapIndexed { i, slot -> "${i + 1}. ${p.exercise(slot)} — ${p.target(slot)}" }.joinToString("\n")
    }

    private fun phaseName(l: Localizer, phase: Phase) = when (phase) {
        Phase.FOUNDATION -> l.string(R.string.phase_foundation)
        Phase.BUILD -> l.string(R.string.phase_build)
        Phase.DELOAD -> l.term(R.string.term_deload)
    }

    private fun ensureOwnCalendar(s: AppSettings): Long? {
        calendars().firstOrNull { it.own }?.let { return it.id }
        val v = ContentValues().apply {
            put(CalendarContract.Calendars.ACCOUNT_NAME, ACCOUNT)
            put(CalendarContract.Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
            put(CalendarContract.Calendars.NAME, ACCOUNT)
            put(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, ACCOUNT)
            put(CalendarContract.Calendars.CALENDAR_COLOR, s.accentArgb.toInt())
            put(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL, CalendarContract.Calendars.CAL_ACCESS_OWNER)
            put(CalendarContract.Calendars.OWNER_ACCOUNT, ACCOUNT)
            put(CalendarContract.Calendars.VISIBLE, 1)
            put(CalendarContract.Calendars.SYNC_EVENTS, 1)
            put(CalendarContract.Calendars.CALENDAR_TIME_ZONE, TimeZone.getDefault().id)
        }
        return runCatching { cr.insert(asSyncAdapter(CalendarContract.Calendars.CONTENT_URI), v)?.let(ContentUris::parseId) }.getOrNull()
    }

    private fun insertEvent(calendarId: Long, e: CalEvent): Long? {
        val v = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.TITLE, e.title)
            put(CalendarContract.Events.DESCRIPTION, e.description)
            put(CalendarContract.Events.DTSTART, e.start)
            put(CalendarContract.Events.DTEND, e.end)
            put(CalendarContract.Events.ALL_DAY, if (e.allDay) 1 else 0)
            put(CalendarContract.Events.EVENT_TIMEZONE, if (e.allDay) "UTC" else TimeZone.getDefault().id)
            put(CalendarContract.Events.HAS_ALARM, if (e.reminderMinutes != null) 1 else 0)
            put(CalendarContract.Events.CUSTOM_APP_PACKAGE, context.packageName)
            put(CalendarContract.Events.CUSTOM_APP_URI, "hyperathlete://day/${e.key}")
        }
        val id = runCatching { cr.insert(CalendarContract.Events.CONTENT_URI, v)?.let(ContentUris::parseId) }.getOrNull() ?: return null
        e.reminderMinutes?.let { m ->
            runCatching {
                cr.insert(
                    CalendarContract.Reminders.CONTENT_URI,
                    ContentValues().apply {
                        put(CalendarContract.Reminders.EVENT_ID, id)
                        put(CalendarContract.Reminders.MINUTES, m)
                        put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
                    },
                )
            }
        }
        return id
    }

    private fun eventExists(id: Long): Boolean = runCatching {
        cr.query(ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, id), arrayOf(CalendarContract.Events.DELETED), null, null, null)
            ?.use { it.moveToFirst() && it.getInt(0) == 0 } ?: false
    }.getOrDefault(false)

    private fun deleteEvent(id: Long) {
        runCatching { cr.delete(ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, id), null, null) }
    }

    private fun asSyncAdapter(uri: Uri): Uri = uri.buildUpon()
        .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
        .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, ACCOUNT)
        .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
        .build()

    companion object {
        const val ACCOUNT = "Hyper Athlete"
        const val HORIZON_WEEKS = 5
    }
}

/** Runs [CalendarSync] off the main thread; daily to roll the window forward, and after every change. */
@HiltWorker
class CalendarSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val sync: CalendarSync,
    private val settings: SettingsRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = runCatching {
        if (inputData.getBoolean(KEY_CLEAR, false) || !settings.current().calendar.sync) sync.clear() else sync.sync()
        Result.success()
    }.getOrElse { Result.retry() }

    companion object {
        private const val KEY_CLEAR = "clear"

        fun syncNow(context: Context, clear: Boolean = false) {
            WorkManager.getInstance(context).enqueueUniqueWork(
                "calendar-sync", ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<CalendarSyncWorker>()
                    .setInputData(androidx.work.workDataOf(KEY_CLEAR to clear))
                    .setInitialDelay(2, TimeUnit.SECONDS)
                    .build(),
            )
        }

        fun scheduleDaily(context: Context) {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "calendar-daily", ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<CalendarSyncWorker>(1, TimeUnit.DAYS).build(),
            )
        }
    }
}
