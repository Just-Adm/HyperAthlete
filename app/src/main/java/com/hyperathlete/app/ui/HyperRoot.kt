package com.hyperathlete.app.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.hyperathlete.app.core.locale.ProvideAppLanguage
import com.hyperathlete.app.ui.main.MainScreen
import com.hyperathlete.app.ui.onboarding.OnboardingRoute
import com.hyperathlete.app.ui.settings.AppearanceScreen
import com.hyperathlete.app.ui.settings.LanguageScreen
import com.hyperathlete.app.ui.settings.PermissionsScreen
import com.hyperathlete.app.ui.settings.ProfileEditScreen
import com.hyperathlete.app.ui.theme.Hyper
import com.hyperathlete.app.ui.theme.glassBackdrop
import com.hyperathlete.app.ui.theme.glass
import com.hyperathlete.app.ui.theme.HyperTheme
import kotlinx.serialization.Serializable

/** Navigation destinations (type-safe). */
sealed interface Dest {
    @Serializable data object Onboarding : Dest
    @Serializable data object Main : Dest
    @Serializable data object Language : Dest
    @Serializable data object Appearance : Dest
    @Serializable data object Permissions : Dest
    @Serializable data object ProfileEdit : Dest
    @Serializable data class Workout(val startToday: Boolean = false) : Dest
    @Serializable data object ProgramSettings : Dest
    @Serializable data object WorkoutSettings : Dest
    @Serializable data object CalendarSettings : Dest
    @Serializable data object PowerSettings : Dest
    @Serializable data class AddFood(val epochDay: Long, val meal: String, val entryId: Long = 0) : Dest
    @Serializable data class OwnFood(val id: Long = 0, val recipe: Boolean = false, val barcode: String = "") : Dest
    @Serializable data object NutritionSettings : Dest
    @Serializable data object TestEntry : Dest
    @Serializable data object ProgressSettings : Dest
    @Serializable data object Backup : Dest
    @Serializable data object Data : Dest
}

@Composable
fun HyperRoot(state: AppState, vm: AppViewModel) {
    val s = state.settings
    ProvideAppLanguage(s.language) {
        HyperTheme(
            themeMode = s.themeMode,
            accent = Color(s.accentArgb.toInt()),
            secondary = Color(s.secondaryArgb.toInt()),
            textScale = s.textSize.scale,
            hapticsEnabled = s.hapticsEnabled,
            weightUnit = s.weightUnit,
        ) {
            val nav = rememberNavController()
            // Decided once; later changes are explicit navigations (finishing or re-running the setup).
            val start: Dest = androidx.compose.runtime.remember { if (s.onboardingDone) Dest.Main else Dest.Onboarding }
            // Re-running the setup from Settings flips the flag; follow it.
            LaunchedEffect(s.onboardingDone) {
                if (!s.onboardingDone && nav.currentDestination?.route?.contains("Onboarding") == false) {
                    nav.navigate(Dest.Onboarding) { popUpTo(nav.graph.id) { inclusive = true } }
                }
            }
            AppNavHost(nav, start, state, vm)
            // Notification / reminder deep links
            val pending by vm.pendingOpen.collectAsStateWithLifecycle()
            LaunchedEffect(pending, s.onboardingDone) {
                val p = pending ?: return@LaunchedEffect
                vm.consumeOpen()
                if (!s.onboardingDone) return@LaunchedEffect
                when (p) {
                    com.hyperathlete.app.core.workout.WorkoutNotifier.OPEN_WORKOUT -> nav.navigate(Dest.Workout()) { launchSingleTop = true }
                    com.hyperathlete.app.core.workout.WorkoutNotifier.OPEN_START -> nav.navigate(Dest.Workout(startToday = true)) { launchSingleTop = true }
                    com.hyperathlete.app.core.workout.WorkoutNotifier.OPEN_TESTS -> nav.navigate(Dest.TestEntry) { launchSingleTop = true }
                    else -> runCatching {
                        nav.getBackStackEntry<Dest.Main>().savedStateHandle[HOME_SHEET_KEY] = p
                        nav.popBackStack(Dest.Main, inclusive = false)
                    }
                }
            }
        }
    }
}

@Composable
private fun AppNavHost(nav: NavHostController, start: Dest, state: AppState, vm: AppViewModel) {
    val autostartAck by vm.autostartAcknowledged.collectAsStateWithLifecycle()
    val slide = tween<androidx.compose.ui.unit.IntOffset>(320, easing = com.hyperathlete.app.ui.components.EmphasizedEasing)
    // Pop only when there's somewhere to go back to (a fast double tap must never leave a blank screen).
    val back: () -> Unit = { if (nav.previousBackStackEntry != null) nav.popBackStack() }
    NavHost(
        navController = nav,
        startDestination = start,
        modifier = Modifier.fillMaxSize().glassBackdrop(Hyper.glass),
        enterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, slide) { it / 5 } + fadeIn(tween(240))
        },
        exitTransition = { fadeOut(tween(160)) },
        popEnterTransition = { fadeIn(tween(240)) },
        popExitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, slide) { it / 5 } + fadeOut(tween(200))
        },
    ) {
        // Each destination reads the latest state itself instead of capturing it when the graph is built.
        composable<Dest.Onboarding>(
            enterTransition = { fadeIn(tween(300)) },
            exitTransition = { fadeOut(tween(300)) },
        ) {
            OnboardingRoute(onFinished = {
                nav.navigate(Dest.Main) { popUpTo(Dest.Onboarding) { inclusive = true } }
            })
        }
        composable<Dest.Main>(enterTransition = { fadeIn(tween(350)) }) { entry ->
            val st = vm.state.collectAsStateWithLifecycle().value ?: return@composable
            val editRequest by entry.savedStateHandle.getStateFlow(EDIT_PAGES_KEY, false).collectAsStateWithLifecycle()
            val sheetRequest by entry.savedStateHandle.getStateFlow<String?>(HOME_SHEET_KEY, null).collectAsStateWithLifecycle()
            androidx.compose.foundation.layout.Box(Modifier.fillMaxSize()) {
            MainScreen(
                state = st,
                autostartAcknowledged = autostartAck,
                vm = vm,
                open = { nav.navigate(it) },
                startInEditMode = editRequest,
                onEditModeConsumed = { entry.savedStateHandle[EDIT_PAGES_KEY] = false },
                requestedSheet = when (sheetRequest) {
                    com.hyperathlete.app.core.workout.WorkoutNotifier.OPEN_TODAY -> com.hyperathlete.app.ui.main.HomeSheet.Day(st.today.dayOfWeek)
                    com.hyperathlete.app.core.workout.WorkoutNotifier.OPEN_KNEE -> com.hyperathlete.app.ui.main.HomeSheet.Knee
                    com.hyperathlete.app.core.workout.WorkoutNotifier.OPEN_NUTRITION -> com.hyperathlete.app.ui.main.HomeSheet.Nutrition
                    com.hyperathlete.app.core.workout.WorkoutNotifier.OPEN_WEIGHT -> com.hyperathlete.app.ui.main.HomeSheet.Weight
                    else -> null
                },
                onSheetConsumed = { entry.savedStateHandle[HOME_SHEET_KEY] = null },
            )
            }
        }
        composable<Dest.Language> {
            val st = vm.state.collectAsStateWithLifecycle().value ?: return@composable
            LanguageScreen(st.settings.language, vm::setLanguage, onBack = back)
        }
        composable<Dest.Appearance> {
            val st = vm.state.collectAsStateWithLifecycle().value ?: return@composable
            AppearanceScreen(
                st.settings, vm, onBack = back,
                onCustomizePages = {
                    nav.getBackStackEntry<Dest.Main>().savedStateHandle[EDIT_PAGES_KEY] = true
                    nav.popBackStack(Dest.Main, inclusive = false)
                },
            )
        }
        composable<Dest.Permissions> {
            PermissionsScreen(autostartAck, vm::setAutostartAcknowledged, onBack = back, onPower = { nav.navigate(Dest.PowerSettings) })
        }
        composable<Dest.PowerSettings> {
            val st = vm.state.collectAsStateWithLifecycle().value ?: return@composable
            com.hyperathlete.app.ui.settings.PowerSettingsScreen(st, vm, autostartAck, vm::setAutostartAcknowledged, onBack = back)
        }
        composable<Dest.ProfileEdit> {
            val st = vm.state.collectAsStateWithLifecycle().value ?: return@composable
            ProfileEditScreen(st.profile, st.today, onSave = { vm.saveProfile(it); back() }, onBack = back)
        }
        composable<Dest.Workout>(
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up, tween(360, easing = com.hyperathlete.app.ui.components.EmphasizedEasing)) { it / 6 } + fadeIn(tween(260)) },
            popExitTransition = { fadeOut(tween(220)) },
        ) { entry ->
            val st = vm.state.collectAsStateWithLifecycle().value ?: return@composable
            val args = entry.toRoute<Dest.Workout>()
            com.hyperathlete.app.ui.workout.WorkoutRoute(st, args.startToday, onClose = {
                if (!nav.popBackStack(Dest.Main, inclusive = false)) nav.navigate(Dest.Main) { popUpTo(nav.graph.id) { inclusive = true } }
            })
        }
        composable<Dest.ProgramSettings> {
            val st = vm.state.collectAsStateWithLifecycle().value ?: return@composable
            com.hyperathlete.app.ui.settings.ProgramSettingsScreen(st, vm, onBack = back)
        }
        composable<Dest.WorkoutSettings> {
            val st = vm.state.collectAsStateWithLifecycle().value ?: return@composable
            val sound: com.hyperathlete.app.ui.settings.SoundTestViewModel = androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel()
            com.hyperathlete.app.ui.settings.WorkoutSettingsScreen(st, vm, onBack = back, onTestSound = sound::play)
        }
        composable<Dest.AddFood> {
            com.hyperathlete.app.ui.nutrition.AddFoodRoute(
                onBack = back,
                onCreateFood = { code -> nav.navigate(Dest.OwnFood(barcode = code.orEmpty())) },
                onEditCustom = { id -> nav.navigate(Dest.OwnFood(id = id)) },
                onCreateRecipe = { nav.navigate(Dest.OwnFood(recipe = true)) },
            )
        }
        composable<Dest.OwnFood> {
            com.hyperathlete.app.ui.nutrition.OwnFoodRoute(onBack = back)
        }
        composable<Dest.NutritionSettings> {
            val st = vm.state.collectAsStateWithLifecycle().value ?: return@composable
            com.hyperathlete.app.ui.settings.NutritionSettingsScreen(st, vm::setNutritionPrefs, onBack = back)
        }
        composable<Dest.TestEntry> {
            val st = vm.state.collectAsStateWithLifecycle().value ?: return@composable
            val pvm: com.hyperathlete.app.ui.progress.ProgressViewModel = androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel()
            val pui by pvm.ui.collectAsStateWithLifecycle()
            val last = pui?.let { u -> com.hyperathlete.app.domain.progress.Metric.Tests.mapNotNull { m ->
                (u.tests.firstOrNull { it.metric == m }?.let { r -> r.latest?.let { v -> m to com.hyperathlete.app.domain.progress.MetricEntry(metric = m, date = r.latestDate ?: st.today, value = v) } })
            }.toMap() } ?: emptyMap()
            com.hyperathlete.app.ui.progress.TestEntryScreen(
                com.hyperathlete.app.ui.common.weekPhaseLabel(st.settings.position(st.today)), last,
                onSave = { pvm.saveTests(it); back() }, onBack = back,
            )
        }
        composable<Dest.ProgressSettings> {
            val st = vm.state.collectAsStateWithLifecycle().value ?: return@composable
            com.hyperathlete.app.ui.settings.ProgressSettingsScreen(st, vm::setProgressPrefs, onBack = back, health = com.hyperathlete.app.ui.settings.rememberHealthDeps())
        }
        composable<Dest.Backup> {
            com.hyperathlete.app.ui.account.BackupScreen(com.hyperathlete.app.ui.account.rememberAccountDeps(), onBack = back)
        }
        composable<Dest.Data> {
            com.hyperathlete.app.ui.account.DataRoute(onBack = back)
        }
        composable<Dest.CalendarSettings> {
            val st = vm.state.collectAsStateWithLifecycle().value ?: return@composable
            com.hyperathlete.app.ui.settings.CalendarSettingsRoute(st, vm, onBack = back)
        }
    }
}

/** Set on Main's back-stack entry to open it in page-customisation mode. */
const val EDIT_PAGES_KEY = "edit_pages"

/** Set on Main's back-stack entry to open a Home sheet ("today", "knee"). */
const val HOME_SHEET_KEY = "home_sheet"
