package com.hyperathlete.app.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hyperathlete.app.R
import com.hyperathlete.app.core.locale.Bidi
import com.hyperathlete.app.core.locale.bidiJoin
import com.hyperathlete.app.core.workout.ActiveSession
import com.hyperathlete.app.domain.program.BlockType
import com.hyperathlete.app.domain.program.PlannedSession
import com.hyperathlete.app.domain.program.PlannedSlot
import com.hyperathlete.app.domain.program.Planner
import com.hyperathlete.app.domain.program.Program
import com.hyperathlete.app.domain.program.ProgramClock
import com.hyperathlete.app.domain.program.ProgramPosition
import com.hyperathlete.app.domain.program.SessionKind
import com.hyperathlete.app.domain.program.WeekPlan
import com.hyperathlete.app.domain.session.SessionEngine
import com.hyperathlete.app.domain.training.KneeAdvice
import com.hyperathlete.app.domain.training.Motivation
import com.hyperathlete.app.ui.AppState
import com.hyperathlete.app.ui.art.Pictogram
import com.hyperathlete.app.ui.common.dayArt
import com.hyperathlete.app.ui.common.dayFocus
import com.hyperathlete.app.ui.common.dayName
import com.hyperathlete.app.ui.common.exerciseName
import com.hyperathlete.app.ui.common.formatTime
import com.hyperathlete.app.ui.common.minutesLabel
import com.hyperathlete.app.ui.common.phaseName
import com.hyperathlete.app.ui.common.rememberProgramStrings
import com.hyperathlete.app.ui.common.restLabel
import com.hyperathlete.app.ui.common.sessionTitle
import com.hyperathlete.app.ui.common.text
import com.hyperathlete.app.ui.common.weekPhaseLabel
import com.hyperathlete.app.ui.components.HyperCard
import com.hyperathlete.app.ui.components.HyperChip
import com.hyperathlete.app.ui.components.HyperSlider
import com.hyperathlete.app.ui.components.HyperSwitch
import com.hyperathlete.app.ui.components.IconTile
import com.hyperathlete.app.ui.components.PrimaryButton
import com.hyperathlete.app.ui.components.SecondaryButton
import com.hyperathlete.app.ui.components.SmallPillButton
import com.hyperathlete.app.ui.components.hyperClickable
import com.hyperathlete.app.ui.theme.Hyper
import com.hyperathlete.app.ui.theme.glass
import com.hyperathlete.app.ui.theme.HyperShapes
import java.time.DayOfWeek
import java.time.LocalDate

/** Training data and actions for the Home boxes; plain values so screenshot tests can fake them. */
class HomeTraining(
    val program: Program,
    val position: ProgramPosition,
    val doneDays: Set<LocalDate>,
    val todayLogged: Set<String>,
    val active: ActiveSession?,
    val kneeDue: Boolean,
    val knee: KneeAdvice,
    val onOpenDay: (DayOfWeek) -> Unit,
    val onChecklist: () -> Unit,
    val onResume: () -> Unit,
    val onKnee: () -> Unit,
) {
    fun plan(day: DayOfWeek): PlannedSession = Planner.plan(program, day, position.week)
}

/* ------------------------------------------------------------------ Today */

@Composable
fun TodayBox(state: AppState, t: HomeTraining) {
    val c = Hyper.colors
    val today = WeekPlan.of(state.today.dayOfWeek)
    val plan = remember(t.program, today.day, t.position.week) { t.plan(today.day) }
    val done = state.today in t.doneDays
    val logged = plan.slots.count { it.key in t.todayLogged }
    val line = Motivation.lineFor(state.today, t.position.week == ProgramClock.WEEKS)
    HyperCard(
        Modifier.fillMaxWidth(),
        borderColor = if (today.kind == SessionKind.TRAINING) c.accent.copy(alpha = 0.55f) else c.border,
        onClick = { t.onOpenDay(today.day) },
        onLongClick = t.onChecklist,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconTile(if (today.kind == SessionKind.TRAINING) R.drawable.ic_fitness_center else R.drawable.ic_self_improvement)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.home_today_label, dayName(today.day)), style = Hyper.type.small, color = c.textMuted)
                Text(
                    if (today.kind == SessionKind.REST) stringResource(R.string.home_rest_title) else sessionTitle(today),
                    style = Hyper.type.h2, color = c.text,
                )
            }
            Box(
                Modifier.size(36.dp).clip(CircleShape).hyperClickable(onClick = t.onChecklist),
                contentAlignment = Alignment.Center,
            ) { Icon(painterResource(R.drawable.ic_checklist), stringResource(R.string.checklist_title), tint = c.textMuted, modifier = Modifier.size(20.dp)) }
        }
        Spacer(Modifier.height(10.dp))
        Text(Motivation.render(line.text(), state.profile.name.trim()), style = Hyper.type.body, color = c.textSecondary)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            when {
                done -> HyperChip(stringResource(R.string.today_done), icon = R.drawable.ic_check, color = c.success, background = c.successSoft)
                logged > 0 -> HyperChip(stringResource(R.string.today_progress, logged, plan.slots.size), icon = R.drawable.ic_timer_play)
                else -> state.settings.trainingTimes[today.day]?.let { HyperChip(formatTime(it), icon = R.drawable.ic_schedule) }
            }
            HyperChip(minutesLabel(today.minutes), icon = R.drawable.ic_timer, color = c.textSecondary, background = c.surfaceAlt)
        }
    }
}

/* ------------------------------------------------------------------ Week schedule */

@Composable
fun ScheduleBox(state: AppState, t: HomeTraining) {
    val c = Hyper.colors
    val weekStart = ProgramClock.weekStart(state.today)
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.box_schedule), style = Hyper.type.h2, color = c.text)
                Text(if (t.position.started) weekPhaseLabel(t.position) else stringResource(R.string.program_starts_in, t.position.daysUntilStart), style = Hyper.type.small, color = c.textMuted)
            }
        }
        Spacer(Modifier.height(10.dp))
        WeekPlan.days.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth().height(androidx.compose.foundation.layout.IntrinsicSize.Max).padding(bottom = 12.dp)) {
                pair.forEach { d ->
                    val date = weekStart.plusDays(((d.day.value % 7)).toLong())
                    DayCard(
                        day = d.day,
                        week = t.position.week,
                        plan = remember(t.program, d.day, t.position.week) { t.plan(d.day) },
                        today = date == state.today,
                        done = date in t.doneDays,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        onClick = { t.onOpenDay(d.day) },
                    )
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun DayCard(day: DayOfWeek, week: Int, plan: PlannedSession, today: Boolean, done: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val c = Hyper.colors
    val rest = plan.isMobility
    val tileBg = if (rest) c.secondarySoft else c.accentSoft
    HyperCard(
        modifier,
        padding = androidx.compose.foundation.layout.PaddingValues(10.dp),
        borderColor = if (today) c.accent else c.border,
        onClick = onClick,
    ) {
        Box(Modifier.fillMaxWidth().height(96.dp).clip(HyperShapes.cardSmall).background(tileBg)) {
            Pictogram(dayArt(day, week), Modifier.fillMaxWidth().height(96.dp).padding(6.dp), figure = if (rest) c.secondary else c.accent, halo = tileBg)
            if (done) {
                Box(
                    Modifier.align(Alignment.TopEnd).padding(7.dp).size(24.dp).clip(CircleShape).background(c.success),
                    contentAlignment = Alignment.Center,
                ) { Icon(painterResource(R.drawable.ic_check), null, tint = Color.White, modifier = Modifier.size(15.dp)) }
            }
        }
        Spacer(Modifier.height(10.dp))
        Column(Modifier.padding(horizontal = 4.dp)) {
            Text(dayName(day), style = Hyper.type.micro, color = if (today) c.accent else c.textMuted, maxLines = 1)
            Text(
                if (rest) stringResource(R.string.home_rest_title) else sessionTitle(plan.template),
                style = Hyper.type.title, color = c.text, maxLines = 2, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.heightIn(min = 44.dp),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                bidiJoin(listOf(stringResource(R.string.exercises_count, plan.slots.size), minutesLabel(plan.template.minutes))),
                style = Hyper.type.micro, color = c.textMuted, maxLines = 1,
            )
            Text(dayFocus(day), style = Hyper.type.micro, color = if (rest) c.secondary else c.accentDeep, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.height(2.dp))
    }
}

/* ------------------------------------------------------------------ Running session / knee */

@Composable
fun ResumeCard(a: ActiveSession, onResume: () -> Unit, modifier: Modifier = Modifier) {
    val c = Hyper.colors
    val slot = a.slot
    val (done, total) = SessionEngine.progress(a.plan, a.state)
    HyperCard(modifier.fillMaxWidth(), color = c.accent, borderColor = c.accent, onClick = onResume) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconTile(R.drawable.ic_timer_play, tint = c.accent, background = Color.White)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.resume_label), style = Hyper.type.small, color = c.onAccent.copy(alpha = 0.85f))
                Text(
                    slot?.let { exerciseName(it.exercise) } ?: sessionTitle(a.plan.template),
                    style = Hyper.type.title, color = c.onAccent, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Text(stringResource(R.string.sets_progress, done, total), style = Hyper.type.micro, color = c.onAccent.copy(alpha = 0.85f))
            }
            SmallPillButton(stringResource(R.string.resume_action), onResume, filled = false)
        }
    }
}

@Composable
fun KneeCheckCard(onAnswer: () -> Unit, modifier: Modifier = Modifier) {
    val c = Hyper.colors
    HyperCard(modifier.fillMaxWidth(), color = c.warnSoft, borderColor = c.warnSoft, elevated = false, onClick = onAnswer) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(R.drawable.ic_healing), null, tint = c.warn, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.knee_morning_title), style = Hyper.type.bodyMedium, color = c.text)
                Text(stringResource(R.string.knee_morning_text), style = Hyper.type.small, color = c.textSecondary)
            }
            SmallPillButton(stringResource(R.string.knee_answer), onAnswer)
        }
    }
}

/* ------------------------------------------------------------------ Sheets */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HyperSheet(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val c = Hyper.colors
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Hyper.glass.fillStrong,
        shape = HyperShapes.sheet,
        tonalElevation = 0.dp,
        dragHandle = { Box(Modifier.padding(top = 10.dp).size(40.dp, 4.dp).clip(HyperShapes.pill).background(c.borderStrong)) },
    ) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding(), content = content)
    }
}

/** Day details (box 3 / box 2 tap): picture, exercises with sets × reps, rest, notes, floor, and Start. */
@Composable
fun DayDetails(
    plan: PlannedSession,
    position: ProgramPosition,
    activeOther: Boolean,
    activeThis: Boolean,
    onStart: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Hyper.colors
    val rest = plan.isMobility
    val tileBg = if (rest) c.secondarySoft else c.accentSoft
    Column(modifier.fillMaxWidth()) {
        Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(8.dp))
            Box(Modifier.fillMaxWidth().height(150.dp).clip(HyperShapes.card).background(tileBg)) {
                Pictogram(dayArt(plan.day, position.week), Modifier.fillMaxWidth().height(150.dp).padding(10.dp), animate = true,
                    figure = if (rest) c.secondary else c.accent, halo = tileBg)
            }
            Spacer(Modifier.height(14.dp))
            Text(dayName(plan.day), style = Hyper.type.smallMedium, color = c.textMuted)
            Text(if (rest) stringResource(R.string.home_rest_title) else sessionTitle(plan.template), style = Hyper.type.h1, color = c.text)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HyperChip(stringResource(R.string.exercises_count, plan.slots.size), icon = R.drawable.ic_format_list_bulleted)
                HyperChip(minutesLabel(plan.template.minutes), icon = R.drawable.ic_timer, color = c.textSecondary, background = c.surfaceAlt)
                plan.template.floor?.let { HyperChip(stringResource(R.string.floor_value, it), icon = R.drawable.ic_stairs, color = c.textSecondary, background = c.surfaceAlt) }
            }
            if (plan.deload) {
                Spacer(Modifier.height(10.dp))
                InfoLine(R.drawable.ic_info, stringResource(R.string.deload_hint), c.secondary)
            }
            plan.notes.forEach {
                Spacer(Modifier.height(10.dp))
                InfoLine(R.drawable.ic_lightbulb, it.text(), c.textSecondary)
            }
            plan.blocks.forEach { block ->
                Spacer(Modifier.height(18.dp))
                if (block.title != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(block.title.text(), style = Hyper.type.title, color = c.text, modifier = Modifier.weight(1f))
                        if (block.type != BlockType.STRAIGHT) {
                            HyperChip(
                                stringResource(if (block.type == BlockType.SUPERSET) R.string.block_superset else R.string.block_circuit, block.rounds),
                                color = c.secondary, background = c.secondarySoft,
                            )
                        }
                    }
                    block.note?.let { Text(it.text(), style = Hyper.type.small, color = c.textMuted) }
                    Spacer(Modifier.height(8.dp))
                } else if (block.note != null) {
                    Text(block.note.text(), style = Hyper.type.small, color = c.textMuted)
                    Spacer(Modifier.height(8.dp))
                }
                block.slots.forEachIndexed { i, slot ->
                    ExerciseRow(slot, number = i + 1, restText = if (block.type == BlockType.STRAIGHT) restLabel(slot.restMin, slot.restMax) else null)
                    if (i < block.slots.lastIndex) Spacer(Modifier.height(10.dp))
                }
                if (block.type != BlockType.STRAIGHT) {
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.rest_after_round, restLabel(block.restMin, block.restMax)), style = Hyper.type.small, color = c.secondary)
                }
            }
            Spacer(Modifier.height(18.dp))
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SecondaryButton(stringResource(R.string.back), onBack, Modifier.weight(0.8f))
            PrimaryButton(
                stringResource(
                    when {
                        activeThis || activeOther -> R.string.resume_workout
                        rest -> R.string.start_mobility
                        else -> R.string.start_training
                    },
                ),
                onStart, Modifier.weight(1.4f), icon = R.drawable.ic_play_arrow_fill,
            )
        }
    }
}

@Composable
private fun InfoLine(icon: Int, text: String, tint: Color) {
    val c = Hyper.colors
    Row(
        Modifier.fillMaxWidth().clip(HyperShapes.cardSmall).background(c.surfaceAlt).padding(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(painterResource(icon), null, tint = tint, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, style = Hyper.type.small, color = c.textSecondary)
    }
}

/** One exercise in a list: small picture, name, sets × reps, rest, note, alternative. */
@Composable
fun ExerciseRow(slot: PlannedSlot, number: Int, restText: String?, checked: Boolean? = null) {
    val c = Hyper.colors
    val p = rememberProgramStrings()
    Row(
        Modifier.fillMaxWidth().clip(HyperShapes.cardSmall).background(c.surface).border(1.dp, c.border, HyperShapes.cardSmall).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(54.dp).clip(HyperShapes.tile).background(c.accentSoft)) {
            Pictogram(slot.exercise.art, Modifier.size(54.dp).padding(3.dp), halo = c.accentSoft)
            if (checked == null) {
                Box(
                    Modifier.align(Alignment.TopStart).padding(3.dp).size(17.dp).clip(CircleShape).background(c.surface),
                    contentAlignment = Alignment.Center,
                ) { Text("$number", style = Hyper.type.micro, color = c.accentDeep) }
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(exerciseName(slot.exercise), style = Hyper.type.bodyMedium, color = c.text, maxLines = 2)
            Text(
                listOfNotNull(p.target(slot), restText, slot.tempo.takeIf { it }?.let { stringResource(R.string.tempo_short) }).joinToString(" · "),
                style = Hyper.type.small, color = c.accentDeep,
            )
            slot.note?.let { Text(it.text(), style = Hyper.type.micro, color = c.textMuted) }
            if (slot.alternatives.isNotEmpty() && !slot.swapped) {
                val lang = com.hyperathlete.app.core.locale.LocalAppLanguage.current
                Text(stringResource(R.string.alternative_value, slot.alternatives.joinToString(" / ") { it.name.term(lang) }), style = Hyper.type.micro, color = c.textMuted)
            }
        }
        if (checked != null) {
            Spacer(Modifier.width(8.dp))
            Icon(
                painterResource(if (checked) R.drawable.ic_check_circle_fill else R.drawable.ic_radio_button_unchecked), null,
                tint = if (checked) c.success else c.borderStrong, modifier = Modifier.size(24.dp),
            )
        }
    }
}

/** Long-press on Today: read-only checklist filled in by the workout screen. */
@Composable
fun ChecklistDetails(plan: PlannedSession, logged: Set<String>, modifier: Modifier = Modifier) {
    val c = Hyper.colors
    Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 20.dp)) {
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.checklist_title), style = Hyper.type.h2, color = c.text)
        Text(stringResource(R.string.checklist_hint), style = Hyper.type.small, color = c.textMuted)
        Spacer(Modifier.height(14.dp))
        plan.slots.forEachIndexed { i, slot ->
            ExerciseRow(slot, i + 1, null, checked = slot.key in logged)
            Spacer(Modifier.height(8.dp))
        }
    }
}

/** Morning knee question: 0–10 and "sharp pain or swelling". */
@Composable
fun KneeMorningDetails(onSave: (Int, Boolean) -> Unit, modifier: Modifier = Modifier) {
    val c = Hyper.colors
    var pain by remember { mutableFloatStateOf(1f) }
    var sharp by remember { mutableStateOf(false) }
    Column(modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 8.dp, bottom = 16.dp)) {
        Text(stringResource(R.string.knee_morning_title), style = Hyper.type.h2, color = c.text)
        Text(stringResource(R.string.knee_scale_hint), style = Hyper.type.small, color = c.textMuted)
        Spacer(Modifier.height(18.dp))
        KneeScale(pain.toInt()) { pain = it.toFloat() }
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.knee_sharp), style = Hyper.type.bodyMedium, color = c.text, modifier = Modifier.weight(1f))
            HyperSwitch(sharp, { sharp = it })
        }
        Spacer(Modifier.height(18.dp))
        PrimaryButton(stringResource(R.string.save), { onSave(pain.toInt(), sharp) }, Modifier.fillMaxWidth())
    }
}

/** 0–10 pain selector with colour from calm to alarming. */
@Composable
fun KneeScale(value: Int, onChange: (Int) -> Unit) {
    val c = Hyper.colors
    Column {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(Bidi.ltr("$value/10"), style = Hyper.type.number, color = kneeColor(value))
            Spacer(Modifier.width(10.dp))
            Text(
                stringResource(
                    when {
                        value == 0 -> R.string.knee_none
                        value <= 3 -> R.string.knee_ok
                        value <= 6 -> R.string.knee_moderate
                        else -> R.string.knee_bad
                    },
                ),
                style = Hyper.type.bodyMedium, color = c.textSecondary, modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        Spacer(Modifier.height(8.dp))
        HyperSlider(value.toFloat(), { onChange(it.toInt()) }, 0f..10f)
    }
}

@Composable
fun kneeColor(v: Int): Color {
    val c = Hyper.colors
    return when {
        v <= 3 -> c.success
        v <= 6 -> c.warn
        else -> c.danger
    }
}

/** A tiny "Week 3 of 9 · Foundation" helper for sheets that only have a position. */
@Composable
fun phaseLine(position: ProgramPosition): String = stringResource(R.string.week_phase, position.week, ProgramClock.WEEKS, phaseName(position.phase))
