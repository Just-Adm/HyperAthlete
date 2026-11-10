package com.hyperathlete.app.ui.permissions

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.hyperathlete.app.R
import com.hyperathlete.app.core.permissions.AppPermission
import com.hyperathlete.app.core.permissions.GrantMethod
import com.hyperathlete.app.core.permissions.PermissionChecker
import com.hyperathlete.app.core.permissions.PermissionGroup
import com.hyperathlete.app.core.permissions.PermissionState
import com.hyperathlete.app.core.util.findActivity
import com.hyperathlete.app.ui.components.HyperCard
import com.hyperathlete.app.ui.components.HyperChip
import com.hyperathlete.app.ui.components.HyperSwitch
import com.hyperathlete.app.ui.components.IconTile
import com.hyperathlete.app.ui.components.SecondaryButton
import com.hyperathlete.app.ui.components.SmallPillButton
import com.hyperathlete.app.ui.components.enterAnimation
import com.hyperathlete.app.ui.theme.Hyper
import com.hyperathlete.app.ui.theme.HyperShapes

/** Live permission states plus a way to request each one. States refresh every time the screen resumes. */
@Stable
class PermissionController(
    val states: Map<AppPermission, PermissionState>,
    val request: (AppPermission) -> Unit,
    val openHonorSettings: () -> Unit,
) {
    fun state(p: AppPermission) = states[p] ?: PermissionState.DENIED
    val grantedCount: Int get() = AppPermission.entries.count { state(it).isOk && it.readyInPhase == null }
    val countable: Int get() = AppPermission.entries.count { it.readyInPhase == null }

    /** Settings → Permissions leaves out the two background ones; they live under Settings → Power. */
    val grantedOutsidePower: Int get() = AppPermission.entries.count { !it.isPower && state(it).isOk && it.readyInPhase == null }
    val countableOutsidePower: Int get() = AppPermission.entries.count { !it.isPower && it.readyInPhase == null }

    /** Background steps still missing (battery optimisation, MagicOS autostart). */
    val powerMissing: Int get() = AppPermission.entries.count { it.isPower && !state(it).isOk }
}

/** Battery optimisation and autostart decide whether the app survives in the background. */
val AppPermission.isPower: Boolean get() = this == AppPermission.BATTERY || this == AppPermission.AUTOSTART

val PermissionState.isOk get() = this == PermissionState.GRANTED || this == PermissionState.NOT_NEEDED

@Composable
fun rememberPermissionController(autostartAcknowledged: Boolean): PermissionController {
    val context = LocalContext.current
    var tick by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        tick++
        onPauseOrDispose { }
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { tick++ }
    val states = remember(tick, autostartAcknowledged) {
        AppPermission.entries.associateWith { PermissionChecker.state(context, it, autostartAcknowledged) }
    }
    return remember(states) {
        PermissionController(
            states = states,
            request = { p -> request(context, p) { perms -> launcher.launch(perms) } },
            openHonorSettings = { PermissionChecker.openHonorAutostart(context.findActivity() ?: context) },
        )
    }
}

private fun request(context: Context, p: AppPermission, launch: (Array<String>) -> Unit) {
    val activity = context.findActivity() ?: return
    when (p.method) {
        GrantMethod.RUNTIME -> {
            val perms = p.runtimePermissions()
            if (perms.isEmpty()) {
                // e.g. notifications switched off on Android < 13: only settings can turn them back on.
                PermissionChecker.openSettingsFor(activity, p)
                return
            }
            val asked = activity.getSharedPreferences("permissions_asked", Context.MODE_PRIVATE)
            val askedBefore = asked.getBoolean(p.name, false)
            val canShowDialog = perms.any { ActivityCompat.shouldShowRequestPermissionRationale(activity, it) }
            if (askedBefore && !canShowDialog) {
                // Denied twice ("don't ask again"): Android won't show the dialog any more.
                activity.startActivity(PermissionChecker.appDetails(activity))
            } else {
                asked.edit().putBoolean(p.name, true).apply()
                launch(perms)
            }
        }
        GrantMethod.SETTINGS -> PermissionChecker.openSettingsFor(activity, p)
        GrantMethod.GUIDE -> Unit // handled inline by the guide card
    }
}

/** Grouped permission cards (essential / optional) used by onboarding and Settings. */
@Composable
fun PermissionList(
    controller: PermissionController,
    autostartAcknowledged: Boolean,
    onAutostartAcknowledged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    startIndex: Int = 0,
    include: (AppPermission) -> Boolean = { true },
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        var i = startIndex
        PermissionGroup.entries.forEach { group ->
            if (AppPermission.entries.none { it.group == group && include(it) }) return@forEach
            Text(
                stringResource(if (group == PermissionGroup.ESSENTIAL) R.string.ob_perm_essential else R.string.ob_perm_optional),
                style = Hyper.type.smallMedium, color = Hyper.colors.textMuted,
                modifier = Modifier.padding(top = if (group == PermissionGroup.OPTIONAL) 10.dp else 0.dp).enterAnimation(i++),
            )
            AppPermission.entries.filter { it.group == group && include(it) }.forEach { p ->
                if (p == AppPermission.AUTOSTART) {
                    AutostartGuideCard(
                        state = controller.state(p),
                        onOpenSettings = controller.openHonorSettings,
                        onAcknowledge = onAutostartAcknowledged,
                        acknowledged = autostartAcknowledged,
                        modifier = Modifier.enterAnimation(i++),
                    )
                } else {
                    PermissionCard(p, controller.state(p), { controller.request(p) }, Modifier.enterAnimation(i++))
                }
            }
        }
    }
}

@Composable
fun PermissionCard(p: AppPermission, state: PermissionState, onAllow: () -> Unit, modifier: Modifier = Modifier) {
    val c = Hyper.colors
    val ok = state.isOk
    val tileTint by animateColorAsState(if (ok) c.success else if (state == PermissionState.LATER) c.lock else c.accent, tween(260), label = "pTint")
    val tileBg by animateColorAsState(if (ok) c.successSoft else if (state == PermissionState.LATER) c.lockSoft else c.accentSoft, tween(260), label = "pBg")
    HyperCard(modifier.fillMaxWidth(), padding = androidx.compose.foundation.layout.PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            IconTile(p.icon, tint = tileTint, background = tileBg, size = 44.dp, iconSize = 22.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(p.title), style = Hyper.type.title, color = c.text)
                Spacer(Modifier.height(3.dp))
                Text(stringResource(p.why), style = Hyper.type.small, color = c.textSecondary)
            }
        }
        if (state == PermissionState.RESTRICTED) {
            Spacer(Modifier.height(12.dp))
            RestrictedHint()
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusChip(state, p)
            Spacer(Modifier.weight(1f))
            val context = LocalContext.current
            when {
                state == PermissionState.RESTRICTED -> SmallPillButton(stringResource(R.string.restricted_open_info), {
                    runCatching { context.startActivity(PermissionChecker.appDetails(context)) }
                })
                !ok && state != PermissionState.LATER -> SmallPillButton(stringResource(R.string.action_allow), onAllow)
            }
        }
    }
}

/** How to lift Android's block on sideloaded apps, step by step. */
@Composable
private fun RestrictedHint() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        GuideStep(1, R.string.restricted_title, R.string.restricted_step1)
        GuideStep(2, null, R.string.restricted_step2)
        GuideStep(3, null, R.string.restricted_step3)
    }
}

@Composable
private fun StatusChip(state: PermissionState, p: AppPermission) {
    val c = Hyper.colors
    when (state) {
        PermissionState.GRANTED, PermissionState.NOT_NEEDED ->
            HyperChip(stringResource(R.string.status_granted), color = c.success, background = c.successSoft, icon = R.drawable.ic_check)
        PermissionState.DENIED ->
            HyperChip(stringResource(R.string.status_not_granted), color = c.textSecondary, background = c.surfaceAlt)
        PermissionState.LATER ->
            HyperChip(stringResource(R.string.coming_in_phase, p.readyInPhase ?: 0), color = c.lock, background = c.lockSoft, icon = R.drawable.ic_lock)
        PermissionState.RESTRICTED ->
            HyperChip(stringResource(R.string.restricted_chip), color = c.warn, background = c.warnSoft, icon = R.drawable.ic_lock)
    }
}

/** MagicOS background steps: there is no API to check them, so the user confirms once done. */
@Composable
fun AutostartGuideCard(
    state: PermissionState,
    acknowledged: Boolean,
    onOpenSettings: () -> Unit,
    onAcknowledge: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Hyper.colors
    var expanded by rememberSaveable { mutableStateOf(!acknowledged) }
    val ok = state.isOk
    HyperCard(modifier.fillMaxWidth(), padding = androidx.compose.foundation.layout.PaddingValues(16.dp), onClick = { expanded = !expanded }) {
        Row(verticalAlignment = Alignment.Top) {
            IconTile(
                AppPermission.AUTOSTART.icon,
                tint = if (ok) c.success else c.accent, background = if (ok) c.successSoft else c.accentSoft,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.perm_autostart), style = Hyper.type.title, color = c.text)
                Spacer(Modifier.height(3.dp))
                Text(stringResource(R.string.perm_autostart_why), style = Hyper.type.small, color = c.textSecondary)
            }
            Icon(
                painterResource(if (expanded) R.drawable.ic_keyboard_arrow_up else R.drawable.ic_keyboard_arrow_down),
                null, tint = c.textMuted, modifier = Modifier.size(22.dp),
            )
        }
        AnimatedVisibility(expanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            Column(Modifier.padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                GuideStep(1, R.string.perm_autostart_step1_title, R.string.perm_autostart_step1) { MockToggleRows() }
                GuideStep(2, R.string.perm_autostart_step2_title, R.string.perm_autostart_step2) { MockBatteryChoice() }
                GuideStep(3, R.string.perm_autostart_step3_title, R.string.perm_autostart_step3) { MockRecentsLock() }
                Spacer(Modifier.height(2.dp))
                SecondaryButton(stringResource(R.string.perm_autostart_open), onOpenSettings, icon = R.drawable.ic_open_in_new)
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusChip(state, AppPermission.AUTOSTART)
            Spacer(Modifier.weight(1f))
            Text(stringResource(R.string.perm_autostart_confirm), style = Hyper.type.smallMedium, color = c.textSecondary)
            Spacer(Modifier.width(10.dp))
            HyperSwitch(acknowledged, onAcknowledge)
        }
    }
}

/** Numbered step with a small illustration of the setting to look for. */
@Composable
fun GuideStep(number: Int, title: Int?, body: Int, illustration: (@Composable () -> Unit)? = null) {
    val c = Hyper.colors
    Row(
        Modifier.fillMaxWidth().clip(HyperShapes.cardSmall).background(c.surfaceAlt).padding(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(Modifier.size(26.dp).clip(CircleShape).background(c.accent), contentAlignment = Alignment.Center) {
            Text(number.toString(), style = Hyper.type.smallMedium.copy(fontWeight = FontWeight.Bold), color = c.onAccent)
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            if (title != null) {
                Text(stringResource(title), style = Hyper.type.bodyMedium.copy(fontWeight = FontWeight.Bold), color = c.text)
                Spacer(Modifier.height(3.dp))
            }
            Text(stringResource(body), style = if (title != null) Hyper.type.small else Hyper.type.bodyMedium, color = if (title != null) c.textSecondary else c.text)
            if (illustration != null) {
                Spacer(Modifier.height(10.dp))
                illustration()
            }
        }
    }
}

/* ---------- tiny illustrations of the MagicOS screens (drawn, so they follow the theme) ---------- */

/** Mirrors the phone's own (English, left-to-right) settings screen, whatever the app language. */
@Composable
private fun MockFrame(content: @Composable () -> Unit) = androidx.compose.runtime.CompositionLocalProvider(
    androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr,
) {
    val c = Hyper.colors
    Column(
        Modifier.fillMaxWidth().clip(HyperShapes.tile).background(c.surface).border(1.dp, c.border, HyperShapes.tile).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) { content() }
}

@Composable
private fun MockRow(label: String, on: Boolean) {
    val c = Hyper.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = Hyper.type.micro, color = c.text, modifier = Modifier.weight(1f))
        Box(Modifier.size(28.dp, 16.dp).clip(HyperShapes.pill).background(if (on) c.accent else c.borderStrong).padding(2.dp)) {
            Box(Modifier.align(if (on) Alignment.CenterEnd else Alignment.CenterStart).size(12.dp).clip(CircleShape).background(androidx.compose.ui.graphics.Color.White))
        }
    }
}

@Composable
private fun MockToggleRows() = MockFrame {
    MockRow("Manage automatically", false)
    MockRow("Auto-launch", true)
    MockRow("Secondary launch", true)
    MockRow("Run in background", true)
}

@Composable
private fun MockBatteryChoice() = MockFrame {
    val c = Hyper.colors
    listOf("Optimized" to false, "Unrestricted" to true).forEach { (label, on) ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = Hyper.type.micro, color = c.text, modifier = Modifier.weight(1f))
            Box(
                Modifier.size(14.dp).clip(CircleShape).background(if (on) c.accent else c.surface)
                    .border(1.5.dp, if (on) c.accent else c.borderStrong, CircleShape),
            )
        }
    }
}

@Composable
private fun MockRecentsLock() {
    val c = Hyper.colors
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
        repeat(3) { i ->
            val focus = i == 1
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (focus) Icon(painterResource(R.drawable.ic_lock), null, tint = c.accent, modifier = Modifier.size(14.dp))
                Spacer(Modifier.height(3.dp))
                Box(
                    Modifier.size(width = 34.dp, height = if (focus) 56.dp else 50.dp).clip(HyperShapes.tile)
                        .background(if (focus) c.accentSoft else c.surface)
                        .border(1.dp, if (focus) c.accent else c.border, HyperShapes.tile),
                )
            }
        }
        Icon(painterResource(R.drawable.ic_swipe), null, tint = c.textMuted, modifier = Modifier.size(18.dp))
    }
}
