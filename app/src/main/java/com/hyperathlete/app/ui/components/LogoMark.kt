package com.hyperathlete.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hyperathlete.app.R
import com.hyperathlete.app.ui.theme.Hyper

/** Brand mark: accent squircle (subtle gradient) with the white rising-H. */
@Composable
fun LogoMark(size: Dp, modifier: Modifier = Modifier) {
    val c = Hyper.colors
    val top = lerp(c.accent, androidx.compose.ui.graphics.Color.White, 0.16f)
    val bottom = c.accentDeep
    Box(
        modifier
            .size(size)
            .shadow(if (size >= 56.dp) 14.dp else 4.dp, RoundedCornerShape(size * 0.3f), ambientColor = c.accent, spotColor = c.accent)
            .clip(RoundedCornerShape(size * 0.3f))
            .background(Brush.linearGradient(listOf(top, bottom))),
        contentAlignment = Alignment.Center,
    ) {
        // The mark is authored in a 108dp adaptive-icon canvas; scale it so it fills the squircle nicely.
        Image(painterResource(R.drawable.ic_logo_mark), null, Modifier.fillMaxSize().graphicsLayer { scaleX = 1.45f; scaleY = 1.45f })
    }
}

/** Welcome-screen logo: springs in, then breathes with a soft accent halo. */
@Composable
fun AnimatedLogo(size: Dp, modifier: Modifier = Modifier) {
    val c = Hyper.colors
    val still = LocalInspectionMode.current
    val enter = remember { Animatable(if (still) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!still) enter.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessLow))
    }
    val pulse = rememberInfiniteTransition(label = "logoPulse")
    val halo by pulse.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2200, easing = DecelerateEasing), RepeatMode.Restart),
        label = "halo",
    )
    val float by pulse.animateFloat(
        initialValue = -1f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2600), RepeatMode.Reverse),
        label = "float",
    )
    Box(modifier.size(size * 1.9f), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(size)
                .drawBehind {
                    if (!still) {
                        val r = this.size.minDimension / 2f
                        drawCircle(c.accent.copy(alpha = 0.18f * (1f - halo)), radius = r * (1f + 0.75f * halo))
                    }
                },
        )
        LogoMark(
            size,
            Modifier.graphicsLayer {
                val s = enter.value
                scaleX = 0.6f + 0.4f * s
                scaleY = 0.6f + 0.4f * s
                alpha = s.coerceIn(0f, 1f)
                rotationZ = (1f - s) * -12f
                translationY = if (still) 0f else float * 3.dp.toPx()
            },
        )
    }
}
