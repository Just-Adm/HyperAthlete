package com.hyperathlete.app.ui.settings

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hyperathlete.app.R
import com.hyperathlete.app.core.permissions.AppPermission
import com.hyperathlete.app.ui.AppState
import com.hyperathlete.app.ui.AppViewModel
import com.hyperathlete.app.ui.components.HyperCard
import com.hyperathlete.app.ui.components.HyperDivider
import com.hyperathlete.app.ui.components.HyperSwitch
import com.hyperathlete.app.ui.components.IconTile
import com.hyperathlete.app.ui.components.SettingsRow
import com.hyperathlete.app.ui.components.enterAnimation
import com.hyperathlete.app.ui.permissions.AutostartGuideCard
import com.hyperathlete.app.ui.permissions.PermissionCard
import com.hyperathlete.app.ui.permissions.PermissionController
import com.hyperathlete.app.ui.permissions.rememberPermissionController
import com.hyperathlete.app.ui.theme.Hyper

/**
 * Settings → Power: everything that decides whether the app keeps running in the background
 * (battery optimisation, MagicOS autostart) plus the screen-on choice during workouts.
 */
@Composable
fun PowerSettingsScreen(
    state: AppState,
    vm: AppViewModel?,
    autostartAcknowledged: Boolean,
    onAutostartAcknowledged: (Boolean) -> Unit,
    onBack: () -> Unit,
    controller: PermissionController = rememberPermissionController(autostartAcknowledged),
) {
    val c = Hyper.colors
    val context = LocalContext.current
    SubScreen(stringResource(R.string.settings_power), onBack) {
        PowerHero(controller.powerMissing, Modifier.enterAnimation(0))

        PowerTitle(R.string.power_section_background)
        Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)) {
            PermissionCard(
                AppPermission.BATTERY, controller.state(AppPermission.BATTERY),
                { controller.request(AppPermission.BATTERY) }, Modifier.enterAnimation(1),
            )
            AutostartGuideCard(
                state = controller.state(AppPermission.AUTOSTART),
                acknowledged = autostartAcknowledged,
                onOpenSettings = controller.openHonorSettings,
                onAcknowledge = onAutostartAcknowledged,
                modifier = Modifier.enterAnimation(2),
            )
        }

        PowerTitle(R.string.power_section_workout)
        HyperCard(Modifier.fillMaxWidth().enterAnimation(3), padding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)) {
            SettingsRow(
                R.drawable.ic_light_mode, stringResource(R.string.settings_keep_screen),
                subtitle = stringResource(R.string.power_keep_screen_desc),
                trailing = { HyperSwitch(state.settings.keepScreenOnDuringWorkout, { vm?.setKeepScreenOn(it) }) },
            )
            HyperDivider()
            SettingsRow(
                R.drawable.ic_open_in_new, stringResource(R.string.power_system_battery),
                subtitle = stringResource(R.string.power_system_battery_desc),
                onClick = { openBatteryUsage(context) },
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(stringResource(R.string.power_note), style = Hyper.type.small, color = c.textMuted)
    }
}

/** "Ready" or "N steps left" at the top of the Power screen. */
@Composable
private fun PowerHero(missing: Int, modifier: Modifier = Modifier) {
    val c = Hyper.colors
    val ok = missing == 0
    HyperCard(modifier.fillMaxWidth(), padding = PaddingValues(16.dp), color = if (ok) c.successSoft else c.warnSoft, elevated = false) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconTile(
                if (ok) R.drawable.ic_battery_charging_full else R.drawable.ic_bolt,
                tint = if (ok) c.success else c.warn, background = c.surface, size = 44.dp, iconSize = 22.dp,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(if (ok) R.string.power_ready_title else R.string.power_todo_title),
                    style = Hyper.type.title, color = c.text,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    if (ok) stringResource(R.string.power_ready_sub) else stringResource(R.string.power_todo_sub, powerSteps(missing)),
                    style = Hyper.type.small, color = c.textSecondary,
                )
            }
        }
    }
}

/** "1 step" / "2 steps" (there are only two background steps). */
@Composable
fun powerSteps(n: Int): String = stringResource(if (n <= 1) R.string.power_steps_one else R.string.power_steps_two)

@Composable
private fun PowerTitle(res: Int) {
    Text(stringResource(res), style = Hyper.type.title, color = Hyper.colors.text, modifier = Modifier.padding(top = 20.dp, bottom = 10.dp))
}

/** The phone's own battery usage page; MagicOS shows per-app usage there. */
private fun openBatteryUsage(context: Context) {
    val intents = listOf(
        Intent(Intent.ACTION_POWER_USAGE_SUMMARY),
        Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.fromParts("package", context.packageName, null)),
    )
    for (i in intents) {
        if (runCatching { context.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess) return
    }
}
