package com.hyperathlete.app.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.hyperathlete.app.R
import com.hyperathlete.app.ui.theme.Hyper
import com.hyperathlete.app.ui.theme.HyperShapes
import com.hyperathlete.app.ui.theme.glass
import androidx.compose.material3.minimumInteractiveComponentSize
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.Locale

/** Selectable card with a round check indicator (onboarding / settings choices). */
@Composable
fun OptionCard(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    @DrawableRes icon: Int? = null,
) {
    val c = Hyper.colors
    val border by animateColorAsState(if (selected) c.accent else c.border, tween(240), label = "ocBorder")
    val fill by animateColorAsState(if (selected) c.accentSoft else c.surface, tween(240), label = "ocFill")
    val dot by animateColorAsState(if (selected) c.accent else c.surface, tween(240), label = "ocDot")
    val ring by animateColorAsState(if (selected) c.accent else c.borderStrong, tween(240), label = "ocRing")
    val checkScale by animateFloatAsState(if (selected) 1f else 0f, tween(if (selected) 220 else 120), label = "ocCheck")
    val haptics = rememberHaptics()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(HyperShapes.card)
            .background(fill)
            .border(1.4.dp, border, HyperShapes.card)
            .hyperClickable(role = Role.RadioButton) { haptics.tick(); onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(24.dp).clip(CircleShape).background(dot).border(1.6.dp, ring, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painterResource(R.drawable.ic_check), null, tint = c.onAccent,
                modifier = Modifier.size(15.dp).scale(checkScale),
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = Hyper.type.title, color = c.text)
            if (!subtitle.isNullOrEmpty()) {
                Spacer(Modifier.height(3.dp))
                Text(subtitle, style = Hyper.type.small, color = c.textSecondary)
            }
        }
        if (icon != null) {
            Spacer(Modifier.width(12.dp))
            IconTile(icon, size = 40.dp, iconSize = 20.dp)
        }
    }
}

/** Animated segmented progress bar for multi-step flows; fills from the start edge (right in RTL). */
@Composable
fun StepIndicator(count: Int, step: Int, modifier: Modifier = Modifier) {
    val c = Hyper.colors
    val progress = remember { Animatable(step.toFloat()) }
    LaunchedEffect(step) { progress.animateTo(step.toFloat(), tween(460, easing = DecelerateEasing)) }
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Canvas(modifier.fillMaxWidth().height(6.dp)) {
        val gap = 7.dp.toPx()
        val segW = (size.width - gap * (count - 1)) / count
        val r = CornerRadius(size.height / 2, size.height / 2)
        for (i in 0 until count) {
            val start = if (rtl) size.width - (i + 1) * segW - i * gap else i * (segW + gap)
            drawRoundRect(c.borderStrong, Offset(start, 0f), Size(segW, size.height), r)
            val f = (progress.value - i).coerceIn(0f, 1f)
            if (f > 0f) {
                val w = segW * f
                val x = if (rtl) start + segW - w else start
                drawRoundRect(c.accent, Offset(x, 0f), Size(w, size.height), r)
            }
        }
    }
}

/** Themed toggle (50x30) — orange track when on. */
@Composable
fun HyperSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val c = Hyper.colors
    val haptics = rememberHaptics()
    val track by animateColorAsState(if (checked && enabled) c.accent else if (checked) c.accentLight else c.borderStrong, tween(220), label = "swTrack")
    val thumbOffset by animateDpAsState(if (checked) 20.dp else 0.dp, tween(220, easing = DecelerateEasing), label = "swThumb")
    Box(
        modifier
            .minimumInteractiveComponentSize()
            .size(50.dp, 30.dp)
            .clip(HyperShapes.pill)
            .background(track)
            .hyperClickable(enabled = enabled, role = Role.Switch, scaleOnPress = false) {
                haptics.tick(); onCheckedChange(!checked)
            }
            .padding(3.dp),
    ) {
        Box(
            Modifier
                .offset(x = thumbOffset)
                .size(24.dp)
                .shadow(2.dp, CircleShape)
                .clip(CircleShape)
                .background(Color.White),
        )
    }
}

/** Segmented control with a sliding raised indicator. */
@Composable
fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val c = Hyper.colors
    val haptics = rememberHaptics()
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .clip(HyperShapes.pill)
            .background(Hyper.glass.hair)
            .padding(4.dp),
    ) {
        val cell = maxWidth / options.size
        val x by animateDpAsState(cell * selected, tween(260, easing = DecelerateEasing), label = "segX")
        Box(
            Modifier
                .offset(x = x)
                .width(cell)
                .height(40.dp)
                .glass(HyperShapes.pill, Hyper.glass, color = Hyper.glass.fillStrong, elevated = !c.isDark),
        )
        Row(Modifier.fillMaxWidth().height(40.dp)) {
            options.forEachIndexed { i, label ->
                val on = i == selected
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(HyperShapes.pill)
                        .hyperClickable(role = Role.Tab, scaleOnPress = false) { if (!on) { haptics.tick(); onSelect(i) } },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label, maxLines = 1,
                        style = if (on) Hyper.type.smallMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) else Hyper.type.smallMedium,
                        color = if (on) c.text else c.textSecondary,
                    )
                }
            }
        }
    }
}

/** Round +/- button with press-and-hold auto repeat. */
@Composable
private fun StepButton(@DrawableRes icon: Int, enabled: Boolean, onStep: () -> Unit) {
    val c = Hyper.colors
    val haptics = rememberHaptics()
    val currentStep by rememberUpdatedState(onStep)
    var holding by remember { mutableStateOf(false) }
    LaunchedEffect(holding) {
        if (!holding) return@LaunchedEffect
        delay(420)
        var interval = 120L
        while (isActive && holding) {
            currentStep(); haptics.tick()
            delay(interval)
            interval = (interval * 0.88).toLong().coerceAtLeast(40L)
        }
    }
    Box(
        Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(if (enabled) c.surfaceAlt else c.lockSoft)
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures(
                    onPress = {
                        holding = true
                        tryAwaitRelease()
                        holding = false
                    },
                    onTap = { haptics.tick(); currentStep() },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(icon), null, tint = if (enabled) c.text else c.lock, modifier = Modifier.size(22.dp))
    }
}

/**
 * Number input: big editable value between − and + (hold to repeat).
 * Typing is allowed too, so 67.4 doesn't take a dozen taps.
 */
@Composable
fun NumberStepper(
    value: Double,
    onValueChange: (Double) -> Unit,
    unit: String,
    modifier: Modifier = Modifier,
    step: Double = 0.5,
    min: Double = 0.0,
    max: Double = 999.0,
    decimals: Int = 1,
) {
    val c = Hyper.colors
    fun fmt(v: Double) = if (decimals == 0) v.toLong().toString() else String.format(Locale.US, "%.${decimals}f", v)
    var text by remember { mutableStateOf(fmt(value)) }
    var focused by remember { mutableStateOf(false) }
    LaunchedEffect(value) { if (!focused) text = fmt(value) }
    Row(
        modifier
            .fillMaxWidth()
            .clip(HyperShapes.card)
            .background(c.surface)
            .border(1.dp, c.border, HyperShapes.card)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepButton(R.drawable.ic_remove, value - step >= min - 1e-9) { onValueChange(Math.round((value - step).coerceIn(min, max) * 10) / 10.0) }
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.Bottom) {
            BasicTextField(
                value = text,
                onValueChange = { raw ->
                    val cleaned = raw.replace(',', '.').filter { it.isDigit() || it == '.' }.take(6)
                    text = cleaned
                    cleaned.toDoubleOrNull()?.let { if (it in min..max) onValueChange(it) }
                },
                singleLine = true,
                textStyle = Hyper.type.number.copy(fontSize = Hyper.type.h1.fontSize * 1.25f, color = c.text, textAlign = TextAlign.Center),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                cursorBrush = SolidColor(c.accent),
                modifier = Modifier
                    .width(110.dp)
                    .onFocusChanged {
                        focused = it.isFocused
                        if (!it.isFocused) text = fmt(value)
                    },
            )
            Text(unit, style = Hyper.type.bodyMedium, color = c.textSecondary, modifier = Modifier.padding(bottom = 6.dp))
        }
        StepButton(R.drawable.ic_add, value + step <= max + 1e-9) { onValueChange(Math.round((value + step).coerceIn(min, max) * 10) / 10.0) }
    }
}

/** Filled text field in the app's style (no Material underline). */
@Composable
fun HyperTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    @DrawableRes leadingIcon: Int? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Done,
    textStyle: TextStyle = Hyper.type.title,
    /** Called when the keyboard's action button (Send / Done) is pressed. */
    onSend: (() -> Unit)? = null,
) {
    val c = Hyper.colors
    var focused by remember { mutableStateOf(false) }
    val border by animateColorAsState(if (focused) c.accent else c.border, tween(200), label = "tfBorder")
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = textStyle.copy(color = c.text),
        cursorBrush = SolidColor(c.accent),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        keyboardActions = if (onSend == null) androidx.compose.foundation.text.KeyboardActions.Default else androidx.compose.foundation.text.KeyboardActions(onSend = { onSend() }, onDone = { onSend() }),
        modifier = modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused },
        decorationBox = { inner ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(HyperShapes.cardSmall)
                    .background(c.surface)
                    .border(1.4.dp, border, HyperShapes.cardSmall)
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (leadingIcon != null) {
                    Icon(painterResource(leadingIcon), null, tint = if (focused) c.accent else c.textMuted, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                }
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty()) Text(placeholder, style = textStyle.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Normal), color = c.textMuted)
                    inner()
                }
            }
        },
    )
}

/** Tappable field that shows a value and opens a picker (date / time). */
@Composable
fun PickerField(
    value: String,
    placeholder: String,
    @DrawableRes icon: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: String? = null,
) {
    val c = Hyper.colors
    Row(
        modifier
            .fillMaxWidth()
            .clip(HyperShapes.cardSmall)
            .background(c.surface)
            .border(1.4.dp, c.border, HyperShapes.cardSmall)
            .hyperClickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(icon), null, tint = c.textMuted, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            value.ifEmpty { placeholder }, Modifier.weight(1f),
            style = if (value.isEmpty()) Hyper.type.title.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Normal) else Hyper.type.title,
            color = if (value.isEmpty()) c.textMuted else c.text,
        )
        if (trailing != null) HyperChip(trailing)
    }
}

/** Thin accent track with a white thumb (same feel as [HyperSwitch]); RTL-aware, tap or drag. */
@Composable
fun HyperSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    range: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
) {
    val c = Hyper.colors
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val haptics = rememberHaptics()
    val current by rememberUpdatedState(onValueChange)
    val fraction = ((value - range.start) / (range.endInclusive - range.start)).coerceIn(0f, 1f)
    fun valueAt(x: Float, width: Float, thumb: Float): Float {
        val usable = (width - thumb).coerceAtLeast(1f)
        var f = ((x - thumb / 2) / usable).coerceIn(0f, 1f)
        if (rtl) f = 1f - f
        return range.start + f * (range.endInclusive - range.start)
    }
    androidx.compose.foundation.layout.BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(32.dp)
            .pointerInput(rtl, range) {
                detectTapGestures { pos -> haptics.tick(); current(valueAt(pos.x, size.width.toFloat(), 24.dp.toPx())) }
            }
            .pointerInput(rtl, range) {
                detectHorizontalDragGestures { change, _ ->
                    change.consume()
                    current(valueAt(change.position.x, size.width.toFloat(), 24.dp.toPx()))
                }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        val travel = maxWidth - 24.dp
        Box(Modifier.fillMaxWidth().padding(horizontal = 12.dp).height(6.dp).clip(HyperShapes.pill).background(c.borderStrong)) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(fraction).clip(HyperShapes.pill).background(c.accent))
        }
        Box(
            Modifier
                .offset(x = travel * fraction)
                .size(24.dp)
                .shadow(3.dp, CircleShape)
                .clip(CircleShape)
                .background(Color.White)
                .border(2.dp, c.accent, CircleShape),
        )
    }
}

/** [NumberStepper] for a weight kept in kg but shown and typed in the user's unit (kg or lb). */
@Composable
fun WeightStepper(
    kg: Double,
    onKg: (Double) -> Unit,
    modifier: Modifier = Modifier,
    stepKg: Double = 0.5,
    minKg: Double = 0.0,
    maxKg: Double = 999.0,
    decimals: Int = 1,
) {
    val u = com.hyperathlete.app.ui.theme.LocalWeightUnit.current
    val w = com.hyperathlete.app.domain.model.Weights
    val lb = u == com.hyperathlete.app.domain.model.WeightUnit.LB
    NumberStepper(
        value = Math.round(w.show(kg, u) * 10) / 10.0,
        onValueChange = { onKg(w.toKg(it, u)) },
        unit = w.label(u),
        modifier = modifier,
        step = if (lb) (if (stepKg < 0.5) 0.2 else 1.0) else stepKg,
        min = kotlin.math.floor(w.show(minKg, u)),
        max = kotlin.math.ceil(w.show(maxKg, u)),
        decimals = decimals,
    )
}
