package com.hyperathlete.app.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hyperathlete.app.ui.theme.Hyper
import com.hyperathlete.app.ui.theme.HyperShapes
import com.hyperathlete.app.ui.theme.glass
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke

/** Orange pill: the one colour reserved for primary actions (start, save, confirm). */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    @DrawableRes icon: Int? = null,
) {
    val c = Hyper.colors
    val haptics = rememberHaptics()
    val bg by animateColorAsState(if (enabled) c.accent else c.lockSoft, tween(220), label = "btnBg")
    val fg by animateColorAsState(if (enabled) c.onAccent else c.lock, tween(220), label = "btnFg")
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .then(
                if (enabled) Modifier.shadow(
                    elevation = 10.dp, shape = HyperShapes.pill,
                    ambientColor = c.accent.copy(alpha = 0.38f), spotColor = c.accent.copy(alpha = 0.5f),
                ) else Modifier,
            )
            .clip(HyperShapes.pill)
            .background(bg)
            // Liquid Glass: light falls from the top, with a thin specular line along the upper edge.
            .then(
                if (enabled) Modifier.drawWithContent {
                    drawRect(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.22f), Color.Transparent, Color.Black.copy(alpha = 0.06f))))
                    drawContent()
                    val outline = HyperShapes.pill.createOutline(size, layoutDirection, this)
                    drawOutline(outline, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.55f), Color.Transparent), endY = size.height * 0.5f), style = Stroke(1.dp.toPx()))
                } else Modifier,
            )
            .hyperClickable(enabled = enabled && !loading) { haptics.confirm(); onClick() }
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (loading) {
            CircularProgressIndicator(Modifier.size(20.dp), color = fg, strokeWidth = 2.4.dp)
        } else {
            if (icon != null) {
                Icon(painterResource(icon), null, Modifier.size(20.dp), tint = fg)
                Spacer(Modifier.width(8.dp))
            }
            Text(text, style = Hyper.type.button, color = fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** Neutral filled pill (grey in light mode) for secondary choices like "Back" or "Skip". */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    @DrawableRes icon: Int? = null,
    container: Color = Color.Unspecified,
    content: Color = Hyper.colors.text,
) {
    val glass = Hyper.glass
    val plain = container == Color.Unspecified
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .then(if (plain) Modifier.glass(HyperShapes.pill, glass, color = glass.fill, elevated = false) else Modifier)
            .clip(HyperShapes.pill)
            .background(if (plain) Color.Transparent else container)
            .hyperClickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val tint = if (enabled) content else Hyper.colors.lock
        if (icon != null) {
            Icon(painterResource(icon), null, Modifier.size(20.dp), tint = tint)
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = Hyper.type.button.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Medium), color = tint, maxLines = 1)
    }
}

/** Borderless text action, e.g. "Skip" in a top bar or under a primary button. */
@Composable
fun TextAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Hyper.colors.textSecondary,
) {
    Text(
        text = text,
        style = Hyper.type.bodyMedium,
        color = color,
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(HyperShapes.pill)
            .hyperClickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    )
}

/** Small pill button used inside cards ("Allow", "Open"). */
@Composable
fun SmallPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    filled: Boolean = true,
    enabled: Boolean = true,
) {
    val c = Hyper.colors
    val haptics = rememberHaptics()
    Text(
        text = text,
        style = Hyper.type.smallMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
        color = when {
            !enabled -> c.lock
            filled -> c.onAccent
            else -> c.accent
        },
        maxLines = 1,
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(HyperShapes.pill)
            .background(
                when {
                    !enabled -> c.lockSoft
                    filled -> c.accent
                    else -> c.accentSoft
                },
            )
            .hyperClickable(enabled = enabled) { haptics.click(); onClick() }
            .padding(horizontal = 16.dp, vertical = 9.dp),
    )
}
