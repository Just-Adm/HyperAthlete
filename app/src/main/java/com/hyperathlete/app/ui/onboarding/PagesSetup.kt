package com.hyperathlete.app.ui.onboarding

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hyperathlete.app.R
import com.hyperathlete.app.core.locale.LocalAppLanguage
import com.hyperathlete.app.domain.model.AppLanguage
import com.hyperathlete.app.domain.program.ProgramClock
import com.hyperathlete.app.domain.program.SessionKind
import com.hyperathlete.app.domain.program.WeekPlan
import com.hyperathlete.app.ui.common.dayName
import com.hyperathlete.app.ui.common.formatTime
import com.hyperathlete.app.ui.common.minutesLabel
import com.hyperathlete.app.ui.common.sessionTitle
import com.hyperathlete.app.ui.common.weekPhaseLabel
import com.hyperathlete.app.ui.components.HyperCard
import com.hyperathlete.app.ui.components.HyperChip
import com.hyperathlete.app.ui.components.HyperSwitch
import com.hyperathlete.app.ui.components.HyperTextField
import com.hyperathlete.app.ui.components.IconTile
import com.hyperathlete.app.ui.components.PrimaryButton
import com.hyperathlete.app.ui.components.Segmented
import com.hyperathlete.app.ui.components.SmallPillButton
import com.hyperathlete.app.ui.components.TextAction
import com.hyperathlete.app.ui.components.enterAnimation
import com.hyperathlete.app.ui.components.hyperClickable
import com.hyperathlete.app.ui.components.rememberHaptics
import com.hyperathlete.app.ui.permissions.GuideStep
import com.hyperathlete.app.ui.permissions.PermissionController
import com.hyperathlete.app.ui.permissions.PermissionList
import com.hyperathlete.app.ui.theme.Hyper
import com.hyperathlete.app.ui.theme.HyperShapes
import java.time.LocalTime
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/* ------------------------------------------------------------------ 4. Permissions */

@Composable
fun PermissionsPage(state: OnboardingState, permissions: PermissionController, actions: OnboardingActions) {
    OnboardingPage(
        bottom = {
            PrimaryButton(stringResource(R.string.action_continue), actions.next)
        },
    ) {
        PageTitle(stringResource(R.string.ob_perm_title), stringResource(R.string.ob_perm_desc), Modifier.enterAnimation(0))
        Spacer(Modifier.height(10.dp))
        HyperChip(
            stringResource(R.string.ob_perm_summary, permissions.grantedCount, permissions.countable),
            icon = R.drawable.ic_shield, modifier = Modifier.enterAnimation(1),
        )
        Spacer(Modifier.height(18.dp))
        PermissionList(
            controller = permissions,
            autostartAcknowledged = state.autostartAcknowledged,
            onAutostartAcknowledged = actions.setAutostartAcknowledged,
            startIndex = 2,
        )
    }
}

/* ------------------------------------------------------------------ 5. Account */

@Composable
fun AccountPage(actions: OnboardingActions, account: com.hyperathlete.app.ui.account.AccountDeps) {
    OnboardingPage(
        bottom = {
            PrimaryButton(stringResource(R.string.action_continue), actions.next)
            if (!account.state.any) {
                Spacer(Modifier.height(4.dp))
                TextAction(stringResource(R.string.action_skip_for_now), actions.next)
            }
        },
    ) {
        PageTitle(stringResource(R.string.ob_account_title), stringResource(R.string.ob_account_desc), Modifier.enterAnimation(0))
        Spacer(Modifier.height(24.dp))
        com.hyperathlete.app.ui.account.OnboardingAccountContent(account)
    }
}

/* ------------------------------------------------------------------ 6. Ready */

@Composable
fun ReadyPage(state: OnboardingState, actions: OnboardingActions) {
    val c = Hyper.colors
    val pos = ProgramClock.position(state.programStart, state.today)
    val today = WeekPlan.of(state.today.dayOfWeek)
    Box(Modifier.fillMaxSize()) {
        OnboardingPage(bottom = { PrimaryButton(stringResource(R.string.ob_ready_cta), actions.finish, Modifier.enterAnimation(5)) }) {
            Spacer(Modifier.height(40.dp))
            Box(
                Modifier.align(Alignment.CenterHorizontally).size(88.dp).clip(androidx.compose.foundation.shape.CircleShape).background(c.accentSoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(R.drawable.ic_celebration), null, tint = c.accent, modifier = Modifier.size(44.dp))
            }
            Spacer(Modifier.height(20.dp))
            Text(
                stringResource(R.string.ob_ready_title, state.profile.name.trim()), style = Hyper.type.h1, color = c.text,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().enterAnimation(1),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.ob_ready_desc), style = Hyper.type.body, color = c.textSecondary,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().enterAnimation(2),
            )
            Spacer(Modifier.height(24.dp))
            HyperCard(Modifier.fillMaxWidth().enterAnimation(3), padding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
                SummaryRow(R.drawable.ic_flag, stringResource(R.string.ob_ready_week),
                    if (pos.started) weekPhaseLabel(pos) else stringResource(R.string.program_starts_in, pos.daysUntilStart))
                com.hyperathlete.app.ui.components.HyperDivider()
                SummaryRow(
                    if (today.kind == SessionKind.TRAINING) R.drawable.ic_fitness_center else R.drawable.ic_self_improvement,
                    stringResource(R.string.ob_ready_today),
                    com.hyperathlete.app.core.locale.bidiJoin(
                        listOf(sessionTitle(today), formatTime(state.trainingTimes[today.day] ?: LocalTime.of(18, 0)), minutesLabel(today.minutes)),
                        " · ",
                    ),
                )
                com.hyperathlete.app.ui.components.HyperDivider()
                val p = state.profile
                SummaryRow(
                    R.drawable.ic_person, stringResource(R.string.ob_ready_profile),
                    com.hyperathlete.app.core.locale.bidiJoin(
                        listOfNotNull(
                            p.birthDate?.let { stringResource(R.string.age_years, com.hyperathlete.app.domain.model.Age.years(it, state.today)) },
                            com.hyperathlete.app.ui.common.wt(p.weightKg),
                            com.hyperathlete.app.ui.common.measure(p.heightCm, stringResource(R.string.unit_cm)),
                        ),
                        " · ",
                    ),
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(dayName(state.today.dayOfWeek), style = Hyper.type.micro, color = Hyper.colors.textMuted, modifier = Modifier.align(Alignment.CenterHorizontally))
        }
        Confetti(Modifier.fillMaxSize())
    }
}


@Composable
private fun SummaryRow(icon: Int, label: String, value: String) {
    val c = Hyper.colors
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        IconTile(icon, size = 38.dp, iconSize = 19.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = Hyper.type.micro, color = c.textMuted)
            Spacer(Modifier.height(2.dp))
            Text(value, style = Hyper.type.bodyMedium, color = c.text)
        }
    }
}

/** One short confetti burst from the top centre. Lightweight: 70 particles, drawn on one canvas. */
@Composable
private fun Confetti(modifier: Modifier) {
    val c = Hyper.colors
    val still = LocalInspectionMode.current
    val progress = remember { Animatable(if (still) 0.42f else 0f) }
    LaunchedEffect(Unit) { if (!still) progress.animateTo(1f, tween(2200, easing = LinearEasing)) }
    val palette = listOf(c.accent, c.accentLight, c.secondary, c.success, c.warn)
    val particles = remember {
        val r = Random(7)
        List(70) {
            val angle = Math.toRadians(-90.0 + (r.nextDouble() - 0.5) * 140.0)
            Particle(
                vx = (cos(angle) * (0.35 + r.nextDouble() * 0.65)).toFloat(),
                vy = (sin(angle) * (0.55 + r.nextDouble() * 0.6)).toFloat(),
                spin = (r.nextFloat() - 0.5f) * 900f,
                color = it % 5,
                w = 6f + r.nextFloat() * 6f,
                h = 3f + r.nextFloat() * 4f,
            )
        }
    }
    Canvas(modifier) {
        val t = progress.value
        if (t >= 1f) return@Canvas
        val origin = Offset(size.width / 2f, size.height * 0.2f)
        val scale = size.width * 0.9f
        val alpha = (1f - t).coerceIn(0f, 1f)
        particles.forEach { p ->
            val x = origin.x + p.vx * scale * t
            val y = origin.y + p.vy * scale * t + 1.1f * scale * t * t
            rotate(p.spin * t, Offset(x, y)) {
                drawRect(
                    palette[p.color].copy(alpha = alpha),
                    topLeft = Offset(x - p.w.dp.toPx() / 2, y - p.h.dp.toPx() / 2),
                    size = Size(p.w.dp.toPx(), p.h.dp.toPx()),
                )
            }
        }
    }
}

private data class Particle(val vx: Float, val vy: Float, val spin: Float, val color: Int, val w: Float, val h: Float)
