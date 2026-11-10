package com.hyperathlete.app.ui.workout

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hyperathlete.app.R
import com.hyperathlete.app.core.locale.Bidi
import com.hyperathlete.app.core.locale.bidiJoin
import com.hyperathlete.app.core.workout.SessionSummary
import com.hyperathlete.app.data.program.ProgramRepository
import com.hyperathlete.app.domain.program.WeekPlan
import com.hyperathlete.app.domain.streak.StreakResult
import com.hyperathlete.app.ui.AppState
import com.hyperathlete.app.ui.art.Pictogram
import com.hyperathlete.app.ui.common.dayArt
import com.hyperathlete.app.ui.common.dayName
import com.hyperathlete.app.ui.common.exerciseName
import com.hyperathlete.app.ui.common.sessionTitle
import com.hyperathlete.app.ui.components.ConfirmDialog
import com.hyperathlete.app.ui.components.LogoMark
import com.hyperathlete.app.ui.components.PrimaryButton
import com.hyperathlete.app.ui.components.SecondaryButton
import com.hyperathlete.app.ui.components.TextAction
import com.hyperathlete.app.ui.theme.Hyper
import com.hyperathlete.app.ui.theme.glassBackdrop
import com.hyperathlete.app.ui.theme.glass
import com.hyperathlete.app.ui.theme.HyperShapes
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

/** Navigation entry: the running session, its summary when finished, or back to Home. */
@Composable
fun WorkoutRoute(state: AppState, startToday: Boolean, onClose: () -> Unit) {
    val vm: WorkoutViewModel = hiltViewModel()
    LaunchedEffect(Unit) { vm.ensure(startToday) }
    val active by vm.active.collectAsStateWithLifecycle()
    val summary by vm.summary.collectAsStateWithLifecycle()
    val closed by vm.closed.collectAsStateWithLifecycle()
    val physio by vm.physio.collectAsStateWithLifecycle()
    val streak by vm.streak.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(closed) { if (closed) onClose() }
    LaunchedEffect(message) {
        message?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show(); vm.consumeMessage() }
    }
    KeepScreenOn(state.settings.keepScreenOnDuringWorkout && active != null && summary == null)
    val s = summary
    val a = active
    when {
        s != null -> SummaryScreen(state, s, streak, vm, onDone = onClose)
        a != null -> WorkoutScreen(state, a, vm)
        else -> Box(Modifier.fillMaxSize().glassBackdrop(Hyper.glass))
    }
    if (physio) {
        ConfirmDialog(
            stringResource(R.string.knee_physio_title), stringResource(R.string.knee_physio), stringResource(R.string.ok),
            onDismiss = vm::dismissPhysio, onConfirm = vm::dismissPhysio,
        )
    }
}

@Composable
fun SummaryScreen(state: AppState, s: SessionSummary, streak: StreakResult?, actions: WorkoutActions, onDone: () -> Unit) {
    val c = Hyper.colors
    val layer = rememberGraphicsLayer()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val program = ProgramRepository.load(context)
    val locale = LocalConfiguration.current.locales[0]
    val template = WeekPlan.of(s.day)
    Column(Modifier.fillMaxSize().glassBackdrop(Hyper.glass)) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(16.dp))
            // the card that becomes the shareable image
            Column(
                Modifier
                    .fillMaxWidth()
                    .drawWithContent {
                        layer.record { this@drawWithContent.drawContent() }
                        drawLayer(layer)
                    }
                    .clip(HyperShapes.card)
                    .background(Brush.verticalGradient(listOf(c.accent, c.accentDeep)))
                    .padding(20.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LogoMark(28.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Hyper Athlete", style = Hyper.type.smallMedium, color = c.onAccent)
                    Spacer(Modifier.weight(1f))
                    Text(
                        bidiJoin(listOf(dayName(s.day), s.date.format(DateTimeFormatter.ofPattern("d MMMM", locale)))),
                        style = Hyper.type.micro, color = c.onAccent.copy(alpha = 0.85f),
                    )
                }
                Spacer(Modifier.height(14.dp))
                Box(Modifier.fillMaxWidth().height(130.dp).clip(HyperShapes.cardSmall).background(c.onAccent.copy(alpha = 0.14f))) {
                    Pictogram(dayArt(s.day, s.week), Modifier.fillMaxSize().padding(8.dp), figure = c.onAccent, halo = c.accent)
                }
                Spacer(Modifier.height(14.dp))
                Text(stringResource(R.string.summary_title), style = Hyper.type.h1, color = c.onAccent)
                Text(
                    if (s.mobility) stringResource(R.string.home_rest_title) else sessionTitle(template),
                    style = Hyper.type.bodyMedium, color = c.onAccent.copy(alpha = 0.9f),
                )
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Stat(stringResource(R.string.stat_duration), Bidi.ltr(clock(s.activeMs)), Modifier.weight(1f))
                    Stat(stringResource(R.string.stat_volume), com.hyperathlete.app.ui.common.wt(s.volumeKg, 0), Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Stat(stringResource(R.string.stat_sets), Bidi.ltr("${s.sets}"), Modifier.weight(1f))
                    Stat(stringResource(R.string.stat_rir), Bidi.ltr(s.avgRir?.let { String.format(java.util.Locale.US, "%.1f", it) } ?: "—"), Modifier.weight(1f))
                }
                if (streak != null) {
                    Spacer(Modifier.height(10.dp))
                    Row(
                        Modifier.fillMaxWidth().clip(HyperShapes.cardSmall).background(c.onAccent.copy(alpha = 0.14f)).padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(painterResource(R.drawable.ic_local_fire_department_fill), null, tint = c.onAccent, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.summary_streak, streak.current), style = Hyper.type.bodyMedium, color = c.onAccent, modifier = Modifier.weight(1f))
                        streak.milestone?.let { Text(stringResource(R.string.milestone_value, it), style = Hyper.type.smallMedium, color = c.onAccent) }
                    }
                }
                if (s.records.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Column(Modifier.fillMaxWidth().clip(HyperShapes.cardSmall).background(c.onAccent.copy(alpha = 0.14f)).padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(painterResource(R.drawable.ic_trophy_fill), null, tint = c.onAccent, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.summary_records, s.records.size), style = Hyper.type.bodyMedium, color = c.onAccent)
                        }
                        s.records.mapNotNull(program::exerciseOrNull).forEach {
                            Text("• " + exerciseName(it), style = Hyper.type.small, color = c.onAccent.copy(alpha = 0.92f))
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.summary_hint), style = Hyper.type.small, color = c.textMuted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
        }
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryButton(stringResource(R.string.summary_save), {
                    scope.launch { actions.saveImage(layer.toImageBitmap().asAndroidBitmap()) }
                }, Modifier.weight(1f), icon = R.drawable.ic_download)
                PrimaryButton(stringResource(R.string.summary_share), {
                    scope.launch { actions.share(layer.toImageBitmap().asAndroidBitmap()) }
                }, Modifier.weight(1f), icon = R.drawable.ic_share)
            }
            TextAction(stringResource(R.string.done), onDone, Modifier.align(Alignment.CenterHorizontally))
        }
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier = Modifier) {
    val c = Hyper.colors
    Column(modifier.clip(HyperShapes.cardSmall).background(c.onAccent.copy(alpha = 0.14f)).padding(12.dp)) {
        Text(label, style = Hyper.type.micro, color = c.onAccent.copy(alpha = 0.85f))
        Text(value, style = Hyper.type.h2, color = c.onAccent, maxLines = 1)
    }
}
