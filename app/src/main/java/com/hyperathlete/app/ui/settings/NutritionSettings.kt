package com.hyperathlete.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hyperathlete.app.R
import com.hyperathlete.app.core.locale.Bidi
import com.hyperathlete.app.domain.nutrition.NutritionPrefs
import com.hyperathlete.app.domain.nutrition.TargetCalculator
import com.hyperathlete.app.ui.AppState
import com.hyperathlete.app.ui.common.formatTime
import com.hyperathlete.app.ui.components.HyperCard
import com.hyperathlete.app.ui.components.HyperDivider
import com.hyperathlete.app.ui.components.HyperSlider
import com.hyperathlete.app.ui.components.HyperSwitch
import com.hyperathlete.app.ui.components.HyperTimePickerDialog
import com.hyperathlete.app.ui.components.Segmented
import com.hyperathlete.app.ui.components.SettingsRow
import com.hyperathlete.app.ui.nutrition.MacroColors
import com.hyperathlete.app.ui.nutrition.SelectChip
import com.hyperathlete.app.ui.nutrition.kcalLabel
import com.hyperathlete.app.ui.nutrition.qtyText
import com.hyperathlete.app.ui.theme.Hyper
import kotlin.math.roundToInt

/** Settings → Nutrition (brief §16, §18): how the daily targets are worked out, sections, reminder. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NutritionSettingsScreen(state: AppState, onSave: (NutritionPrefs) -> Unit, onBack: () -> Unit) {
    val c = Hyper.colors
    // Local copy is the source of truth while the screen is open (saves are debounced).
    var n by androidx.compose.runtime.remember { mutableStateOf(state.settings.nutrition) }
    val onChange: (NutritionPrefs) -> Unit = { n = it; onSave(it) }
    val t = TargetCalculator.targets(state.profile, n, state.today)
    val base = TargetCalculator.targets(state.profile, n.copy(carbCycling = false), state.today)
    var pickTime by rememberSaveable { mutableStateOf(false) }
    SubScreen(stringResource(R.string.nutrition_settings), onBack) {
        HyperCard(Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.targets_today), style = Hyper.type.small, color = c.textMuted)
            Text(kcalLabel(t.kcal), style = Hyper.type.h1, color = c.accent)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                TargetCell(stringResource(R.string.macro_protein), t.protein, MacroColors.Protein)
                TargetCell(stringResource(R.string.macro_carbs), t.carbs, MacroColors.Carbs)
                TargetCell(stringResource(R.string.macro_fat), t.fat, MacroColors.Fat)
            }
            Spacer(Modifier.height(10.dp))
            Text(
                stringResource(R.string.targets_explain, Bidi.ltr(t.bmr.toString()), Bidi.ltr(t.maintenance.toString()), Bidi.ltr(n.surplus.toString())),
                style = Hyper.type.small, color = c.textSecondary,
            )
            if (n.carbCycling) {
                Text(stringResource(R.string.targets_carb_days, Bidi.ltr(base.kcal.toString()), Bidi.ltr((base.kcal + n.carbBoostKcal).toString())), style = Hyper.type.small, color = c.textSecondary)
            }
            Text(stringResource(R.string.targets_from_profile), style = Hyper.type.micro, color = c.textMuted, modifier = Modifier.padding(top = 6.dp))
        }
        Spacer(Modifier.height(18.dp))
        Section(R.string.nutri_sex)
        Segmented(listOf(stringResource(R.string.sex_male), stringResource(R.string.sex_female)), if (n.male) 0 else 1, { onChange(n.copy(male = it == 0)) })
        Spacer(Modifier.height(18.dp))
        Section(R.string.nutri_activity, R.string.nutri_activity_desc)
        val labels = listOf(R.string.activity_1, R.string.activity_2, R.string.activity_3, R.string.activity_4, R.string.activity_5)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            NutritionPrefs.ActivityLevels.forEachIndexed { i, a ->
                SelectChip("${stringResource(labels[i])} ${Bidi.ltr("×$a")}", n.activity == a) { onChange(n.copy(activity = a)) }
            }
        }
        Spacer(Modifier.height(18.dp))
        SliderBlock(
            stringResource(R.string.nutri_surplus), stringResource(R.string.nutri_surplus_desc),
            Bidi.ltr("+${n.surplus}"), n.surplus.toFloat(), 0f..600f,
        ) { onChange(n.copy(surplus = ((it / 50).roundToInt() * 50))) }
        SliderBlock(
            stringResource(R.string.nutri_protein), stringResource(R.string.nutri_protein_desc, Bidi.isolate(stringResource(R.string.unit_g_value, t.protein.toString()))),
            Bidi.isolate(stringResource(R.string.unit_g_per_kg, qtyText(n.proteinPerKg))), n.proteinPerKg.toFloat(), 1.6f..2.2f,
        ) { onChange(n.copy(proteinPerKg = (it * 10).roundToInt() / 10.0)) }
        SliderBlock(
            stringResource(R.string.nutri_fat), stringResource(R.string.nutri_fat_desc),
            Bidi.ltr("${n.fatPercent}%"), n.fatPercent.toFloat(), 20f..35f,
        ) { onChange(n.copy(fatPercent = it.roundToInt())) }
        HyperCard(Modifier.fillMaxWidth(), padding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)) {
            SettingsRow(
                R.drawable.ic_bolt, stringResource(R.string.nutri_carb_days), subtitle = stringResource(R.string.nutri_carb_days_desc, Bidi.ltr("+${n.carbBoostKcal}")),
                trailing = { HyperSwitch(n.carbCycling, { onChange(n.copy(carbCycling = it)) }) },
            )
            HyperDivider()
            SettingsRow(
                R.drawable.ic_fitness_center, stringResource(R.string.nutri_workout_meals), subtitle = stringResource(R.string.nutri_workout_meals_desc),
                trailing = { HyperSwitch(n.workoutMeals, { onChange(n.copy(workoutMeals = it)) }) },
            )
            HyperDivider()
            SettingsRow(
                R.drawable.ic_notifications, stringResource(R.string.nutri_reminder), subtitle = stringResource(R.string.nutri_reminder_desc),
                trailing = { HyperSwitch(n.reminder, { onChange(n.copy(reminder = it)) }) },
            )
            if (n.reminder) {
                HyperDivider()
                SettingsRow(
                    R.drawable.ic_schedule, stringResource(R.string.reminder_time),
                    trailing = { Text(formatTime(n.reminderTime), style = Hyper.type.bodyMedium, color = c.accent) },
                    onClick = { pickTime = true },
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }
    if (pickTime) {
        HyperTimePickerDialog(stringResource(R.string.nutri_reminder), n.reminderTime, { pickTime = false }) { onChange(n.copy(reminderTime = it)) }
    }
}

@Composable
private fun Section(title: Int, subtitle: Int? = null) {
    Text(stringResource(title), style = Hyper.type.title, color = Hyper.colors.text)
    if (subtitle != null) Text(stringResource(subtitle), style = Hyper.type.small, color = Hyper.colors.textMuted)
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun TargetCell(label: String, grams: Int, color: androidx.compose.ui.graphics.Color) {
    Column {
        Text(Bidi.isolate(stringResource(R.string.unit_g_value, grams.toString())), style = Hyper.type.title, color = color)
        Text(label, style = Hyper.type.micro, color = Hyper.colors.textMuted)
    }
}

@Composable
private fun SliderBlock(title: String, desc: String, value: String, v: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    val c = Hyper.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = Hyper.type.title, color = c.text)
            Text(desc, style = Hyper.type.small, color = c.textMuted)
        }
        Text(value, style = Hyper.type.title, color = c.accent)
    }
    HyperSlider(v, onChange, range)
    Spacer(Modifier.height(14.dp))
}
