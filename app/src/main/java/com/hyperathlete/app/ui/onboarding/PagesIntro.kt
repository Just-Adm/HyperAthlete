package com.hyperathlete.app.ui.onboarding

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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hyperathlete.app.R
import com.hyperathlete.app.core.locale.termResource
import com.hyperathlete.app.domain.model.Age
import com.hyperathlete.app.domain.program.ProgramClock
import com.hyperathlete.app.domain.program.SessionKind
import com.hyperathlete.app.domain.program.WeekPlan
import com.hyperathlete.app.ui.common.dayName
import com.hyperathlete.app.ui.common.formatDate
import com.hyperathlete.app.ui.common.formatTime
import com.hyperathlete.app.ui.common.sessionTitle
import com.hyperathlete.app.ui.common.weekPhaseLabel
import com.hyperathlete.app.ui.components.AnimatedLogo
import com.hyperathlete.app.ui.components.HyperCard
import com.hyperathlete.app.ui.components.HyperChip
import com.hyperathlete.app.ui.components.HyperDatePickerDialog
import com.hyperathlete.app.ui.components.HyperTextField
import com.hyperathlete.app.ui.components.HyperTimePickerDialog
import com.hyperathlete.app.ui.components.NumberStepper
import com.hyperathlete.app.ui.components.PickerField
import com.hyperathlete.app.ui.components.PrimaryButton
import com.hyperathlete.app.ui.components.TextAction
import com.hyperathlete.app.ui.components.enterAnimation
import com.hyperathlete.app.ui.components.hyperClickable
import com.hyperathlete.app.ui.theme.Hyper
import com.hyperathlete.app.ui.theme.HyperShapes
import java.time.DayOfWeek
import java.time.LocalTime

/* ------------------------------------------------------------------ 1. Welcome */

@Composable
fun WelcomePage(state: OnboardingState, actions: OnboardingActions) {
    val c = Hyper.colors
    OnboardingPage(
        bottom = {
            PrimaryButton(stringResource(R.string.ob_welcome_cta), actions.next, Modifier.enterAnimation(4))
            Spacer(Modifier.height(14.dp))
            Text(
                stringResource(R.string.ob_welcome_footer), style = Hyper.type.small, color = c.textMuted,
                textAlign = TextAlign.Center, modifier = Modifier.enterAnimation(5),
            )
        },
    ) {
        Spacer(Modifier.height(36.dp))
        AnimatedLogo(92.dp, Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(10.dp))
        Text(
            stringResource(R.string.app_name), style = Hyper.type.display, color = c.text,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().enterAnimation(1),
        )
        Spacer(Modifier.height(14.dp))
        Text(
            stringResource(R.string.ob_welcome_headline), style = Hyper.type.h2.copy(fontWeight = FontWeight.Medium), color = c.text,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).enterAnimation(2),
        )
        Spacer(Modifier.height(10.dp))
        Text(
            stringResource(R.string.ob_welcome_desc, com.hyperathlete.app.core.locale.Bidi.isolate(termResource(R.string.term_hybrid_athlete))),
            style = Hyper.type.body, color = c.textSecondary, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).enterAnimation(3),
        )
    }
}

/* ------------------------------------------------------------------ 2. Profile */

@Composable
fun ProfilePage(state: OnboardingState, actions: OnboardingActions) {
    val c = Hyper.colors
    var showDate by rememberSaveable { mutableStateOf(false) }
    val p = state.profile
    OnboardingPage(
        bottom = { PrimaryButton(stringResource(R.string.action_next), actions.next, enabled = state.profileValid) },
    ) {
        PageTitle(stringResource(R.string.ob_profile_title), stringResource(R.string.ob_profile_desc), Modifier.enterAnimation(0))
        Spacer(Modifier.height(24.dp))

        FieldLabel(stringResource(R.string.ob_profile_name), Modifier.enterAnimation(1))
        HyperTextField(
            p.name, actions.setName, placeholder = stringResource(R.string.ob_profile_name_hint),
            leadingIcon = R.drawable.ic_badge, modifier = Modifier.enterAnimation(1),
        )
        Spacer(Modifier.height(18.dp))

        FieldLabel(stringResource(R.string.ob_profile_birth), Modifier.enterAnimation(2))
        PickerField(
            value = p.birthDate?.let { formatDate(it) } ?: "",
            placeholder = stringResource(R.string.ob_profile_birth_hint),
            icon = R.drawable.ic_cake,
            onClick = { showDate = true },
            trailing = p.birthDate?.let { stringResource(R.string.age_years, Age.years(it, state.today)) },
            modifier = Modifier.enterAnimation(2),
        )
        Spacer(Modifier.height(18.dp))

        FieldLabel(stringResource(R.string.ob_profile_weight), Modifier.enterAnimation(3))
        com.hyperathlete.app.ui.components.WeightStepper(
            p.weightKg, actions.setWeight, stepKg = 0.5, minKg = 30.0, maxKg = 200.0, decimals = 1, modifier = Modifier.enterAnimation(3),
        )
        Spacer(Modifier.height(18.dp))

        FieldLabel(stringResource(R.string.ob_profile_height), Modifier.enterAnimation(4))
        NumberStepper(
            p.heightCm, actions.setHeight, unit = stringResource(R.string.unit_cm),
            step = 1.0, min = 120.0, max = 230.0, decimals = 0, modifier = Modifier.enterAnimation(4),
        )
        Spacer(Modifier.height(14.dp))
        Text(stringResource(R.string.ob_profile_note), style = Hyper.type.small, color = c.textMuted)
    }
    if (showDate) {
        HyperDatePickerDialog(
            initial = p.birthDate ?: state.today.minusYears(18),
            yearRange = (state.today.year - 80)..state.today.year,
            selectable = { !it.isAfter(state.today) },
            onDismiss = { showDate = false },
            onConfirm = actions.setBirthDate,
        )
    }
}

@Composable
fun FieldLabel(text: String, modifier: Modifier = Modifier) {
    Text(text, style = Hyper.type.smallMedium, color = Hyper.colors.textSecondary, modifier = modifier.padding(bottom = 8.dp, start = 4.dp))
}

/* ------------------------------------------------------------------ 3. Program */

@Composable
fun ProgramPage(state: OnboardingState, actions: OnboardingActions) {
    val c = Hyper.colors
    var showStart by rememberSaveable { mutableStateOf(false) }
    var editDay by rememberSaveable { mutableStateOf<DayOfWeek?>(null) }
    var applyAll by rememberSaveable { mutableStateOf(false) }
    val pos = ProgramClock.position(state.programStart, state.today)

    OnboardingPage(bottom = { PrimaryButton(stringResource(R.string.action_next), actions.next) }) {
        PageTitle(stringResource(R.string.ob_program_title), stringResource(R.string.ob_program_desc), Modifier.enterAnimation(0))
        Spacer(Modifier.height(22.dp))

        FieldLabel(stringResource(R.string.ob_program_start), Modifier.enterAnimation(1))
        PickerField(
            value = com.hyperathlete.app.core.locale.bidiJoin(listOf(dayName(state.programStart.dayOfWeek), formatDate(state.programStart)), " · "),
            placeholder = "",
            icon = R.drawable.ic_event,
            onClick = { showStart = true },
            modifier = Modifier.enterAnimation(1),
        )
        Spacer(Modifier.height(8.dp))
        Row(Modifier.padding(start = 4.dp).enterAnimation(2), verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(R.drawable.ic_flag), null, tint = c.accent, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                if (pos.started) stringResource(R.string.ob_program_today, weekPhaseLabel(pos))
                else stringResource(R.string.program_starts_in, pos.daysUntilStart),
                style = Hyper.type.smallMedium, color = c.accentDeep,
            )
        }

        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth().enterAnimation(3), verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.ob_program_times), style = Hyper.type.title, color = c.text)
                Text(stringResource(R.string.ob_program_times_desc), style = Hyper.type.small, color = c.textMuted)
            }
        }
        Spacer(Modifier.height(12.dp))
        HyperCard(Modifier.fillMaxWidth().enterAnimation(4), padding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 6.dp)) {
            WeekPlan.days.forEachIndexed { i, d ->
                val time = state.trainingTimes[d.day] ?: LocalTime.of(18, 0)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(HyperShapes.cardSmall)
                        .hyperClickable(scaleOnPress = false) { editDay = d.day }
                        .padding(vertical = 10.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(8.dp).clip(androidx.compose.foundation.shape.CircleShape)
                            .background(if (d.kind == SessionKind.TRAINING) c.accent else c.borderStrong),
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(dayName(d.day), style = Hyper.type.bodyMedium, color = c.text)
                        Text(sessionTitle(d), style = Hyper.type.small, color = c.textMuted, maxLines = 1)
                    }
                    Text(
                        formatTime(time), style = Hyper.type.smallMedium, color = c.text,
                        modifier = Modifier.clip(HyperShapes.pill).background(c.surfaceAlt).padding(horizontal = 12.dp, vertical = 7.dp),
                    )
                }
                if (i < WeekPlan.days.lastIndex) com.hyperathlete.app.ui.components.HyperDivider()
            }
        }
        Spacer(Modifier.height(4.dp))
        TextAction(stringResource(R.string.ob_program_apply_all), { applyAll = true }, color = c.accent, modifier = Modifier.enterAnimation(5))
    }

    if (showStart) {
        HyperDatePickerDialog(
            initial = state.programStart,
            yearRange = (state.today.year - 1)..(state.today.year + 1),
            onDismiss = { showStart = false },
            onConfirm = actions.setProgramStart,
        )
    }
    editDay?.let { day ->
        HyperTimePickerDialog(
            title = stringResource(R.string.ob_program_time_title, dayName(day)),
            initial = state.trainingTimes[day] ?: LocalTime.of(18, 0),
            onDismiss = { editDay = null },
            onConfirm = { actions.setTrainingTime(day, it) },
        )
    }
    if (applyAll) {
        HyperTimePickerDialog(
            title = stringResource(R.string.ob_program_apply_all),
            initial = state.trainingTimes[DayOfWeek.SUNDAY] ?: LocalTime.of(18, 0),
            onDismiss = { applyAll = false },
            onConfirm = actions.applyTimeToAll,
        )
    }
}
