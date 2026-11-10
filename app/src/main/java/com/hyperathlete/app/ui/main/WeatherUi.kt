package com.hyperathlete.app.ui.main

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.text.drawText
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hyperathlete.app.R
import com.hyperathlete.app.core.locale.Bidi
import com.hyperathlete.app.core.locale.termResource
import com.hyperathlete.app.data.weather.WeatherState
import com.hyperathlete.app.domain.weather.OutdoorAlert
import com.hyperathlete.app.domain.weather.Sky
import com.hyperathlete.app.domain.weather.WeatherSnapshot
import com.hyperathlete.app.domain.weather.skyOf
import com.hyperathlete.app.ui.common.dayShort
import com.hyperathlete.app.ui.components.SmallPillButton
import com.hyperathlete.app.ui.components.hyperClickable
import com.hyperathlete.app.ui.theme.Hyper
import com.hyperathlete.app.ui.theme.glass
import com.hyperathlete.app.ui.theme.HyperShapes
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@DrawableRes
fun skyIcon(sky: Sky, day: Boolean = true): Int = when (sky) {
    Sky.CLEAR -> if (day) R.drawable.ic_clear_day_fill else R.drawable.ic_moon_stars_fill
    Sky.PARTLY_CLOUDY -> if (day) R.drawable.ic_partly_cloudy_day_fill else R.drawable.ic_partly_cloudy_night_fill
    Sky.CLOUDY -> R.drawable.ic_cloud_fill
    Sky.FOG -> R.drawable.ic_foggy_fill
    Sky.DRIZZLE, Sky.RAIN -> R.drawable.ic_rainy_fill
    Sky.SNOW -> R.drawable.ic_weather_snowy_fill
    Sky.THUNDER -> R.drawable.ic_thunderstorm_fill
}

@StringRes
fun skyLabel(sky: Sky): Int = when (sky) {
    Sky.CLEAR -> R.string.sky_clear
    Sky.PARTLY_CLOUDY -> R.string.sky_partly
    Sky.CLOUDY -> R.string.sky_cloudy
    Sky.FOG -> R.string.sky_fog
    Sky.DRIZZLE -> R.string.sky_drizzle
    Sky.RAIN -> R.string.sky_rain
    Sky.SNOW -> R.string.sky_snow
    Sky.THUNDER -> R.string.sky_thunder
}

@Composable
fun skyTint(sky: Sky, day: Boolean): Color = when (sky) {
    Sky.CLEAR -> if (day) Color(0xFFF5A300) else Color(0xFF7C83DB)
    Sky.PARTLY_CLOUDY -> if (day) Color(0xFFF5A300) else Color(0xFF7C83DB)
    Sky.RAIN, Sky.DRIZZLE, Sky.THUNDER -> Hyper.colors.secondary
    else -> Hyper.colors.textSecondary
}

fun deg(c: Double): String = Bidi.ltr("${c.roundToInt()}°")
fun pct(n: Int): String = Bidi.ltr("$n%")
fun degC(c: Double): String = Bidi.ltr("${c.roundToInt()}°C")

/** Replaces the logo in the Home header: icon + °C, city underneath. Tap → details. */
@Composable
fun WeatherChip(state: WeatherState, onClick: () -> Unit, onEnableLocation: () -> Unit) {
    val c = Hyper.colors
    Column(
        Modifier
            .widthIn(min = 76.dp)
            .clip(HyperShapes.cardSmall)
            .background(c.surfaceAlt)
            .hyperClickable(onClick = if (state is WeatherState.NoPermission) onEnableLocation else onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (state) {
            is WeatherState.Ready -> {
                val s = state.snapshot
                val sky = skyOf(s.code)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(skyIcon(sky, s.isDay)), null, tint = skyTint(sky, s.isDay), modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(degC(s.tempC), style = Hyper.type.title, color = c.text, maxLines = 1)
                }
                Text(s.city ?: stringResource(skyLabel(sky)), style = Hyper.type.micro, color = c.textMuted, maxLines = 1)
            }
            WeatherState.NoPermission -> {
                Icon(painterResource(R.drawable.ic_location_off), null, tint = c.textSecondary, modifier = Modifier.size(22.dp))
                Text(stringResource(R.string.weather_enable), style = Hyper.type.micro, color = c.accent, maxLines = 1)
            }
            is WeatherState.Failed -> {
                Icon(painterResource(R.drawable.ic_refresh), null, tint = c.textSecondary, modifier = Modifier.size(22.dp))
                Text("--°C", style = Hyper.type.micro, color = c.textMuted)
            }
            WeatherState.Loading -> SkeletonBlock(Modifier.size(width = 64.dp, height = 36.dp))
        }
    }
}

/** Pulsing placeholder used while something loads (never an empty gap). */
@Composable
fun SkeletonBlock(modifier: Modifier) {
    val c = Hyper.colors
    val still = LocalInspectionMode.current
    val t = rememberInfiniteTransition(label = "skeleton")
    val a by t.animateFloat(0.45f, 1f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "skeletonAlpha")
    Box(modifier.alpha(if (still) 0.7f else a).clip(HyperShapes.tile).background(c.border))
}

/** Heads-up the day before the Outdoor session. */
@Composable
fun OutdoorAlertCard(alert: OutdoorAlert, modifier: Modifier = Modifier) {
    val c = Hyper.colors
    val outdoor = termResource(R.string.term_outdoor)
    val text = when {
        alert.rainChance != null && alert.hotMaxC != null ->
            stringResource(R.string.weather_alert_both, Bidi.isolate(outdoor), pct(alert.rainChance), deg(alert.hotMaxC))
        alert.rainChance != null -> stringResource(R.string.weather_alert_rain, Bidi.isolate(outdoor), pct(alert.rainChance))
        else -> stringResource(R.string.weather_alert_heat, Bidi.isolate(outdoor), deg(alert.hotMaxC ?: 0.0))
    }
    Row(
        modifier.fillMaxWidth().clip(HyperShapes.cardSmall).background(c.warnSoft).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painterResource(if (alert.rainChance != null) R.drawable.ic_umbrella else R.drawable.ic_thermostat),
            null, tint = c.warn, modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(text, style = Hyper.type.smallMedium, color = c.text)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherSheet(snapshot: WeatherSnapshot, today: LocalDate, onRefresh: () -> Unit, onDismiss: () -> Unit) {
    val now = androidx.compose.runtime.remember { java.time.LocalTime.now() }
    val c = Hyper.colors
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheet,
        containerColor = Hyper.glass.fillStrong,
        shape = HyperShapes.sheet,
        tonalElevation = 0.dp,
        dragHandle = { Box(Modifier.padding(top = 10.dp).size(40.dp, 4.dp).clip(HyperShapes.pill).background(c.borderStrong)) },
    ) {
        WeatherDetails(snapshot, today, onRefresh, Modifier.navigationBarsPadding(), now = now)
    }
}

@Composable
fun WeatherDetails(s: WeatherSnapshot, today: LocalDate, onRefresh: () -> Unit, modifier: Modifier = Modifier, now: java.time.LocalTime? = null) {
    val c = Hyper.colors
    val sky = skyOf(s.code)
    val locale = LocalConfiguration.current.locales[0]
    Column(modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 14.dp, bottom = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(skyIcon(sky, s.isDay)), null, tint = skyTint(sky, s.isDay), modifier = Modifier.size(56.dp))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(degC(s.tempC), style = Hyper.type.display, color = c.text)
                Text(
                    listOfNotNull(stringResource(skyLabel(sky)), s.city).joinToString(" · "),
                    style = Hyper.type.bodyMedium, color = c.textSecondary,
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val t = s.today
            StatTile(R.drawable.ic_thermostat, stringResource(R.string.weather_feels), degC(s.feelsLikeC), Modifier.weight(1f))
            StatTile(R.drawable.ic_air, stringResource(R.string.weather_wind), Bidi.ltr("${s.windKmh.roundToInt()} km/h"), Modifier.weight(1f))
            StatTile(R.drawable.ic_umbrella, stringResource(R.string.weather_rain), pct(t?.rainChance ?: 0), Modifier.weight(1f))
        }
        s.today?.let { t ->
            Spacer(Modifier.height(10.dp))
            Text(
                stringResource(R.string.weather_high_low, deg(t.maxC), deg(t.minC)),
                style = Hyper.type.smallMedium, color = c.textSecondary,
            )
        }
        val hours = s.dayHours(today)
        if (hours.size >= 2) {
            Spacer(Modifier.height(18.dp))
            Text(stringResource(R.string.weather_hourly), style = Hyper.type.title, color = c.text)
            Spacer(Modifier.height(8.dp))
            HourlyStrip(hours, today, now?.hour)
        }
        Spacer(Modifier.height(18.dp))
        Text(stringResource(R.string.weather_7days), style = Hyper.type.title, color = c.text)
        Spacer(Modifier.height(8.dp))
        val lo = s.daily.minOfOrNull { it.minC } ?: 0.0
        val hi = s.daily.maxOfOrNull { it.maxC } ?: 1.0
        s.daily.forEach { d ->
            val date = d.localDate
            val label = when (date) {
                today -> stringResource(R.string.weather_today)
                today.plusDays(1) -> stringResource(R.string.weather_tomorrow)
                else -> dayShort(date.dayOfWeek)
            }
            val dsky = skyOf(d.code)
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(label, style = Hyper.type.bodyMedium, color = c.text, modifier = Modifier.width(62.dp), maxLines = 1)
                Icon(painterResource(skyIcon(dsky)), null, tint = skyTint(dsky, true), modifier = Modifier.size(22.dp))
                Text(
                    if (d.rainChance > 0) Bidi.ltr("${d.rainChance}%") else "",
                    style = Hyper.type.micro, color = c.secondary, modifier = Modifier.width(40.dp).padding(start = 6.dp),
                )
                Text(deg(d.minC), style = Hyper.type.smallMedium, color = c.textMuted, modifier = Modifier.width(34.dp))
                RangeBar(lo, hi, d.minC, d.maxC, Modifier.weight(1f))
                Text(deg(d.maxC), style = Hyper.type.smallMedium.copy(fontWeight = FontWeight.Bold), color = c.text, modifier = Modifier.padding(start = 8.dp))
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            val updated = Instant.ofEpochMilli(s.fetchedAtMillis).atZone(ZoneId.systemDefault()).toLocalTime()
                .format(DateTimeFormatter.ofPattern("h:mm a", locale))
            Text(
                stringResource(R.string.weather_updated, updated) + " · Open-Meteo",
                style = Hyper.type.micro, color = c.textMuted, modifier = Modifier.weight(1f),
            )
            SmallPillButton(stringResource(R.string.weather_refresh), onRefresh, filled = false)
        }
    }
}

@Composable
private fun StatTile(@DrawableRes icon: Int, label: String, value: String, modifier: Modifier) {
    val c = Hyper.colors
    Column(modifier.clip(HyperShapes.cardSmall).background(c.surfaceAlt).padding(12.dp)) {
        Icon(painterResource(icon), null, tint = c.accent, modifier = Modifier.size(18.dp))
        Spacer(Modifier.height(6.dp))
        Text(value, style = Hyper.type.title, color = c.text, maxLines = 1)
        Text(label, style = Hyper.type.micro, color = c.textMuted, maxLines = 1)
    }
}

/** Min–max bar positioned on the week's overall temperature range. */
@Composable
private fun RangeBar(lo: Double, hi: Double, min: Double, max: Double, modifier: Modifier) {
    val c = Hyper.colors
    val span = (hi - lo).coerceAtLeast(1.0)
    val start = ((min - lo) / span).toFloat().coerceIn(0f, 1f)
    val end = ((max - lo) / span).toFloat().coerceIn(start, 1f)
    BoxWithConstraints(modifier.height(6.dp).clip(HyperShapes.pill).background(c.surfaceAlt)) {
        Box(
            Modifier
                .offset(x = maxWidth * start)
                .width(maxWidth * (end - start).coerceAtLeast(0.06f))
                .fillMaxHeight()
                .clip(HyperShapes.pill)
                .background(androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(c.secondary, c.accent))),
        )
    }
}

/* ------------------------------------------------------------------ hourly */

private val HourCol = 54.dp

/** "12 AM" … "11 PM", "12 AM": hour by hour for the whole day, with a temperature line. Scrolls to now. */
@Composable
fun HourlyStrip(hours: List<com.hyperathlete.app.domain.weather.HourlyForecast>, today: LocalDate, nowHour: Int?) {
    val c = Hyper.colors
    val am = stringResource(R.string.weather_am)
    val pm = stringResource(R.string.weather_pm)
    val nowLabel = stringResource(R.string.weather_now)
    val scroll = androidx.compose.foundation.rememberScrollState()
    val density = androidx.compose.ui.platform.LocalDensity.current
    val nowIndex = nowHour?.let { h -> hours.indexOfFirst { it.localTime.toLocalDate() == today && it.localTime.hour == h } }?.takeIf { it >= 0 }
    androidx.compose.runtime.LaunchedEffect(nowIndex) {
        // Keep one hour of context on the left of "now".
        if (nowIndex != null) scroll.scrollTo(with(density) { (HourCol * (nowIndex - 1).coerceAtLeast(0)).roundToPx() })
    }
    val temps = hours.map { it.tempC }
    val lo = temps.min()
    val hi = temps.max()
    val measurer = androidx.compose.ui.text.rememberTextMeasurer()
    val tempStyle = Hyper.type.smallMedium.copy(color = c.text, fontWeight = FontWeight.Bold)
    // A timeline reads left to right in both languages, like the charts.
    androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr) {
        Box(
            Modifier.fillMaxWidth().clip(HyperShapes.cardSmall).background(c.surfaceAlt)
                .horizontalScroll(scroll).padding(vertical = 10.dp),
        ) {
            Column(Modifier.width(HourCol * hours.size)) {
                Row {
                    hours.forEachIndexed { i, h ->
                        val hr = h.localTime.hour
                        val label = if (i == nowIndex) nowLabel else "${if (hr % 12 == 0) 12 else hr % 12} ${if (hr < 12) am else pm}"
                        Box(
                            Modifier.width(HourCol).padding(horizontal = 4.dp).clip(HyperShapes.pill)
                                .background(if (i == nowIndex) c.accent else Color.Transparent).padding(vertical = 3.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                label, style = Hyper.type.micro.copy(fontWeight = if (i == nowIndex) FontWeight.Bold else FontWeight.Medium),
                                color = if (i == nowIndex) c.onAccent else c.textSecondary, maxLines = 1,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row {
                    hours.forEach { h ->
                        val k = skyOf(h.code)
                        Box(Modifier.width(HourCol), contentAlignment = Alignment.Center) {
                            Icon(painterResource(skyIcon(k, h.isDay)), null, tint = skyTint(k, h.isDay), modifier = Modifier.size(22.dp))
                        }
                    }
                }
                val line = c.accent
                val line2 = c.secondary
                val dot = c.surface
                androidx.compose.foundation.Canvas(Modifier.fillMaxWidth().height(74.dp)) {
                    val col = HourCol.toPx()
                    val top = 26.dp.toPx()
                    val bottom = size.height - 8.dp.toPx()
                    val span = (hi - lo).coerceAtLeast(1.0)
                    fun y(t: Double) = (bottom - ((t - lo) / span).toFloat() * (bottom - top))
                    val pts = temps.mapIndexed { i, t -> androidx.compose.ui.geometry.Offset(col * i + col / 2, y(t)) }
                    val path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(pts[0].x, pts[0].y)
                        for (i in 1 until pts.size) {
                            val p0 = pts[i - 1]; val p1 = pts[i]
                            val mx = (p0.x + p1.x) / 2
                            cubicTo(mx, p0.y, mx, p1.y, p1.x, p1.y)
                        }
                    }
                    drawPath(
                        path, androidx.compose.ui.graphics.Brush.verticalGradient(listOf(line, line2), top, bottom),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round),
                    )
                    pts.forEachIndexed { i, p ->
                        val now = i == nowIndex
                        drawCircle(if (now) line else dot, if (now) 5.dp.toPx() else 3.5.dp.toPx(), p)
                        if (!now) drawCircle(line, 3.5.dp.toPx(), p, style = androidx.compose.ui.graphics.drawscope.Stroke(1.5.dp.toPx()))
                        val txt = measurer.measure("${temps[i].roundToInt()}°", tempStyle)
                        drawText(txt, topLeft = androidx.compose.ui.geometry.Offset(p.x - txt.size.width / 2f, p.y - txt.size.height - 6.dp.toPx()))
                    }
                }
                Row {
                    hours.forEach { h ->
                        Box(Modifier.width(HourCol), contentAlignment = Alignment.Center) {
                            Text(
                                if (h.rainChance >= 10) "${h.rainChance}%" else "",
                                style = Hyper.type.micro, color = c.secondary, maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}
