package com.hyperathlete.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.hyperathlete.app.R
import com.hyperathlete.app.ui.theme.Hyper
import com.hyperathlete.app.ui.theme.glass
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

private fun LocalDate.toUtcMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
private fun Long.toUtcDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

/** Material date picker styled with the app tokens. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HyperDatePickerDialog(
    initial: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
    yearRange: IntRange = DatePickerDefaults.YearRange,
    selectable: (LocalDate) -> Boolean = { true },
) {
    val c = Hyper.colors
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial.toUtcMillis(),
        yearRange = yearRange,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = selectable(utcTimeMillis.toUtcDate())
            override fun isSelectableYear(year: Int) = year in yearRange
        },
    )
    val colors = DatePickerDefaults.colors(
        containerColor = c.surfaceRaised,
        selectedDayContainerColor = c.accent,
        selectedDayContentColor = c.onAccent,
        todayDateBorderColor = c.accent,
        todayContentColor = c.accent,
        selectedYearContainerColor = c.accent,
        selectedYearContentColor = c.onAccent,
        currentYearContentColor = c.accent,
        headlineContentColor = c.text,
        weekdayContentColor = c.textMuted,
        dayContentColor = c.text,
        navigationContentColor = c.text,
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        colors = colors,
        confirmButton = {
            TextAction(stringResource(R.string.action_save), {
                state.selectedDateMillis?.let { onConfirm(it.toUtcDate()) }
                onDismiss()
            }, color = c.accent)
        },
        dismissButton = { TextAction(stringResource(R.string.action_cancel), onDismiss) },
    ) {
        DatePicker(state = state, colors = colors, showModeToggle = true)
    }
}

/** Clock-face time picker inside an app-styled dialog. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HyperTimePickerDialog(
    title: String,
    initial: LocalTime,
    onDismiss: () -> Unit,
    onConfirm: (LocalTime) -> Unit,
) {
    val c = Hyper.colors
    val state = rememberTimePickerState(initial.hour, initial.minute, is24Hour = false)
    Dialog(onDismissRequest = onDismiss) {
        GlassDialogWindow()
        HyperCard(shape = com.hyperathlete.app.ui.theme.HyperShapes.dialog, color = Hyper.glass.fillStrong) {
            Text(title, style = Hyper.type.title, color = c.text, modifier = Modifier.padding(bottom = 16.dp))
            TimePicker(
                state = state,
                modifier = Modifier.align(Alignment.CenterHorizontally),
                colors = TimePickerDefaults.colors(
                    clockDialColor = c.surfaceAlt,
                    selectorColor = c.accent,
                    containerColor = c.surfaceRaised,
                    periodSelectorBorderColor = c.border,
                    periodSelectorSelectedContainerColor = c.accentSoft,
                    periodSelectorSelectedContentColor = c.accentDeep,
                    periodSelectorUnselectedContentColor = c.textSecondary,
                    timeSelectorSelectedContainerColor = c.accentSoft,
                    timeSelectorSelectedContentColor = c.accentDeep,
                    timeSelectorUnselectedContainerColor = c.surfaceAlt,
                    timeSelectorUnselectedContentColor = c.text,
                    clockDialSelectedContentColor = c.onAccent,
                    clockDialUnselectedContentColor = c.text,
                ),
            )
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextAction(stringResource(R.string.action_cancel), onDismiss)
                TextAction(stringResource(R.string.action_save), {
                    onConfirm(LocalTime.of(state.hour, state.minute))
                    onDismiss()
                }, color = c.accent)
            }
        }
    }
}

/** Confirmation dialog in the app style (primary action orange, cancel grey). */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirm: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    destructive: Boolean = false,
) {
    val c = Hyper.colors
    HyperDialog(onDismiss) {
            Text(title, style = Hyper.type.h2, color = c.text)
            Spacer(Modifier.height(10.dp))
            Text(message, style = Hyper.type.body, color = c.textSecondary)
            Spacer(Modifier.height(22.dp))
            PrimaryButton(confirm, { onConfirm(); onDismiss() })
            Spacer(Modifier.height(8.dp))
            SecondaryButton(stringResource(R.string.action_cancel), onDismiss, content = if (destructive) c.text else c.textSecondary)
    }
}

/**
 * Liquid Glass dialog: a frosted card over the app, which Android 12+ blurs behind the dialog window
 * (cross-window blur). Older phones, or when blur is off in system settings, get the usual dim instead.
 */
@Composable
fun HyperDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Dialog(onDismissRequest = onDismiss, properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
        GlassDialogWindow()
        HyperCard(
            modifier.padding(horizontal = 24.dp).widthIn(max = 420.dp), shape = com.hyperathlete.app.ui.theme.HyperShapes.dialog, color = Hyper.glass.fillStrong,
            padding = androidx.compose.foundation.layout.PaddingValues(22.dp), content = content,
        )
    }
}

/** Asks the dialog's window to blur what is behind it (Android 12+). */
@Composable
fun GlassDialogWindow() {
    val view = androidx.compose.ui.platform.LocalView.current
    val radius = with(androidx.compose.ui.platform.LocalDensity.current) { Hyper.glass.blur.roundToPx() }
    androidx.compose.runtime.SideEffect {
        if (com.hyperathlete.app.ui.theme.GlassBlurSupported) runCatching {
            val window = (view.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window ?: return@runCatching
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            window.attributes = window.attributes.apply { blurBehindRadius = radius }
        }
    }
}
