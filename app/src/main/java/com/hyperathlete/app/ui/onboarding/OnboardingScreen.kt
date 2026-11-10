package com.hyperathlete.app.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hyperathlete.app.R
import com.hyperathlete.app.domain.model.AppLanguage
import com.hyperathlete.app.ui.components.StepIndicator
import com.hyperathlete.app.ui.components.hyperClickable
import com.hyperathlete.app.ui.components.topFade
import com.hyperathlete.app.ui.permissions.PermissionController
import com.hyperathlete.app.ui.permissions.rememberPermissionController
import com.hyperathlete.app.ui.theme.Hyper
import com.hyperathlete.app.ui.theme.glassBackdrop
import com.hyperathlete.app.ui.theme.glass
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/** Everything the onboarding pages can do; keeps the pages free of ViewModel/Hilt so they render in tests. */
class OnboardingActions(
    val next: () -> Unit,
    val back: () -> Unit,
    val setLanguage: (AppLanguage) -> Unit,
    val setName: (String) -> Unit,
    val setBirthDate: (LocalDate) -> Unit,
    val setWeight: (Double) -> Unit,
    val setHeight: (Double) -> Unit,
    val setProgramStart: (LocalDate) -> Unit,
    val setTrainingTime: (DayOfWeek, LocalTime) -> Unit,
    val applyTimeToAll: (LocalTime) -> Unit,
    val setAutostartAcknowledged: (Boolean) -> Unit,
    val finish: () -> Unit,
)

@Composable
fun OnboardingRoute(onFinished: () -> Unit, vm: OnboardingViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    if (!state.loaded) return
    val permissions = rememberPermissionController(state.autostartAcknowledged)
    val actions = OnboardingActions(
        next = vm::next, back = vm::back, setLanguage = { vm.setLanguage(it) },
        setName = vm::setName, setBirthDate = vm::setBirthDate, setWeight = vm::setWeight, setHeight = vm::setHeight,
        setProgramStart = vm::setProgramStart, setTrainingTime = vm::setTrainingTime, applyTimeToAll = vm::applyTimeToAll,
        setAutostartAcknowledged = { vm.setAutostartAcknowledged(it) },
        finish = { vm.finish(onFinished) },
    )
    // Restoring a Drive backup during setup brings back finished settings too: go straight to the app.
    val account = com.hyperathlete.app.ui.account.rememberAccountDeps(onNotice = { if (it == com.hyperathlete.app.ui.account.Notice.RESTORED) onFinished() })
    OnboardingScreen(state, permissions, actions, account)
}

@Composable
fun OnboardingScreen(
    state: OnboardingState,
    permissions: PermissionController,
    actions: OnboardingActions,
    account: com.hyperathlete.app.ui.account.AccountDeps = com.hyperathlete.app.ui.account.AccountDeps(com.hyperathlete.app.domain.account.AccountState()),
) {
    val c = Hyper.colors
    BackHandler(enabled = state.stepIndex > 0) { actions.back() }
    Column(
        Modifier
            .fillMaxSize()
            .glassBackdrop(Hyper.glass)
            .systemBarsPadding()
            .imePadding(),
    ) {
        TopRow(state, actions)
        StepIndicator(
            count = state.totalSteps, step = state.stepIndex + 1,
            modifier = Modifier.padding(horizontal = 22.dp).padding(top = 12.dp),
        )
        val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
        AnimatedContent(
            targetState = state.step,
            modifier = Modifier.weight(1f),
            transitionSpec = {
                // Forward slides toward the reading direction's end (left in Arabic).
                val dir = (if (state.forward) 1 else -1) * (if (rtl) -1 else 1)
                (slideInHorizontally(tween(320, delayMillis = 60)) { it / 6 * dir } + fadeIn(tween(260, delayMillis = 60)))
                    .togetherWith(slideOutHorizontally(tween(220)) { -it / 6 * dir } + fadeOut(tween(180)))
            },
            label = "onboardingStep",
        ) { step ->
            when (step) {
                OnboardingStep.WELCOME -> WelcomePage(state, actions)
                OnboardingStep.PROFILE -> ProfilePage(state, actions)
                OnboardingStep.PROGRAM -> ProgramPage(state, actions)
                OnboardingStep.PERMISSIONS -> PermissionsPage(state, permissions, actions)
                OnboardingStep.ACCOUNT -> AccountPage(actions, account)
                OnboardingStep.READY -> ReadyPage(state, actions)
            }
        }
    }
}

@Composable
private fun TopRow(state: OnboardingState, actions: OnboardingActions) {
    val c = Hyper.colors
    val showBack = state.stepIndex > 0
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 18.dp).padding(top = 10.dp).height(40.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(38.dp)
                .alpha(if (showBack) 1f else 0f)
                .clip(CircleShape)
                .background(c.surfaceAlt)
                .then(if (showBack) Modifier.hyperClickable(onClick = actions.back) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.action_back), tint = c.textSecondary, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(
            stringResource(R.string.ob_step_format, state.stepIndex + 1, state.totalSteps),
            style = Hyper.type.micro, color = c.textMuted, modifier = Modifier.weight(1f),
        )
        if (state.step == OnboardingStep.WELCOME) LanguageToggle(state.language, actions.setLanguage)
    }
}

/** Quick Arabic/English switch on the first screen (all three modes live in Settings). */
@Composable
private fun LanguageToggle(language: AppLanguage, onChange: (AppLanguage) -> Unit) {
    val c = Hyper.colors
    val english = language == AppLanguage.ENGLISH
    Row(
        Modifier
            .clip(CircleShape)
            .background(c.surfaceAlt)
            .hyperClickable { onChange(if (english) AppLanguage.MIXED else AppLanguage.ENGLISH) }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(R.drawable.ic_translate), null, tint = c.textSecondary, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(if (english) "عربي" else "English", style = Hyper.type.smallMedium, color = c.text)
    }
}

/** Standard page frame: scrolling content + pinned bottom actions. */
@Composable
fun OnboardingPage(
    bottom: @Composable ColumnScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp)
                .padding(top = 22.dp, bottom = 12.dp),
            content = content,
        )
        Column(
            Modifier.fillMaxWidth().topFade(Hyper.colors.bg).padding(horizontal = 22.dp).padding(top = 6.dp, bottom = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = bottom,
        )
    }
}

@Composable
fun PageTitle(title: String, desc: String?, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(title, style = Hyper.type.h1, color = Hyper.colors.text)
        if (desc != null) {
            Spacer(Modifier.height(8.dp))
            Text(desc, style = Hyper.type.body, color = Hyper.colors.textSecondary)
        }
    }
}
