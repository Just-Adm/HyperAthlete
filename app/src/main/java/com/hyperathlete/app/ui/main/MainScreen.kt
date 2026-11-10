package com.hyperathlete.app.ui.main

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hyperathlete.app.R
import com.hyperathlete.app.data.weather.WeatherState
import com.hyperathlete.app.domain.layout.PageId
import com.hyperathlete.app.domain.layout.PageLayout
import com.hyperathlete.app.ui.AppState
import com.hyperathlete.app.ui.AppViewModel
import com.hyperathlete.app.ui.Dest
import com.hyperathlete.app.ui.components.hyperClickable
import com.hyperathlete.app.ui.components.rememberHaptics
import com.hyperathlete.app.ui.theme.Hyper
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.asPaddingValues
import com.hyperathlete.app.ui.theme.glassSource
import com.hyperathlete.app.ui.theme.glassPane
import com.hyperathlete.app.ui.theme.glassBackdrop
import com.hyperathlete.app.ui.theme.glass
import com.hyperathlete.app.ui.theme.HyperShapes

enum class Tab(@StringRes val label: Int, @DrawableRes val icon: Int, @DrawableRes val iconOn: Int, val page: PageId?) {
    HOME(R.string.nav_home, R.drawable.ic_home, R.drawable.ic_home_fill, PageId.HOME),
    NUTRITION(R.string.nav_nutrition, R.drawable.ic_restaurant, R.drawable.ic_restaurant_fill, PageId.NUTRITION),
    PROGRESS(R.string.nav_progress, R.drawable.ic_monitoring, R.drawable.ic_monitoring_fill, PageId.PROGRESS),
    ACCOUNT(R.string.nav_account, R.drawable.ic_person, R.drawable.ic_person_fill, null),
}

/** Hilt-backed entry: weather, layouts and edit mode wired to view models. */
@Composable
fun MainScreen(
    state: AppState,
    autostartAcknowledged: Boolean,
    vm: AppViewModel,
    open: (Dest) -> Unit,
    startInEditMode: Boolean,
    onEditModeConsumed: () -> Unit,
    requestedSheet: HomeSheet? = null,
    onSheetConsumed: () -> Unit = {},
) {
    val weatherVm: WeatherViewModel = hiltViewModel()
    val weather by weatherVm.state.collectAsStateWithLifecycle()
    val layouts by vm.layouts.collectAsStateWithLifecycle()
    val locale = LocalConfiguration.current.locales[0]
    LifecycleResumeEffect(locale) {
        weatherVm.refresh(locale)
        onPauseOrDispose { }
    }
    val locationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) weatherVm.refresh(locale, force = true)
    }
    val trainingVm: com.hyperathlete.app.ui.training.TrainingViewModel = hiltViewModel()
    val training by trainingVm.state.collectAsStateWithLifecycle()
    val nutritionVm: com.hyperathlete.app.ui.nutrition.NutritionViewModel = hiltViewModel()
    val nutritionDay by nutritionVm.day.collectAsStateWithLifecycle()
    val nutritionToday by nutritionVm.todaySummary.collectAsStateWithLifecycle()
    val undo by nutritionVm.undo.collectAsStateWithLifecycle()
    val progressVm: com.hyperathlete.app.ui.progress.ProgressViewModel = hiltViewModel()
    val progressUi by progressVm.ui.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        trainingVm.refreshDay()
        nutritionVm.refreshDay()
        progressVm.refreshDay()
        onPauseOrDispose { }
    }
    MainContent(
        account = { com.hyperathlete.app.ui.account.rememberAccountDeps() },
        updates = { com.hyperathlete.app.ui.settings.rememberUpdateDeps() },
        state = state,
        autostartAcknowledged = autostartAcknowledged,
        vm = vm,
        open = open,
        layouts = layouts,
        onLayoutChange = vm::saveLayout,
        onLayoutReset = vm::resetLayout,
        weather = weather,
        onWeatherRefresh = { weatherVm.refresh(locale, force = true) },
        onEnableLocation = { locationPermission.launch(Manifest.permission.ACCESS_COARSE_LOCATION) },
        startInEditMode = startInEditMode,
        onEditModeConsumed = onEditModeConsumed,
        training = training,
        onStartDay = { day -> trainingVm.start(day) { open(Dest.Workout()) } },
        onResumeWorkout = { open(Dest.Workout()) },
        onKneeMorning = trainingVm::answerKneeMorning,
        onStreakSeen = trainingVm::markStreakSeen,
        requestedSheet = requestedSheet,
        onSheetConsumed = onSheetConsumed,
        nutrition = nutritionDay,
        nutritionToday = nutritionToday,
        nutritionActions = NutritionActions(
            month = nutritionVm::month,
            onSelect = nutritionVm::select,
            onShift = nutritionVm::shift,
            onDelete = nutritionVm::delete,
            undo = undo,
            onUndo = nutritionVm::undoDelete,
        ),
        progress = progressUi,
        progressActions = ProgressActions(
            onRange = progressVm::setRange,
            onAddWeight = progressVm::addWeight,
            onMeasurements = progressVm::saveMeasurements,
            onApplySuggestion = progressVm::applySuggestion,
            onDismissSuggestion = progressVm::dismissSuggestion,
        ),
    )
}

/** Progress page actions (no-ops in screenshot tests). */
class ProgressActions(
    val onRange: (com.hyperathlete.app.domain.progress.TimeRange) -> Unit = {},
    val onAddWeight: (Double) -> Unit = {},
    val onMeasurements: (Double?, Double?) -> Unit = { _, _ -> },
    val onApplySuggestion: () -> Unit = {},
    val onDismissSuggestion: () -> Unit = {},
)

/** Diary actions the nutrition page forwards to its view model (no-ops in screenshot tests). */
class NutritionActions(
    val month: (java.time.YearMonth) -> kotlinx.coroutines.flow.Flow<Map<java.time.LocalDate, com.hyperathlete.app.domain.nutrition.Adherence>> = { kotlinx.coroutines.flow.emptyFlow() },
    val onSelect: (java.time.LocalDate) -> Unit = {},
    val onShift: (Long) -> Unit = {},
    val onDelete: (com.hyperathlete.app.domain.nutrition.MealEntry) -> Unit = {},
    val undo: com.hyperathlete.app.domain.nutrition.MealEntry? = null,
    val onUndo: () -> Unit = {},
)

@Composable
fun MainContent(
    state: AppState,
    autostartAcknowledged: Boolean,
    vm: AppViewModel?,
    open: (Dest) -> Unit,
    layouts: Map<PageId, PageLayout>,
    onLayoutChange: (PageLayout) -> Unit,
    onLayoutReset: (PageId) -> Unit,
    weather: WeatherState,
    onWeatherRefresh: () -> Unit,
    onEnableLocation: () -> Unit,
    initialTab: Tab = Tab.HOME,
    startInEditMode: Boolean = false,
    onEditModeConsumed: () -> Unit = {},
    training: com.hyperathlete.app.ui.training.TrainingState? = null,
    onStartDay: (java.time.DayOfWeek) -> Unit = {},
    onResumeWorkout: () -> Unit = {},
    onKneeMorning: (Int, Boolean) -> Unit = { _, _ -> },
    requestedSheet: HomeSheet? = null,
    onSheetConsumed: () -> Unit = {},
    onStreakSeen: (Int) -> Unit = {},
    nutrition: com.hyperathlete.app.ui.nutrition.NutritionDay? = null,
    nutritionToday: com.hyperathlete.app.ui.nutrition.NutritionDay? = null,
    nutritionActions: NutritionActions = NutritionActions(),
    progress: com.hyperathlete.app.ui.progress.ProgressUi? = null,
    progressActions: ProgressActions = ProgressActions(),
    /** Built only when the Account tab is shown (it owns a view model). */
    account: (@Composable () -> com.hyperathlete.app.ui.account.AccountDeps)? = null,
    updates: (@Composable () -> com.hyperathlete.app.ui.settings.UpdateDeps)? = null,
) {
    var tab by rememberSaveable { mutableIntStateOf(initialTab.ordinal) }
    val emptyDay = remember(state.today, state.profile, state.settings.nutrition) {
        com.hyperathlete.app.ui.nutrition.NutritionDay.of(state.today, state.today, emptyList(), state.profile, state.settings.nutrition)
    }
    var sheet by remember { mutableStateOf<HomeSheet?>(null) }
    LaunchedEffect(requestedSheet) {
        when (requestedSheet) {
            null -> Unit
            HomeSheet.Nutrition -> { tab = Tab.NUTRITION.ordinal; onSheetConsumed() }
            HomeSheet.Weight -> { tab = Tab.PROGRESS.ordinal; sheet = HomeSheet.Weight; onSheetConsumed() }
            else -> {
                tab = Tab.HOME.ordinal
                sheet = requestedSheet
                onSheetConsumed()
            }
        }
    }
    val context = androidx.compose.ui.platform.LocalContext.current
    val tr = training ?: remember(state.today) { com.hyperathlete.app.ui.training.TrainingState.empty(context, state.today) }
    val position = state.settings.position(state.today)
    // A broken streak resets the "already celebrated" mark quietly.
    LaunchedEffect(tr.streak.current, tr.streakSeen) {
        if (training != null && tr.streak.current < tr.streakSeen) onStreakSeen(tr.streak.current)
    }
    val homeTraining = HomeTraining(
        program = tr.program,
        position = position,
        doneDays = tr.days.filterValues { it.sessionDone || it.mobilityDone }.keys,
        todayLogged = tr.todayLogged,
        active = tr.active,
        kneeDue = tr.kneeDue,
        knee = tr.knee,
        onOpenDay = { sheet = HomeSheet.Day(it) },
        onChecklist = { sheet = HomeSheet.Checklist },
        onResume = onResumeWorkout,
        onKnee = { sheet = HomeSheet.Knee },
    )
    var editMode by rememberSaveable { mutableStateOf(startInEditMode) }
    var showWeather by rememberSaveable { mutableStateOf(false) }
    // Arriving from Appearance → "Customise pages": open Home in edit mode.
    LaunchedEffect(startInEditMode) {
        if (startInEditMode) {
            editMode = true
            tab = Tab.HOME.ordinal
            onEditModeConsumed()
        }
    }
    BackHandler(enabled = editMode) { editMode = false }
    val holder = rememberSaveableStateHolder()
    val current = Tab.entries[tab]
    val glass = Hyper.glass
    val backdrop = com.hyperathlete.app.ui.theme.rememberGlassBackdrop()
    val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val barInset = navInset + NavBarHeight + NavBarGap * 2
    androidx.compose.runtime.CompositionLocalProvider(com.hyperathlete.app.ui.theme.LocalFloatingBarInset provides barInset) {
    Box(Modifier.fillMaxSize().glassBackdrop(glass)) {
    Column(Modifier.fillMaxSize().glassSource(backdrop)) {
        AnimatedVisibility(editMode, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            EditModeBar(
                onReset = { current.page?.let(onLayoutReset) },
                onDone = { editMode = false },
                modifier = Modifier.statusBarsPadding(),
            )
        }
        AnimatedContent(
            targetState = current,
            modifier = Modifier
                .weight(1f)
                .then(if (editMode) Modifier.consumeWindowInsets(WindowInsets.statusBars) else Modifier),
            transitionSpec = { fadeIn(tween(200, delayMillis = 40)) togetherWith fadeOut(tween(120)) },
            label = "tab",
        ) { t ->
            holder.SaveableStateProvider(t.name) {
                when (t) {
                    Tab.HOME -> HomeScreen(
                        state,
                        HomeDeps(
                            layout = layouts[PageId.HOME] ?: PageLayout.default(PageId.HOME),
                            editMode = editMode,
                            onLayoutChange = onLayoutChange,
                            weather = weather,
                            onWeatherOpen = { showWeather = true },
                            onEnableLocation = onEnableLocation,
                            streak = tr.streak,
                            training = homeTraining,
                            celebrate = training != null && tr.streak.current > tr.streakSeen,
                            onCelebrated = { onStreakSeen(tr.streak.current) },
                            nutrition = nutritionToday ?: emptyDay,
                            testsDue = progress?.let { it.testWeek && !it.testsDone } ?: false,
                            onTests = { open(Dest.TestEntry) },
                            onOpenNutrition = { tab = Tab.NUTRITION.ordinal },
                        ),
                    )
                    Tab.NUTRITION -> {
                        val day = nutrition ?: emptyDay
                        com.hyperathlete.app.ui.nutrition.NutritionScreen(
                            day,
                            com.hyperathlete.app.ui.nutrition.NutritionDeps(
                                layout = layouts[PageId.NUTRITION] ?: PageLayout.default(PageId.NUTRITION),
                                editMode = editMode,
                                onLayoutChange = onLayoutChange,
                                month = nutritionActions.month,
                                onSelect = nutritionActions.onSelect,
                                onShift = nutritionActions.onShift,
                                onAdd = { meal -> open(Dest.AddFood(day.date.toEpochDay(), meal.name)) },
                                onEdit = { e -> open(Dest.AddFood(e.date.toEpochDay(), e.meal.name, e.id)) },
                                onDelete = nutritionActions.onDelete,
                                undo = nutritionActions.undo,
                                onUndo = nutritionActions.onUndo,
                                onSettings = { open(Dest.NutritionSettings) },
                            ),
                        )
                    }
                    Tab.PROGRESS -> {
                        val ctx = androidx.compose.ui.platform.LocalContext.current
                        val pui = progress ?: remember(state) {
                            com.hyperathlete.app.ui.progress.ProgressUi.of(
                                com.hyperathlete.app.ui.progress.ProgressInput(), state.settings, state.profile,
                                com.hyperathlete.app.data.program.ProgramRepository.load(ctx), state.today, com.hyperathlete.app.domain.progress.TimeRange.W8,
                            )
                        }
                        com.hyperathlete.app.ui.progress.ProgressScreen(
                            pui,
                            com.hyperathlete.app.ui.progress.ProgressDeps(
                                layout = layouts[PageId.PROGRESS] ?: PageLayout.default(PageId.PROGRESS),
                                editMode = editMode,
                                onLayoutChange = onLayoutChange,
                                onRange = progressActions.onRange,
                                onAddWeight = { sheet = HomeSheet.Weight },
                                onAddMeasurement = { sheet = HomeSheet.Measurements },
                                onTests = { open(Dest.TestEntry) },
                                onApplySuggestion = progressActions.onApplySuggestion,
                                onDismissSuggestion = progressActions.onDismissSuggestion,
                                onSettings = { open(Dest.ProgressSettings) },
                            ),
                        )
                    }
                    Tab.ACCOUNT -> AccountScreen(state, autostartAcknowledged, vm, open, account?.invoke() ?: com.hyperathlete.app.ui.account.AccountDeps(com.hyperathlete.app.domain.account.AccountState()), updates?.invoke() ?: com.hyperathlete.app.ui.settings.UpdateDeps())
                }
            }
        }
    }
        FloatingTabBar(current, backdrop, Modifier.align(Alignment.BottomCenter)) { tab = it.ordinal }
    }
    }
    val snapshot = (weather as? WeatherState.Ready)?.snapshot
    if (showWeather && snapshot != null) {
        WeatherSheet(snapshot, state.today, onWeatherRefresh) { showWeather = false }
    }
    when (val sh = sheet) {
        is HomeSheet.Day -> HyperSheet({ sheet = null }) {
            val plan = remember(tr.program, sh.day, position.week) { homeTraining.plan(sh.day) }
            DayDetails(
                plan = plan,
                position = position,
                activeOther = tr.active != null && tr.active.state.day != sh.day,
                activeThis = tr.active?.state?.day == sh.day,
                onStart = { sheet = null; if (tr.active != null) onResumeWorkout() else onStartDay(sh.day) },
                onBack = { sheet = null },
            )
        }
        HomeSheet.Checklist -> HyperSheet({ sheet = null }) {
            ChecklistDetails(homeTraining.plan(state.today.dayOfWeek), tr.todayLogged)
        }
        HomeSheet.Knee -> HyperSheet({ sheet = null }) {
            KneeMorningDetails({ pain, sharp -> onKneeMorning(pain, sharp); sheet = null })
        }
        HomeSheet.Nutrition -> Unit
        HomeSheet.Weight -> HyperSheet({ sheet = null }) {
            com.hyperathlete.app.ui.progress.AddWeightSheet(progress?.weight?.current ?: state.profile.weightKg) { kg ->
                progressActions.onAddWeight(kg); sheet = null
            }
        }
        HomeSheet.Measurements -> HyperSheet({ sheet = null }) {
            com.hyperathlete.app.ui.progress.AddMeasurementSheet(progress?.thigh?.lastOrNull()?.value, progress?.arm?.lastOrNull()?.value) { t, a ->
                progressActions.onMeasurements(t, a); sheet = null
            }
        }
        null -> Unit
    }
}

/** Sheets that open over Home (from boxes, notifications or reminders). */
sealed interface HomeSheet {
    data class Day(val day: java.time.DayOfWeek) : HomeSheet
    data object Checklist : HomeSheet
    data object Knee : HomeSheet
    /** Not a sheet: the food reminder opens the nutrition tab. */
    data object Nutrition : HomeSheet
    /** Quick weigh-in (from the progress page or the morning reminder). */
    data object Weight : HomeSheet
    data object Measurements : HomeSheet
}

private val NavBarHeight = 66.dp
private val NavBarGap = 10.dp

/**
 * Liquid Glass tab bar: a floating pill over the page (content scrolls under it, blurred on Android 12+),
 * with a lens that slides to the selected tab. Each tab is at least 48dp tall.
 */
@Composable
private fun FloatingTabBar(selected: Tab, backdrop: com.hyperathlete.app.ui.theme.GlassBackdrop, modifier: Modifier = Modifier, onSelect: (Tab) -> Unit) {
    val c = Hyper.colors
    val glass = Hyper.glass
    val haptics = rememberHaptics()
    androidx.compose.foundation.layout.BoxWithConstraints(
        modifier
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = NavBarGap)
            .widthIn(max = 440.dp)
            .fillMaxWidth()
            .height(NavBarHeight)
            .glassPane(HyperShapes.pill, glass, backdrop)
            .padding(6.dp),
    ) {
        val cell = maxWidth / Tab.entries.size
        val lensX by androidx.compose.animation.core.animateDpAsState(
            cell * selected.ordinal,
            androidx.compose.animation.core.spring(dampingRatio = 0.72f, stiffness = 420f),
            label = "lens",
        )
        // The lens: accent-tinted glass under the active tab.
        Box(
            Modifier
                .offset(x = lensX) // offset() already mirrors in RTL
                .width(cell)
                .fillMaxHeight()
                .clip(HyperShapes.pill)
                .background(c.accent.copy(alpha = if (c.isDark) 0.22f else 0.14f))
                .border(0.5.dp, glass.edgeLow, HyperShapes.pill),
        )
        Row(Modifier.fillMaxSize()) {
            Tab.entries.forEach { t ->
                val on = t == selected
                val tint by animateColorAsState(if (on) c.accent else c.textSecondary, tween(200), label = "navTint")
                val label by animateColorAsState(if (on) c.text else c.textSecondary, tween(200), label = "navLabel")
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(HyperShapes.pill)
                        .hyperClickable(role = Role.Tab, scaleOnPress = true) { if (!on) { haptics.tick(); onSelect(t) } },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(painterResource(if (on) t.iconOn else t.icon), null, tint = tint, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.height(2.dp))
                    Text(
                        stringResource(t.label), maxLines = 1,
                        style = if (on) Hyper.type.micro.copy(fontWeight = FontWeight.Bold) else Hyper.type.micro,
                        color = label,
                    )
                }
            }
        }
    }
}
