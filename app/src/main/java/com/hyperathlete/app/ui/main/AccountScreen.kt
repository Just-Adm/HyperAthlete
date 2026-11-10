package com.hyperathlete.app.ui.main

import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hyperathlete.app.BuildConfig
import com.hyperathlete.app.R
import com.hyperathlete.app.core.locale.Bidi
import com.hyperathlete.app.ui.common.measure
import com.hyperathlete.app.domain.model.AppLanguage
import com.hyperathlete.app.domain.model.Age
import com.hyperathlete.app.ui.AppState
import com.hyperathlete.app.ui.AppViewModel
import com.hyperathlete.app.ui.Dest
import com.hyperathlete.app.ui.components.ConfirmDialog
import com.hyperathlete.app.ui.components.HyperCard
import com.hyperathlete.app.ui.components.HyperChip
import com.hyperathlete.app.ui.components.HyperDivider
import com.hyperathlete.app.ui.components.HyperSwitch
import com.hyperathlete.app.ui.components.SettingsRow
import com.hyperathlete.app.ui.components.SmallPillButton
import com.hyperathlete.app.ui.components.enterAnimation
import com.hyperathlete.app.ui.permissions.rememberPermissionController
import com.hyperathlete.app.ui.theme.Hyper

@Composable
fun AccountScreen(
    state: AppState, autostartAcknowledged: Boolean, vm: AppViewModel?, open: (Dest) -> Unit,
    account: com.hyperathlete.app.ui.account.AccountDeps = com.hyperathlete.app.ui.account.AccountDeps(com.hyperathlete.app.domain.account.AccountState()),
    updates: com.hyperathlete.app.ui.settings.UpdateDeps = com.hyperathlete.app.ui.settings.UpdateDeps(),
) {
    val c = Hyper.colors
    val s = state.settings
    val p = state.profile
    var confirmRerun by rememberSaveable { mutableStateOf(false) }
    val permissions = rememberPermissionController(autostartAcknowledged)

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 12.dp, bottom = 24.dp),
    ) {
        Text(stringResource(R.string.account_title), style = Hyper.type.h1, color = c.text, modifier = Modifier.padding(top = 18.dp, bottom = 16.dp))

        // Profile card
        HyperCard(Modifier.fillMaxWidth().enterAnimation(0)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                com.hyperathlete.app.ui.account.ProfileAvatar(p.photoUri, p.name, 58.dp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(p.name.ifBlank { "—" }, style = Hyper.type.h2, color = c.text, maxLines = 1)
                    Spacer(Modifier.height(4.dp))
                    val parts = listOfNotNull(
                        p.birthDate?.let { stringResource(R.string.age_years, Age.years(it, state.today)) },
                        p.weightKg.takeIf { it > 0 }?.let { com.hyperathlete.app.ui.common.wt(it) },
                        measure(p.heightCm, stringResource(R.string.unit_cm)),
                    )
                    Text(com.hyperathlete.app.core.locale.bidiJoin(parts), style = Hyper.type.small, color = c.textSecondary)
                }
                SmallPillButton(stringResource(R.string.action_edit), { open(Dest.ProfileEdit) }, filled = false)
            }
        }

        Spacer(Modifier.height(12.dp))
        com.hyperathlete.app.ui.account.AccountsCard(account, Modifier.enterAnimation(1))
        Spacer(Modifier.height(12.dp))
        com.hyperathlete.app.ui.account.BackupAndDataCard(account, onBackup = { open(Dest.Backup) }, onData = { open(Dest.Data) }, modifier = Modifier.enterAnimation(1))

        Text(
            stringResource(R.string.settings_training), style = Hyper.type.h2, color = c.text,
            modifier = Modifier.padding(top = 26.dp, bottom = 12.dp).enterAnimation(2),
        )
        HyperCard(Modifier.fillMaxWidth().enterAnimation(2), padding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)) {
            val pos = s.position(state.today)
            SettingsRow(
                R.drawable.ic_flag, stringResource(R.string.settings_program),
                subtitle = com.hyperathlete.app.ui.common.weekPhaseLabel(pos), onClick = { open(Dest.ProgramSettings) },
            )
            HyperDivider()
            SettingsRow(
                R.drawable.ic_timer, stringResource(R.string.settings_workout),
                subtitle = stringResource(R.string.settings_workout_desc), onClick = { open(Dest.WorkoutSettings) },
            )
            HyperDivider()
            SettingsRow(
                R.drawable.ic_calendar_month, stringResource(R.string.settings_calendar),
                subtitle = stringResource(if (s.calendar.sync) R.string.calendar_on else R.string.calendar_off),
                onClick = { open(Dest.CalendarSettings) },
            )
            HyperDivider()
            SettingsRow(
                R.drawable.ic_restaurant, stringResource(R.string.nutrition_settings),
                subtitle = stringResource(R.string.nutrition_settings_desc), onClick = { open(Dest.NutritionSettings) },
            )
            HyperDivider()
            SettingsRow(
                R.drawable.ic_monitoring, stringResource(R.string.progress_settings),
                subtitle = stringResource(R.string.progress_settings_desc), onClick = { open(Dest.ProgressSettings) },
            )
        }

        Text(
            stringResource(R.string.settings_title), style = Hyper.type.h2, color = c.text,
            modifier = Modifier.padding(top = 26.dp, bottom = 12.dp).enterAnimation(3),
        )
        HyperCard(Modifier.fillMaxWidth().enterAnimation(3), padding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)) {
            SettingsRow(
                R.drawable.ic_translate, stringResource(R.string.settings_language),
                subtitle = stringResource(
                    when (s.language) {
                        AppLanguage.MIXED -> R.string.lang_mixed
                        AppLanguage.ARABIC -> R.string.lang_arabic
                        AppLanguage.ENGLISH -> R.string.lang_english
                    },
                ),
                onClick = { open(Dest.Language) },
            )
            HyperDivider()
            SettingsRow(
                R.drawable.ic_palette, stringResource(R.string.settings_appearance),
                subtitle = stringResource(R.string.settings_appearance_desc), onClick = { open(Dest.Appearance) },
            )
            HyperDivider()
            SettingsRow(
                R.drawable.ic_shield, stringResource(R.string.settings_permissions),
                subtitle = stringResource(R.string.ob_perm_summary, permissions.grantedOutsidePower, permissions.countableOutsidePower),
                onClick = { open(Dest.Permissions) },
            )
            HyperDivider()
            SettingsRow(
                R.drawable.ic_battery_charging_full, stringResource(R.string.settings_power),
                subtitle = if (permissions.powerMissing == 0) stringResource(R.string.power_ready_short)
                else stringResource(R.string.power_todo_short, com.hyperathlete.app.ui.settings.powerSteps(permissions.powerMissing)),
                iconTint = if (permissions.powerMissing == 0) c.accent else c.warn,
                iconBg = if (permissions.powerMissing == 0) c.accentSoft else c.warnSoft,
                onClick = { open(Dest.PowerSettings) },
            )
            HyperDivider()
            SettingsRow(
                R.drawable.ic_monitor_weight, stringResource(R.string.settings_weight_unit),
                trailing = {
                    com.hyperathlete.app.ui.components.Segmented(
                        listOf("kg", "lb"), s.weightUnit.ordinal,
                        { vm?.setWeightUnit(com.hyperathlete.app.domain.model.WeightUnit.entries[it]) },
                        Modifier.width(116.dp),
                    )
                },
            )
            HyperDivider()
            SettingsRow(
                R.drawable.ic_mobile_vibrate, stringResource(R.string.settings_haptics),
                subtitle = stringResource(R.string.settings_haptics_desc),
                trailing = { HyperSwitch(s.hapticsEnabled, { vm?.setHaptics(it) }) },
            )

        }

        Spacer(Modifier.height(12.dp))
        HyperCard(Modifier.fillMaxWidth().enterAnimation(4), padding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)) {
            SettingsRow(R.drawable.ic_restart_alt, stringResource(R.string.settings_rerun_onboarding), onClick = { confirmRerun = true })
            HyperDivider()
            com.hyperathlete.app.ui.settings.UpdateRows(updates)
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "Hyper Athlete", style = Hyper.type.micro.copy(fontWeight = FontWeight.Bold), color = c.textMuted,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        Spacer(Modifier.height(com.hyperathlete.app.ui.theme.LocalFloatingBarInset.current))
    }

    if (confirmRerun) {
        ConfirmDialog(
            title = stringResource(R.string.settings_rerun_onboarding),
            message = stringResource(R.string.rerun_confirm),
            confirm = stringResource(R.string.action_confirm),
            onDismiss = { confirmRerun = false },
            onConfirm = { vm?.rerunOnboarding() },
        )
    }
}
