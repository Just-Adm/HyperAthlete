package com.hyperathlete.app.core.workout

import android.content.Context
import com.hyperathlete.app.data.program.ProgramRepository
import com.hyperathlete.app.data.settings.SettingsRepository
import com.hyperathlete.app.data.workout.KneePainEntity
import com.hyperathlete.app.data.workout.WorkoutRepository
import com.hyperathlete.app.data.workout.WorkoutSessionEntity
import com.hyperathlete.app.domain.model.WorkoutPrefs
import com.hyperathlete.app.domain.program.Measure
import com.hyperathlete.app.domain.program.Planner
import com.hyperathlete.app.domain.program.PlannedSession
import com.hyperathlete.app.domain.program.PlannedSlot
import com.hyperathlete.app.domain.program.ProgramClock
import com.hyperathlete.app.domain.session.SessionEngine
import com.hyperathlete.app.domain.session.SessionState
import com.hyperathlete.app.domain.session.Stage
import com.hyperathlete.app.domain.training.KneeAdvice
import com.hyperathlete.app.domain.training.KneeReport
import com.hyperathlete.app.domain.training.KneeRule
import com.hyperathlete.app.domain.training.PastExercise
import com.hyperathlete.app.domain.training.Progression
import com.hyperathlete.app.domain.training.SetValues
import com.hyperathlete.app.domain.training.Suggestion
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** Per-exercise context shown in the logger. */
data class ExerciseContext(
    val last: PastExercise?,
    val suggestion: Suggestion?,
    val lastNote: String?,
)

data class ActiveSession(
    val state: SessionState,
    val plan: PlannedSession,
    /** By slot key. */
    val context: Map<String, ExerciseContext>,
    /** From the last knee report (brief §15.5), shown on knee-loading exercises. */
    val knee: KneeAdvice,
) {
    val step get() = SessionEngine.current(plan, state)
    val slot: PlannedSlot? get() = step?.let(plan::slotOf)
    val done: Boolean get() = SessionEngine.allDone(plan, state)

    /** Numbers the logger starts with for the current set (last time, or the earned jump). */
    fun prefill(): SetValues {
        val step = step ?: return SetValues()
        val slot = plan.slotOf(step)
        val ctx = context[slot.key]
        return Progression.prefill(slot, step.set, ctx?.last, ctx?.suggestion, SessionEngine.setsFor(state, slot.key))
    }
}

data class SessionSummary(
    val sessionId: Long,
    val date: LocalDate,
    val day: DayOfWeek,
    val mobility: Boolean,
    val activeMs: Long,
    val volumeKg: Double,
    val sets: Int,
    val avgRir: Double?,
    /** Exercise ids with a new record. */
    val records: List<String>,
    val week: Int,
)

sealed interface SessionEvent {
    data class Finished(val summary: SessionSummary) : SessionEvent
    /** Sharp pain or swelling reported: show the physiotherapist message. */
    data object SeePhysio : SessionEvent
}

/**
 * Owns the running workout: state transitions (pure [SessionEngine]), the database rows, the snapshot
 * that brings a killed session back, timers with sounds, and the exact alarm that rings with the screen
 * off. The UI, the foreground service and notification actions all talk to this one object.
 */
@Singleton
class SessionController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repo: WorkoutRepository,
    private val programs: ProgramRepository,
    private val settings: SettingsRepository,
    private val feedback: Feedback,
    private val alarms: WorkoutAlarms,
    private val reminders: com.hyperathlete.app.core.reminders.ReminderScheduler,
    private val clock: Clock,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default.limitedParallelism(1))
    private val lock = Mutex()

    private val _active = MutableStateFlow<ActiveSession?>(null)
    val active: StateFlow<ActiveSession?> = _active.asStateFlow()

    private val _events = MutableSharedFlow<SessionEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<SessionEvent> = _events.asSharedFlow()

    @Volatile private var restored = false
    private var ticker: Job? = null
    @Volatile var prefs: WorkoutPrefs = WorkoutPrefs(); private set

    // cue bookkeeping so every sound plays exactly once per timer
    private var readyCuedFor = 0L
    private var lastTickSecond = -1L
    private var endCuedFor = 0L

    init {
        scope.launch { settings.settings.collect { prefs = it.workout } }
    }

    fun now(): Long = clock.millis()

    /** Brings back a session after the app or phone was killed. Safe to call many times. */
    suspend fun restore(): ActiveSession? = lock.withLock {
        if (restored) return@withLock _active.value
        restored = true
        val json = repo.snapshot() ?: return@withLock null
        val s = SessionState.decode(json)
        if (s == null || repo.session(s.sessionId) == null) {
            repo.clearSnapshot(); return@withLock null
        }
        val plan = planFor(s)
        val a = ActiveSession(s, plan, loadContext(plan, s), kneeAdvice())
        _active.value = a
        startTicker()
        syncAlarm(s)
        a
    }

    /** Starts (or returns the already running) session for a program day. */
    suspend fun start(day: DayOfWeek, date: LocalDate): ActiveSession {
        restore()
        return lock.withLock {
            _active.value?.let { return@withLock it }
            val st = settings.current()
            settings.ensureStreakSince(date)
            val pos = st.position(date)
            val week = pos.week
            val plan = Planner.plan(programs.program, day, week)
            val now = now()
            val id = repo.createSession(
                WorkoutSessionEntity(
                    epochDay = date.toEpochDay(), programDay = day.value, week = week, cycle = pos.cycle,
                    phase = ProgramClock.phaseOf(week).name, mobility = plan.isMobility, startedAt = now,
                ),
            )
            val s = SessionState(sessionId = id, epochDay = date.toEpochDay(), dayValue = day.value, week = week, cycle = pos.cycle, startedAt = now)
            val a = ActiveSession(s, plan, loadContext(plan, s), kneeAdvice())
            _active.value = a
            repo.saveSnapshot(s.encode(), now)
            feedback.warmUp()
            startTicker()
            a
        }
    }

    private fun planFor(s: SessionState): PlannedSession = Planner.plan(programs.program, s.day, s.week, s.swaps)

    private suspend fun loadContext(plan: PlannedSession, s: SessionState): Map<String, ExerciseContext> =
        plan.slots.associate { slot ->
            val last = repo.lastTime(slot.exercise.id, s.sessionId)
            slot.key to ExerciseContext(last, Progression.suggest(slot, last, plan.deload), repo.lastNote(slot.exercise.id))
        }

    private suspend fun kneeAdvice(): KneeAdvice {
        val last = repo.lastKnee() ?: return KneeAdvice.NORMAL
        val during = last.sessionId?.let { repo.maxKneeDuring(it) } ?: last.during
        return KneeRule.evaluate(KneeReport(during, last.morning, last.sharp))
    }

    /* ---------------- actions (UI, notification, receivers) ---------------- */

    private fun mutate(block: suspend (ActiveSession) -> SessionState?) {
        scope.launch {
            lock.withLock {
                val a = _active.value ?: return@withLock
                val next = block(a) ?: return@withLock
                apply(a, next)
            }
        }
    }

    private suspend fun apply(old: ActiveSession, s: SessionState) {
        val plan = if (s.swaps != old.state.swaps) planFor(s) else old.plan
        val ctx = if (plan !== old.plan) loadContext(plan, s) else old.context
        _active.value = old.copy(state = s, plan = plan, context = ctx)
        repo.saveSnapshot(s.encode(), now())
        syncAlarm(s)
    }

    fun toggleWarmup() = mutate { SessionEngine.toggleWarmup(it.state, now()) }

    fun begin() = mutate { a -> SessionEngine.startAt(a.plan, a.state, now()) }

    /** Saves the current set with these values and starts the rest. */
    fun log(values: SetValues) = mutate { a -> logNow(a, values) }

    private suspend fun logNow(a: ActiveSession, values: SetValues): SessionState? {
        val s = a.state
        if (s.stage != Stage.WORK) return null
        val step = a.plan.steps.getOrNull(s.stepIndex) ?: return null
        if (a.done && SessionEngine.isLogged(s, s.stepIndex)) return null
        val slot = a.plan.slotOf(step)
        val setNo = s.logs.count { it.slotKey == slot.key } + 1
        val now = now()
        val id = repo.logSet(s.sessionId, slot.exercise.id, slot.key, setNo, values, a.plan.deload, now)
        resetCues()
        return SessionEngine.log(a.plan, s, values, id, now)
    }

    /** "Set done" from the notification / lock screen: logs the prefilled numbers, or starts a hold. */
    fun quickLog() = mutate { a ->
        val slot = a.slot ?: return@mutate null
        val v = a.prefill()
        if (slot.exercise.measure == Measure.TIME && a.state.hold == null) {
            resetCues(); SessionEngine.startHold(a.plan, a.state, v.seconds ?: slot.min, now())
        } else logNow(a, v)
    }

    fun startHold(seconds: Int) = mutate { resetCues(); SessionEngine.startHold(it.plan, it.state, seconds, now()) }
    fun cancelHold() = mutate { it.state.copy(hold = null) }
    fun skipRest() = mutate { a -> resetCues(); SessionEngine.endRest(a.plan, a.state, now()) }
    fun addRest(seconds: Int) = mutate { endCuedFor = 0; SessionEngine.extendRest(it.state, seconds) }
    fun pause() = mutate { SessionEngine.pause(it.state, now()) }
    fun resume() = mutate { SessionEngine.resume(it.state, now()) }
    fun togglePause() = mutate { if (it.state.paused) SessionEngine.resume(it.state, now()) else SessionEngine.pause(it.state, now()) }
    fun skipExercise(key: String) = mutate { resetCues(); SessionEngine.skipSlot(it.plan, it.state, key, now()) }
    fun jumpTo(key: String) = mutate { resetCues(); SessionEngine.jumpTo(it.plan, it.state, key) }
    fun previous() = mutate { a -> SessionEngine.previousSlotKey(a.plan, a.state)?.let { SessionEngine.jumpTo(a.plan, a.state, it) } }
    fun swap(key: String, exerciseId: String) = mutate { SessionEngine.swap(it.plan, it.state, key, exerciseId) }

    fun note(key: String, text: String) = mutate { a ->
        val slot = a.plan.slot(key) ?: return@mutate null
        if (text.isNotBlank()) repo.addNote(a.state.sessionId, slot.exercise.id, text.trim(), now())
        SessionEngine.note(a.state, key, text)
    }

    fun undo() = mutate { a ->
        val (s, removed) = SessionEngine.undo(a.state)
        removed?.let { repo.deleteSet(it.dbId) }
        resetCues()
        s
    }

    fun answerKnee(pain: Int?, sharp: Boolean) = mutate { a ->
        val ex = a.state.kneeAsk
        if (pain != null || sharp) {
            repo.saveKnee(
                KneePainEntity(
                    epochDay = a.state.epochDay, sessionId = a.state.sessionId, exerciseId = ex,
                    during = pain, sharp = sharp, createdAt = now(),
                ),
            )
        }
        if (sharp) _events.tryEmit(SessionEvent.SeePhysio)
        SessionEngine.answerKnee(a.state)
    }

    /** Ends the session: totals, records, streak day, and clears the running state. */
    suspend fun finish(): SessionSummary? = lock.withLock {
        val a = _active.value ?: return@withLock null
        val s = a.state
        val now = now()
        val rows = repo.setsOf(s.sessionId)
        if (rows.isEmpty()) {
            discardLocked(a); return@withLock null
        }
        val values = rows.map { SetValues(it.weightKg, it.reps, it.seconds, it.meters, it.rir) }
        val rirs = rows.mapNotNull { it.rir }
        val records = rows.groupBy { it.exerciseId }.filter { (id, sets) ->
            Progression.isRecord(sets.map { SetValues(it.weightKg, it.reps) }, repo.everythingBefore(id, s.sessionId))
        }.keys.toList()
        val activeMs = SessionEngine.activeMs(s, now)
        val session = repo.session(s.sessionId) ?: return@withLock null
        repo.updateSession(
            session.copy(
                endedAt = now, activeSeconds = (activeMs / 1000).toInt(), completed = true,
                volumeKg = Progression.volume(values), sets = rows.size,
                avgRir = rirs.takeIf { it.isNotEmpty() }?.average(), records = records.size,
            ),
        )
        val summary = SessionSummary(
            s.sessionId, LocalDate.ofEpochDay(s.epochDay), s.day, a.plan.isMobility, activeMs,
            Progression.volume(values), rows.size, rirs.takeIf { it.isNotEmpty() }?.average(), records, s.week,
        )
        endLocked()
        feedback.play(Cue.SESSION_DONE, prefs)
        if (repo.maxKneeDuring(s.sessionId) != null) reminders.requestKneeCheck(summary.date) else reminders.reschedule()
        _events.tryEmit(SessionEvent.Finished(summary))
        summary
    }

    suspend fun discard() = lock.withLock { _active.value?.let { discardLocked(it) } }

    private suspend fun discardLocked(a: ActiveSession) {
        repo.deleteSession(a.state.sessionId)
        endLocked()
    }

    private suspend fun endLocked() {
        repo.clearSnapshot()
        alarms.cancelTimer()
        ticker?.cancel(); ticker = null
        _active.value = null
    }

    /* ---------------- timers ---------------- */

    private fun resetCues() {
        readyCuedFor = 0; lastTickSecond = -1; endCuedFor = 0
    }

    private fun syncAlarm(s: SessionState) {
        val at = when {
            s.paused -> null
            s.hold != null -> s.hold.switchUntil?.takeIf { it > now() } ?: s.hold.endsAt
            s.rest != null -> s.rest.endsAt
            else -> null
        }
        if (at == null) alarms.cancelTimer() else alarms.scheduleTimer(at)
    }

    private fun startTicker() {
        if (ticker?.isActive == true) return
        ticker = scope.launch {
            while (isActive) {
                tick()
                delay(200)
            }
        }
    }

    /** Called by the exact alarm when the screen is off and the ticker may be asleep. */
    suspend fun onAlarm() {
        restore()
        startTicker()
        tick()
    }

    private suspend fun tick() = lock.withLock {
        val a = _active.value ?: return@withLock
        val s = a.state
        if (s.paused) return@withLock
        val now = now()
        s.rest?.let { r ->
            if (r.readyAt < r.endsAt && now >= r.readyAt && readyCuedFor != r.startedAt) {
                readyCuedFor = r.startedAt
                feedback.play(Cue.READY, prefs)
            }
            countdownTicks(r.endsAt, now)
            if (now >= r.endsAt) {
                if (endCuedFor != r.endsAt) {
                    endCuedFor = r.endsAt
                    feedback.play(Cue.REST_END, prefs)
                }
                val next = SessionEngine.endRest(a.plan, s, now)
                apply(a, next)
                resetCues()
            }
            return@withLock
        }
        s.hold?.let { h ->
            if (h.switchUntil != null && now < h.switchUntil) {
                countdownTicks(h.switchUntil, now)
                return@withLock
            }
            countdownTicks(h.endsAt, now)
            val (next, finished) = SessionEngine.tickHold(s, now)
            if (finished) {
                feedback.play(Cue.READY, prefs)
                resetCues()
                logNow(a, SetValues(seconds = h.seconds))?.let { apply(a, it) }
            } else if (next !== s) {
                feedback.play(Cue.READY, prefs)
                lastTickSecond = -1
                apply(a, next)
            }
        }
    }

    private fun countdownTicks(endsAt: Long, now: Long) {
        val left = (endsAt - now + 999) / 1000
        if (left in 1..3 && left != lastTickSecond) {
            lastTickSecond = left
            feedback.play(Cue.TICK, prefs)
        }
    }
}
