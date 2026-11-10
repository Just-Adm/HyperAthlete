package com.hyperathlete.app.ui.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.hyperathlete.app.R
import com.hyperathlete.app.domain.progress.Metric
import com.hyperathlete.app.domain.progress.MetricEntry
import com.hyperathlete.app.domain.progress.MetricUnit
import com.hyperathlete.app.ui.components.HyperTextField
import com.hyperathlete.app.ui.components.NumberStepper
import com.hyperathlete.app.ui.components.PrimaryButton
import com.hyperathlete.app.ui.components.hyperClickable
import com.hyperathlete.app.ui.theme.Hyper
import com.hyperathlete.app.ui.theme.glassBackdrop
import com.hyperathlete.app.ui.theme.glass
import com.hyperathlete.app.ui.theme.HyperShapes
import kotlin.math.roundToInt

/** Quick weigh-in (brief §12.3: "زر إضافة وزن سريع"). */
@Composable
fun AddWeightSheet(last: Double, onSave: (Double) -> Unit) {
    val c = Hyper.colors
    var kg by remember { mutableStateOf(last) }
    Column(Modifier.padding(horizontal = 20.dp).padding(top = 6.dp, bottom = 20.dp)) {
        Text(stringResource(R.string.add_weight_title), style = Hyper.type.h2, color = c.text)
        Text(stringResource(R.string.add_weight_hint), style = Hyper.type.small, color = c.textMuted)
        Spacer(Modifier.height(16.dp))
        com.hyperathlete.app.ui.components.WeightStepper(kg, { kg = it }, stepKg = 0.1, minKg = 30.0, maxKg = 250.0, decimals = 1)
        Spacer(Modifier.height(18.dp))
        PrimaryButton(stringResource(R.string.save), { onSave(kg) }, icon = R.drawable.ic_check)
    }
}

/** Thigh and arm with the tape (brief §12.10). */
@Composable
fun AddMeasurementSheet(thigh: Double?, arm: Double?, onSave: (Double?, Double?) -> Unit) {
    val c = Hyper.colors
    var t by remember { mutableStateOf(thigh ?: 55.0) }
    var a by remember { mutableStateOf(arm ?: 32.0) }
    Column(Modifier.padding(horizontal = 20.dp).padding(top = 6.dp, bottom = 20.dp)) {
        Text(stringResource(R.string.add_measurement_title), style = Hyper.type.h2, color = c.text)
        Text(stringResource(R.string.add_measurement_hint), style = Hyper.type.small, color = c.textMuted)
        Spacer(Modifier.height(14.dp))
        Text(stringResource(R.string.metric_thigh), style = Hyper.type.smallMedium, color = c.textSecondary)
        Spacer(Modifier.height(6.dp))
        NumberStepper(t, { t = it }, stringResource(R.string.unit_cm), step = 0.5, min = 20.0, max = 120.0, decimals = 1)
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.metric_arm), style = Hyper.type.smallMedium, color = c.textSecondary)
        Spacer(Modifier.height(6.dp))
        NumberStepper(a, { a = it }, stringResource(R.string.unit_cm), step = 0.5, min = 15.0, max = 80.0, decimals = 1)
        Spacer(Modifier.height(18.dp))
        PrimaryButton(stringResource(R.string.save), { onSave(t, a) }, icon = R.drawable.ic_check)
    }
}

/** How each test is done (brief §17). */
@Composable
fun testHow(m: Metric): String = stringResource(
    when (m) {
        Metric.VERTICAL_JUMP -> R.string.how_jump
        Metric.KNEE_TO_WALL_L, Metric.KNEE_TO_WALL_R -> R.string.how_ktw
        Metric.DEEP_SQUAT -> R.string.how_deep_squat
        Metric.BALANCE_L, Metric.BALANCE_R -> R.string.how_balance
        Metric.PLANK -> R.string.how_plank
        Metric.RUN_5K, Metric.RUN_10K -> R.string.how_run
        Metric.WEIGHT -> R.string.how_weight
        Metric.THIGH, Metric.ARM -> R.string.how_tape
    },
)

/** Parses what was typed: "1:05" or "65" for times, decimals for the rest. */
fun parseMetric(m: Metric, text: String): Double? {
    val t = text.trim().replace(',', '.')
    if (t.isEmpty()) return null
    if (m.unit == MetricUnit.SECONDS && ':' in t) {
        val parts = t.split(':')
        val mins = parts.getOrNull(0)?.toIntOrNull() ?: return null
        val secs = parts.getOrNull(1)?.toIntOrNull() ?: return null
        if (secs !in 0..59) return null
        return (mins * 60 + secs).toDouble()
    }
    return t.toDoubleOrNull()?.takeIf { it >= 0 }
}

private fun timeLike(m: Metric) = m == Metric.PLANK || m == Metric.RUN_5K || m == Metric.RUN_10K

fun prefill(m: Metric, v: Double?): String = when {
    v == null -> ""
    timeLike(m) -> clock(v)
    m.unit == MetricUnit.SECONDS -> v.roundToInt().toString()
    else -> kg(v)
}

/** Test-week form (brief §17): every test with how it's measured and last time's result. */
@Composable
fun TestEntryScreen(weekLabel: String, last: Map<Metric, MetricEntry>, onSave: (Map<Metric, Double>) -> Unit, onBack: () -> Unit, initiallyOpen: Metric? = null) {
    val c = Hyper.colors
    val texts = remember { mutableStateMapOf<Metric, String>() }
    val context = androidx.compose.ui.platform.LocalContext.current
    var expanded by rememberSaveable { mutableStateOf<String?>(initiallyOpen?.name) }
    Column(Modifier.fillMaxSize().glassBackdrop(Hyper.glass).statusBarsPadding().imePadding()) {
        Row(Modifier.padding(start = 8.dp, end = 12.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(CircleShape).hyperClickable(onClick = onBack), contentAlignment = Alignment.Center) {
                Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.back), tint = c.text, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.tests_form_title), style = Hyper.type.h2, color = c.text)
                Text(weekLabel, style = Hyper.type.small, color = c.textMuted)
            }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 10.dp)) {
            Text(stringResource(R.string.tests_form_hint), style = Hyper.type.small, color = c.textSecondary)
            Spacer(Modifier.height(12.dp))
            Metric.Tests.forEach { m ->
                val open = expanded == m.name
                Column(Modifier.fillMaxWidth().padding(bottom = 10.dp).clip(HyperShapes.cardSmall).background(c.surfaceAlt).padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(metricName(m), style = Hyper.type.bodyMedium, color = c.text)
                            val prev = last[m]
                            Text(
                                if (prev != null) stringResource(R.string.last_time_value, testValue(m, prev.value))
                                else m.baseline?.let { stringResource(R.string.start_value, if (m == Metric.DEEP_SQUAT && it == 0.0) stringResource(R.string.deep_squat_none) else metricValue(m, it)) } ?: "",
                                style = Hyper.type.micro, color = c.textMuted,
                            )
                        }
                        Text(
                            stringResource(R.string.how_to), style = Hyper.type.micro, color = c.accent,
                            modifier = Modifier.clip(HyperShapes.pill).hyperClickable { expanded = if (open) null else m.name }.padding(horizontal = 8.dp, vertical = 6.dp),
                        )
                        Box(Modifier.width(110.dp)) {
                            HyperTextField(
                                texts[m] ?: "", { texts[m] = it.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' || ch == ':' }.take(7) },
                                placeholder = unitHint(m), keyboardType = if (timeLike(m)) KeyboardType.Text else KeyboardType.Decimal,
                                textStyle = Hyper.type.bodyMedium,
                            )
                        }
                    }
                    if (open) {
                        Spacer(Modifier.height(8.dp))
                        Text(testHow(m), style = Hyper.type.small, color = c.textSecondary)
                        Spacer(Modifier.height(8.dp))
                        Row(
                            Modifier.clip(HyperShapes.pill).background(c.surface).hyperClickable { com.hyperathlete.app.core.workout.ExerciseVideo.search(context, Metric.videoQuery(m)) }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(painterResource(R.drawable.ic_smart_display), null, tint = c.accent, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.test_watch), style = Hyper.type.smallMedium, color = c.accent)
                        }
                    }
                }
            }
        }
        val values = Metric.Tests.mapNotNull { m -> parseMetric(m, texts[m] ?: "")?.let { m to it } }.toMap()
        Box(Modifier.navigationBarsPadding().padding(20.dp)) {
            PrimaryButton(stringResource(R.string.tests_save, values.size), { onSave(values) }, enabled = values.isNotEmpty(), icon = R.drawable.ic_check)
        }
    }
}

@Composable
private fun unitHint(m: Metric): String = when {
    timeLike(m) -> "m:ss"
    m.unit == MetricUnit.SECONDS -> stringResource(R.string.unit_sec)
    m.unit == MetricUnit.KG -> stringResource(R.string.unit_kg)
    else -> stringResource(R.string.unit_cm)
}
