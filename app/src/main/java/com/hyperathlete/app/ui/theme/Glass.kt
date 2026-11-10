package com.hyperathlete.app.ui.theme

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Liquid Glass tokens, all in one place (after the Saja site: `--glass`, `--edge`, `--shadow`, `--r`, blur 22px).
 *
 * - [fill] / [fillStrong]: frosted surface colours (cards / bars, sheets and dialogs).
 * - [tint]: the inner sheen at the top-left of every pane.
 * - [edgeHigh] / [edgeLow]: the specular rim, bright at the top-left and bottom-right corners.
 * - [hair]: hairline separators and quiet fills on glass.
 * - [canvas] + [blobs]: the page backdrop the glass sits on (soft colour fields from the accent colours).
 * - [blur]: backdrop blur for floating panes (Android 12+), [elevation] / [shadow]: their lift.
 *
 * Text colours that sit on glass are chosen in [hyperColors] to stay at WCAG AA (4.5:1) on these fills.
 */
@Immutable
data class HyperGlass(
    val fill: Color,
    val fillStrong: Color,
    val tint: Color,
    val edgeHigh: Color,
    val edgeLow: Color,
    val hair: Color,
    val canvas: Color,
    val blobs: List<Color>,
    val shadow: Color,
    val elevation: Dp = 10.dp,
    val blur: Dp = 22.dp,
    val rim: Dp = 1.dp,
    val radius: Dp = 26.dp,
    val radiusSmall: Dp = 18.dp,
    val radiusSheet: Dp = 34.dp,
)

fun hyperGlass(dark: Boolean, accent: Color = Palette.Orange, secondary: Color = Palette.Blue): HyperGlass = if (!dark) {
    HyperGlass(
        fill = Color.White.copy(alpha = 0.64f),
        fillStrong = Color.White.copy(alpha = 0.84f),
        tint = Color.White.copy(alpha = 0.5f),
        edgeHigh = Color.White.copy(alpha = 0.95f),
        edgeLow = Color.White.copy(alpha = 0.3f),
        hair = Color(0x14141428),
        canvas = Color(0xFFF2F3F8),
        blobs = listOf(accent.copy(alpha = 0.12f), secondary.copy(alpha = 0.084f), lerp(accent, Color(0xFFFFB86B), 0.6f).copy(alpha = 0.08f)),
        shadow = Color(0x2E1E1E46),
    )
} else {
    HyperGlass(
        fill = Color(0xFF2C2C36).copy(alpha = 0.56f),
        fillStrong = Color(0xFF1C1C22).copy(alpha = 0.86f),
        tint = Color.White.copy(alpha = 0.07f),
        edgeHigh = Color.White.copy(alpha = 0.34f),
        edgeLow = Color.White.copy(alpha = 0.06f),
        hair = Color(0x14FFFFFF),
        canvas = Color(0xFF08090C),
        blobs = listOf(accent.copy(alpha = 0.14f), secondary.copy(alpha = 0.1f), Color(0xFFFF5E8A).copy(alpha = 0.07f)),
        shadow = Color(0x73000000),
        elevation = 14.dp,
    )
}

val LocalHyperGlass = staticCompositionLocalOf { hyperGlass(dark = false) }

/** Room the floating tab bar takes at the bottom of the main pages; scrolling lists pad by this much. */
val LocalFloatingBarInset = staticCompositionLocalOf { 0.dp }

/** Real backdrop blur needs RenderEffect (Android 12, API 31). Older phones get a frosted gradient instead. */
val GlassBlurSupported: Boolean get() = Build.VERSION.SDK_INT >= 31

/** Specular rim: light caught at the top-left and bottom-right edges, like a lens. */
private fun rimBrush(g: HyperGlass, width: Float, height: Float) = Brush.linearGradient(
    0f to g.edgeHigh, 0.35f to g.edgeLow, 0.65f to g.edgeLow, 1f to g.edgeHigh,
    start = Offset.Zero, end = Offset(width, height * 1.2f),
)

private fun Outline.path(): Path = when (this) {
    is Outline.Rectangle -> Path().apply { addRect(rect) }
    is Outline.Rounded -> Path().apply { addRoundRect(roundRect) }
    is Outline.Generic -> path
}

/**
 * Soft shadow outside the shape only (like CSS box-shadow): an elevation shadow would show through the
 * translucent glass as a lighter inner rectangle. Two layers, after Saja's `--shadow`.
 */
private fun DrawScope.glassShadow(outline: Outline, glass: HyperGlass) {
    val path = outline.path()
    val e = glass.elevation.toPx()
    drawIntoCanvas { canvas ->
        canvas.save()
        canvas.clipPath(path, ClipOp.Difference)
        val paint = Paint().asFrameworkPaint()
        paint.isAntiAlias = true
        listOf(Triple(e * 1.7f, e * 0.6f, 1f), Triple(e * 0.3f, e * 0.1f, 0.5f)).forEach { (blur, dy, k) ->
            paint.color = glass.shadow.copy(alpha = glass.shadow.alpha * k).toArgb()
            paint.maskFilter = android.graphics.BlurMaskFilter(blur.coerceAtLeast(0.5f), android.graphics.BlurMaskFilter.Blur.NORMAL)
            canvas.nativeCanvas.save()
            canvas.nativeCanvas.translate(0f, dy)
            canvas.nativeCanvas.drawPath(path.asAndroidPath(), paint)
            canvas.nativeCanvas.restore()
        }
        canvas.restore()
    }
}

/**
 * A frosted glass pane: soft shadow, translucent fill (or [color]), inner sheen and specular rim.
 * Visual only: it does not change size or layout.
 */
fun Modifier.glass(
    shape: Shape,
    glass: HyperGlass,
    color: Color = glass.fill,
    elevated: Boolean = true,
    rim: Boolean = true,
): Modifier = this
    .drawBehind {
        val outline = shape.createOutline(size, layoutDirection, this)
        if (elevated) glassShadow(outline, glass)
        drawOutline(outline, color)
        drawOutline(
            outline,
            Brush.radialGradient(listOf(glass.tint, Color.Transparent), center = Offset(size.width * 0.2f, 0f), radius = size.maxDimension * 0.75f),
        )
    }
    .drawWithContent {
        drawContent()
        if (rim) {
            val outline = shape.createOutline(size, layoutDirection, this)
            drawOutline(outline, rimBrush(glass, size.width, size.height), style = Stroke(glass.rim.toPx()))
        }
    }

/** The page backdrop glass sits on: the canvas colour with soft colour fields (same on every Android version). */
fun Modifier.glassBackdrop(glass: HyperGlass): Modifier = drawBehind {
    drawRect(glass.canvas)
    val m = size.maxDimension
    val spots = listOf(Offset(size.width * 0.05f, size.height * 0.02f), Offset(size.width * 1.0f, size.height * 0.45f), Offset(size.width * 0.35f, size.height * 1.0f))
    val radii = listOf(m * 0.75f, m * 0.65f, m * 0.55f)
    glass.blobs.forEachIndexed { i, col ->
        drawCircle(Brush.radialGradient(listOf(col, col.copy(alpha = 0f)), center = spots[i], radius = radii[i]), radii[i], spots[i])
    }
}

/**
 * Captures what is drawn under floating glass (the tab bar) so the bar can show it blurred.
 * [source] records the content; [pane] draws the blurred copy behind the bar on Android 12+.
 */
@Stable
class GlassBackdrop internal constructor(internal val content: GraphicsLayer, internal val blurred: GraphicsLayer) {
    internal var sourceOrigin by mutableStateOf(Offset.Zero)
    internal var paneOrigin by mutableStateOf(Offset.Zero)
}

@Composable
fun rememberGlassBackdrop(): GlassBackdrop {
    val content = rememberGraphicsLayer()
    val blurred = rememberGraphicsLayer()
    return remember(content, blurred) { GlassBackdrop(content, blurred) }
}

/** Marks the content that floating glass blurs. Draws it exactly as before. */
fun Modifier.glassSource(backdrop: GlassBackdrop, enabled: Boolean = GlassBlurSupported): Modifier =
    if (!enabled) this else this
        .onGloballyPositioned { backdrop.sourceOrigin = it.positionInRoot() }
        .drawWithContent {
            backdrop.content.record { this@drawWithContent.drawContent() }
            drawLayer(backdrop.content)
        }

/**
 * Floating glass: the content under this pane, blurred by [HyperGlass.blur] (RenderEffect, Android 12+),
 * then the frosted fill, sheen and rim. Below Android 12 it falls back to a near-opaque fill with a soft
 * vertical gradient so text stays readable without blur.
 */
fun Modifier.glassPane(
    shape: Shape,
    glass: HyperGlass,
    backdrop: GlassBackdrop?,
    blurEnabled: Boolean = GlassBlurSupported,
): Modifier {
    val blur = blurEnabled && backdrop != null
    return this
        .then(if (backdrop != null) Modifier.onGloballyPositioned { backdrop.paneOrigin = it.positionInRoot() } else Modifier)
        .drawBehind {
            val outline = shape.createOutline(size, layoutDirection, this)
            glassShadow(outline, glass)
            if (blur && backdrop != null) {
                val r = glass.blur.toPx()
                backdrop.blurred.renderEffect = BlurEffect(r, r, TileMode.Clamp)
                backdrop.blurred.record(backdrop.content.size) { drawLayer(backdrop.content) }
                clipPath(outline.path()) {
                    val o = backdrop.sourceOrigin - backdrop.paneOrigin
                    translate(o.x, o.y) { drawLayer(backdrop.blurred) }
                }
                // Labels sit on this pane: keep it dense enough for AA contrast even over busy content.
                drawOutline(outline, glass.fillStrong.copy(alpha = glass.fillStrong.alpha * 0.86f))
            } else {
                drawOutline(outline, glass.fillStrong)
                drawOutline(outline, Brush.verticalGradient(listOf(glass.tint, Color.Transparent)))
            }
            drawOutline(
                outline,
                Brush.radialGradient(listOf(glass.tint, Color.Transparent), center = Offset(size.width * 0.2f, 0f), radius = size.maxDimension * 0.6f),
            )
        }
        .drawWithContent {
            drawContent()
            val outline = shape.createOutline(size, layoutDirection, this)
            drawOutline(outline, rimBrush(glass, size.width, size.height), style = Stroke(glass.rim.toPx()))
        }
}
