package com.hyperathlete.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hyperathlete.app.R
import com.hyperathlete.app.domain.model.AppLanguage
import com.hyperathlete.app.domain.model.AppSettings
import com.hyperathlete.app.domain.model.TextSize
import com.hyperathlete.app.domain.model.ThemeMode
import com.hyperathlete.app.domain.model.UserProfile
import com.hyperathlete.app.domain.model.Age
import com.hyperathlete.app.domain.program.WeekPlan
import com.hyperathlete.app.ui.AppViewModel
import com.hyperathlete.app.ui.common.formatDate
import com.hyperathlete.app.ui.common.sessionTitle
import com.hyperathlete.app.ui.components.ConfirmDialog
import com.hyperathlete.app.ui.components.HyperCard
import com.hyperathlete.app.ui.components.SettingsRow
import com.hyperathlete.app.ui.permissions.isPower
import com.hyperathlete.app.ui.components.HyperChip
import com.hyperathlete.app.ui.components.HyperDatePickerDialog
import com.hyperathlete.app.ui.components.HyperTextField
import com.hyperathlete.app.ui.components.IconTile
import com.hyperathlete.app.ui.components.NumberStepper
import com.hyperathlete.app.ui.components.OptionCard
import com.hyperathlete.app.ui.components.PickerField
import com.hyperathlete.app.ui.components.PrimaryButton
import com.hyperathlete.app.ui.components.SecondaryButton
import com.hyperathlete.app.ui.components.Segmented
import com.hyperathlete.app.ui.components.enterAnimation
import com.hyperathlete.app.ui.components.hyperClickable
import com.hyperathlete.app.ui.components.topFade
import com.hyperathlete.app.ui.components.rememberHaptics
import com.hyperathlete.app.ui.onboarding.FieldLabel
import com.hyperathlete.app.ui.permissions.PermissionList
import com.hyperathlete.app.ui.permissions.rememberPermissionController
import com.hyperathlete.app.ui.theme.Hyper
import com.hyperathlete.app.ui.theme.glassBackdrop
import com.hyperathlete.app.ui.theme.glass
import com.hyperathlete.app.ui.theme.HyperShapes
import com.hyperathlete.app.ui.theme.Palette
import java.time.DayOfWeek
import java.time.LocalDate

/** Sub-screen frame: round back button + title, scrolling body. */
@Composable
fun SubScreen(title: String, onBack: () -> Unit, bottom: (@Composable ColumnScope.() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val c = Hyper.colors
    Column(Modifier.fillMaxSize().glassBackdrop(Hyper.glass).statusBarsPadding().navigationBarsPadding().imePadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(c.surfaceAlt).hyperClickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.action_back), tint = c.text, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text(title, style = Hyper.type.h2, color = c.text)
        }
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(top = 8.dp, bottom = 20.dp),
            content = content,
        )
        if (bottom != null) Column(Modifier.fillMaxWidth().topFade(c.bg).padding(horizontal = 20.dp, vertical = 14.dp), content = bottom)
    }
}

/* ------------------------------------------------------------------ Language */

@Composable
fun LanguageScreen(current: AppLanguage, onSelect: (AppLanguage) -> Unit, onBack: () -> Unit) {
    SubScreen(stringResource(R.string.settings_language), onBack) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf(
                Triple(AppLanguage.MIXED, R.string.lang_mixed, R.string.lang_mixed_desc),
                Triple(AppLanguage.ARABIC, R.string.lang_arabic, R.string.lang_arabic_desc),
                Triple(AppLanguage.ENGLISH, R.string.lang_english, R.string.lang_english_desc),
            ).forEachIndexed { i, (lang, title, desc) ->
                OptionCard(
                    stringResource(title), current == lang, { onSelect(lang) },
                    subtitle = stringResource(desc), modifier = Modifier.enterAnimation(i),
                )
            }
        }
    }
}

/* ------------------------------------------------------------------ Appearance */

@Composable
fun AppearanceScreen(settings: AppSettings, vm: AppViewModel?, onBack: () -> Unit, onCustomizePages: () -> Unit = {}) {
    val c = Hyper.colors
    var confirmReset by rememberSaveable { mutableStateOf(false) }
    SubScreen(stringResource(R.string.settings_appearance), onBack) {
        // Page customisation: jumps to Home in edit mode (drag boxes, hide / show them).
        CustomizePagesCard(onCustomizePages, Modifier.enterAnimation(0))
        Spacer(Modifier.height(22.dp))

        FieldLabel(stringResource(R.string.appearance_presets))
        ThemePresets(settings, Modifier.enterAnimation(1)) { vm?.applyThemePreset(it) }
        Spacer(Modifier.height(22.dp))

        FieldLabel(stringResource(R.string.appearance_theme))
        Segmented(
            listOf(stringResource(R.string.theme_light), stringResource(R.string.theme_dark), stringResource(R.string.theme_system)),
            selected = settings.themeMode.ordinal,
            onSelect = { vm?.setThemeMode(ThemeMode.entries[it]) },
            modifier = Modifier.enterAnimation(1),
        )
        Spacer(Modifier.height(22.dp))

        FieldLabel(stringResource(R.string.appearance_accent))
        Swatches(Palette.AccentPresets, settings.accentArgb, Modifier.enterAnimation(2)) { vm?.setAccent(it) }
        Spacer(Modifier.height(18.dp))

        FieldLabel(stringResource(R.string.appearance_secondary))
        Swatches(Palette.SecondaryPresets, settings.secondaryArgb, Modifier.enterAnimation(3)) { vm?.setSecondary(it) }
        Spacer(Modifier.height(22.dp))

        FieldLabel(stringResource(R.string.appearance_text_size))
        Segmented(
            listOf(
                stringResource(R.string.text_size_small), stringResource(R.string.text_size_default),
                stringResource(R.string.text_size_large), stringResource(R.string.text_size_xlarge),
            ),
            selected = settings.textSize.ordinal,
            onSelect = { vm?.setTextSize(TextSize.entries[it]) },
            modifier = Modifier.enterAnimation(4),
        )
        Spacer(Modifier.height(26.dp))
        SecondaryButton(stringResource(R.string.appearance_reset), { confirmReset = true }, icon = R.drawable.ic_restart_alt)
        Spacer(Modifier.height(12.dp))
    }
    if (confirmReset) {
        ConfirmDialog(
            title = stringResource(R.string.appearance_reset),
            message = stringResource(R.string.appearance_reset_confirm),
            confirm = stringResource(R.string.action_reset),
            onDismiss = { confirmReset = false },
            onConfirm = { vm?.resetAppearance() },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Swatches(colors: List<Color>, selectedArgb: Long, modifier: Modifier = Modifier, onPick: (Long) -> Unit) {
    val c = Hyper.colors
    val haptics = rememberHaptics()
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        colors.forEach { color ->
            val argb = color.toArgb().toLong() and 0xFFFFFFFFL
            val on = argb == (selectedArgb and 0xFFFFFFFFL)
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .border(if (on) 2.5.dp else 0.dp, if (on) c.text else Color.Transparent, CircleShape)
                    .padding(if (on) 5.dp else 0.dp)
                    .clip(CircleShape)
                    .background(color)
                    .hyperClickable { haptics.tick(); onPick(argb) },
                contentAlignment = Alignment.Center,
            ) {
                if (on) Icon(painterResource(R.drawable.ic_check), null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }
    }
}

/** Ready-made themes as small previews: page colour, a card, the main colour bar and the secondary dot. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ThemePresets(settings: AppSettings, modifier: Modifier = Modifier, onPick: (com.hyperathlete.app.ui.theme.ThemePreset) -> Unit) {
    val c = Hyper.colors
    val haptics = rememberHaptics()
    FlowRow(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp), maxItemsInEachRow = 3) {
        com.hyperathlete.app.ui.theme.ThemePreset.All.forEach { p ->
            val on = p.matches(settings.themeMode, settings.accentArgb, settings.secondaryArgb)
            val pc = com.hyperathlete.app.ui.theme.hyperColors(p.mode == ThemeMode.DARK, p.accent, p.secondary)
            Column(
                Modifier.weight(1f).clip(HyperShapes.cardSmall)
                    .border(if (on) 2.dp else 1.dp, if (on) c.accent else c.border, HyperShapes.cardSmall)
                    .hyperClickable { haptics.tick(); onPick(p) }
                    .padding(8.dp),
            ) {
                Box(Modifier.fillMaxWidth().height(58.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp)).background(pc.bg).border(1.dp, pc.border, androidx.compose.foundation.shape.RoundedCornerShape(12.dp)).padding(7.dp)) {
                    Column(Modifier.fillMaxWidth().clip(androidx.compose.foundation.shape.RoundedCornerShape(7.dp)).background(pc.surfaceAlt).padding(6.dp)) {
                        Box(Modifier.fillMaxWidth(0.7f).height(5.dp).clip(CircleShape).background(pc.text.copy(alpha = 0.7f)))
                        Spacer(Modifier.height(5.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.weight(1f).height(9.dp).clip(CircleShape).background(pc.accent))
                            Spacer(Modifier.width(5.dp))
                            Box(Modifier.size(9.dp).clip(CircleShape).background(pc.secondary))
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(themeName(p.id), style = Hyper.type.smallMedium, color = c.text, maxLines = 1, modifier = Modifier.weight(1f))
                    if (on) Icon(painterResource(R.drawable.ic_check), null, tint = c.accent, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun themeName(id: String): String = stringResource(
    when (id) {
        "night" -> R.string.preset_night
        "ocean" -> R.string.preset_ocean
        "forest" -> R.string.preset_forest
        "midnight" -> R.string.preset_midnight
        "rose" -> R.string.preset_rose
        else -> R.string.preset_calisthenics
    },
)

/** Big entry into page customisation mode. */
@Composable
private fun CustomizePagesCard(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = Hyper.colors
    HyperCard(modifier.fillMaxWidth(), borderColor = c.accent.copy(alpha = 0.6f), onClick = onClick, padding = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconTile(R.drawable.ic_dashboard_customize)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.customize_pages), style = Hyper.type.title, color = c.text)
                Spacer(Modifier.height(2.dp))
                Text(stringResource(R.string.customize_pages_desc), style = Hyper.type.small, color = c.textSecondary)
            }
            Icon(painterResource(R.drawable.ic_chevron_right), null, tint = c.accent, modifier = Modifier.size(22.dp))
        }
    }
}

/* ------------------------------------------------------------------ Permissions */

@Composable
fun PermissionsScreen(autostartAcknowledged: Boolean, onAutostartAcknowledged: (Boolean) -> Unit, onBack: () -> Unit, onPower: () -> Unit = {}) {
    val controller = rememberPermissionController(autostartAcknowledged)
    SubScreen(stringResource(R.string.settings_permissions), onBack) {
        HyperChip(stringResource(R.string.ob_perm_summary, controller.grantedOutsidePower, controller.countableOutsidePower), icon = R.drawable.ic_shield)
        Spacer(Modifier.height(16.dp))
        PermissionList(controller, autostartAcknowledged, onAutostartAcknowledged, include = { !it.isPower })
        Spacer(Modifier.height(16.dp))
        // Battery and autostart moved to their own Power section.
        HyperCard(Modifier.fillMaxWidth(), padding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)) {
            SettingsRow(
                R.drawable.ic_battery_charging_full, stringResource(R.string.settings_power),
                subtitle = stringResource(R.string.power_moved_hint), onClick = onPower,
            )
        }
    }
}

/* ------------------------------------------------------------------ Profile edit */

@Composable
fun ProfileEditScreen(profile: UserProfile, today: LocalDate, onSave: (UserProfile) -> Unit, onBack: () -> Unit) {
    var draft by rememberSaveable(stateSaver = ProfileSaver) { mutableStateOf(profile) }
    var showDate by rememberSaveable { mutableStateOf(false) }
    SubScreen(
        stringResource(R.string.profile_edit_title), onBack,
        bottom = { PrimaryButton(stringResource(R.string.action_save), { onSave(draft) }, enabled = draft.name.isNotBlank()) },
    ) {
        FieldLabel(stringResource(R.string.ob_profile_name))
        HyperTextField(draft.name, { draft = draft.copy(name = it.take(30)) }, placeholder = stringResource(R.string.ob_profile_name_hint), leadingIcon = R.drawable.ic_badge)
        Spacer(Modifier.height(18.dp))
        FieldLabel(stringResource(R.string.ob_profile_birth))
        PickerField(
            value = draft.birthDate?.let { formatDate(it) } ?: "",
            placeholder = stringResource(R.string.ob_profile_birth_hint),
            icon = R.drawable.ic_cake,
            onClick = { showDate = true },
            trailing = draft.birthDate?.let { stringResource(R.string.age_years, Age.years(it, today)) },
        )
        Spacer(Modifier.height(18.dp))
        FieldLabel(stringResource(R.string.ob_profile_weight))
        com.hyperathlete.app.ui.components.WeightStepper(draft.weightKg, { draft = draft.copy(weightKg = it) }, stepKg = 0.5, minKg = 30.0, maxKg = 200.0)
        Spacer(Modifier.height(18.dp))
        FieldLabel(stringResource(R.string.ob_profile_height))
        NumberStepper(draft.heightCm, { draft = draft.copy(heightCm = it) }, stringResource(R.string.unit_cm), step = 1.0, min = 120.0, max = 230.0, decimals = 0)
    }
    if (showDate) {
        HyperDatePickerDialog(
            initial = draft.birthDate ?: today.minusYears(18),
            yearRange = (today.year - 80)..today.year,
            selectable = { !it.isAfter(today) },
            onDismiss = { showDate = false },
            onConfirm = { draft = draft.copy(birthDate = it) },
        )
    }
}

private val ProfileSaver = androidx.compose.runtime.saveable.listSaver<UserProfile, Any?>(
    save = { listOf(it.name, it.birthDate?.toEpochDay(), it.weightKg, it.heightCm, it.photoUri) },
    restore = { UserProfile(it[0] as String, (it[1] as Long?)?.let(LocalDate::ofEpochDay), it[2] as Double, it[3] as Double, it[4] as String?) },
)
