package com.hyperathlete.app.ui.main

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hyperathlete.app.R
import com.hyperathlete.app.domain.layout.BoxId
import com.hyperathlete.app.domain.layout.PageLayout
import com.hyperathlete.app.ui.components.hyperClickable
import com.hyperathlete.app.ui.components.rememberHaptics
import com.hyperathlete.app.ui.theme.Hyper
import com.hyperathlete.app.ui.theme.HyperShapes
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

/**
 * A page made of movable boxes. Normal mode shows the visible boxes in the saved order.
 * Edit mode shows every box with a handle and an eye toggle; long-press a box and drag it up or down.
 * The new order is saved when the finger lifts.
 */
@Composable
fun EditableBoxList(
    layout: PageLayout,
    editMode: Boolean,
    onLayoutChange: (PageLayout) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    header: LazyListScope.() -> Unit = {},
    footer: LazyListScope.() -> Unit = {},
    box: @Composable (id: BoxId, interactive: Boolean) -> Unit,
) {
    val haptics = rememberHaptics()
    var working by remember(layout) { mutableStateOf(layout) }
    val listState = rememberLazyListState()
    val reorder = rememberReorderableLazyListState(listState) { from, to ->
        val f = from.key as? BoxId
        val t = to.key as? BoxId
        if (f != null && t != null) {
            working = working.move(f, t)
            haptics.tick()
        }
    }
    val items = if (editMode) working.order else working.visible

    val barInset = com.hyperathlete.app.ui.theme.LocalFloatingBarInset.current
    val dir = androidx.compose.ui.platform.LocalLayoutDirection.current
    val padding = if (barInset <= 0.dp) contentPadding else PaddingValues(
        start = contentPadding.calculateStartPadding(dir), end = contentPadding.calculateEndPadding(dir),
        top = contentPadding.calculateTopPadding(), bottom = maxOf(contentPadding.calculateBottomPadding(), barInset + 16.dp),
    )
    LazyColumn(modifier, state = listState, contentPadding = padding) {
        header()
        items.forEach { id ->
            item(key = id) {
                ReorderableItem(reorder, key = id, enabled = editMode) { dragging ->
                    val hidden = id in working.hidden
                    if (!editMode) {
                        Box(Modifier.padding(bottom = 12.dp)) { box(id, true) }
                    } else {
                        val lift by animateFloatAsState(if (dragging) 1.03f else 1f, tween(160), label = "lift")
                        val elevation by animateDpAsState(if (dragging) 16.dp else 0.dp, tween(160), label = "elev")
                        val border by animateColorAsState(
                            if (dragging) Hyper.colors.accent else Hyper.colors.accent.copy(alpha = 0.45f), tween(160), label = "editBorder",
                        )
                        Box(
                            Modifier
                                .padding(bottom = 12.dp)
                                .graphicsLayer { scaleX = lift; scaleY = lift }
                                .shadow(elevation, HyperShapes.card, ambientColor = Hyper.colors.accent, spotColor = Hyper.colors.accent)
                                .longPressDraggableHandle(
                                    onDragStarted = { haptics.longPress() },
                                    onDragStopped = { onLayoutChange(working) },
                                ),
                        ) {
                            Box(Modifier.graphicsLayer { alpha = if (hidden) 0.38f else 1f }) { box(id, false) }
                            // Edit chrome: accent outline + controls.
                            Box(Modifier.matchParentSize().clip(HyperShapes.card).border(1.6.dp, border, HyperShapes.card))
                            EditControls(
                                hidden = hidden,
                                onToggle = {
                                    working = working.toggle(id)
                                    onLayoutChange(working)
                                },
                                modifier = Modifier.align(Alignment.TopEnd).padding(10.dp),
                            )
                        }
                    }
                }
            }
        }
        footer()
    }
}

@Composable
private fun EditControls(hidden: Boolean, onToggle: () -> Unit, modifier: Modifier) {
    val c = Hyper.colors
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(34.dp).clip(CircleShape).background(if (hidden) c.surfaceAlt else c.accentSoft)
                .hyperClickable(onClick = onToggle),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painterResource(if (hidden) R.drawable.ic_visibility_off else R.drawable.ic_visibility),
                stringResource(if (hidden) R.string.edit_show else R.string.edit_hide),
                tint = if (hidden) c.textMuted else c.accent, modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.width(6.dp))
        Box(Modifier.size(34.dp).clip(CircleShape).background(c.surfaceAlt), contentAlignment = Alignment.Center) {
            Icon(painterResource(R.drawable.ic_drag_indicator), null, tint = c.textSecondary, modifier = Modifier.size(18.dp))
        }
    }
}

/** Banner shown on top of the page while customising. */
@Composable
fun EditModeBar(onReset: () -> Unit, onDone: () -> Unit, modifier: Modifier = Modifier) {
    val c = Hyper.colors
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().clip(HyperShapes.card).background(c.accentSoft).padding(start = 14.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painterResource(R.drawable.ic_dashboard_customize), null, tint = c.accent, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.edit_title), style = Hyper.type.title, color = c.text)
                Text(stringResource(R.string.edit_hint), style = Hyper.type.micro, color = c.textSecondary)
            }
            Text(
                stringResource(R.string.action_reset), style = Hyper.type.smallMedium, color = c.textSecondary,
                modifier = Modifier.clip(HyperShapes.pill).hyperClickable(onClick = onReset).padding(horizontal = 10.dp, vertical = 8.dp),
            )
            Text(
                stringResource(R.string.action_done), style = Hyper.type.smallMedium, color = c.onAccent,
                modifier = Modifier.clip(HyperShapes.pill).background(c.accent).hyperClickable(onClick = onDone)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
}
