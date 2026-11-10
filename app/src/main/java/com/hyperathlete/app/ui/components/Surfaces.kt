package com.hyperathlete.app.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hyperathlete.app.R
import com.hyperathlete.app.ui.theme.Hyper
import com.hyperathlete.app.ui.theme.HyperShapes
import com.hyperathlete.app.ui.theme.glass

/**
 * Liquid Glass card: frosted translucent fill, inner sheen, specular rim and a soft shadow (26dp corners).
 * Pass [color] for a tinted card (accent-soft notices); it keeps the sheen and lift. A [borderColor]
 * replaces the specular rim with a plain outline.
 */
@Composable
fun HyperCard(
    modifier: Modifier = Modifier,
    shape: Shape = HyperShapes.card,
    padding: PaddingValues = PaddingValues(18.dp),
    color: Color = Color.Unspecified,
    borderColor: Color = Color.Unspecified,
    elevated: Boolean = true,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val glass = Hyper.glass
    val fill = if (color == Color.Unspecified) glass.fill else color
    val plainEdge = borderColor != Color.Unspecified && borderColor != fill
    Column(
        modifier = modifier
            .glass(shape, glass, color = fill, elevated = elevated, rim = !plainEdge)
            .then(if (plainEdge) Modifier.border(1.dp, borderColor, shape) else Modifier)
            .clip(shape)
            .then(if (onClick != null) Modifier.hyperClickable(onLongClick = onLongClick, onClick = onClick) else Modifier)
            .padding(padding),
        content = content,
    )
}

/** Icon inside a soft rounded tile: the app's standard leading affordance. */
@Composable
fun IconTile(
    @DrawableRes icon: Int,
    modifier: Modifier = Modifier,
    tint: Color = Hyper.colors.accent,
    background: Color = Hyper.colors.accentSoft,
    size: Dp = 44.dp,
    iconSize: Dp = 22.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.34f))
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = tint, modifier = Modifier.size(iconSize))
    }
}

@Composable
fun HyperChip(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Hyper.colors.accent,
    background: Color = Hyper.colors.accentSoft,
    @DrawableRes icon: Int? = null,
) {
    Row(
        modifier = modifier
            .clip(HyperShapes.pill)
            .background(background)
            .padding(start = if (icon != null) 8.dp else 10.dp, end = 10.dp, top = 5.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(painterResource(icon), null, Modifier.size(13.dp), tint = color)
            Spacer(Modifier.width(4.dp))
        }
        Text(text, style = Hyper.type.micro, color = color, maxLines = 1)
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(modifier = modifier.fillMaxWidth().padding(top = 26.dp, bottom = 12.dp), verticalAlignment = Alignment.Bottom) {
        Column(Modifier.weight(1f)) {
            Text(title, style = Hyper.type.h2, color = Hyper.colors.text)
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(subtitle, style = Hyper.type.small, color = Hyper.colors.textMuted)
            }
        }
        if (action != null && onAction != null) TextAction(action, onAction, color = Hyper.colors.accent)
    }
}

@Composable
fun HyperDivider(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(0.8.dp).background(Hyper.glass.hair))
}

/** A row for settings lists: icon tile, title + subtitle, trailing slot (switch, chevron, value). */
@Composable
fun SettingsRow(
    @DrawableRes icon: Int,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    iconTint: Color = Hyper.colors.accent,
    iconBg: Color = Hyper.colors.accentSoft,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    trailing: @Composable RowScope.() -> Unit = {
        if (onClick != null) Icon(
            // "forward" chevron; autoMirrored flips it to point left in RTL.
            painterResource(R.drawable.ic_chevron_right), null,
            tint = Hyper.colors.textMuted, modifier = Modifier.size(20.dp),
        )
    },
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(HyperShapes.cardSmall)
            .then(if (onClick != null) Modifier.hyperClickable(enabled = enabled, scaleOnPress = false, onClick = onClick) else Modifier)
            .padding(horizontal = 4.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconTile(icon, tint = if (enabled) iconTint else Hyper.colors.lock, background = if (enabled) iconBg else Hyper.colors.lockSoft, size = 40.dp, iconSize = 20.dp)
        Column(Modifier.weight(1f)) {
            Text(title, style = Hyper.type.bodyMedium, color = if (enabled) Hyper.colors.text else Hyper.colors.textMuted)
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(subtitle, style = Hyper.type.small, color = Hyper.colors.textMuted)
            }
        }
        trailing()
    }
}

@Composable
fun EmptyState(
    @DrawableRes icon: Int,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IconTile(icon, size = 64.dp, iconSize = 30.dp)
        Spacer(Modifier.height(16.dp))
        Text(title, style = Hyper.type.title, color = Hyper.colors.text, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(body, style = Hyper.type.small, color = Hyper.colors.textSecondary, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        if (action != null && onAction != null) {
            Spacer(Modifier.height(18.dp))
            SmallPillButton(action, onAction)
        }
    }
}
