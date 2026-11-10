package com.hyperathlete.app.ui.progress

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hyperathlete.app.domain.progress.Muscle
import com.hyperathlete.app.domain.progress.Point
import com.hyperathlete.app.domain.streak.DayMark
import com.hyperathlete.app.ui.theme.Hyper
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.max
import kotlin.math.min

/** One line on a [LineChart]. [dashed] lines are references (targets, averages of others). */
data class ChartLine(val points: List<Point>, val color: Color, val width: Dp = 2.5.dp, val dots: Boolean = true, val dashed: Boolean = false)

/** 0 → 1 once when shown (instant in previews / screenshots), so lines draw themselves and numbers count up. */
@Composable
fun rememberReveal(key: Any?, durationMs: Int = 900): Float {
    val still = LocalInspectionMode.current
    val a = remember(key) { Animatable(if (still) 1f else 0f) }
    LaunchedEffect(key) { if (!still) a.animateTo(1f, tween(durationMs, easing = FastOutSlowInEasing)) }
    return a.value
}

/**
 * Small time-series chart. Time always runs left → right (also in Arabic), y-axis labels on the left,
 * first / last date under the plot. [band] shades a "good zone" (e.g. knee pain 0–3).
 */
@Composable
fun LineChart(
    lines: List<ChartLine>,
    modifier: Modifier = Modifier,
    height: Dp = 150.dp,
    from: LocalDate? = null,
    to: LocalDate? = null,
    minY: Double? = null,
    maxY: Double? = null,
    band: ClosedFloatingPointRange<Double>? = null,
    yLabel: ((Double) -> String)? = null,
) {
    val c = Hyper.colors
    val pts = lines.flatMap { it.points }
    val reveal = rememberReveal(pts.hashCode())
    val measurer = rememberTextMeasurer()
    val locale = LocalConfiguration.current.locales[0]
    val labelStyle = TextStyle(fontSize = Hyper.type.micro.fontSize, color = c.textMuted, fontFamily = Hyper.type.micro.fontFamily)
    val grid = c.border
    val bandColor = c.success.copy(alpha = 0.08f)
    val surface = c.surface
    if (pts.isEmpty()) return
    val x0 = (from ?: pts.minOf { it.date }).toEpochDay().toDouble()
    val x1 = max((to ?: pts.maxOf { it.date }).toEpochDay().toDouble(), x0 + 1)
    var lo = minY ?: pts.minOf { it.value }
    var hi = maxY ?: pts.maxOf { it.value }
    if (hi - lo < 1e-6) { lo -= 1; hi += 1 }
    val pad = (hi - lo) * 0.12
    if (minY == null) lo -= pad
    if (maxY == null) hi += pad
    val fmt = DateTimeFormatter.ofPattern("d MMM", locale)
    val digits = if (hi - lo < 2) 2 else if (hi - lo < 20) 1 else 0
    val label: (Double) -> String = yLabel ?: { v -> String.format(java.util.Locale.US, "%.${digits}f", v) }
    Box(modifier.fillMaxWidth().height(height)) {
        Canvas(Modifier.fillMaxWidth().height(height)) {
            val left = 34.dp.toPx()
            val bottom = 18.dp.toPx()
            val top = 6.dp.toPx()
            val w = size.width - left - 4.dp.toPx()
            val h = size.height - bottom - top
            fun xOf(d: LocalDate) = left + ((d.toEpochDay() - x0) / (x1 - x0) * w).toFloat()
            fun yOf(v: Double) = top + (h - ((v - lo) / (hi - lo) * h)).toFloat()
            // grid + labels
            for (i in 0..2) {
                val v = lo + (hi - lo) * i / 2
                val y = yOf(v)
                drawLine(grid, Offset(left, y), Offset(left + w, y), strokeWidth = 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)))
                val t = measurer.measure(com.hyperathlete.app.core.locale.Bidi.ltr(label(v)), labelStyle)
                drawText(t, topLeft = Offset(left - t.size.width - 6.dp.toPx(), y - t.size.height / 2))
            }
            band?.let { b ->
                val yt = yOf(min(b.endInclusive, hi)); val yb = yOf(max(b.start, lo))
                drawRect(bandColor, Offset(left, yt), Size(w, yb - yt))
            }
            val d0 = LocalDate.ofEpochDay(x0.toLong())
            val d1 = LocalDate.ofEpochDay(x1.toLong())
            val t0 = measurer.measure(d0.format(fmt), labelStyle)
            val t1 = measurer.measure(d1.format(fmt), labelStyle)
            drawText(t0, topLeft = Offset(left, size.height - t0.size.height))
            drawText(t1, topLeft = Offset(left + w - t1.size.width, size.height - t1.size.height))
            clipRect(right = left + w * reveal + 8.dp.toPx()) {
                for (line in lines) {
                    if (line.points.isEmpty()) continue
                    val sorted = line.points.sortedBy { it.date }
                    val path = Path()
                    sorted.forEachIndexed { i, p ->
                        val o = Offset(xOf(p.date), yOf(p.value))
                        if (i == 0) path.moveTo(o.x, o.y) else path.lineTo(o.x, o.y)
                    }
                    drawPath(
                        path, line.color,
                        style = Stroke(
                            line.width.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round,
                            pathEffect = if (line.dashed) PathEffect.dashPathEffect(floatArrayOf(10f, 8f)) else null,
                        ),
                    )
                    if (line.dots && sorted.size <= 40) sorted.forEach { p ->
                        val o = Offset(xOf(p.date), yOf(p.value))
                        drawCircle(surface, 4.5.dp.toPx(), o)
                        drawCircle(line.color, 3.dp.toPx(), o)
                    }
                }
            }
        }
    }
}

/** Placeholder line for an empty chart. */
@Composable
fun NoData(text: String, height: Dp = 90.dp) {
    Box(Modifier.fillMaxWidth().height(height), contentAlignment = Alignment.Center) {
        Text(text, style = Hyper.type.small, color = Hyper.colors.textMuted)
    }
}

/** GitHub-style grid: one column per week (Sunday on top), newest week at the end. */
@Composable
fun Heatmap(marks: List<Pair<LocalDate, DayMark>>, modifier: Modifier = Modifier, cell: Dp = 14.dp, gap: Dp = 3.dp) {
    val c = Hyper.colors
    if (marks.isEmpty()) return
    val first = marks.first().first
    val start = first.minusDays((first.dayOfWeek.value % 7).toLong())
    val weeks = ((marks.last().first.toEpochDay() - start.toEpochDay()) / 7 + 1).toInt()
    val reveal = rememberReveal(marks.size, 700)
    val map = marks.toMap()
    Canvas(modifier.height(cell * 7 + gap * 6)) {
        val g = gap.toPx()
        // Cells grow to fill the width (up to 1.4× when there are few weeks); newest week on the right.
        val cs = min(cell.toPx() * 1.4f, max(cell.toPx(), (size.width + g) / weeks - g)).let { min(it, (size.height - 6 * g) / 7f) }
        val total = weeks * (cs + g) - g
        val offset = size.width - total
        for (wk in 0 until weeks) for (day in 0 until 7) {
            val d = start.plusDays((wk * 7 + day).toLong())
            val m = map[d] ?: continue
            val x = offset + wk * (cs + g)
            if (x + cs < 0) continue
            val col = when (m) {
                DayMark.DONE -> c.accent
                DayMark.REST -> c.successSoft
                DayMark.MISSED -> c.dangerSoft
                DayMark.TODAY -> c.accentSoft
                else -> c.surfaceAlt
            }
            val alpha = if (wk.toFloat() / max(1, weeks - 1) <= reveal + 0.05f) 1f else 0f
            drawRoundRect(col.copy(alpha = col.alpha * alpha), Offset(x, day * (cs + g)), Size(cs, cs), CornerRadius(cs * 0.28f))
            if (m == DayMark.TODAY) drawRoundRect(c.accent, Offset(x, day * (cs + g)), Size(cs, cs), CornerRadius(cs * 0.28f), style = Stroke(1.5.dp.toPx()))
        }
    }
}

/** Colour for a muscle's weekly hard sets (0, under 6, 6–10, 10–20, 20+). */
@Composable
fun setsColor(sets: Double): Color {
    val c = Hyper.colors
    return when {
        sets < 0.5 -> c.surfaceAlt
        sets < 6 -> c.accent.copy(alpha = 0.25f)
        sets < 10 -> c.accent.copy(alpha = 0.5f)
        sets < 20 -> c.accent.copy(alpha = 0.8f)
        else -> c.accentDeep
    }
}

/** A body-map view's shapes, parsed once: drawing paths plus hit-test regions (viewport units × [HIT_SCALE]). */
private class BodyShapes(front: Boolean) {
    val body = PathParser().parsePathString(BodyArt.BODY).toPath()
    val head = PathParser().parsePathString(BodyArt.HEAD).toPath()
    val parts: List<Pair<Muscle, Path>> = (if (front) BodyArt.front else BodyArt.back).map { (m, d) -> m to PathParser().parsePathString(d).toPath() }
    private val hit: List<Pair<Muscle, android.graphics.Region>> = parts.map { (m, p) ->
        val scaled = android.graphics.Path(p.asAndroidPath()).apply {
            transform(android.graphics.Matrix().apply { setScale(HIT_SCALE, HIT_SCALE) })
        }
        val clip = android.graphics.Region(0, 0, (BodyArt.WIDTH * HIT_SCALE).toInt(), (BodyArt.HEIGHT * HIT_SCALE).toInt())
        m to android.graphics.Region().apply { setPath(scaled, clip) }
    }

    /**
     * The muscle under a point in viewport units, else the nearest one within [slop] units, so small muscles
     * (neck, rotator cuff) still get a finger-sized target.
     */
    fun muscleAt(x: Float, y: Float, slop: Float): Muscle? {
        val hx = (x * HIT_SCALE).toInt()
        val hy = (y * HIT_SCALE).toInt()
        hit.lastOrNull { it.second.contains(hx, hy) }?.let { return it.first }
        var best: Muscle? = null
        var bestD = Float.MAX_VALUE
        for ((m, r) in hit) {
            val b = r.bounds
            val dx = maxOf(b.left - hx, 0, hx - b.right).toFloat() / HIT_SCALE
            val dy = maxOf(b.top - hy, 0, hy - b.bottom).toFloat() / HIT_SCALE
            val d = kotlin.math.hypot(dx, dy)
            if (d < bestD) { bestD = d; best = m }
        }
        return best.takeIf { bestD <= slop }
    }

    private companion object { const val HIT_SCALE = 8f }
}

/**
 * Front or back body with each muscle filled by its weekly sets. With [onSelect], tapping a muscle selects
 * it (tap again to clear); the selected one gets an outline and the rest dim, on both views.
 */
@Composable
fun BodyMap(
    weekly: Map<Muscle, Double>,
    front: Boolean,
    modifier: Modifier = Modifier,
    selected: Muscle? = null,
    onSelect: ((Muscle?) -> Unit)? = null,
) {
    val c = Hyper.colors
    val shapes = remember(front) { BodyShapes(front) }
    val colors = Muscle.entries.associateWith { setsColor(weekly[it] ?: 0.0) }
    val reveal = rememberReveal(weekly.hashCode(), 800)
    val silhouette = c.surfaceAlt
    val edge = c.border
    val divider = c.surface
    val ring = c.text
    val haptics = androidx.compose.ui.platform.LocalHapticFeedback.current
    val view = androidx.compose.ui.res.stringResource(if (front) com.hyperathlete.app.R.string.body_front else com.hyperathlete.app.R.string.body_back)
    val chosen = selected?.let { muscleName(it) }
        ?: androidx.compose.ui.res.stringResource(com.hyperathlete.app.R.string.body_map_none)
    val description = androidx.compose.ui.res.stringResource(com.hyperathlete.app.R.string.body_map_desc, view, chosen)
    val slopPx = with(androidx.compose.ui.platform.LocalDensity.current) { 24.dp.toPx() }
    val tap = if (onSelect == null) Modifier else Modifier.pointerInput(front, selected) {
        detectTapGestures { pos ->
            val s = min(size.width / BodyArt.WIDTH, size.height / BodyArt.HEIGHT)
            val ox = (size.width - BodyArt.WIDTH * s) / 2
            val oy = (size.height - BodyArt.HEIGHT * s) / 2
            val m = shapes.muscleAt((pos.x - ox) / s, (pos.y - oy) / s, slopPx / s)
            haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
            onSelect(if (m == null || m == selected) null else m)
        }
    }
    Canvas(modifier.then(tap).semantics { contentDescription = description }) {
        val s = min(size.width / BodyArt.WIDTH, size.height / BodyArt.HEIGHT)
        translate((size.width - BodyArt.WIDTH * s) / 2, (size.height - BodyArt.HEIGHT * s) / 2) {
            scale(s, s, pivot = Offset.Zero) {
                drawPath(shapes.body, silhouette)
                drawPath(shapes.head, silhouette)
                drawPath(shapes.body, edge, style = Stroke(0.6f))
                drawPath(shapes.head, edge, style = Stroke(0.6f))
                for ((m, p) in shapes.parts) {
                    val col = colors[m] ?: silhouette
                    val dim = if (selected != null && m != selected) 0.4f else 1f
                    drawPath(p, col.copy(alpha = col.alpha * reveal * dim))
                    drawPath(p, divider, style = Stroke(0.7f))
                }
                if (selected != null) {
                    for ((m, p) in shapes.parts) if (m == selected) {
                        drawPath(p, ring, style = Stroke(1.3f, join = StrokeJoin.Round))
                    }
                }
            }
        }
    }
}
