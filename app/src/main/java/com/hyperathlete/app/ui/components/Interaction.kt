package com.hyperathlete.app.ui.components

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.hyperathlete.app.ui.theme.Hyper
import com.hyperathlete.app.ui.theme.LocalHapticsEnabled

/** Light haptics, honouring the user's vibration setting. */
@Stable
class Haptics(private val view: View, private val enabled: Boolean) {
    private fun play(constant: Int) {
        if (enabled) view.performHapticFeedback(constant)
    }

    fun tick() = play(HapticFeedbackConstants.CLOCK_TICK)
    fun click() = play(HapticFeedbackConstants.CONTEXT_CLICK)
    fun longPress() = play(HapticFeedbackConstants.LONG_PRESS)
    fun confirm() = play(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.CONFIRM
        else HapticFeedbackConstants.VIRTUAL_KEY,
    )
    fun reject() = play(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.REJECT
        else HapticFeedbackConstants.LONG_PRESS,
    )
}

@Composable
fun rememberHaptics(): Haptics {
    val view = LocalView.current
    val enabled = LocalHapticsEnabled.current
    return remember(view, enabled) { Haptics(view, enabled) }
}

/** The reference app's signature press: scale to 0.972 in 90ms, release in 140ms. */
fun Modifier.pressScale(interactionSource: MutableInteractionSource, pressedScale: Float = 0.972f): Modifier =
    composed {
        val pressed by interactionSource.collectIsPressedAsState()
        val scale by animateFloatAsState(
            targetValue = if (pressed) pressedScale else 1f,
            animationSpec = tween(if (pressed) 90 else 140, easing = DecelerateEasing),
            label = "pressScale",
        )
        graphicsLayer { scaleX = scale; scaleY = scale }
    }

/** Click + long-click with press-scale, themed ripple and haptics. */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.hyperClickable(
    enabled: Boolean = true,
    role: Role? = Role.Button,
    scaleOnPress: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
): Modifier = composed {
    val source = remember { MutableInteractionSource() }
    val haptics = rememberHaptics()
    this
        .then(if (scaleOnPress) Modifier.pressScale(source) else Modifier)
        .combinedClickable(
            interactionSource = source,
            indication = ripple(color = Hyper.colors.accent),
            enabled = enabled,
            role = role,
            onLongClick = onLongClick?.let { lc -> { haptics.longPress(); lc() } },
            onClick = onClick,
        )
}

val DecelerateEasing = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)
val EmphasizedEasing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)

/**
 * Fade + rise entry, staggered by [index] (40ms per step, 320ms), like the reference app.
 * Runs once per composition of the element; skipped in previews/screenshot tests.
 */
fun Modifier.enterAnimation(index: Int = 0, rise: Int = 14): Modifier = composed {
    if (LocalInspectionMode.current) return@composed this
    val alpha = remember { Animatable(0f) }
    val offset = remember { Animatable(1f) }
    val risePx = with(LocalDensity.current) { rise.dp.toPx() }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(40L * index)
        launch { alpha.animateTo(1f, tween(320, easing = DecelerateEasing)) }
        launch { offset.animateTo(0f, tween(320, easing = DecelerateEasing)) }
    }
    graphicsLayer {
        this.alpha = alpha.value
        translationY = offset.value * risePx
    }
}

/** Soft fade drawn just above a pinned bottom bar so scrolling content dissolves instead of being cut. */
fun Modifier.topFade(color: androidx.compose.ui.graphics.Color, height: androidx.compose.ui.unit.Dp = 24.dp): Modifier =
    this.drawBehind {
        val h = height.toPx()
        drawRect(
            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                listOf(color.copy(alpha = 0f), color), startY = -h, endY = 0f,
            ),
            topLeft = androidx.compose.ui.geometry.Offset(0f, -h),
            size = androidx.compose.ui.geometry.Size(size.width, h),
        )
        drawRect(color)
    }
