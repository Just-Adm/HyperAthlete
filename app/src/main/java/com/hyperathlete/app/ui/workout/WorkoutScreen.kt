package com.hyperathlete.app.ui.workout

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hyperathlete.app.R
import com.hyperathlete.app.core.locale.Bidi
import com.hyperathlete.app.core.locale.bidiJoin
import com.hyperathlete.app.core.locale.termResource
import com.hyperathlete.app.core.workout.ExerciseVideo
import com.hyperathlete.app.core.workout.ActiveSession
import com.hyperathlete.app.core.workout.ProgramStrings
import com.hyperathlete.app.domain.program.BlockType
import com.hyperathlete.app.domain.program.Gear
import com.hyperathlete.app.domain.program.Load
import com.hyperathlete.app.domain.program.Measure
import com.hyperathlete.app.domain.program.PlannedSlot
import com.hyperathlete.app.domain.session.SessionEngine
import com.hyperathlete.app.domain.session.Stage
import com.hyperathlete.app.domain.training.KneeAdvice
import com.hyperathlete.app.domain.training.Progression
import com.hyperathlete.app.domain.training.RirTarget
import com.hyperathlete.app.domain.training.SetValues
import com.hyperathlete.app.domain.training.Suggestion
import com.hyperathlete.app.ui.AppState
import com.hyperathlete.app.ui.art.Pictogram
import com.hyperathlete.app.ui.common.exerciseName
import com.hyperathlete.app.ui.common.minutesLabel
import com.hyperathlete.app.ui.common.rememberProgramStrings
import com.hyperathlete.app.ui.common.restLabel
import com.hyperathlete.app.ui.common.sessionTitle
import com.hyperathlete.app.ui.common.text
import com.hyperathlete.app.ui.common.weekPhaseLabel
import com.hyperathlete.app.ui.components.ConfirmDialog
import com.hyperathlete.app.ui.components.HyperCard
import com.hyperathlete.app.ui.components.HyperChip
import com.hyperathlete.app.ui.components.HyperSwitch
import com.hyperathlete.app.ui.components.HyperTextField
import com.hyperathlete.app.ui.components.PrimaryButton
import com.hyperathlete.app.ui.components.SecondaryButton
import com.hyperathlete.app.ui.components.TextAction
import com.hyperathlete.app.ui.components.hyperClickable
import com.hyperathlete.app.ui.components.rememberHaptics
import com.hyperathlete.app.ui.main.ExerciseRow
import com.hyperathlete.app.ui.main.HyperSheet
import com.hyperathlete.app.ui.main.KneeScale
import com.hyperathlete.app.ui.theme.Hyper
import com.hyperathlete.app.ui.theme.glassBackdrop
import com.hyperathlete.app.ui.theme.glass
import com.hyperathlete.app.ui.theme.HyperShapes
import kotlinx.coroutines.delay
import kotlin.math.ceil

/** Current time for countdowns: ticks while visible, frozen in screenshot tests. */
@Composable
fun rememberNow(fixed: Long? = null): Long {
    if (fixed != null || LocalInspectionMode.current) return fixed ?: 0L
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(200)
        }
    }
    return now
}

@Composable
fun KeepScreenOn(on: Boolean) {
    val view = LocalView.current
    DisposableEffect(on) {
        view.keepScreenOn = on
        onDispose { view.keepScreenOn = false }
    }
}

fun clock(ms: Long): String {
    val total = ceil(ms.coerceAtLeast(0) / 1000.0).toLong()
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) String.format(java.util.Locale.US, "%d:%02d:%02d", h, m, s) else String.format(java.util.Locale.US, "%d:%02d", m, s)
}

private enum class Panel { NONE, LIST, SWAP, NOTE, LEAVE }

@Composable
fun WorkoutScreen(state: AppState, a: ActiveSession, actions: WorkoutActions, fixedNow: Long? = null) {
    val c = Hyper.colors
    val now = rememberNow(fixedNow)
    val s = a.state
    var panel by remember { mutableStateOf(Panel.NONE) }
    val holder = remember { LoggerHolder() }
    BackHandler { panel = Panel.LEAVE }
    Column(Modifier.fillMaxSize().glassBackdrop(Hyper.glass)) {
        TopBar(a, now, onClose = { panel = Panel.LEAVE }, onList = { panel = Panel.LIST }, onPause = actions::togglePause)
        SessionProgress(a)
        Box(Modifier.weight(1f)) {
            AnimatedContent(
                targetState = when {
                    s.stage == Stage.WARMUP -> "overview"
                    a.done && s.rest == null -> "done"
                    else -> "work"
                },
                transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(120)) },
                label = "stage",
            ) { page ->
                when (page) {
                    "overview" -> Overview(state, a, now, actions)
                    "done" -> AllDone(a)
                    else -> WorkPage(a, now, actions, holder, onSwap = { panel = Panel.SWAP }, onNote = { panel = Panel.NOTE })
                }
            }
            if (s.paused) PausedOverlay(actions::togglePause)
        }
        BottomActions(a, actions, holder)
    }

    when (panel) {
        Panel.LIST -> HyperSheet({ panel = Panel.NONE }) { ExerciseList(a) { key -> actions.jumpTo(key); panel = Panel.NONE } }
        Panel.SWAP -> a.slot?.let { slot ->
            HyperSheet({ panel = Panel.NONE }) { SwapSheet(slot) { id -> actions.swap(slot.key, id); panel = Panel.NONE } }
        }
        Panel.NOTE -> a.slot?.let { slot ->
            HyperSheet({ panel = Panel.NONE }) {
                NoteSheet(a.state.notes[slot.key] ?: a.context[slot.key]?.lastNote.orEmpty()) { text -> actions.note(slot.key, text); panel = Panel.NONE }
            }
        }
        Panel.LEAVE -> HyperSheet({ panel = Panel.NONE }) {
            LeaveSheet(
                a,
                onFinish = { panel = Panel.NONE; actions.finish() },
                onDiscard = { panel = Panel.NONE; actions.discard() },
                onMinimize = { panel = Panel.NONE; actions.close() },
                onStay = { panel = Panel.NONE },
            )
        }
        Panel.NONE -> Unit
    }
}

/* ------------------------------------------------------------------ chrome */

@Composable
private fun TopBar(a: ActiveSession, now: Long, onClose: () -> Unit, onList: () -> Unit, onPause: () -> Unit) {
    val c = Hyper.colors
    val (done, total) = SessionEngine.progress(a.plan, a.state)
    val elapsed = if (a.state.stage == Stage.WORK) SessionEngine.activeMs(a.state, now) else null
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RoundIcon(R.drawable.ic_close, onClose)
        Column(Modifier.weight(1f).padding(horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                if (a.plan.isMobility) stringResource(R.string.home_rest_title) else sessionTitle(a.plan.template),
                style = Hyper.type.title, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Text(
                bidiJoin(listOfNotNull(stringResource(R.string.sets_progress, done, total), elapsed?.let { Bidi.ltr(clock(it)) })),
                style = Hyper.type.micro, color = c.textMuted,
            )
        }
        if (a.state.stage == Stage.WORK) {
            RoundIcon(if (a.state.paused) R.drawable.ic_play_arrow_fill else R.drawable.ic_pause_fill, onPause)
            Spacer(Modifier.width(6.dp))
        }
        RoundIcon(R.drawable.ic_format_list_bulleted, onList)
    }
}

@Composable
fun RoundIcon(icon: Int, onClick: () -> Unit, tint: Color = Hyper.colors.text, background: Color = Hyper.colors.surfaceAlt) {
    Box(
        Modifier.size(42.dp).clip(CircleShape).background(background).hyperClickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(painterResource(icon), null, tint = tint, modifier = Modifier.size(20.dp)) }
}

/** Thin bar split per block (A, B, C, D on Thursday). */
@Composable
private fun SessionProgress(a: ActiveSession) {
    val c = Hyper.colors
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        a.plan.blocks.forEachIndexed { bi, _ ->
            val idx = a.plan.steps.indices.filter { a.plan.steps[it].block == bi }
            val frac = if (idx.isEmpty()) 0f else idx.count { SessionEngine.isLogged(a.state, it) } / idx.size.toFloat()
            val f by animateFloatAsState(frac, tween(400), label = "seg")
            Box(Modifier.weight(idx.size.coerceAtLeast(1).toFloat()).height(5.dp).clip(HyperShapes.pill).background(c.surfaceAlt)) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(f).clip(HyperShapes.pill).background(if (bi % 2 == 0) c.accent else c.secondary))
            }
        }
    }
}

@Composable
private fun PausedOverlay(onResume: () -> Unit) {
    val c = Hyper.colors
    Box(Modifier.fillMaxSize().background(c.bg.copy(alpha = 0.92f)).hyperClickable(scaleOnPress = false, role = null) {}, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Icon(painterResource(R.drawable.ic_pause_fill), null, tint = c.accent, modifier = Modifier.size(56.dp))
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.notif_paused), style = Hyper.type.h1, color = c.text)
            Text(stringResource(R.string.paused_hint), style = Hyper.type.body, color = c.textSecondary, textAlign = TextAlign.Center)
            Spacer(Modifier.height(20.dp))
            PrimaryButton(stringResource(R.string.notif_resume), onResume, icon = R.drawable.ic_play_arrow_fill)
        }
    }
}

/* ------------------------------------------------------------------ overview & warm-up */

@Composable
private fun Overview(state: AppState, a: ActiveSession, now: Long, actions: WorkoutActions) {
    val c = Hyper.colors
    val plan = a.plan
    val warm = SessionEngine.warmupElapsed(a.state, now)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(8.dp))
        Text(weekPhaseLabel(state.settings.position(state.today)), style = Hyper.type.smallMedium, color = c.textMuted)
        Text(stringResource(R.string.overview_title), style = Hyper.type.h1, color = c.text)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HyperChip(stringResource(R.string.exercises_count, plan.slots.size), icon = R.drawable.ic_format_list_bulleted)
            HyperChip(minutesLabel(plan.template.minutes), icon = R.drawable.ic_timer, color = c.textSecondary, background = c.surfaceAlt)
            plan.template.floor?.let { HyperChip(stringResource(R.string.floor_value, it), icon = R.drawable.ic_stairs, color = c.textSecondary, background = c.surfaceAlt) }
        }
        if (a.knee == KneeAdvice.EASE_OFF && plan.slots.any { it.exercise.knee }) {
            Spacer(Modifier.height(12.dp)); Banner(R.drawable.ic_healing, stringResource(R.string.knee_ease_off), c.warn, c.warnSoft)
        }
        if (a.knee == KneeAdvice.SEE_PHYSIO) {
            Spacer(Modifier.height(12.dp)); Banner(R.drawable.ic_medical_services, stringResource(R.string.knee_physio), c.danger, c.dangerSoft)
        }
        if (plan.deload) {
            Spacer(Modifier.height(12.dp)); Banner(R.drawable.ic_info, stringResource(R.string.deload_hint), c.secondary, c.secondarySoft)
        }
        Spacer(Modifier.height(16.dp))
        HyperCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(termResource(R.string.term_warmup), style = Hyper.type.title, color = c.text)
                    Text(stringResource(R.string.warmup_hint), style = Hyper.type.small, color = c.textMuted)
                }
                Text(Bidi.ltr(clock(warm)), style = Hyper.type.number.copy(fontSize = 34.sp), color = if (a.state.warmupStartedAt != null) c.accent else c.text)
            }
            Spacer(Modifier.height(12.dp))
            SecondaryButton(
                stringResource(if (a.state.warmupStartedAt != null) R.string.warmup_stop else if (warm > 0) R.string.warmup_resume else R.string.warmup_start),
                actions::toggleWarmup, Modifier.fillMaxWidth(),
                icon = if (a.state.warmupStartedAt != null) R.drawable.ic_pause_fill else R.drawable.ic_play_arrow_fill,
            )
        }
        Spacer(Modifier.height(18.dp))
        plan.blocks.forEach { block ->
            block.title?.let {
                Text(it.text(), style = Hyper.type.title, color = c.text)
                Spacer(Modifier.height(8.dp))
            }
            block.slots.forEachIndexed { i, slot ->
                ExerciseRow(slot, i + 1, if (block.type == BlockType.STRAIGHT) restLabel(slot.restMin, slot.restMax) else null)
                Spacer(Modifier.height(8.dp))
            }
            Spacer(Modifier.height(8.dp))
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
fun Banner(icon: Int, text: String, tint: Color, background: Color) {
    Row(
        Modifier.fillMaxWidth().clip(HyperShapes.cardSmall).background(background).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(icon), null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, style = Hyper.type.smallMedium, color = Hyper.colors.text)
    }
}

/* ------------------------------------------------------------------ the set */

@Composable
private fun WorkPage(a: ActiveSession, now: Long, actions: WorkoutActions, holder: LoggerHolder, onSwap: () -> Unit, onNote: () -> Unit) {
    val c = Hyper.colors
    val step = a.step ?: return
    val slot = a.plan.slotOf(step)
    val block = a.plan.blockOf(step)
    val ctx = a.context[slot.key]
    val p = rememberProgramStrings()
    val rest = a.state.rest
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(6.dp))
        if (rest != null) {
            RestPanel(a, now, actions)
            return@Column
        }
        if (block.title != null) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                Text(block.title.text(), style = Hyper.type.smallMedium, color = c.secondary, modifier = Modifier.weight(1f))
                if (block.type != BlockType.STRAIGHT) {
                    HyperChip(termResource(R.string.term_round_of, step.set, step.of), color = c.secondary, background = c.secondarySoft)
                    a.state.roundStartedAt?.let {
                        Spacer(Modifier.width(6.dp))
                        HyperChip(Bidi.ltr(clock(now - it)), icon = R.drawable.ic_timer, color = c.secondary, background = c.secondarySoft)
                    }
                }
            }
        }
        // picture + name
        Box(Modifier.fillMaxWidth().height(176.dp).clip(HyperShapes.card).background(c.accentSoft)) {
            Pictogram(slot.exercise.art, Modifier.fillMaxSize().padding(10.dp), animate = true, halo = c.accentSoft)
            if (slot.swapped) {
                HyperChip(stringResource(R.string.swapped), Modifier.align(Alignment.TopEnd).padding(10.dp), color = c.secondary, background = c.surface)
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(exerciseName(slot.exercise), style = Hyper.type.h1, color = c.text)
        Text(slot.exercise.muscles.text(), style = Hyper.type.small, color = c.textMuted)
        Spacer(Modifier.height(10.dp))
        FlowChips(slot, step.set, step.of, block.type == BlockType.STRAIGHT, p, a.plan.template.floor)
        // notes & cues
        slot.note?.let { Spacer(Modifier.height(10.dp)); NoteLine(it.text()) }
        slot.exercise.cue?.let { Spacer(Modifier.height(6.dp)); NoteLine(it.text(), muted = true) }
        a.state.notes[slot.key]?.let { Spacer(Modifier.height(6.dp)); NoteLine("✎ $it", muted = true) }
        // suggestion & knee
        ctx?.suggestion?.let { sug ->
            Spacer(Modifier.height(12.dp))
            val text = when (sug) {
                is Suggestion.Increase -> stringResource(R.string.suggest_increase, com.hyperathlete.app.ui.common.wt(sug.toKg - sug.fromKg), sug.targetReps)
                is Suggestion.AddBelt -> stringResource(R.string.suggest_belt, com.hyperathlete.app.ui.common.wt(sug.kg))
                is Suggestion.Deload -> stringResource(R.string.suggest_deload, com.hyperathlete.app.ui.common.wt(sug.kg))
            }
            Banner(R.drawable.ic_trophy_fill, text, if (sug is Suggestion.Deload) c.secondary else c.accent, if (sug is Suggestion.Deload) c.secondarySoft else c.accentSoft)
        }
        if (slot.exercise.knee && a.knee == KneeAdvice.EASE_OFF) {
            Spacer(Modifier.height(8.dp)); Banner(R.drawable.ic_healing, stringResource(R.string.knee_ease_off), c.warn, c.warnSoft)
        }
        Spacer(Modifier.height(14.dp))
        SetHeader(a, slot, step.set, step.of, block.type, onPrevious = actions::previous)
        Spacer(Modifier.height(10.dp))
        Logger(a, slot, now, holder)
        Spacer(Modifier.height(14.dp))
        ToolRow(a, slot, onSwap = onSwap, onNote = onNote, actions = actions)
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun NoteLine(text: String, muted: Boolean = false) {
    val c = Hyper.colors
    Row(verticalAlignment = Alignment.Top) {
        Box(Modifier.padding(top = 7.dp).size(5.dp).clip(CircleShape).background(if (muted) c.borderStrong else c.accent))
        Spacer(Modifier.width(8.dp))
        Text(text, style = Hyper.type.small, color = if (muted) c.textMuted else c.textSecondary)
    }
}

@Composable
private fun FlowChips(slot: PlannedSlot, set: Int, of: Int, straight: Boolean, p: ProgramStrings, floor: Int?) {
    val c = Hyper.colors
    val rir = Progression.rirTarget(slot, set, of)
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        HyperChip(p.target(slot), icon = R.drawable.ic_fitness_center)
        if (straight) HyperChip(restLabel(slot.restMin, slot.restMax), icon = R.drawable.ic_timer, color = c.textSecondary, background = c.surfaceAlt)
        when (rir) {
            RirTarget.STOP_SHORT -> HyperChip(stringResource(R.string.rir_stop_short), color = c.secondary, background = c.secondarySoft)
            RirTarget.FAILURE -> HyperChip(stringResource(R.string.rir_failure), color = c.danger, background = c.dangerSoft)
            RirTarget.NONE -> Unit
        }
        if (slot.tempo) HyperChip(stringResource(R.string.tempo_short), icon = R.drawable.ic_hourglass_top, color = c.textSecondary, background = c.surfaceAlt)
        if (slot.exercise.perSide) HyperChip(stringResource(R.string.per_side_short), color = c.textSecondary, background = c.surfaceAlt)
        floor?.let { HyperChip(stringResource(R.string.floor_value, it), icon = R.drawable.ic_stairs, color = c.textSecondary, background = c.surfaceAlt) }
    }
}

@Composable
private fun SetHeader(a: ActiveSession, slot: PlannedSlot, set: Int, of: Int, type: BlockType, onPrevious: () -> Unit) {
    val c = Hyper.colors
    val doneSets = a.state.logs.count { it.slotKey == slot.key }
    val last = a.context[slot.key]?.last
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            val label = if (type == BlockType.STRAIGHT) termResource(R.string.term_set_of, doneSets + 1, slot.sets.coerceAtLeast(doneSets + 1))
            else termResource(R.string.term_round_of, set, of)
            Text(label, style = Hyper.type.h2, color = c.text)
            val lastText = last?.let { Progression.summarize(it.sets, com.hyperathlete.app.domain.model.Weights.label(com.hyperathlete.app.ui.theme.LocalWeightUnit.current), ", ", com.hyperathlete.app.ui.theme.LocalWeightUnit.current) }
            when {
                lastText != null -> Text(stringResource(R.string.last_time, Bidi.ltr(lastText)), style = Hyper.type.small, color = c.textMuted)
                slot.load != Load.NONE -> Text(stringResource(R.string.first_time), style = Hyper.type.small, color = c.textMuted)
            }
        }
        if (SessionEngine.previousSlotKey(a.plan, a.state) != null) {
            TextAction(stringResource(R.string.previous_exercise), onPrevious)
        }
    }
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        val total = maxOf(slot.sets, doneSets)
        repeat(total) { i ->
            Box(
                Modifier.weight(1f).height(6.dp).clip(HyperShapes.pill)
                    .background(if (i < doneSets) c.accent else if (i == doneSets) c.accentLight else c.surfaceAlt),
            )
        }
    }
}

/* ------------------------------------------------------------------ logger */

@Composable
private fun Logger(a: ActiveSession, slot: PlannedSlot, now: Long, holder: LoggerHolder) {
    val c = Hyper.colors
    val key = "${a.state.stepIndex}:${a.state.logs.size}:${slot.exercise.id}"
    val pre = remember(key) { a.prefill() }
    var weight by remember(key) { mutableDoubleStateOf(pre.weightKg ?: 0.0) }
    var reps by remember(key) { mutableIntStateOf(pre.reps ?: slot.max) }
    var seconds by remember(key) { mutableIntStateOf(pre.seconds ?: slot.min) }
    var meters by remember(key) { mutableIntStateOf(pre.meters ?: slot.max) }
    var rir by remember(key) { mutableStateOf<Int?>(null) }
    val ex = slot.exercise
    val weighted = slot.load == Load.WEIGHTED
    val belt = slot.load == Load.BODYWEIGHT && ex.gear == Gear.BELT
    val step = if (belt) Gear.BELT.stepKg else Progression.stepOf(ex.gear).takeIf { it > 0 } ?: 1.0
    val hold = a.state.hold

    // the bottom button reads the latest values through the holder
    val values = when (ex.measure) {
        Measure.REPS, Measure.LENGTH -> SetValues(weightKg = if (weighted || belt) weight else null, reps = reps, rir = rir)
        Measure.TIME -> SetValues(seconds = seconds)
        Measure.DISTANCE -> SetValues(weightKg = if (weighted) weight else null, meters = meters)
    }
    androidx.compose.runtime.SideEffect { holder.current = values }

    when (ex.measure) {
        Measure.TIME -> HoldPanel(slot, seconds, { seconds = it }, hold, now)
        else -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (weighted || belt) {
                // Kept in kg; pounds are only how it is shown and typed.
                val u = com.hyperathlete.app.ui.theme.LocalWeightUnit.current
                val lb = u == com.hyperathlete.app.domain.model.WeightUnit.LB
                ValueStepper(
                    label = stringResource(if (belt) R.string.belt_weight else R.string.weight),
                    value = Math.round(com.hyperathlete.app.domain.model.Weights.show(weight, u) * 10) / 10.0, unit = com.hyperathlete.app.domain.model.Weights.label(u),
                    step = com.hyperathlete.app.domain.model.Weights.step(step, u), min = 0.0, decimals = if (!lb && step % 1.0 == 0.0) 0 else 1,
                    onChange = { weight = com.hyperathlete.app.domain.model.Weights.toKg(it, u) }, modifier = Modifier.weight(1f),
                )
            }
            when (ex.measure) {
                Measure.DISTANCE -> ValueStepper(
                    label = stringResource(R.string.distance), value = meters.toDouble(), unit = "m", step = 5.0, min = 0.0, decimals = 0,
                    onChange = { meters = it.toInt() }, modifier = Modifier.weight(1f),
                )
                else -> ValueStepper(
                    label = stringResource(if (ex.measure == Measure.LENGTH) R.string.lengths else R.string.reps),
                    value = reps.toDouble(), unit = "", step = 1.0, min = 0.0, decimals = 0,
                    onChange = { reps = it.toInt() }, modifier = Modifier.weight(1f),
                )
            }
        }
    }
    if (Progression.asksRir(slot)) {
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.rir_question), style = Hyper.type.smallMedium, color = c.textSecondary)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(0, 1, 2, 3).forEach { v ->
                val on = rir == v
                val bg by animateColorAsState(if (on) c.accent else c.surface, tween(160), label = "rir")
                Box(
                    Modifier.weight(1f).height(46.dp).clip(HyperShapes.cardSmall).background(bg)
                        .border(1.dp, if (on) c.accent else c.border, HyperShapes.cardSmall)
                        .hyperClickable { rir = if (on) null else v },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(if (v == 3) Bidi.ltr("3+") else "$v", style = Hyper.type.title, color = if (on) c.onAccent else c.text)
                }
            }
        }
    }
}

/** Latest logger values, read by the bottom "Set done" button when tapped. */
class LoggerHolder {
    var current: SetValues = SetValues()
}

@Composable
fun ValueStepper(
    label: String, value: Double, unit: String, step: Double, min: Double, decimals: Int,
    onChange: (Double) -> Unit, modifier: Modifier = Modifier, max: Double = 999.0,
) {
    val c = Hyper.colors
    val haptics = rememberHaptics()
    fun fmt(v: Double) = if (decimals == 0 && v % 1.0 == 0.0) v.toLong().toString() else ProgramStrings.fmt(v)
    Column(
        modifier.clip(HyperShapes.card).background(c.surface).border(1.dp, c.border, HyperShapes.card).padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(if (unit.isNotEmpty()) "$label · $unit" else label, style = Hyper.type.micro, color = c.textMuted)
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp)) {
            StepCircle(R.drawable.ic_remove, value - step >= min - 1e-9, onLong = { haptics.tick(); onChange((value - step * 5).coerceAtLeast(min)) }) {
                haptics.tick(); onChange(Progression.roundTo((value - step).coerceAtLeast(min), if (step < 1) step else 0.25))
            }
            androidx.compose.foundation.text.BasicText(
                Bidi.ltr(fmt(value)),
                modifier = Modifier.weight(1f).padding(horizontal = 2.dp),
                style = Hyper.type.number.copy(color = c.text, textAlign = TextAlign.Center),
                maxLines = 1,
                autoSize = androidx.compose.foundation.text.TextAutoSize.StepBased(minFontSize = 16.sp, maxFontSize = 32.sp, stepSize = 1.sp),
            )
            StepCircle(R.drawable.ic_add, value + step <= max + 1e-9, onLong = { haptics.tick(); onChange((value + step * 5).coerceAtMost(max)) }) {
                haptics.tick(); onChange((value + step).coerceAtMost(max))
            }
        }
    }
}

@Composable
private fun StepCircle(icon: Int, enabled: Boolean, onLong: () -> Unit, onClick: () -> Unit) {
    val c = Hyper.colors
    Box(
        Modifier.size(38.dp).clip(CircleShape).background(c.surfaceAlt)
            .hyperClickable(enabled = enabled, onLongClick = onLong, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(painterResource(icon), null, tint = if (enabled) c.text else c.lock, modifier = Modifier.size(20.dp)) }
}

/** Timed holds: pick seconds, then a ring counts down (both sides for per-side holds). */
@Composable
private fun HoldPanel(slot: PlannedSlot, seconds: Int, onSeconds: (Int) -> Unit, hold: com.hyperathlete.app.domain.session.HoldTimer?, now: Long) {
    val c = Hyper.colors
    if (hold == null) {
        ValueStepper(
            stringResource(R.string.hold_seconds), seconds.toDouble(), stringResource(R.string.seconds_unit), 5.0, 5.0, 0,
            onChange = { onSeconds(it.toInt()) }, modifier = Modifier.fillMaxWidth(), max = 600.0,
        )
        return
    }
    val switching = hold.switchUntil != null && now < hold.switchUntil
    val end = if (switching) hold.switchUntil!! else hold.endsAt
    val total = if (switching) 5_000L else hold.seconds * 1000L
    val left = (end - now).coerceIn(0, total)
    Box(Modifier.fillMaxWidth().aspectRatio(1.6f), contentAlignment = Alignment.Center) {
        Ring(left / total.toFloat(), if (switching) c.secondary else c.accent, Modifier.fillMaxHeight().aspectRatio(1f))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (hold.sides > 1) Text(stringResource(R.string.notif_side, hold.side, hold.sides), style = Hyper.type.smallMedium, color = c.textMuted)
            Text(Bidi.ltr(clock(left)), style = Hyper.type.number.copy(fontSize = 48.sp), color = c.text)
            Text(stringResource(if (switching) R.string.notif_switch else R.string.notif_hold), style = Hyper.type.bodyMedium, color = if (switching) c.secondary else c.accent)
        }
    }
}

@Composable
fun Ring(fraction: Float, color: Color, modifier: Modifier = Modifier, track: Color = Hyper.colors.surfaceAlt, width: Float = 14f) {
    Canvas(modifier) {
        val w = width.dp.toPx()
        val d = size.minDimension - w
        val tl = Offset((size.width - d) / 2, (size.height - d) / 2)
        drawArc(track, 0f, 360f, false, tl, Size(d, d), style = Stroke(w))
        drawArc(color, -90f, 360f * fraction.coerceIn(0f, 1f), false, tl, Size(d, d), style = Stroke(w, cap = StrokeCap.Round))
    }
}

/* ------------------------------------------------------------------ rest */

@Composable
private fun RestPanel(a: ActiveSession, now: Long, actions: WorkoutActions) {
    val c = Hyper.colors
    val r = a.state.rest ?: return
    val left = r.remainingMs(if (a.state.paused) a.state.pausedAt!! else now)
    val ready = now >= r.readyAt && r.readyAt < r.endsAt
    val color by animateColorAsState(if (ready) c.success else c.accent, tween(300), label = "restColor")
    val next = a.slot
    val step = a.step
    Spacer(Modifier.height(6.dp))
    Box(Modifier.fillMaxWidth().aspectRatio(1.25f), contentAlignment = Alignment.Center) {
        Ring(left / r.totalMs.toFloat(), color, Modifier.fillMaxHeight().aspectRatio(1f), width = 16f)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.notif_rest), style = Hyper.type.bodyMedium, color = c.textMuted)
            Text(Bidi.ltr(clock(left)), style = Hyper.type.number.copy(fontSize = 64.sp), color = c.text)
            AnimatedVisibility(ready) {
                Text(stringResource(R.string.rest_ready), style = Hyper.type.smallMedium, color = c.success)
            }
        }
    }
    if (a.state.kneeAsk != null) {
        Spacer(Modifier.height(10.dp))
        KneeDuringCard(a.state.kneeAsk, a, actions)
    }
    if (next != null && step != null) {
        Spacer(Modifier.height(12.dp))
        val p = rememberProgramStrings()
        HyperCard(Modifier.fillMaxWidth(), padding = PaddingValues(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(64.dp).clip(HyperShapes.tile).background(c.accentSoft)) {
                    Pictogram(next.exercise.art, Modifier.fillMaxSize().padding(4.dp), halo = c.accentSoft)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.up_next), style = Hyper.type.micro, color = c.textMuted)
                    Text(exerciseName(next.exercise), style = Hyper.type.title, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    val type = a.plan.blockOf(step).type
                    val label = if (type == BlockType.STRAIGHT) termResource(R.string.term_set_of, a.state.logs.count { it.slotKey == next.key } + 1, next.sets)
                    else termResource(R.string.term_round_of, step.set, step.of)
                    Text(bidiJoin(listOf(label, p.planned(next, a.prefill()))), style = Hyper.type.small, color = c.accentDeep)
                }
            }
        }
    }
    Spacer(Modifier.height(16.dp))
}

@Composable
private fun KneeDuringCard(exerciseId: String, a: ActiveSession, actions: WorkoutActions) {
    val c = Hyper.colors
    var pain by remember(exerciseId) { mutableIntStateOf(0) }
    var sharp by remember(exerciseId) { mutableStateOf(false) }
    val name = a.plan.slots.firstOrNull { it.exercise.id == exerciseId }?.let { exerciseName(it.exercise) } ?: ""
    HyperCard(Modifier.fillMaxWidth(), color = c.warnSoft, borderColor = c.warnSoft, elevated = false) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(R.drawable.ic_healing), null, tint = c.warn, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.knee_during_title), style = Hyper.type.bodyMedium, color = c.text)
                Text(stringResource(R.string.knee_during_after, name), style = Hyper.type.micro, color = c.textMuted)
            }
        }
        Spacer(Modifier.height(10.dp))
        KneeScale(pain) { pain = it }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.knee_sharp), style = Hyper.type.small, color = c.text, modifier = Modifier.weight(1f))
            HyperSwitch(sharp, { sharp = it })
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SecondaryButton(stringResource(R.string.skip), { actions.answerKnee(null, false) }, Modifier.weight(1f), container = c.surface)
            PrimaryButton(stringResource(R.string.save), { actions.answerKnee(pain, sharp) }, Modifier.weight(1.4f))
        }
    }
}

/* ------------------------------------------------------------------ tools & bottom */

@Composable
internal fun ToolRow(a: ActiveSession, slot: PlannedSlot, onSwap: () -> Unit, onNote: () -> Unit, actions: WorkoutActions) {
    val context = LocalContext.current
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Top) {
        val w = Modifier.weight(1f)
        if (a.state.logs.isNotEmpty()) Tool(R.drawable.ic_undo, stringResource(R.string.undo_set), w, actions::undo)
        if (slot.alternatives.isNotEmpty()) Tool(R.drawable.ic_swap_horiz, stringResource(R.string.swap_exercise), w, onSwap)
        Tool(R.drawable.ic_smart_display, stringResource(R.string.exercise_howto), w) { ExerciseVideo.open(context, slot.exercise.name.en) }
        Tool(R.drawable.ic_edit_note, stringResource(R.string.add_note), w, onNote)
        Tool(R.drawable.ic_skip_next, stringResource(R.string.skip_exercise), w) { actions.skipExercise(slot.key) }
    }
}

@Composable
private fun Tool(icon: Int, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val c = Hyper.colors
    Column(
        modifier.clip(HyperShapes.cardSmall).hyperClickable(onClick = onClick).padding(horizontal = 2.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(c.surfaceAlt), contentAlignment = Alignment.Center) {
            Icon(painterResource(icon), null, tint = c.textSecondary, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(label, style = Hyper.type.micro, color = c.textSecondary, maxLines = 2, textAlign = TextAlign.Center)
    }
}

@Composable
private fun BottomActions(a: ActiveSession, actions: WorkoutActions, holder: LoggerHolder) {
    val c = Hyper.colors
    val s = a.state
    val haptics = rememberHaptics()
    Column(
        Modifier.fillMaxWidth().background(c.bg).navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        when {
            s.stage == Stage.WARMUP -> PrimaryButton(stringResource(R.string.notif_begin), actions::begin, Modifier.fillMaxWidth(), icon = R.drawable.ic_play_arrow_fill)
            a.done && s.rest == null -> PrimaryButton(stringResource(R.string.finish_session), actions::finish, Modifier.fillMaxWidth(), icon = R.drawable.ic_sports_score)
            s.rest != null -> Column {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecondaryButton(Bidi.ltr("+15s"), { actions.addRest(15) }, Modifier.weight(1f))
                    SecondaryButton(Bidi.ltr("+30s"), { actions.addRest(30) }, Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                PrimaryButton(stringResource(R.string.skip_rest), actions::skipRest, Modifier.fillMaxWidth(), icon = R.drawable.ic_skip_next_fill)
            }
            s.hold != null -> SecondaryButton(stringResource(R.string.cancel_hold), actions::cancelHold, Modifier.fillMaxWidth())
            else -> {
                val slot = a.slot
                if (slot?.exercise?.measure == Measure.TIME) {
                    PrimaryButton(stringResource(R.string.notif_start_hold), { haptics.confirm(); actions.startHold(holder.current.seconds ?: slot.min) }, Modifier.fillMaxWidth(), icon = R.drawable.ic_timer_play)
                } else {
                    PrimaryButton(stringResource(R.string.notif_done_set), { haptics.confirm(); actions.log(holder.current) }, Modifier.fillMaxWidth(), icon = R.drawable.ic_check_fill)
                }
            }
        }
    }
}

@Composable
private fun AllDone(a: ActiveSession) {
    val c = Hyper.colors
    Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(Modifier.size(96.dp).clip(CircleShape).background(c.successSoft), contentAlignment = Alignment.Center) {
            Icon(painterResource(R.drawable.ic_check), null, tint = c.success, modifier = Modifier.size(48.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.all_sets_done), style = Hyper.type.h1, color = c.text, textAlign = TextAlign.Center)
        Text(stringResource(R.string.all_sets_done_hint), style = Hyper.type.body, color = c.textSecondary, textAlign = TextAlign.Center)
        if (a.state.kneeAsk != null) Unit
    }
}

/* ------------------------------------------------------------------ sheets */

@Composable
private fun ExerciseList(a: ActiveSession, onJump: (String) -> Unit) {
    val c = Hyper.colors
    val p = rememberProgramStrings()
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 20.dp)) {
        Spacer(Modifier.height(6.dp))
        Text(stringResource(R.string.session_exercises), style = Hyper.type.h2, color = c.text)
        Spacer(Modifier.height(12.dp))
        a.plan.slots.forEach { slot ->
            val done = a.state.logs.count { it.slotKey == slot.key }
            val skipped = slot.key in a.state.skipped
            val current = a.slot?.key == slot.key
            Row(
                Modifier.fillMaxWidth().padding(bottom = 8.dp).clip(HyperShapes.cardSmall)
                    .background(if (current) c.accentSoft else c.surface)
                    .border(1.dp, if (current) c.accent else c.border, HyperShapes.cardSmall)
                    .hyperClickable { onJump(slot.key) }.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(exerciseName(slot.exercise), style = Hyper.type.bodyMedium, color = if (skipped) c.textMuted else c.text)
                    Text(p.target(slot), style = Hyper.type.micro, color = c.textMuted)
                }
                Text(
                    when {
                        skipped -> stringResource(R.string.skipped)
                        else -> Bidi.ltr("$done/${slot.sets}")
                    },
                    style = Hyper.type.smallMedium, color = if (done >= slot.sets) c.success else c.textSecondary,
                )
            }
        }
    }
}

@Composable
private fun SwapSheet(slot: PlannedSlot, onPick: (String) -> Unit) {
    val c = Hyper.colors
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 20.dp)) {
        Spacer(Modifier.height(6.dp))
        Text(stringResource(R.string.swap_title), style = Hyper.type.h2, color = c.text)
        Text(stringResource(R.string.swap_hint), style = Hyper.type.small, color = c.textMuted)
        Spacer(Modifier.height(12.dp))
        slot.alternatives.forEach { alt ->
            Row(
                Modifier.fillMaxWidth().padding(bottom = 8.dp).clip(HyperShapes.cardSmall).background(c.surface)
                    .border(1.dp, c.border, HyperShapes.cardSmall).hyperClickable { onPick(alt.id) }.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(52.dp).clip(HyperShapes.tile).background(c.accentSoft)) {
                    Pictogram(alt.art, Modifier.fillMaxSize().padding(3.dp), halo = c.accentSoft)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(exerciseName(alt), style = Hyper.type.bodyMedium, color = c.text)
                    Text(alt.muscles.text(), style = Hyper.type.micro, color = c.textMuted)
                }
                Icon(painterResource(R.drawable.ic_swap_horiz), null, tint = c.accent, modifier = Modifier.size(22.dp))
            }
        }
    }
}

@Composable
private fun NoteSheet(initial: String, onSave: (String) -> Unit) {
    val c = Hyper.colors
    var text by remember { mutableStateOf(initial) }
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 20.dp)) {
        Spacer(Modifier.height(6.dp))
        Text(stringResource(R.string.add_note), style = Hyper.type.h2, color = c.text)
        Spacer(Modifier.height(12.dp))
        HyperTextField(text, { text = it }, Modifier.fillMaxWidth(), placeholder = stringResource(R.string.note_placeholder), textStyle = Hyper.type.body)
        Spacer(Modifier.height(14.dp))
        PrimaryButton(stringResource(R.string.save), { onSave(text) }, Modifier.fillMaxWidth())
    }
}

@Composable
private fun LeaveSheet(a: ActiveSession, onFinish: () -> Unit, onDiscard: () -> Unit, onMinimize: () -> Unit, onStay: () -> Unit) {
    val c = Hyper.colors
    var confirmDiscard by remember { mutableStateOf(false) }
    val (done, total) = SessionEngine.progress(a.plan, a.state)
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 20.dp)) {
        Spacer(Modifier.height(6.dp))
        Text(stringResource(R.string.leave_title), style = Hyper.type.h2, color = c.text)
        Text(stringResource(R.string.leave_body, done, total), style = Hyper.type.small, color = c.textMuted)
        Spacer(Modifier.height(16.dp))
        PrimaryButton(stringResource(R.string.finish_session), onFinish, Modifier.fillMaxWidth(), enabled = done > 0, icon = R.drawable.ic_sports_score)
        Spacer(Modifier.height(8.dp))
        SecondaryButton(stringResource(R.string.minimize_session), onMinimize, Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        SecondaryButton(stringResource(R.string.discard_session), { confirmDiscard = true }, Modifier.fillMaxWidth(), content = c.danger)
        Spacer(Modifier.height(4.dp))
        TextAction(stringResource(R.string.keep_going), onStay, Modifier.align(Alignment.CenterHorizontally))
    }
    if (confirmDiscard) {
        ConfirmDialog(
            stringResource(R.string.discard_title), stringResource(R.string.discard_body), stringResource(R.string.discard_session),
            onDismiss = { confirmDiscard = false }, onConfirm = onDiscard, destructive = true,
        )
    }
}

