package com.hyperathlete.app.ui.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.hyperathlete.app.R
import com.hyperathlete.app.core.calendar.CalendarInfo
import com.hyperathlete.app.core.calendar.CalendarSync
import com.hyperathlete.app.core.workout.Cue
import com.hyperathlete.app.core.workout.Feedback
import com.hyperathlete.app.domain.model.WorkoutPrefs
import com.hyperathlete.app.domain.program.ProgramClock
import com.hyperathlete.app.ui.AppState
import com.hyperathlete.app.ui.AppViewModel
import com.hyperathlete.app.ui.common.dayName
import com.hyperathlete.app.ui.common.formatDate
import com.hyperathlete.app.ui.common.formatTime
import com.hyperathlete.app.ui.common.weekPhaseLabel
import com.hyperathlete.app.ui.components.ConfirmDialog
import com.hyperathlete.app.ui.components.HyperCard
import com.hyperathlete.app.ui.components.HyperDatePickerDialog
import com.hyperathlete.app.ui.components.HyperDivider
import com.hyperathlete.app.ui.components.HyperSlider
import com.hyperathlete.app.ui.components.HyperSwitch
import com.hyperathlete.app.ui.components.HyperTimePickerDialog
import com.hyperathlete.app.ui.components.PrimaryButton
import com.hyperathlete.app.ui.components.SecondaryButton
import com.hyperathlete.app.ui.components.Segmented
import com.hyperathlete.app.ui.components.SettingsRow
import com.hyperathlete.app.ui.components.hyperClickable
import com.hyperathlete.app.ui.theme.Hyper
import com.hyperathlete.app.ui.theme.HyperShapes
import com.hyperathlete.app.ui.workout.ValueStepper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalTime
import javax.inject.Inject

private val CardPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)

/* ------------------------------------------------------------------ Program */

@Composable
fun ProgramSettingsScreen(state: AppState, vm: AppViewModel?, onBack: () -> Unit) {
    val c = Hyper.colors
    val s = state.settings
    val pos = s.position(state.today)
    var pickStart by rememberSaveable { mutableStateOf(false) }
    var pickTime by rememberSaveable { mutableStateOf<Int?>(null) }
    var confirmRestart by rememberSaveable { mutableStateOf(false) }
    var confirmDeload by rememberSaveable { mutableStateOf(false) }
    SubScreen(stringResource(R.string.settings_program), onBack) {
        HyperCard(Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.program_now), style = Hyper.type.small, color = c.textMuted)
            Text(if (pos.started) weekPhaseLabel(pos) else stringResource(R.string.program_starts_in, pos.daysUntilStart), style = Hyper.type.h2, color = c.text)
            if (pos.cycle > 1) Text(stringResource(R.string.cycle_value, pos.cycle), style = Hyper.type.small, color = c.textSecondary)
            Spacer(Modifier.height(14.dp))
            val minWeek = if (pos.cycle > 1) ProgramClock.REPEAT_FROM else 1
            ValueStepper(
                stringResource(R.string.current_week), pos.week.toDouble(), "", 1.0, minWeek.toDouble(), 0,
                onChange = { w -> vm?.setWeekOffset(s.weekOffset + (w.toInt() - pos.week)) },
                modifier = Modifier.fillMaxWidth(), max = ProgramClock.WEEKS.toDouble(),
            )
        }
        Spacer(Modifier.height(12.dp))
        HyperCard(Modifier.fillMaxWidth(), padding = CardPadding) {
            SettingsRow(
                R.drawable.ic_event, stringResource(R.string.program_start_date),
                subtitle = s.programStart?.let { formatDate(it) } ?: "—", onClick = { pickStart = true },
            )
            HyperDivider()
            SettingsRow(R.drawable.ic_bedtime, stringResource(R.string.deload_now), subtitle = stringResource(R.string.deload_now_desc), onClick = { confirmDeload = true })
            HyperDivider()
            SettingsRow(R.drawable.ic_restart_alt, stringResource(R.string.restart_cycle), subtitle = stringResource(R.string.restart_cycle_desc), onClick = { confirmRestart = true })
        }
        Spacer(Modifier.height(20.dp))
        Text(stringResource(R.string.training_times), style = Hyper.type.title, color = c.text)
        Text(stringResource(R.string.training_times_desc), style = Hyper.type.small, color = c.textMuted)
        Spacer(Modifier.height(10.dp))
        HyperCard(Modifier.fillMaxWidth(), padding = CardPadding) {
            com.hyperathlete.app.domain.program.WeekPlan.sundayFirst.forEachIndexed { i, d ->
                val t = s.trainingTimes[d] ?: LocalTime.of(18, 0)
                SettingsRow(
                    if (com.hyperathlete.app.domain.program.WeekPlan.of(d).kind == com.hyperathlete.app.domain.program.SessionKind.REST) R.drawable.ic_self_improvement else R.drawable.ic_fitness_center,
                    dayName(d),
                    trailing = { Text(formatTime(t), style = Hyper.type.bodyMedium, color = c.accent) },
                    onClick = { pickTime = d.value },
                )
                if (i < 6) HyperDivider()
            }
        }
    }
    if (pickStart) {
        HyperDatePickerDialog(s.programStart ?: state.today, { pickStart = false }, { d -> vm?.setProgramStart(ProgramClock.weekStart(d)); pickStart = false })
    }
    pickTime?.let { v ->
        val d = DayOfWeek.of(v)
        HyperTimePickerDialog(dayName(d), s.trainingTimes[d] ?: LocalTime.of(18, 0), { pickTime = null }) { t ->
            vm?.setTrainingTime(d, t); pickTime = null
        }
    }
    if (confirmRestart) {
        ConfirmDialog(
            stringResource(R.string.restart_cycle), stringResource(R.string.restart_cycle_confirm), stringResource(R.string.action_confirm),
            onDismiss = { confirmRestart = false }, onConfirm = { vm?.setProgramStart(ProgramClock.weekStart(state.today)) },
        )
    }
    if (confirmDeload) {
        ConfirmDialog(
            stringResource(R.string.deload_now), stringResource(R.string.deload_now_confirm), stringResource(R.string.action_confirm),
            onDismiss = { confirmDeload = false }, onConfirm = { vm?.setWeekOffset(s.weekOffset + (ProgramClock.WEEKS - pos.week)) },
        )
    }
}

/* ------------------------------------------------------------------ Workout & reminders */

@HiltViewModel
class SoundTestViewModel @Inject constructor(private val feedback: Feedback) : ViewModel() {
    fun play(prefs: WorkoutPrefs) = feedback.play(Cue.REST_END, prefs)
}

@Composable
fun WorkoutSettingsScreen(state: AppState, vm: AppViewModel?, onBack: () -> Unit, onTestSound: (WorkoutPrefs) -> Unit = {}) {
    val c = Hyper.colors
    val w = state.settings.workout
    val r = state.settings.reminders
    val context = LocalContext.current
    var pickKneeTime by rememberSaveable { mutableStateOf(false) }
    SubScreen(stringResource(R.string.settings_workout), onBack) {
        SectionTitle(R.string.section_timer)
        HyperCard(Modifier.fillMaxWidth(), padding = CardPadding) {
            SettingsRow(R.drawable.ic_volume_up, stringResource(R.string.timer_sounds), subtitle = stringResource(R.string.timer_sounds_desc),
                trailing = { HyperSwitch(w.sounds, { vm?.setWorkoutPrefs(w.copy(sounds = it)) }) })
            if (w.sounds) {
                Column(Modifier.padding(start = 56.dp, end = 8.dp, bottom = 8.dp)) {
                    Text(stringResource(R.string.volume_value, com.hyperathlete.app.core.locale.Bidi.ltr("${(w.volume * 100).toInt()}%")), style = Hyper.type.small, color = c.textMuted)
                    HyperSlider(w.volume, { vm?.setWorkoutPrefs(w.copy(volume = (it * 20).toInt() / 20f)) }, 0.1f..1f)
                }
            }
            HyperDivider()
            SettingsRow(R.drawable.ic_timer, stringResource(R.string.countdown_ticks), subtitle = stringResource(R.string.countdown_ticks_desc),
                trailing = { HyperSwitch(w.countdownTicks, { vm?.setWorkoutPrefs(w.copy(countdownTicks = it)) }) })
            HyperDivider()
            SettingsRow(R.drawable.ic_mobile_vibrate, stringResource(R.string.timer_vibration),
                trailing = { HyperSwitch(w.vibration, { vm?.setWorkoutPrefs(w.copy(vibration = it)) }) })
        }
        Spacer(Modifier.height(10.dp))
        SecondaryButton(stringResource(R.string.test_sound), { onTestSound(w) }, Modifier.fillMaxWidth(), icon = R.drawable.ic_volume_up)

        SectionTitle(R.string.section_capsule)
        HyperCard(Modifier.fillMaxWidth(), padding = CardPadding) {
            SettingsRow(R.drawable.ic_pill, stringResource(R.string.live_updates), subtitle = stringResource(R.string.live_updates_desc),
                trailing = { HyperSwitch(w.liveUpdates, { vm?.setWorkoutPrefs(w.copy(liveUpdates = it)) }) })
            if (Build.VERSION.SDK_INT >= 36) {
                HyperDivider()
                SettingsRow(R.drawable.ic_open_in_new, stringResource(R.string.live_updates_system), onClick = { openPromotion(context) })
            }
        }

        SectionTitle(R.string.section_reminders)
        HyperCard(Modifier.fillMaxWidth(), padding = CardPadding) {
            SettingsRow(R.drawable.ic_notifications_active, stringResource(R.string.reminder_workout), subtitle = stringResource(R.string.reminder_workout_desc),
                trailing = { HyperSwitch(r.workout, { vm?.setReminderPrefs(r.copy(workout = it)) }) })
            if (r.workout) {
                val options = listOf(15, 30, 45, 60)
                Segmented(
                    options.map { stringResource(R.string.minutes_single, it) },
                    options.indexOf(r.minutesBefore).coerceAtLeast(0),
                    { vm?.setReminderPrefs(r.copy(minutesBefore = options[it])) },
                    Modifier.padding(start = 4.dp, end = 4.dp, bottom = 10.dp),
                )
            }
            HyperDivider()
            SettingsRow(R.drawable.ic_self_improvement, stringResource(R.string.reminder_mobility), subtitle = stringResource(R.string.reminder_mobility_desc),
                trailing = { HyperSwitch(r.mobility, { vm?.setReminderPrefs(r.copy(mobility = it)) }) })
            HyperDivider()
            SettingsRow(R.drawable.ic_healing, stringResource(R.string.reminder_knee), subtitle = stringResource(R.string.reminder_knee_desc, formatTime(r.kneeMorningTime)),
                trailing = { HyperSwitch(r.kneeMorning, { vm?.setReminderPrefs(r.copy(kneeMorning = it)) }) },
                onClick = { pickKneeTime = true })
        }
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.reminders_more_later), style = Hyper.type.small, color = c.textMuted)
    }
    if (pickKneeTime) {
        HyperTimePickerDialog(stringResource(R.string.reminder_knee), r.kneeMorningTime, { pickKneeTime = false }) { t ->
            vm?.setReminderPrefs(r.copy(kneeMorningTime = t)); pickKneeTime = false
        }
    }
}

private fun openPromotion(context: Context) {
    val intents = listOfNotNull(
        if (Build.VERSION.SDK_INT >= 36) Intent(Settings.ACTION_APP_NOTIFICATION_PROMOTION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName) else null,
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
    )
    for (i in intents) {
        if (runCatching { context.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess) return
    }
}

@Composable
private fun SectionTitle(res: Int) {
    Text(stringResource(res), style = Hyper.type.title, color = Hyper.colors.text, modifier = Modifier.padding(top = 20.dp, bottom = 10.dp))
}

/* ------------------------------------------------------------------ Calendar */

@HiltViewModel
class CalendarSettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sync: CalendarSync,
) : ViewModel() {
    private val _calendars = MutableStateFlow<List<CalendarInfo>>(emptyList())
    val calendars: StateFlow<List<CalendarInfo>> = _calendars.asStateFlow()
    private val _status = MutableStateFlow<String?>(null)
    val status: StateFlow<String?> = _status.asStateFlow()

    fun hasPermission() = sync.hasPermission()

    fun refresh() {
        viewModelScope.launch { _calendars.value = withContext(Dispatchers.IO) { sync.calendars() } }
    }

    fun syncNow() {
        viewModelScope.launch {
            val n = withContext(Dispatchers.IO) { runCatching { sync.sync() }.getOrDefault(-1) }
            _status.value = if (n < 0) context.getString(R.string.calendar_sync_failed) else context.getString(R.string.calendar_synced, n)
            refresh()
        }
    }
}

@Composable
fun CalendarSettingsRoute(state: AppState, vm: AppViewModel, onBack: () -> Unit) {
    val cvm: CalendarSettingsViewModel = hiltViewModel()
    val calendars by cvm.calendars.collectAsStateWithLifecycle()
    val status by cvm.status.collectAsStateWithLifecycle()
    var granted by remember { mutableStateOf(cvm.hasPermission()) }
    val cal = state.settings.calendar
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { res ->
        granted = res.values.all { it }
        if (granted) {
            vm.setCalendarPrefs(cal.copy(sync = true)); cvm.refresh()
        }
    }
    LaunchedEffect(granted) { if (granted) cvm.refresh() }
    CalendarSettingsScreen(
        state, calendars, granted, status, onBack,
        onToggle = { on ->
            if (on && !granted) ask.launch(arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR))
            else vm.setCalendarPrefs(cal.copy(sync = on))
        },
        onChange = vm::setCalendarPrefs,
        onSyncNow = cvm::syncNow,
    )
}

@Composable
fun CalendarSettingsScreen(
    state: AppState,
    calendars: List<CalendarInfo>,
    granted: Boolean,
    status: String?,
    onBack: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onChange: (com.hyperathlete.app.domain.model.CalendarPrefs) -> Unit,
    onSyncNow: () -> Unit,
) {
    val c = Hyper.colors
    val cal = state.settings.calendar
    SubScreen(stringResource(R.string.settings_calendar), onBack) {
        HyperCard(Modifier.fillMaxWidth(), padding = CardPadding) {
            SettingsRow(
                R.drawable.ic_calendar_month, stringResource(R.string.calendar_sync), subtitle = stringResource(R.string.calendar_sync_desc),
                trailing = { HyperSwitch(cal.sync && granted, onToggle) },
            )
        }
        if (cal.sync && granted) {
            Text(stringResource(R.string.calendar_which), style = Hyper.type.title, color = c.text, modifier = Modifier.padding(top = 20.dp, bottom = 10.dp))
            val own = calendars.firstOrNull { it.own }
            CalendarOption(stringResource(R.string.calendar_own), stringResource(R.string.calendar_own_desc), c.accent, cal.calendarId == null || cal.calendarId == own?.id) {
                onChange(cal.copy(calendarId = null))
            }
            calendars.filterNot { it.own }.forEach { info ->
                Spacer(Modifier.height(8.dp))
                CalendarOption(info.name, info.account, Color(info.color), cal.calendarId == info.id) {
                    onChange(cal.copy(calendarId = info.id))
                }
            }
            Text(stringResource(R.string.calendar_reminder), style = Hyper.type.title, color = c.text, modifier = Modifier.padding(top = 20.dp, bottom = 10.dp))
            val options = listOf(15, 30, 60, 90)
            Segmented(options.map { stringResource(R.string.minutes_single, it) }, options.indexOf(cal.reminderMinutes).coerceAtLeast(1), {
                onChange(cal.copy(reminderMinutes = options[it]))
            })
            Spacer(Modifier.height(16.dp))
            PrimaryButton(stringResource(R.string.calendar_sync_now), onSyncNow, Modifier.fillMaxWidth(), icon = R.drawable.ic_sync)
            status?.let { Text(it, style = Hyper.type.small, color = c.textMuted, modifier = Modifier.padding(top = 8.dp)) }
        }
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.calendar_explain), style = Hyper.type.small, color = c.textMuted)
    }
}

@Composable
private fun CalendarOption(title: String, subtitle: String, color: Color, selected: Boolean, onClick: () -> Unit) {
    val c = Hyper.colors
    Row(
        Modifier.fillMaxWidth().clip(HyperShapes.cardSmall).background(if (selected) c.accentSoft else c.surface)
            .border(1.dp, if (selected) c.accent else c.border, HyperShapes.cardSmall).hyperClickable(onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(14.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = Hyper.type.bodyMedium, color = c.text)
            if (subtitle.isNotBlank()) Text(subtitle, style = Hyper.type.micro, color = c.textMuted)
        }
        if (selected) Icon(painterResource(R.drawable.ic_check), null, tint = c.accent, modifier = Modifier.size(20.dp))
    }
}
