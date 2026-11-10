package com.hyperathlete.app.domain.program

import java.time.DayOfWeek
import kotlin.math.ceil

/** One exercise as it will be done today: phase variant picked, deload applied, swap resolved. */
data class PlannedSlot(
    /** Stable id inside the day ("main:bench_press") used for swaps, skips and logs. */
    val key: String,
    val exercise: Exercise,
    /** The programmed exercise before any swap. */
    val programmed: Exercise,
    val alternatives: List<Exercise>,
    val sets: Int,
    val min: Int,
    val max: Int,
    val restMin: Int,
    val restMax: Int,
    val note: Txt?,
    val load: Load,
    /** 3-second lowering (foundation phase, lower body). */
    val tempo: Boolean,
) {
    val swapped: Boolean get() = exercise.id != programmed.id
    val isRange: Boolean get() = min != max
}

data class PlannedBlock(
    val id: String,
    val type: BlockType,
    val rounds: Int,
    val restMin: Int,
    val restMax: Int,
    val title: Txt?,
    val minutes: Int?,
    val note: Txt?,
    val slots: List<PlannedSlot>,
)

/** A single "do this now" moment of the session. For supersets/circuits [set] is the round. */
data class Step(
    val block: Int,
    val slot: Int,
    val set: Int,
    val of: Int,
    val restMin: Int,
    val restMax: Int,
)

data class PlannedSession(
    val day: DayOfWeek,
    val template: DayTemplate,
    val week: Int,
    val phase: Phase,
    val deload: Boolean,
    val notes: List<Txt>,
    val blocks: List<PlannedBlock>,
) {
    val slots: List<PlannedSlot> get() = blocks.flatMap { it.slots }
    val isMobility: Boolean get() = template.kind == SessionKind.REST

    fun slot(key: String): PlannedSlot? = slots.firstOrNull { it.key == key }
    fun slotOf(step: Step): PlannedSlot = blocks[step.block].slots[step.slot]
    fun blockOf(step: Step): PlannedBlock = blocks[step.block]

    /** Every set / round in order, with the rest that follows it. The very last step has no rest. */
    val steps: List<Step> by lazy {
        val out = mutableListOf<Step>()
        blocks.forEachIndexed { bi, b ->
            when (b.type) {
                BlockType.STRAIGHT -> b.slots.forEachIndexed { si, s ->
                    for (set in 1..s.sets) out += Step(bi, si, set, s.sets, s.restMin, s.restMax)
                }
                BlockType.SUPERSET, BlockType.CIRCUIT -> for (round in 1..b.rounds) {
                    b.slots.forEachIndexed { si, _ ->
                        val last = si == b.slots.lastIndex
                        out += Step(bi, si, round, b.rounds, if (last) b.restMin else 0, if (last) b.restMax else 0)
                    }
                }
            }
        }
        if (out.isNotEmpty()) out[out.lastIndex] = out.last().copy(restMin = 0, restMax = 0)
        out
    }

    val totalSets: Int get() = steps.size
}

object Planner {

    /**
     * @param week week inside the 9-week cycle (1..9); picks phase variants (Goblet → Back Squat …).
     * @param swaps slot key → alternative exercise id chosen in the session.
     */
    fun plan(program: Program, day: DayOfWeek, week: Int, swaps: Map<String, String> = emptyMap()): PlannedSession {
        val template = WeekPlan.of(day)
        val phase = ProgramClock.phaseOf(week)
        val deload = phase == Phase.DELOAD && template.kind == SessionKind.TRAINING
        val programDay = program.day(day)
        val blocks = programDay.blocks.map { b ->
            val rounds = if (deload) half(b.rounds) else b.rounds
            val slots = b.slots.filter { it.appliesTo(week) }.map { s ->
                val programmed = program.exercise(s.exercise)
                val key = "${b.id}:${s.exercise}"
                val chosen = swaps[key]?.takeIf { it in s.alts }?.let(program::exerciseOrNull) ?: programmed
                val sets = when {
                    b.type != BlockType.STRAIGHT -> rounds
                    deload -> half(s.sets)
                    else -> s.sets
                }
                val rest = s.rest ?: b.rest
                PlannedSlot(
                    key = key,
                    exercise = chosen,
                    programmed = programmed,
                    alternatives = (listOf(programmed.id) + s.alts).filter { it != chosen.id }.map(program::exercise),
                    sets = sets,
                    min = s.min,
                    max = s.max,
                    restMin = rest.first(),
                    restMax = rest.last(),
                    note = s.note,
                    load = s.load ?: chosen.load,
                    tempo = phase == Phase.FOUNDATION && chosen.lower,
                )
            }
            PlannedBlock(b.id, b.type, rounds, b.rest.first(), b.rest.last(), b.title, b.minutes, b.note, slots)
        }.filter { it.slots.isNotEmpty() }
        return PlannedSession(day, template, week, phase, deload, programDay.notes, blocks)
    }

    /** "Half the sets" in the deload week, never below one. */
    fun half(n: Int): Int = ceil(n / 2.0).toInt().coerceAtLeast(1)
}
