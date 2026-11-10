package com.hyperathlete.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hyperathlete.app.R
import com.hyperathlete.app.domain.program.WeekPlan
import com.hyperathlete.app.domain.progress.ProgressPrefs
import com.hyperathlete.app.ui.AppState
import com.hyperathlete.app.ui.common.dayShort
import com.hyperathlete.app.ui.common.formatTime
import com.hyperathlete.app.ui.components.HyperCard
import com.hyperathlete.app.ui.components.HyperDivider
import com.hyperathlete.app.ui.components.HyperSwitch
import com.hyperathlete.app.ui.components.HyperTimePickerDialog
import com.hyperathlete.app.ui.components.SettingsRow
import com.hyperathlete.app.ui.nutrition.SelectChip
import com.hyperathlete.app.ui.theme.Hyper

/** Settings → Progress: morning weigh-in reminder and test-week reminders (brief §18). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProgressSettingsScreen(state: AppState, onSave: (ProgressPrefs) -> Unit, onBack: () -> Unit, health: HealthDeps = HealthDeps()) {
    val c = Hyper.colors
    var p by remember { mutableStateOf(state.settings.progress) }
    val change: (ProgressPrefs) -> Unit = { p = it; onSave(it) }
    var pickTime by rememberSaveable { mutableStateOf(false) }
    SubScreen(stringResource(R.string.progress_settings), onBack) {
        HyperCard(Modifier.fillMaxWidth(), padding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)) {
            SettingsRow(
                R.drawable.ic_monitor_weight, stringResource(R.string.weight_reminder), subtitle = stringResource(R.string.weight_reminder_desc),
                trailing = { HyperSwitch(p.weightReminder, { change(p.copy(weightReminder = it)) }) },
            )
            if (p.weightReminder) {
                HyperDivider()
                SettingsRow(
                    R.drawable.ic_schedule, stringResource(R.string.reminder_time),
                    trailing = { Text(formatTime(p.weightTime), style = Hyper.type.bodyMedium, color = c.accent) },
                    onClick = { pickTime = true },
                )
                FlowRow(
                    Modifier.padding(start = 4.dp, end = 4.dp, bottom = 12.dp, top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    WeekPlan.sundayFirst.forEach { d ->
                        SelectChip(dayShort(d), d in p.weightDays) {
                            change(p.copy(weightDays = if (d in p.weightDays) p.weightDays - d else p.weightDays + d))
                        }
                    }
                }
            }
            HyperDivider()
            SettingsRow(
                R.drawable.ic_flag, stringResource(R.string.test_reminder), subtitle = stringResource(R.string.test_reminder_desc),
                trailing = { HyperSwitch(p.testReminder, { change(p.copy(testReminder = it)) }) },
            )
        }

        // Health Connect: body weight both ways (a smart scale's weigh-ins land here, ours go there).
        Spacer(Modifier.height(20.dp))
        Text(stringResource(R.string.health_title), style = Hyper.type.title, color = c.text, modifier = Modifier.padding(bottom = 10.dp))
        HyperCard(Modifier.fillMaxWidth(), padding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)) {
            val on = p.healthSync && health.granted == true
            SettingsRow(
                R.drawable.ic_ecg_heart, stringResource(R.string.health_sync), subtitle = stringResource(
                    when {
                        health.granted == null -> R.string.health_unavailable
                        on -> R.string.health_on
                        p.healthSync -> R.string.health_needs_permission
                        else -> R.string.health_sync_desc
                    },
                ),
                enabled = health.granted != null,
                trailing = {
                    HyperSwitch(on, { want ->
                        if (!want) change(p.copy(healthSync = false))
                        else if (health.granted == true) change(p.copy(healthSync = true))
                        else health.request { ok -> if (ok) change(p.copy(healthSync = true)) }
                    }, enabled = health.granted != null)
                },
            )
        }
        Spacer(Modifier.height(24.dp))
    }
    if (pickTime) {
        HyperTimePickerDialog(stringResource(R.string.weight_reminder), p.weightTime, { pickTime = false }) { change(p.copy(weightTime = it)) }
    }
}

/** Health Connect status for the settings screen; null [granted] = Health Connect isn't on this phone. */
class HealthDeps(val granted: Boolean? = null, val request: (onResult: (Boolean) -> Unit) -> Unit = {})

@Composable
fun rememberHealthDeps(): HealthDeps {
    val context = androidx.compose.ui.platform.LocalContext.current
    val sync = remember {
        dagger.hilt.android.EntryPointAccessors.fromApplication(context.applicationContext, com.hyperathlete.app.core.AppEntryPoint::class.java).healthSync()
    }
    var granted by remember { mutableStateOf<Boolean?>(if (sync.available()) false else null) }
    var tick by remember { mutableStateOf(0) }
    androidx.compose.runtime.LaunchedEffect(tick) { if (sync.available()) granted = sync.granted() }
    androidx.lifecycle.compose.LifecycleResumeEffect(Unit) { tick++; onPauseOrDispose { } }
    var pending by remember { mutableStateOf<((Boolean) -> Unit)?>(null) }
    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(com.hyperathlete.app.core.health.HealthSync.requestContract()) { got ->
        val ok = got.containsAll(sync.permissions)
        granted = ok
        pending?.invoke(ok); pending = null
    }
    return HealthDeps(granted) { cb -> pending = cb; runCatching { launcher.launch(sync.permissions) }.onFailure { cb(false) } }
}
