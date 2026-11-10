package com.hyperathlete.app.ui.main

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.drawscope.rotate
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hyperathlete.app.R
import com.hyperathlete.app.core.locale.bidiJoin
import com.hyperathlete.app.data.weather.WeatherState
import com.hyperathlete.app.domain.layout.BoxId
import com.hyperathlete.app.domain.layout.PageLayout
import com.hyperathlete.app.domain.program.ProgramClock
import com.hyperathlete.app.domain.program.SessionKind
import com.hyperathlete.app.domain.program.WeekPlan
import com.hyperathlete.app.domain.streak.DayMark
import com.hyperathlete.app.domain.streak.StreakCalculator
import com.hyperathlete.app.domain.streak.StreakResult
import com.hyperathlete.app.domain.weather.OutdoorRules
import com.hyperathlete.app.ui.AppState
import com.hyperathlete.app.ui.common.dayName
import com.hyperathlete.app.ui.common.dayShort
import com.hyperathlete.app.ui.common.formatTime
import com.hyperathlete.app.ui.common.minutesLabel
import com.hyperathlete.app.ui.common.sessionTitle
import com.hyperathlete.app.ui.common.weekPhaseLabel
import com.hyperathlete.app.ui.components.EmptyState
import com.hyperathlete.app.ui.components.HyperCard
import com.hyperathlete.app.ui.components.HyperChip
import com.hyperathlete.app.ui.components.IconTile
import com.hyperathlete.app.ui.components.enterAnimation
import com.hyperathlete.app.ui.theme.Hyper
import com.hyperathlete.app.ui.theme.HyperShapes
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/** Everything the Home page needs from outside; stateless so it renders in screenshot tests. */
class HomeDeps(
    val layout: PageLayout,
    val editMode: Boolean,
    val onLayoutChange: (PageLayout) -> Unit,
    val weather: WeatherState,
    val onWeatherOpen: () -> Unit,
    val onEnableLocation: () -> Unit,
    val streak: StreakResult,
    val training: HomeTraining,
    /** The streak went up since Home last showed it: play the celebration once. */
    val celebrate: Boolean = false,
    val onCelebrated: () -> Unit = {},
    /** Today's food numbers for the small nutrition box (brief §9 extra boxes). */
    val nutrition: com.hyperathlete.app.ui.nutrition.NutritionDay? = null,
    val onOpenNutrition: () -> Unit = {},
    /** Test week and nothing recorded yet (brief §17). */
    val testsDue: Boolean = false,
    val onTests: () -> Unit = {},
)

@Composable
fun HomeScreen(state: AppState, deps: HomeDeps, now: LocalTime = if (LocalInspectionMode.current) LocalTime.of(9, 0) else LocalTime.now()) {
    val c = Hyper.colors
    val name = state.profile.name.trim()
    val greeting = when (now.hour) {
        in 5..11 -> R.string.greet_morning
        in 12..16 -> R.string.greet_afternoon
        else -> R.string.greet_evening
    }
    val locale = LocalConfiguration.current.locales[0]
    val dateText = bidiJoin(listOf(dayName(state.today.dayOfWeek), state.today.format(DateTimeFormatter.ofPattern("d MMMM", locale))))
    val alert = (deps.weather as? WeatherState.Ready)?.snapshot?.let { OutdoorRules.alertFor(it, state.today) }

    EditableBoxList(
        layout = deps.layout,
        editMode = deps.editMode,
        onLayoutChange = deps.onLayoutChange,
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
        header = {
            item(key = "header") {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 18.dp).enterAnimation(0)) {
                    Column(Modifier.weight(1f)) {
                        Text(dateText, style = Hyper.type.smallMedium, color = c.textMuted)
                        Spacer(Modifier.height(2.dp))
                        Text(stringResource(greeting, name), style = Hyper.type.h1, color = c.text, maxLines = 2)
                    }
                    Spacer(Modifier.width(12.dp))
                    WeatherChip(deps.weather, deps.onWeatherOpen, deps.onEnableLocation)
                }
            }
            val t = deps.training
            if (t.active != null && !deps.editMode) {
                item(key = "resume") { ResumeCard(t.active, t.onResume, Modifier.padding(bottom = 12.dp)) }
            }
            if (t.kneeDue && !deps.editMode) {
                item(key = "knee") { KneeCheckCard(t.onKnee, Modifier.padding(bottom = 12.dp)) }
            }
            if (deps.testsDue && !deps.editMode) {
                item(key = "tests") { TestsDueCard(deps.onTests, Modifier.padding(bottom = 12.dp)) }
            }
            if (alert != null && !deps.editMode) {
                item(key = "alert") { OutdoorAlertCard(alert, Modifier.padding(bottom = 12.dp)) }
            }
        },
        footer = {},
    ) { id, _ ->
        when (id) {
            BoxId.HOME_STREAK -> StreakBox(deps.streak, deps.celebrate, deps.onCelebrated)
            BoxId.HOME_TODAY -> TodayBox(state, deps.training)
            BoxId.HOME_SCHEDULE -> ScheduleBox(state, deps.training)
            BoxId.HOME_WEEK -> WeekBox(state)
            BoxId.HOME_NUTRITION -> deps.nutrition?.let { com.hyperathlete.app.ui.nutrition.NutritionMiniBox(it, deps.onOpenNutrition) }
            else -> Unit
        }
    }
}

/* ------------------------------------------------------------------ boxes */

@Composable
private fun TestsDueCard(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = Hyper.colors
    HyperCard(modifier.fillMaxWidth(), color = c.accentSoft, borderColor = c.accentSoft, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconTile(R.drawable.ic_flag, background = c.surface)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.reminder_tests_title), style = Hyper.type.title, color = c.text)
                Text(stringResource(R.string.tests_due_body), style = Hyper.type.small, color = c.textSecondary)
            }
            Icon(painterResource(R.drawable.ic_chevron_right), null, tint = c.accent, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
fun StreakBox(streak: StreakResult, celebrate: Boolean = false, onCelebrated: () -> Unit = {}) {
    val c = Hyper.colors
    val still = LocalInspectionMode.current
    val pulse = rememberInfiniteTransition(label = "flame")
    val flame by pulse.animateFloat(1f, 1.12f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "flameScale")
    val bounce = remember { Animatable(1f) }
    val burst = remember { Animatable(0f) }
    LaunchedEffect(celebrate) {
        if (celebrate && !still) {
            launch {
                bounce.animateTo(1.35f, tween(200))
                bounce.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = 300f))
            }
            burst.snapTo(0f)
            burst.animateTo(1f, tween(1500, easing = LinearEasing))
            burst.snapTo(0f)
            onCelebrated()
        }
    }
    Box {
        HyperCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconTile(R.drawable.ic_local_fire_department_fill)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.box_streak), style = Hyper.type.title, color = c.text)
                    Text(stringResource(R.string.streak_best, streak.best), style = Hyper.type.small, color = c.textMuted)
                }
                streak.milestone?.let {
                    HyperChip(stringResource(R.string.milestone_value, it), icon = R.drawable.ic_celebration)
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    streak.current.toString(), style = Hyper.type.number,
                    color = if (streak.current > 0) c.accent else c.text,
                    modifier = Modifier.graphicsLayer { scaleX = bounce.value; scaleY = bounce.value },
                )
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.streak_days), style = Hyper.type.bodyMedium, color = c.textSecondary, modifier = Modifier.padding(bottom = 6.dp))
                Spacer(Modifier.weight(1f))
                if (streak.current > 0) {
                    Icon(
                        painterResource(R.drawable.ic_local_fire_department_fill), null, tint = c.accent,
                        modifier = Modifier.size(34.dp).graphicsLayer { val k = (if (still) 1f else flame) * bounce.value; scaleX = k; scaleY = k },
                    )
                }
            }
            if (streak.current == 0) {
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.streak_empty), style = Hyper.type.small, color = c.textMuted)
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                streak.week.forEach { d ->
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            dayShort(d.date.dayOfWeek), style = Hyper.type.micro, maxLines = 1,
                            color = if (d.mark == DayMark.DONE || d.mark == DayMark.TODAY) c.accent else c.textMuted,
                        )
                        Spacer(Modifier.height(6.dp))
                        DayCell(d.mark, d.bonus)
                    }
                }
            }
        }
        if (burst.value > 0f) Confetti(burst.value, Modifier.matchParentSize())
    }
}

/** Short confetti burst for a growing streak (deterministic, no allocations per frame). */
@Composable
private fun Confetti(t: Float, modifier: Modifier) {
    val c = Hyper.colors
    val colors = listOf(c.accent, c.secondary, c.success, c.warn, c.accentLight)
    val pieces = remember {
        val rnd = java.util.Random(7)
        List(30) { Triple(rnd.nextFloat() * 2f * Math.PI.toFloat(), 0.35f + rnd.nextFloat() * 0.65f, rnd.nextInt(5)) }
    }
    androidx.compose.foundation.Canvas(modifier) {
        val origin = androidx.compose.ui.geometry.Offset(size.width * 0.5f, size.height * 0.42f)
        val reach = size.minDimension * 0.75f
        pieces.forEachIndexed { i, (angle, speed, ci) ->
            val d = reach * speed * t
            val x = origin.x + kotlin.math.cos(angle) * d
            val y = origin.y + kotlin.math.sin(angle) * d * 0.7f + 260f * t * t
            val a = (1f - t).coerceIn(0f, 1f)
            rotate(degrees = 360f * t * (if (i % 2 == 0) 1 else -1) + i * 23f, pivot = androidx.compose.ui.geometry.Offset(x, y)) {
                drawRoundRect(
                    colors[ci].copy(alpha = a), topLeft = androidx.compose.ui.geometry.Offset(x - 6f, y - 3f),
                    size = androidx.compose.ui.geometry.Size(12f, 6f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f),
                )
            }
        }
    }
}

@Composable
private fun DayCell(mark: DayMark, bonus: Boolean) {
    val c = Hyper.colors
    val shape = RoundedCornerShape(12.dp)
    val (bg, border) = when (mark) {
        DayMark.DONE -> c.accent to c.accent
        DayMark.TODAY -> c.accentSoft to c.accent
        DayMark.MISSED -> c.dangerSoft to c.dangerSoft
        else -> c.surfaceAlt to c.surfaceAlt
    }
    Box(
        Modifier.fillMaxWidth().aspectRatio(1f)
            .clip(shape).background(bg).border(1.5.dp, border, shape),
        contentAlignment = Alignment.Center,
    ) {
        when (mark) {
            DayMark.DONE -> Icon(painterResource(R.drawable.ic_check), null, tint = c.onAccent, modifier = Modifier.size(18.dp))
            DayMark.REST -> Icon(painterResource(R.drawable.ic_bedtime_fill), null, tint = if (bonus) c.accent else c.textMuted, modifier = Modifier.size(15.dp))
            DayMark.MISSED -> Icon(painterResource(R.drawable.ic_close), null, tint = c.danger, modifier = Modifier.size(15.dp))
            else -> Unit
        }
    }
}

@Composable
private fun WeekBox(state: AppState) {
    val c = Hyper.colors
    val pos = state.settings.position(state.today)
    HyperCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconTile(R.drawable.ic_flag)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.home_week_card), style = Hyper.type.small, color = c.textMuted)
                Text(
                    if (pos.started) weekPhaseLabel(pos) else stringResource(R.string.program_starts_in, pos.daysUntilStart),
                    style = Hyper.type.title, color = c.text,
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        LineProgress(if (pos.started) pos.week / ProgramClock.WEEKS.toFloat() else 0f)
        if (pos.started) {
            Spacer(Modifier.height(8.dp))
            Text(
                if (pos.weeksUntilDeload > 0) stringResource(R.string.home_weeks_to_deload, pos.weeksUntilDeload)
                else stringResource(R.string.home_deload_now),
                style = Hyper.type.micro, color = c.textMuted,
            )
        }
    }
}

@Composable
fun LineProgress(fraction: Float, modifier: Modifier = Modifier) {
    val c = Hyper.colors
    val f by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(700), label = "line")
    Box(modifier.fillMaxWidth().height(8.dp).clip(HyperShapes.pill).background(c.surfaceAlt)) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(f).clip(HyperShapes.pill).background(c.accent))
    }
}


