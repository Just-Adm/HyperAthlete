package com.hyperathlete.app.ui.progress

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hyperathlete.app.R
import com.hyperathlete.app.core.locale.Bidi
import com.hyperathlete.app.core.locale.LocalAppLanguage
import com.hyperathlete.app.core.locale.bidiJoin
import com.hyperathlete.app.domain.layout.BoxId
import com.hyperathlete.app.domain.layout.PageLayout
import com.hyperathlete.app.domain.nutrition.Adherence
import com.hyperathlete.app.domain.progress.KeyLift
import com.hyperathlete.app.domain.progress.Metric
import com.hyperathlete.app.domain.progress.MetricUnit
import com.hyperathlete.app.domain.progress.Muscle
import com.hyperathlete.app.domain.progress.NutritionAverages
import com.hyperathlete.app.domain.progress.Point
import com.hyperathlete.app.domain.progress.TimeRange
import com.hyperathlete.app.domain.progress.WeightPace
import com.hyperathlete.app.domain.streak.DayMark
import com.hyperathlete.app.ui.common.weekPhaseLabel
import com.hyperathlete.app.ui.components.HyperCard
import com.hyperathlete.app.ui.components.HyperChip
import com.hyperathlete.app.ui.components.IconTile
import com.hyperathlete.app.ui.components.PrimaryButton
import com.hyperathlete.app.ui.components.Segmented
import com.hyperathlete.app.ui.components.SmallPillButton
import com.hyperathlete.app.ui.components.TextAction
import com.hyperathlete.app.ui.components.hyperClickable
import com.hyperathlete.app.ui.main.EditableBoxList
import com.hyperathlete.app.ui.nutrition.MacroColors
import com.hyperathlete.app.ui.nutrition.SelectChip
import com.hyperathlete.app.ui.nutrition.kcalLabel
import com.hyperathlete.app.ui.nutrition.qtyText
import com.hyperathlete.app.ui.theme.Hyper
import com.hyperathlete.app.ui.theme.glassBackdrop
import com.hyperathlete.app.ui.theme.glass
import com.hyperathlete.app.ui.theme.HyperShapes
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.foundation.layout.heightIn
import kotlin.math.abs
import kotlin.math.roundToInt

/** Actions the page forwards (no-ops in screenshot tests). */
class ProgressDeps(
    val layout: PageLayout,
    val editMode: Boolean = false,
    val onLayoutChange: (PageLayout) -> Unit = {},
    val onRange: (TimeRange) -> Unit = {},
    val onAddWeight: () -> Unit = {},
    val onAddMeasurement: () -> Unit = {},
    val onTests: () -> Unit = {},
    val onApplySuggestion: () -> Unit = {},
    val onDismissSuggestion: () -> Unit = {},
    val onSettings: () -> Unit = {},
    /** Muscle pre-selected on the body map (screenshot tests). */
    val bodySelection: Muscle? = null,
)

@Composable
fun ProgressScreen(ui: ProgressUi, deps: ProgressDeps) {
    val c = Hyper.colors
    Column(Modifier.fillMaxSize().glassBackdrop(Hyper.glass)) {
        if (!deps.editMode) {
            Column(Modifier.statusBarsPadding().padding(start = 20.dp, end = 12.dp, top = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.nav_progress), style = Hyper.type.h1, color = c.text, modifier = Modifier.weight(1f))
                    Box(
                        Modifier.size(42.dp).clip(CircleShape).background(c.surfaceAlt).hyperClickable(onClick = deps.onSettings),
                        contentAlignment = Alignment.Center,
                    ) { Icon(painterResource(R.drawable.ic_tune), stringResource(R.string.progress_settings), tint = c.textSecondary, modifier = Modifier.size(20.dp)) }
                }
                Spacer(Modifier.height(10.dp))
                Box(Modifier.padding(end = 8.dp)) {
                    Segmented(
                        listOf(stringResource(R.string.range_4w), stringResource(R.string.range_8w), stringResource(R.string.range_6m), stringResource(R.string.range_all)),
                        ui.range.ordinal, { deps.onRange(TimeRange.entries[it]) },
                    )
                }
            }
        }
        EditableBoxList(
            layout = deps.layout,
            editMode = deps.editMode,
            onLayoutChange = deps.onLayoutChange,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 32.dp),
        ) { id, _ ->
            when (id) {
                BoxId.PROGRESS_PHASE -> PhaseBox(ui)
                BoxId.PROGRESS_OVERVIEW -> OverviewBox(ui)
                BoxId.PROGRESS_WEIGHT -> WeightBox(ui, deps)
                BoxId.PROGRESS_NUTRITION -> NutritionBox(ui)
                BoxId.PROGRESS_STRENGTH -> StrengthBox(ui)
                BoxId.PROGRESS_BALANCE -> BalanceBox(ui, deps.bodySelection)
                BoxId.PROGRESS_KNEE -> KneeBox(ui)
                BoxId.PROGRESS_TESTS -> TestsBox(ui, deps)
                BoxId.PROGRESS_CONSISTENCY -> ConsistencyBox(ui)
                BoxId.PROGRESS_MEASUREMENTS -> MeasurementsBox(ui, deps)
                BoxId.PROGRESS_RECAP -> RecapBox(ui)
                else -> Unit
            }
        }
    }
}

/* ------------------------------------------------------------------ helpers */

@Composable
private fun BoxTitle(title: String, icon: Int, subtitle: String? = null, trailing: @Composable () -> Unit = {}) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconTile(icon, size = 38.dp, iconSize = 20.dp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = Hyper.type.title, color = Hyper.colors.text)
            if (subtitle != null) Text(subtitle, style = Hyper.type.small, color = Hyper.colors.textMuted)
        }
        trailing()
    }
}

/** Number that counts up from 0 the first time it appears. */
@Composable
fun CountUp(value: Double, format: (Double) -> String, style: androidx.compose.ui.text.TextStyle, color: Color, modifier: Modifier = Modifier) {
    val k = rememberReveal(value, 900)
    Text(Bidi.ltr(format(value * k)), style = style, color = color, modifier = modifier, maxLines = 1)
}

fun kg(v: Double): String = String.format(Locale.US, "%.1f", v).removeSuffix(".0")
fun signed(v: Double, digits: Int = 1): String = (if (v > 0) "+" else if (v < 0) "−" else "") + String.format(Locale.US, "%.${digits}f", abs(v)).let { if (digits > 0) it.removeSuffix(".0") else it }

/** Chart points (kept in kg) in the user's unit. */
@Composable
@androidx.compose.runtime.ReadOnlyComposable
private fun inUnit(points: List<Point>): List<Point> {
    val u = com.hyperathlete.app.ui.theme.LocalWeightUnit.current
    return if (u == com.hyperathlete.app.domain.model.WeightUnit.KG) points
    else points.map { it.copy(value = com.hyperathlete.app.domain.model.Weights.show(it.value, u)) }
}

/** m:ss for seconds-based metrics. */
fun clock(seconds: Double): String { val s = seconds.roundToInt(); return "${s / 60}:${(s % 60).toString().padStart(2, '0')}" }

@Composable
fun metricValue(m: Metric, v: Double): String = Bidi.isolate(
    when (m.unit) {
        MetricUnit.KG -> Bidi.ltr(com.hyperathlete.app.domain.model.Weights.text(v, com.hyperathlete.app.ui.theme.LocalWeightUnit.current))
        MetricUnit.CM -> stringResource(R.string.unit_cm_value, kg(v))
        MetricUnit.SECONDS -> if (v >= 60 || m == Metric.PLANK || m == Metric.RUN_5K || m == Metric.RUN_10K) Bidi.ltr(clock(v)) else stringResource(R.string.unit_sec_value, v.roundToInt())
    },
)

/** A test value, with "can't yet" for a zero deep-squat hold. */
@Composable
fun testValue(m: Metric, v: Double): String = if (m == Metric.DEEP_SQUAT && v < 0.5) stringResource(R.string.deep_squat_none) else metricValue(m, v)

@Composable
fun metricName(m: Metric): String = stringResource(
    when (m) {
        Metric.WEIGHT -> R.string.metric_weight
        Metric.VERTICAL_JUMP -> R.string.metric_jump
        Metric.KNEE_TO_WALL_L -> R.string.metric_ktw_l
        Metric.KNEE_TO_WALL_R -> R.string.metric_ktw_r
        Metric.DEEP_SQUAT -> R.string.metric_deep_squat
        Metric.BALANCE_L -> R.string.metric_balance_l
        Metric.BALANCE_R -> R.string.metric_balance_r
        Metric.PLANK -> R.string.metric_plank
        Metric.RUN_5K -> R.string.metric_5k
        Metric.RUN_10K -> R.string.metric_10k
        Metric.THIGH -> R.string.metric_thigh
        Metric.ARM -> R.string.metric_arm
    },
)

/** Short lift labels for chips and the recap (sports terms, so English in the mixed mode). */
@Composable
private fun liftName(@Suppress("UNUSED_PARAMETER") ui: ProgressUi, lift: KeyLift, @Suppress("UNUSED_PARAMETER") exerciseId: String?): String =
    com.hyperathlete.app.core.locale.termResource(
        when (lift) {
            KeyLift.BENCH -> R.string.lift_bench
            KeyLift.SQUAT -> R.string.lift_squat
            KeyLift.RDL -> R.string.lift_rdl
            KeyLift.LAT_PULLDOWN -> R.string.lift_pulldown
            KeyLift.SHOULDER_PRESS -> R.string.lift_shoulder
            KeyLift.DIPS -> R.string.lift_dips
            KeyLift.CURL -> R.string.lift_curl
        },
    )

@Composable
private fun exerciseName(ui: ProgressUi, id: String): String {
    val lang = LocalAppLanguage.current
    return ui.program.exercises.firstOrNull { it.id == id }?.name?.term(lang) ?: id
}

@Composable
private fun Bar(fraction: Float, color: Color, modifier: Modifier = Modifier, height: androidx.compose.ui.unit.Dp = 8.dp, track: Color = Hyper.colors.surfaceAlt) {
    val k = rememberReveal(fraction, 800)
    Box(modifier.fillMaxWidth().height(height).clip(HyperShapes.pill).background(track)) {
        Box(Modifier.fillMaxWidth((fraction.coerceIn(0f, 1f) * k)).height(height).clip(HyperShapes.pill).background(color))
    }
}

/* ------------------------------------------------------------------ 1. phase */

@Composable
private fun PhaseBox(ui: ProgressUi) {
    val c = Hyper.colors
    val p = ui.position
    HyperCard(Modifier.fillMaxWidth()) {
        BoxTitle(stringResource(R.string.box_progress_phase), R.drawable.ic_flag)
        Spacer(Modifier.height(12.dp))
        Text(if (p.started) weekPhaseLabel(p) else stringResource(R.string.program_starts_in, p.daysUntilStart), style = Hyper.type.h2, color = c.text)
        Spacer(Modifier.height(10.dp))
        Bar(if (p.started) p.week / 9f else 0f, c.accent, height = 10.dp)
        Spacer(Modifier.height(8.dp))
        Text(
            when {
                !p.started -> stringResource(R.string.phase_not_started)
                p.week == 9 -> stringResource(R.string.phase_deload_now)
                else -> androidx.compose.ui.res.pluralStringResource(R.plurals.phase_weeks_left, p.weeksUntilDeload, p.weeksUntilDeload)
            },
            style = Hyper.type.small, color = c.textSecondary,
        )
        if (ui.testWeek) {
            Spacer(Modifier.height(8.dp))
            HyperChip(stringResource(if (ui.testsDone) R.string.tests_done_chip else R.string.tests_due_chip), icon = R.drawable.ic_flag)
        }
    }
}

/* ------------------------------------------------------------------ 2. overview */

@Composable
private fun OverviewBox(ui: ProgressUi) {
    val c = Hyper.colors
    val o = ui.overview
    HyperCard(Modifier.fillMaxWidth()) {
        BoxTitle(stringResource(R.string.box_progress_overview), R.drawable.ic_monitoring)
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(stringResource(R.string.stat_sessions), Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MiniRing(o.sessionsDone / o.sessionsPlanned.toFloat().coerceAtLeast(1f), c.accent)
                    Spacer(Modifier.width(8.dp))
                    Text(Bidi.ltr("${o.sessionsDone} / ${o.sessionsPlanned}"), style = Hyper.type.h2, color = c.text)
                }
            }
            StatTile(stringResource(R.string.stat_streak), Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(R.drawable.ic_local_fire_department_fill), null, tint = c.accent, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(6.dp))
                    CountUp(ui.streak.current.toDouble(), { it.roundToInt().toString() }, Hyper.type.h2, c.text)
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(stringResource(R.string.stat_volume), Modifier.weight(1f)) {
                val u = com.hyperathlete.app.ui.theme.LocalWeightUnit.current
                CountUp(o.volumeKg, { v -> com.hyperathlete.app.domain.model.Weights.text(v, u, 0) }, Hyper.type.h2, c.text)
            }
            StatTile(stringResource(R.string.stat_rir), Modifier.weight(1f)) {
                Text(o.avgRir?.let { Bidi.ltr(String.format(Locale.US, "%.1f", it)) } ?: "—", style = Hyper.type.h2, color = c.text)
            }
        }
    }
}

@Composable
private fun StatTile(label: String, modifier: Modifier, content: @Composable () -> Unit) {
    val c = Hyper.colors
    Column(modifier.clip(HyperShapes.cardSmall).background(c.surfaceAlt).padding(12.dp)) {
        Text(label, style = Hyper.type.micro, color = c.textSecondary, maxLines = 1)
        Spacer(Modifier.height(6.dp))
        content()
    }
}

@Composable
private fun MiniRing(f: Float, color: Color) {
    val c = Hyper.colors
    val k = rememberReveal(f, 800)
    Canvas(Modifier.size(26.dp)) {
        val s = 4.dp.toPx()
        drawArc(c.border, 0f, 360f, false, Offset(s / 2, s / 2), Size(size.width - s, size.height - s), style = Stroke(s))
        drawArc(color, -90f, 360f * f.coerceIn(0f, 1f) * k, false, Offset(s / 2, s / 2), Size(size.width - s, size.height - s), style = Stroke(s, cap = StrokeCap.Round))
    }
}

/* ------------------------------------------------------------------ 3. weight */

@Composable
private fun WeightBox(ui: ProgressUi, deps: ProgressDeps) {
    val c = Hyper.colors
    val w = ui.weight
    HyperCard(Modifier.fillMaxWidth()) {
        BoxTitle(stringResource(R.string.box_progress_weight), R.drawable.ic_monitor_weight) {
            SmallPillButton(stringResource(R.string.add_weight), deps.onAddWeight)
        }
        Spacer(Modifier.height(12.dp))
        if (w.current == null) {
            NoData(stringResource(R.string.weight_empty))
            return@HyperCard
        }
        Row(verticalAlignment = Alignment.Bottom) {
            val u = com.hyperathlete.app.ui.theme.LocalWeightUnit.current
            CountUp(w.current, { com.hyperathlete.app.domain.model.Weights.num(com.hyperathlete.app.domain.model.Weights.show(it, u)) }, Hyper.type.number, c.text)
            Spacer(Modifier.width(4.dp))
            Text(com.hyperathlete.app.domain.model.Weights.label(u), style = Hyper.type.bodyMedium, color = c.textSecondary, modifier = Modifier.padding(bottom = 6.dp))
            Spacer(Modifier.weight(1f))
            w.change?.let { ch ->
                HyperChip(
                    stringResource(R.string.weight_since_start, com.hyperathlete.app.ui.common.wtSigned(ch)),
                    color = if (ch >= 0) c.success else c.warn, background = if (ch >= 0) c.successSoft else c.warnSoft,
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        LineChart(
            listOf(ChartLine(inUnit(w.points), c.accent.copy(alpha = 0.45f), 1.5.dp), ChartLine(inUnit(w.average), c.accent, 2.8.dp, dots = false)),
            from = ui.from, to = ui.today,
        )
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp, 3.dp).background(c.accent))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.weight_avg7), style = Hyper.type.micro, color = c.textMuted)
        }
        Spacer(Modifier.height(12.dp))
        // Goal bar: 0 … 1.5 kg / month, target zone 0.5 – 1
        val rate = w.monthlyRate
        Text(stringResource(if (com.hyperathlete.app.ui.theme.LocalWeightUnit.current == com.hyperathlete.app.domain.model.WeightUnit.LB) R.string.weight_goal_lb else R.string.weight_goal), style = Hyper.type.smallMedium, color = c.textSecondary)
        Spacer(Modifier.height(6.dp))
        GoalBar(rate)
        Spacer(Modifier.height(6.dp))
        Text(
            when (w.pace) {
                WeightPace.GAINING_RIGHT -> stringResource(R.string.pace_right, com.hyperathlete.app.ui.common.wtSigned(rate ?: 0.0))
                WeightPace.TOO_SLOW -> stringResource(R.string.pace_slow, com.hyperathlete.app.ui.common.wtSigned(rate ?: 0.0))
                WeightPace.TOO_FAST -> stringResource(R.string.pace_fast, com.hyperathlete.app.ui.common.wtSigned(rate ?: 0.0))
                WeightPace.LOSING -> stringResource(R.string.pace_losing, com.hyperathlete.app.ui.common.wtSigned(rate ?: 0.0))
                WeightPace.UNKNOWN -> stringResource(R.string.pace_unknown)
            },
            style = Hyper.type.small, color = when (w.pace) {
                WeightPace.GAINING_RIGHT -> c.success
                WeightPace.UNKNOWN -> c.textMuted
                else -> c.warn
            },
        )
        if (ui.showSuggestion) {
            Spacer(Modifier.height(12.dp))
            Column(Modifier.fillMaxWidth().clip(HyperShapes.cardSmall).background(c.accentSoft).padding(14.dp)) {
                Text(stringResource(R.string.suggest_more_title), style = Hyper.type.bodyMedium, color = c.text)
                Text(stringResource(R.string.suggest_more_body), style = Hyper.type.small, color = c.textSecondary)
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SmallPillButton(stringResource(R.string.suggest_apply), deps.onApplySuggestion)
                    Spacer(Modifier.width(8.dp))
                    TextAction(stringResource(R.string.suggest_later), deps.onDismissSuggestion)
                }
            }
        }
    }
}

@Composable
private fun GoalBar(rate: Double?) {
    val c = Hyper.colors
    val maxRate = 1.5
    val k = rememberReveal(rate, 800)
    val rtl = androidx.compose.ui.platform.LocalLayoutDirection.current == androidx.compose.ui.unit.LayoutDirection.Rtl
    Canvas(Modifier.fillMaxWidth().height(16.dp)) {
        fun x(v: Double): Float { val f = (v / maxRate).coerceIn(0.0, 1.0).toFloat() * size.width; return if (rtl) size.width - f else f }
        val h = 8.dp.toPx(); val y = (size.height - h) / 2
        drawRoundRect(c.surfaceAlt, Offset(0f, y), Size(size.width, h), androidx.compose.ui.geometry.CornerRadius(h / 2))
        val a = x(0.5); val b = x(1.0)
        drawRoundRect(c.successSoft, Offset(minOf(a, b), y), Size(abs(b - a), h), androidx.compose.ui.geometry.CornerRadius(h / 2))
        if (rate != null) {
            val px = x(rate.coerceAtLeast(0.0) * k)
            drawCircle(c.surface, 8.dp.toPx(), Offset(px, size.height / 2))
            drawCircle(if (rate in 0.5..1.0) c.success else c.warn, 6.dp.toPx(), Offset(px, size.height / 2))
        }
    }
    Row(Modifier.fillMaxWidth()) {
        Text(Bidi.ltr("0"), style = Hyper.type.micro, color = c.textMuted)
        Spacer(Modifier.weight(1f))
        Text(Bidi.ltr(if (com.hyperathlete.app.ui.theme.LocalWeightUnit.current == com.hyperathlete.app.domain.model.WeightUnit.LB) "1.1 – 2.2 lb" else "0.5 – 1 kg"), style = Hyper.type.micro, color = c.success)
        Spacer(Modifier.weight(1f))
        Text(Bidi.ltr(if (com.hyperathlete.app.ui.theme.LocalWeightUnit.current == com.hyperathlete.app.domain.model.WeightUnit.LB) "3.3" else "1.5"), style = Hyper.type.micro, color = c.textMuted)
    }
}

/* ------------------------------------------------------------------ 4. nutrition */

@Composable
private fun NutritionBox(ui: ProgressUi) {
    val c = Hyper.colors
    var month by rememberSaveable { mutableStateOf(false) }
    val n: NutritionAverages = if (month) ui.nutrition30 else ui.nutrition7
    HyperCard(Modifier.fillMaxWidth()) {
        BoxTitle(stringResource(R.string.box_progress_nutrition), R.drawable.ic_restaurant)
        Spacer(Modifier.height(10.dp))
        Segmented(listOf(stringResource(R.string.last_7_days), stringResource(R.string.last_30_days)), if (month) 1 else 0, { month = it == 1 })
        Spacer(Modifier.height(14.dp))
        if (n.loggedDays == 0) {
            NoData(stringResource(R.string.nutrition_empty_range))
            return@HyperCard
        }
        AvgRow(stringResource(R.string.ring_kcal), kcalLabel(n.avgKcal.roundToInt()), kcalLabel(n.targetKcal.roundToInt()), (n.avgKcal / n.targetKcal).toFloat(), c.accent)
        Spacer(Modifier.height(10.dp))
        AvgRow(
            stringResource(R.string.macro_protein),
            Bidi.isolate(stringResource(R.string.unit_g_value, n.avgProtein.roundToInt().toString())),
            Bidi.isolate(stringResource(R.string.unit_g_value, n.targetProtein.roundToInt().toString())),
            (n.avgProtein / n.targetProtein).toFloat(), MacroColors.Protein,
        )
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.adherence_pct), style = Hyper.type.smallMedium, color = c.textSecondary, modifier = Modifier.weight(1f))
            CountUp(n.adherence * 100, { "${it.roundToInt()}%" }, Hyper.type.title, c.success)
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(if (month) 3.dp else 8.dp)) {
            n.marks.forEach { (_, a) ->
                Box(
                    Modifier.weight(1f).height(if (month) 10.dp else 14.dp).clip(CircleShape).background(
                        when (a) {
                            Adherence.GOOD -> c.success
                            Adherence.CLOSE -> c.warn
                            Adherence.MISSED -> c.danger.copy(alpha = 0.6f)
                            Adherence.NONE -> c.surfaceAlt
                        },
                    ),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(stringResource(R.string.logged_days, n.loggedDays, n.days), style = Hyper.type.micro, color = c.textMuted)
    }
}

@Composable
private fun AvgRow(label: String, value: String, target: String, f: Float, color: Color) {
    val c = Hyper.colors
    Row(verticalAlignment = Alignment.Bottom) {
        Text(label, style = Hyper.type.smallMedium, color = c.textSecondary, modifier = Modifier.weight(1f))
        Text(value, style = Hyper.type.bodyMedium, color = c.text)
        Text(" / ", style = Hyper.type.small, color = c.textMuted)
        Text(target, style = Hyper.type.small, color = c.textMuted)
    }
    Spacer(Modifier.height(5.dp))
    Bar(f, color)
}

/* ------------------------------------------------------------------ 5. strength */

@Composable
private fun StrengthBox(ui: ProgressUi) {
    val c = Hyper.colors
    var pick by rememberSaveable { mutableStateOf(KeyLift.BENCH.ordinal) }
    val lift = ui.lifts[pick]
    HyperCard(Modifier.fillMaxWidth()) {
        BoxTitle(stringResource(R.string.box_progress_strength), R.drawable.ic_fitness_center, stringResource(R.string.strength_hint))
        Spacer(Modifier.height(10.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ui.lifts.forEachIndexed { i, l -> SelectChip(liftName(ui, l.lift, l.exerciseId), i == pick) { pick = i } }
        }
        Spacer(Modifier.height(12.dp))
        if (lift.series.isEmpty()) {
            NoData(stringResource(R.string.strength_empty))
        } else {
            Row(verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.e1rm), style = Hyper.type.micro, color = c.textMuted)
                        val u = com.hyperathlete.app.ui.theme.LocalWeightUnit.current
                    CountUp(lift.latest ?: 0.0, { com.hyperathlete.app.domain.model.Weights.text(it, u) }, Hyper.type.h1, c.text)
                }
                if (lift.first != null && lift.latest != null && lift.series.size > 1) {
                    val d = lift.latest - lift.first
                    HyperChip(com.hyperathlete.app.ui.common.wtSigned(d), color = if (d >= 0) c.success else c.warn, background = if (d >= 0) c.successSoft else c.warnSoft)
                }
            }
            Spacer(Modifier.height(8.dp))
            LineChart(listOf(ChartLine(inUnit(lift.series), MacroColors.Protein)), from = ui.from, to = ui.today, height = 130.dp)
            lift.best?.let { b ->
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.best_set, Bidi.ltr(com.hyperathlete.app.domain.model.Weights.text(b.weightKg, com.hyperathlete.app.ui.theme.LocalWeightUnit.current) + " × ${b.reps}"), com.hyperathlete.app.ui.common.wt(b.e1rm)),
                    style = Hyper.type.small, color = c.textSecondary,
                )
            }
        }
        if (ui.records.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            Text(stringResource(R.string.records_title), style = Hyper.type.smallMedium, color = c.textSecondary)
            val fmt = DateTimeFormatter.ofPattern("d MMM", LocalConfiguration.current.locales[0])
            ui.records.forEach { r ->
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(R.drawable.ic_trophy_fill), null, tint = c.accent, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(exerciseName(ui, r.exerciseId), style = Hyper.type.smallMedium, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(r.date.format(fmt), style = Hyper.type.micro, color = c.textMuted)
                    }
                    Text(Bidi.ltr(com.hyperathlete.app.domain.model.Weights.text(r.weightKg, com.hyperathlete.app.ui.theme.LocalWeightUnit.current) + " × ${r.reps}"), style = Hyper.type.smallMedium, color = c.text)
                }
            }
        }
    }
}

/* ------------------------------------------------------------------ 6. balance */

@Composable
private fun BalanceBox(ui: ProgressUi, initialMuscle: Muscle? = null) {
    val c = Hyper.colors
    val b = ui.balance
    HyperCard(Modifier.fillMaxWidth()) {
        BoxTitle(stringResource(R.string.box_progress_balance), R.drawable.ic_sports_gymnastics, stringResource(R.string.balance_hint))
        Spacer(Modifier.height(12.dp))
        var selected by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(initialMuscle) }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                BodyMap(b.weeklySets, front = true, modifier = Modifier.size(132.dp, 276.dp), selected = selected, onSelect = { selected = it })
                Text(stringResource(R.string.body_front), style = Hyper.type.micro, color = c.textMuted)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                BodyMap(b.weeklySets, front = false, modifier = Modifier.size(132.dp, 276.dp), selected = selected, onSelect = { selected = it })
                Text(stringResource(R.string.body_back), style = Hyper.type.micro, color = c.textMuted)
            }
        }
        Spacer(Modifier.height(10.dp))
        MuscleLabel(selected, b.weeklySets)
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            listOf(0.0 to "0", 3.0 to "<6", 8.0 to "6-10", 15.0 to "10-20", 25.0 to "20+").forEach { (v, l) ->
                Box(Modifier.size(12.dp).clip(CircleShape).background(setsColor(v)))
                Text(Bidi.ltr(l), style = Hyper.type.micro, color = c.textMuted)
            }
            Spacer(Modifier.weight(1f))
            Text(stringResource(R.string.sets_per_week), style = Hyper.type.micro, color = c.textMuted)
        }
        Spacer(Modifier.height(14.dp))
        // Weakest muscles first: what the program is fixing
        val lowest = b.weeklySets.filterKeys { it.major }.entries.sortedBy { it.value }.take(3)
        if (ui.hasTraining) {
            val names = lowest.map { muscleName(it.key) }
            Text(
                stringResource(R.string.balance_lowest, bidiJoin(names, "، ")),
                style = Hyper.type.small, color = c.textSecondary,
            )
            Spacer(Modifier.height(10.dp))
        }
        val last = b.ratio.lastOrNull()
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.upper_lower), style = Hyper.type.smallMedium, color = c.textSecondary)
                Text(
                    when (b.closing) {
                        true -> stringResource(R.string.gap_closing)
                        false -> stringResource(R.string.gap_not_closing)
                        null -> stringResource(R.string.gap_unknown)
                    },
                    style = Hyper.type.small, color = when (b.closing) { true -> c.success; false -> c.warn; null -> c.textMuted },
                )
            }
            Text(last?.let { Bidi.ltr(String.format(Locale.US, "%.1f : 1", it.value)) } ?: "—", style = Hyper.type.h2, color = c.text)
        }
        if (b.ratio.size > 1) {
            Spacer(Modifier.height(8.dp))
            LineChart(listOf(ChartLine(b.ratio, c.secondary)), from = ui.from, to = ui.today, height = 110.dp, band = 0.0..1.2, minY = 0.0)
        }
    }
}

/** The tapped muscle's name and weekly sets (a hint until one is tapped). Fixed height so the card doesn't jump. */
@Composable
private fun MuscleLabel(selected: Muscle?, weekly: Map<Muscle, Double>) {
    val c = Hyper.colors
    Row(
        Modifier.fillMaxWidth().heightIn(min = 52.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
            .background(if (selected != null) c.accentSoft else c.surfaceAlt).padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selected == null) {
            Text(stringResource(R.string.body_tap_hint), style = Hyper.type.small, color = c.textSecondary)
            return@Row
        }
        Box(Modifier.size(14.dp).clip(CircleShape).background(setsColor(weekly[selected] ?: 0.0)))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(muscleName(selected), style = Hyper.type.title, color = c.text)
            selected.parent?.let {
                Text(stringResource(R.string.muscle_part_of, muscleName(it)), style = Hyper.type.micro, color = c.textSecondary)
            }
        }
        Text(
            stringResource(R.string.muscle_sets_value, Bidi.ltr(String.format(Locale.US, "%.1f", weekly[selected] ?: 0.0).removeSuffix(".0"))),
            style = Hyper.type.smallMedium, color = c.text,
        )
    }
}

@Composable
fun muscleName(m: Muscle): String {
    return stringResource(
        when (m) {
            Muscle.CHEST -> R.string.m_chest
            Muscle.FRONT_DELTS -> R.string.m_front_delts
            Muscle.SIDE_DELTS -> R.string.m_side_delts
            Muscle.REAR_DELTS -> R.string.m_rear_delts
            Muscle.BICEPS -> R.string.m_biceps
            Muscle.TRICEPS -> R.string.m_triceps
            Muscle.FOREARMS -> R.string.m_forearms
            Muscle.TRAPS -> R.string.m_traps
            Muscle.LATS -> R.string.m_lats
            Muscle.MID_BACK -> R.string.m_mid_back
            Muscle.LOWER_BACK -> R.string.m_lower_back
            Muscle.ABS -> R.string.m_abs
            Muscle.OBLIQUES -> R.string.m_obliques
            Muscle.GLUTES -> R.string.m_glutes
            Muscle.QUADS -> R.string.m_quads
            Muscle.HAMSTRINGS -> R.string.m_hamstrings
            Muscle.ADDUCTORS -> R.string.m_adductors
            Muscle.CALVES -> R.string.m_calves
            Muscle.TIBIALIS -> R.string.m_tibialis
            Muscle.FOREARM_FLEXORS -> R.string.m_forearm_flexors
            Muscle.FOREARM_EXTENSORS -> R.string.m_forearm_extensors
            Muscle.GASTROCNEMIUS -> R.string.m_gastrocnemius
            Muscle.SOLEUS -> R.string.m_soleus
            Muscle.SERRATUS -> R.string.m_serratus
            Muscle.ROTATOR_CUFF -> R.string.m_rotator_cuff
            Muscle.HIP_FLEXORS -> R.string.m_hip_flexors
            Muscle.NECK -> R.string.m_neck
        },
    )
}

/* ------------------------------------------------------------------ 7. knee & mobility */

@Composable
private fun KneeBox(ui: ProgressUi) {
    val c = Hyper.colors
    HyperCard(Modifier.fillMaxWidth()) {
        BoxTitle(stringResource(R.string.box_progress_knee), R.drawable.ic_healing)
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.knee_pain_curve), style = Hyper.type.smallMedium, color = c.textSecondary)
        Spacer(Modifier.height(6.dp))
        if (ui.knee.isEmpty()) NoData(stringResource(R.string.knee_empty), 60.dp)
        else LineChart(listOf(ChartLine(ui.knee, c.danger)), from = ui.from, to = ui.today, height = 120.dp, minY = 0.0, maxY = 10.0, band = 0.0..3.0)
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MobilityTile(stringResource(R.string.metric_ktw_l), ui.kneeToWallL.lastOrNull()?.value, Metric.KNEE_TO_WALL_GOAL, Modifier.weight(1f))
            MobilityTile(stringResource(R.string.metric_ktw_r), ui.kneeToWallR.lastOrNull()?.value, Metric.KNEE_TO_WALL_GOAL, Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        Column(Modifier.fillMaxWidth().clip(HyperShapes.cardSmall).background(c.surfaceAlt).padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.metric_deep_squat), style = Hyper.type.smallMedium, color = c.textSecondary, modifier = Modifier.weight(1f))
                Text(
                    ui.deepSquat.lastOrNull()?.let { Bidi.isolate(stringResource(R.string.unit_sec_value, it.value.roundToInt())) } ?: stringResource(R.string.deep_squat_none),
                    style = Hyper.type.title, color = c.text,
                )
            }
            if (ui.deepSquat.size > 1) {
                Spacer(Modifier.height(6.dp))
                LineChart(listOf(ChartLine(ui.deepSquat, c.success)), height = 90.dp, minY = 0.0)
            }
        }
    }
}

@Composable
private fun MobilityTile(label: String, value: Double?, goal: Double, modifier: Modifier) {
    val c = Hyper.colors
    Column(modifier.clip(HyperShapes.cardSmall).background(c.surfaceAlt).padding(12.dp)) {
        Text(label, style = Hyper.type.micro, color = c.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(4.dp))
        Text(value?.let { Bidi.isolate(stringResource(R.string.unit_cm_value, kg(it))) } ?: "—", style = Hyper.type.h2, color = if (value != null && value >= goal) c.success else c.text)
        Spacer(Modifier.height(6.dp))
        Bar(((value ?: 0.0) / goal).toFloat(), if (value != null && value >= goal) c.success else c.accent, height = 6.dp, track = c.border)
        Spacer(Modifier.height(4.dp))
        Text(stringResource(R.string.goal_value, Bidi.isolate(stringResource(R.string.unit_cm_value, kg(goal)))), style = Hyper.type.micro, color = c.textMuted)
    }
}

/* ------------------------------------------------------------------ 8. tests */

@Composable
private fun TestsBox(ui: ProgressUi, deps: ProgressDeps) {
    val c = Hyper.colors
    HyperCard(Modifier.fillMaxWidth()) {
        BoxTitle(stringResource(R.string.box_progress_tests), R.drawable.ic_flag, stringResource(R.string.tests_hint))
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
            Text(stringResource(R.string.test_col), style = Hyper.type.micro, color = c.textMuted, modifier = Modifier.weight(1.6f))
            Text(stringResource(R.string.test_start), style = Hyper.type.micro, color = c.textMuted, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            Text(stringResource(R.string.test_latest), style = Hyper.type.micro, color = c.textMuted, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            Spacer(Modifier.width(22.dp))
        }
        ui.tests.forEachIndexed { i, r ->
            if (i > 0) Box(Modifier.fillMaxWidth().height(0.8.dp).background(c.border))
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(metricName(r.metric), style = Hyper.type.small, color = c.text, modifier = Modifier.weight(1.6f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    r.first?.let { testValue(r.metric, it) } ?: "—",
                    style = Hyper.type.small, color = if (r.firstIsBaseline) c.textMuted else c.textSecondary,
                    modifier = Modifier.weight(1f), textAlign = TextAlign.Center, maxLines = 1,
                )
                Text(r.latest?.let { testValue(r.metric, it) } ?: "—", style = Hyper.type.smallMedium, color = c.text, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, maxLines = 1)
                Box(Modifier.size(22.dp), contentAlignment = Alignment.Center) {
                    when (r.trend) {
                        1 -> Text("▲", style = Hyper.type.micro, color = c.success)
                        -1 -> Text("▼", style = Hyper.type.micro, color = c.danger)
                        else -> Unit
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        if (ui.testWeek && !ui.testsDone) PrimaryButton(stringResource(R.string.tests_record), deps.onTests, icon = R.drawable.ic_edit_note)
        else TextAction(stringResource(R.string.tests_record), deps.onTests, Modifier.align(Alignment.CenterHorizontally), color = c.accent)
    }
}

/* ------------------------------------------------------------------ 9. consistency */

@Composable
private fun ConsistencyBox(ui: ProgressUi) {
    val c = Hyper.colors
    HyperCard(Modifier.fillMaxWidth()) {
        BoxTitle(stringResource(R.string.box_progress_consistency), R.drawable.ic_calendar_month) {
            HyperChip(stringResource(R.string.streak_best, ui.streak.best), icon = R.drawable.ic_trophy_fill)
        }
        Spacer(Modifier.height(14.dp))
        androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr) {
            Heatmap(ui.heatmap, Modifier.fillMaxWidth())
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Legend(c.accent, stringResource(R.string.heat_done))
            Legend(c.successSoft, stringResource(R.string.heat_rest))
            Legend(c.dangerSoft, stringResource(R.string.heat_missed))
        }
        val done = ui.heatmap.count { it.second == DayMark.DONE }
        val due = ui.heatmap.count { it.second == DayMark.DONE || it.second == DayMark.MISSED }
        if (due > 0) {
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.consistency_pct, (done * 100.0 / due).roundToInt()), style = Hyper.type.small, color = c.textSecondary)
        }
    }
}

@Composable
private fun Legend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(12.dp).clip(HyperShapes.pill).background(color))
        Spacer(Modifier.width(5.dp))
        Text(label, style = Hyper.type.micro, color = Hyper.colors.textSecondary)
    }
}

/* ------------------------------------------------------------------ 10. measurements */

@Composable
private fun MeasurementsBox(ui: ProgressUi, deps: ProgressDeps) {
    val c = Hyper.colors
    HyperCard(Modifier.fillMaxWidth()) {
        BoxTitle(stringResource(R.string.box_progress_measurements), R.drawable.ic_height) {
            SmallPillButton(stringResource(R.string.add_measurement), deps.onAddMeasurement)
        }
        Spacer(Modifier.height(12.dp))
        if (ui.thigh.isEmpty() && ui.arm.isEmpty()) {
            NoData(stringResource(R.string.measurements_empty), 60.dp)
            return@HyperCard
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MeasureTile(stringResource(R.string.metric_thigh), ui.thigh, MacroColors.Carbs, Modifier.weight(1f))
            MeasureTile(stringResource(R.string.metric_arm), ui.arm, MacroColors.Protein, Modifier.weight(1f))
        }
        if (ui.thigh.size + ui.arm.size > 2) {
            Spacer(Modifier.height(10.dp))
            LineChart(listOf(ChartLine(ui.thigh, MacroColors.Carbs), ChartLine(ui.arm, MacroColors.Protein)), height = 120.dp)
        }
    }
}

@Composable
private fun MeasureTile(label: String, series: List<Point>, color: Color, modifier: Modifier) {
    val c = Hyper.colors
    Column(modifier.clip(HyperShapes.cardSmall).background(c.surfaceAlt).padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(color))
            Spacer(Modifier.width(6.dp))
            Text(label, style = Hyper.type.micro, color = c.textSecondary)
        }
        Spacer(Modifier.height(4.dp))
        Text(series.lastOrNull()?.let { Bidi.isolate(stringResource(R.string.unit_cm_value, kg(it.value))) } ?: "—", style = Hyper.type.h2, color = c.text)
        if (series.size > 1) {
            val d = series.last().value - series.first().value
            Text(Bidi.ltr(signed(d) + " cm"), style = Hyper.type.micro, color = if (d >= 0) c.success else c.warn)
        }
    }
}

/* ------------------------------------------------------------------ 11. weekly recap */

@Composable
private fun RecapBox(ui: ProgressUi) {
    val c = Hyper.colors
    val r = ui.recap
    val parts = buildList {
        r.liftGain?.let { (lift, kgUp) ->
            add(stringResource(R.string.recap_lift, liftName(ui, lift, ui.lifts.first { it.lift == lift }.exerciseId), com.hyperathlete.app.ui.common.wt(kgUp)))
        }
        r.proteinPct?.let { add(stringResource(R.string.recap_protein, it)) }
        add(stringResource(R.string.recap_sessions, r.sessions, r.planned))
        r.weightChange?.let { add(stringResource(R.string.recap_weight, com.hyperathlete.app.ui.common.wtSigned(it))) }
    }
    HyperCard(Modifier.fillMaxWidth(), color = c.accentSoft, borderColor = c.accentSoft) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconTile(R.drawable.ic_monitoring, background = c.surface, size = 38.dp, iconSize = 20.dp)
            Spacer(Modifier.width(10.dp))
            Text(stringResource(R.string.box_progress_recap), style = Hyper.type.title, color = c.text, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        Text(bidiJoin(parts, "، "), style = Hyper.type.bodyMedium, color = c.text)
    }
}
