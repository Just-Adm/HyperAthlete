package com.hyperathlete.app.domain.session

import com.hyperathlete.app.domain.program.BlockType
import com.hyperathlete.app.domain.program.Measure
import com.hyperathlete.app.domain.program.PlannedSession
import com.hyperathlete.app.domain.program.Step
import com.hyperathlete.app.domain.training.SetValues
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
enum class Stage { WARMUP, WORK }

@Serializable
data class LoggedSet(
    val dbId: Long,
    val stepIndex: Int,
    val slotKey: String,
    val exerciseId: String,
    /** 1-based set (or round) number as shown to the user; extra sets go past the plan. */
    val set: Int,
    val values: SetValues,
    val at: Long,
)

/** Rest after a set. [readyAt] = the bottom of the rest range ("you can go"), [endsAt] = the top (alarm). */
@Serializable
data class RestTimer(val startedAt: Long, val readyAt: Long, val endsAt: Long) {
    val totalMs: Long get() = endsAt - startedAt
    fun remainingMs(now: Long) = (endsAt - now).coerceAtLeast(0)
}

/** Countdown for timed holds (plank, Copenhagen, stretches). Per-side holds run twice with a short switch. */
@Serializable
data class HoldTimer(
    val stepIndex: Int,
    val seconds: Int,
    val side: Int,
    val sides: Int,
    val endsAt: Long,
    /** While switching sides: the hold of side 2 starts at this time. */
    val switchUntil: Long? = null,
)

/**
 * Everything about the running session. Serialised to Room after every change; the plan itself is
 * rebuilt from (day, week, swaps) so the snapshot stays small and survives program edits.
 */
@Serializable
data class SessionState(
    val sessionId: Long,
    val epochDay: Long,
    /** java.time.DayOfWeek value of the program day being trained. */
    val dayValue: Int,
    val week: Int,
    val cycle: Int,
    val startedAt: Long,
    val stage: Stage = Stage.WARMUP,
    val warmupStartedAt: Long? = null,
    val warmupMs: Long = 0,
    val workStartedAt: Long? = null,
    val stepIndex: Int = 0,
    val swaps: Map<String, String> = emptyMap(),
    val skipped: Set<String> = emptySet(),
    val logs: List<LoggedSet> = emptyList(),
    val rest: RestTimer? = null,
    val hold: HoldTimer? = null,
    val roundStartedAt: Long? = null,
    val pausedAt: Long? = null,
    val pausedMs: Long = 0,
    /** Knee check waiting for an answer: the exercise that triggered it. */
    val kneeAsk: String? = null,
    val kneeAsked: Set<String> = emptySet(),
    val notes: Map<String, String> = emptyMap(),
) {
    val paused: Boolean get() = pausedAt != null
    val day: java.time.DayOfWeek get() = java.time.DayOfWeek.of(dayValue)

    fun encode(): String = JSON.encodeToString(serializer(), this)

    companion object {
        private val JSON = Json { ignoreUnknownKeys = true; encodeDefaults = false }
        fun decode(text: String): SessionState? = runCatching { JSON.decodeFromString(serializer(), text) }.getOrNull()
    }
}

/** Pure state transitions; time is always passed in so every rule is unit-testable. */
object SessionEngine {

    const val SWITCH_SIDES_MS = 5_000L

    fun isLogged(s: SessionState, index: Int) = s.logs.any { it.stepIndex == index }

    fun stepKey(plan: PlannedSession, index: Int): String = plan.slotOf(plan.steps[index]).key

    private fun open(plan: PlannedSession, s: SessionState, index: Int) =
        !isLogged(s, index) && stepKey(plan, index) !in s.skipped

    /** Next set to do after [from]: forward first, then anything left behind earlier. Null = all done. */
    fun nextIndex(plan: PlannedSession, s: SessionState, from: Int): Int? {
        val n = plan.steps.size
        for (i in (from + 1) until n) if (open(plan, s, i)) return i
        for (i in 0..minOf(from, n - 1)) if (open(plan, s, i)) return i
        return null
    }

    fun allDone(plan: PlannedSession, s: SessionState): Boolean =
        plan.steps.indices.none { open(plan, s, it) }

    fun current(plan: PlannedSession, s: SessionState): Step? =
        if (allDone(plan, s) && isLogged(s, s.stepIndex)) null else plan.steps.getOrNull(s.stepIndex)

    fun progress(plan: PlannedSession, s: SessionState): Pair<Int, Int> {
        val counted = plan.steps.indices.filter { stepKey(plan, it) !in s.skipped || isLogged(s, it) }
        return counted.count { isLogged(s, it) } to counted.size
    }

    fun start(s: SessionState, now: Long): SessionState = s.copy(
        stage = Stage.WORK,
        workStartedAt = now,
        warmupMs = s.warmupMs + (s.warmupStartedAt?.let { now - it } ?: 0),
        warmupStartedAt = null,
    )

    fun startAt(plan: PlannedSession, s: SessionState, now: Long): SessionState {
        val first = plan.steps.indices.firstOrNull { open(plan, s, it) } ?: 0
        val started = start(s, now).copy(stepIndex = first)
        return if (plan.steps.isNotEmpty() && isRoundStart(plan, first)) started.copy(roundStartedAt = now) else started
    }

    fun toggleWarmup(s: SessionState, now: Long): SessionState =
        if (s.warmupStartedAt == null) s.copy(warmupStartedAt = now)
        else s.copy(warmupMs = s.warmupMs + (now - s.warmupStartedAt), warmupStartedAt = null)

    fun warmupElapsed(s: SessionState, now: Long): Long = s.warmupMs + (s.warmupStartedAt?.let { now - it } ?: 0)

    /** First exercise of a superset/circuit round: the round timer restarts here. */
    fun isRoundStart(plan: PlannedSession, index: Int): Boolean {
        val step = plan.steps.getOrNull(index) ?: return false
        return plan.blockOf(step).type != BlockType.STRAIGHT && step.slot == 0
    }

    /**
     * Records the current set and moves on: starts the rest (if the plan has one here and there is a
     * next set), and raises the knee check when a knee-loading exercise / block just finished.
     */
    fun log(plan: PlannedSession, s: SessionState, values: SetValues, dbId: Long, now: Long): SessionState {
        val index = s.stepIndex
        val step = plan.steps[index]
        val slot = plan.slotOf(step)
        val already = s.logs.count { it.slotKey == slot.key }
        val entry = LoggedSet(dbId, index, slot.key, slot.exercise.id, already + 1, values, now)
        var next = s.copy(logs = s.logs + entry, hold = null)
        val nextIndex = nextIndex(plan, next, index)
        val nextStep = nextIndex?.let { plan.steps[it] }
        val block = plan.blockOf(step)
        val range = when {
            nextStep == null -> null
            step.restMax > 0 -> step.restMin to step.restMax
            // a skipped last exercise must not swallow the rest between rounds
            block.type != BlockType.STRAIGHT && nextStep.block == step.block && nextStep.set != step.set -> block.restMin to block.restMax
            else -> null
        }
        val rest = range?.let { (lo, hi) -> RestTimer(now, now + lo * 1000L, now + hi * 1000L) }
        next = next.copy(stepIndex = nextIndex ?: index, rest = rest)
        if (rest == null && nextIndex != null && isRoundStart(plan, nextIndex)) next = next.copy(roundStartedAt = now)
        // Knee check once per unit (an exercise in straight sets, a whole block for supersets/circuits).
        val unit = kneeUnit(plan, step)
        if (unit != null && unit !in next.kneeAsked && unitDone(plan, next, step)) {
            next = next.copy(kneeAsk = slot.exercise.id, kneeAsked = next.kneeAsked + unit)
        }
        return next
    }

    private fun kneeUnit(plan: PlannedSession, step: Step): String? {
        val block = plan.blockOf(step)
        return if (block.type == BlockType.STRAIGHT) {
            plan.slotOf(step).takeIf { it.exercise.knee }?.key
        } else block.takeIf { b -> b.slots.any { it.exercise.knee } }?.id
    }

    private fun unitDone(plan: PlannedSession, s: SessionState, step: Step): Boolean {
        val block = plan.blockOf(step)
        val keys = if (block.type == BlockType.STRAIGHT) setOf(plan.slotOf(step).key) else block.slots.map { it.key }.toSet()
        return plan.steps.indices.filter { stepKey(plan, it) in keys }.none { open(plan, s, it) }
    }

    fun answerKnee(s: SessionState): SessionState = s.copy(kneeAsk = null)

    fun endRest(plan: PlannedSession, s: SessionState, now: Long): SessionState {
        val r = s.copy(rest = null)
        return if (isRoundStart(plan, s.stepIndex)) r.copy(roundStartedAt = now) else r
    }

    fun extendRest(s: SessionState, seconds: Int): SessionState =
        s.rest?.let { s.copy(rest = it.copy(endsAt = it.endsAt + seconds * 1000L, readyAt = it.readyAt + seconds * 1000L)) } ?: s

    fun pause(s: SessionState, now: Long): SessionState = if (s.paused) s else s.copy(pausedAt = now)

    /** Resuming shifts every running timer by the time spent paused. */
    fun resume(s: SessionState, now: Long): SessionState {
        val at = s.pausedAt ?: return s
        val d = now - at
        return s.copy(
            pausedAt = null,
            pausedMs = s.pausedMs + d,
            rest = s.rest?.let { it.copy(startedAt = it.startedAt + d, readyAt = it.readyAt + d, endsAt = it.endsAt + d) },
            hold = s.hold?.let { it.copy(endsAt = it.endsAt + d, switchUntil = it.switchUntil?.plus(d)) },
            roundStartedAt = s.roundStartedAt?.plus(d),
            warmupStartedAt = s.warmupStartedAt?.plus(d),
        )
    }

    fun skipSlot(plan: PlannedSession, s: SessionState, key: String, now: Long): SessionState {
        val skipped = s.copy(skipped = s.skipped + key, rest = null, hold = null)
        val onIt = plan.steps.getOrNull(s.stepIndex)?.let { plan.slotOf(it).key } == key
        if (!onIt) return skipped
        val n = nextIndex(plan, skipped, s.stepIndex)
        val moved = skipped.copy(stepIndex = n ?: s.stepIndex)
        return if (n != null && isRoundStart(plan, n)) moved.copy(roundStartedAt = now) else moved
    }

    /** Jump to an exercise (also "previous exercise"). Lands on its first set not done yet, or its last set for an extra one. */
    fun jumpTo(plan: PlannedSession, s: SessionState, key: String): SessionState {
        val indices = plan.steps.indices.filter { stepKey(plan, it) == key }
        if (indices.isEmpty()) return s
        val target = indices.firstOrNull { !isLogged(s, it) } ?: indices.last()
        return s.copy(stepIndex = target, skipped = s.skipped - key, rest = null, hold = null)
    }

    /** The exercise before the current one (by slot order), for the "back" control. */
    fun previousSlotKey(plan: PlannedSession, s: SessionState): String? {
        val keys = plan.slots.map { it.key }
        val cur = plan.steps.getOrNull(s.stepIndex)?.let { plan.slotOf(it).key } ?: return null
        return keys.getOrNull(keys.indexOf(cur) - 1)
    }

    fun swap(plan: PlannedSession, s: SessionState, key: String, exerciseId: String): SessionState {
        val slot = plan.slot(key) ?: return s
        val swaps = if (exerciseId == slot.programmed.id) s.swaps - key else s.swaps + (key to exerciseId)
        return s.copy(swaps = swaps)
    }

    fun undo(s: SessionState): Pair<SessionState, LoggedSet?> {
        val last = s.logs.lastOrNull() ?: return s to null
        return s.copy(logs = s.logs.dropLast(1), stepIndex = last.stepIndex, rest = null, hold = null, kneeAsk = null) to last
    }

    /** True when the current step is a timed hold (counts down instead of reps). */
    fun isHold(plan: PlannedSession, s: SessionState): Boolean =
        plan.steps.getOrNull(s.stepIndex)?.let { plan.slotOf(it).exercise.measure == Measure.TIME } == true

    fun startHold(plan: PlannedSession, s: SessionState, seconds: Int, now: Long): SessionState {
        val slot = plan.slotOf(plan.steps[s.stepIndex])
        return s.copy(rest = null, hold = HoldTimer(s.stepIndex, seconds, 1, if (slot.exercise.perSide) 2 else 1, now + seconds * 1000L))
    }

    /** Advances a hold: side 1 → short switch → side 2. Returns (state, finished). */
    fun tickHold(s: SessionState, now: Long): Pair<SessionState, Boolean> {
        val h = s.hold ?: return s to false
        if (now < h.endsAt) return s to false
        if (h.side < h.sides) {
            val switchUntil = now + SWITCH_SIDES_MS
            return s.copy(hold = h.copy(side = h.side + 1, switchUntil = switchUntil, endsAt = switchUntil + h.seconds * 1000L)) to false
        }
        return s to true
    }

    fun note(s: SessionState, key: String, text: String): SessionState =
        s.copy(notes = if (text.isBlank()) s.notes - key else s.notes + (key to text.trim()))

    /** Training time without pauses (warm-up excluded). */
    fun activeMs(s: SessionState, now: Long): Long {
        val from = s.workStartedAt ?: s.startedAt
        val pausedNow = s.pausedAt?.let { now - it } ?: 0
        return (now - from - s.pausedMs - pausedNow).coerceAtLeast(0)
    }

    fun setsFor(s: SessionState, key: String): List<SetValues> = s.logs.filter { it.slotKey == key }.map { it.values }
}
