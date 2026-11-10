package com.hyperathlete.app.ui.widgets

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider as UnitColorProvider
import com.hyperathlete.app.HomeActivity
import com.hyperathlete.app.R
import com.hyperathlete.app.core.workout.WorkoutNotifier
import com.hyperathlete.app.domain.streak.DayMark
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/* ------------------------------------------------------------------ colours */

/** Widget colours: the app's accent on a light / dark card that follows the phone's dark mode. */
class WColors(accentArgb: Long, secondaryArgb: Long) {
    val accent = Color(accentArgb.toInt())
    val secondary = Color(secondaryArgb.toInt())
    val text = ColorProvider(day = Color(0xFF14151A), night = Color(0xFFF3F4F6))
    val muted = ColorProvider(day = Color(0xFF6B7080), night = Color(0xFF9CA0AE))
    val track = ColorProvider(day = Color(0xFFEDEEF2), night = Color(0xFF2A2C33))
    val chip = ColorProvider(day = accent.copy(alpha = 0.12f), night = accent.copy(alpha = 0.22f))
    val accentP = ColorProvider(day = accent, night = accent)
    val secondaryP = ColorProvider(day = secondary, night = secondary)
    val success = ColorProvider(day = Color(0xFF16A34A), night = Color(0xFF4ADE80))
    val danger = ColorProvider(day = Color(0xFFDC2626), night = Color(0xFFF87171))
    val white = ColorProvider(day = Color.White, night = Color.White)
}

/* ------------------------------------------------------------------ opening the app */

private fun openApp(context: Context, what: String?): Action = actionStartActivity(
    Intent(context, HomeActivity::class.java).apply {
        what?.let { putExtra(WorkoutNotifier.EXTRA_OPEN, it) }
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    },
)

/* ------------------------------------------------------------------ shared pieces */

@Composable
private fun Card(c: WColors, onClick: Action?, content: @Composable () -> Unit) {
    var m = GlanceModifier.fillMaxSize().background(ImageProvider(R.drawable.widget_bg)).cornerRadius(24.dp).padding(14.dp)
    if (onClick != null) m = m.clickable(onClick)
    Box(m) { content() }
}

@Composable
private fun Icon(res: Int, tint: UnitColorProvider, size: Int = 20) {
    Image(ImageProvider(res), contentDescription = null, modifier = GlanceModifier.size(size.dp), colorFilter = ColorFilter.tint(tint))
}

@Composable
private fun Pill(text: String, fg: UnitColorProvider, bg: UnitColorProvider, onClick: Action? = null) {
    var m = GlanceModifier.background(bg).cornerRadius(100.dp).padding(horizontal = 12.dp, vertical = 6.dp)
    if (onClick != null) m = m.clickable(onClick)
    Box(m) { Text(text, style = TextStyle(color = fg, fontSize = 12.sp, fontWeight = FontWeight.Bold), maxLines = 1) }
}

@Composable
private fun Header(c: WColors, icon: Int, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, c.accentP, 18)
        Spacer(GlanceModifier.width(6.dp))
        Text(title, style = TextStyle(color = c.muted, fontSize = 12.sp, fontWeight = FontWeight.Medium), maxLines = 1)
    }
}

/* ------------------------------------------------------------------ widget contents (also used by tests) */

@Composable
fun TodayContent(s: WidgetSnapshot, context: Context) {
    val c = WColors(s.accent, s.secondary)
    val t = s.today
    val size = LocalSize.current
    val wide = size.width >= 220.dp
    val roomy = size.height >= 150.dp
    val tall = size.height >= 220.dp
    Card(c, openApp(context, WorkoutNotifier.OPEN_TODAY)) {
        Column(GlanceModifier.fillMaxSize()) {
            Header(c, R.drawable.ic_fitness_center, t.dayLabel)
            Spacer(GlanceModifier.height(4.dp))
            Text(t.title, style = TextStyle(color = c.text, fontSize = if (roomy) 17.sp else 15.sp, fontWeight = FontWeight.Bold), maxLines = if (roomy) 2 else 1)
            if (roomy) Text(t.sub, style = TextStyle(color = c.muted, fontSize = 12.sp), maxLines = 1)
            if (tall && t.exercises.isNotEmpty() && t.status == TodayStatus.PENDING) {
                Spacer(GlanceModifier.height(6.dp))
                t.exercises.take(3).forEach { e -> Text("• $e", style = TextStyle(color = c.text, fontSize = 12.sp), maxLines = 1) }
            }
            Spacer(GlanceModifier.defaultWeight())
            Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                val done = t.status == TodayStatus.DONE || t.status == TodayStatus.REST_DONE
                Pill(t.statusText, if (done) c.success else c.accentP, if (done) ColorProvider(day = Color(0x2216A34A), night = Color(0x334ADE80)) else c.chip)
                if (wide) {
                    Spacer(GlanceModifier.defaultWeight())
                    Pill(
                        t.action, c.white, c.accentP,
                        openApp(context, if (t.status == TodayStatus.PENDING) WorkoutNotifier.OPEN_START else WorkoutNotifier.OPEN_TODAY),
                    )
                }
            }
        }
    }
}

@Composable
fun StreakContent(s: WidgetSnapshot, context: Context) {
    val c = WColors(s.accent, s.secondary)
    val st = s.streak
    val wide = LocalSize.current.width >= 220.dp
    Card(c, openApp(context, null)) {
        Column(GlanceModifier.fillMaxSize()) {
            Header(c, R.drawable.ic_local_fire_department_fill, st.title)
            Spacer(GlanceModifier.defaultWeight())
            Row(verticalAlignment = Alignment.Bottom) {
                Text("${st.count}", style = TextStyle(color = c.text, fontSize = 34.sp, fontWeight = FontWeight.Bold))
                Spacer(GlanceModifier.width(6.dp))
                Column {
                    Text(st.unit, style = TextStyle(color = c.text, fontSize = 13.sp, fontWeight = FontWeight.Medium))
                    Text(st.best, style = TextStyle(color = c.muted, fontSize = 11.sp))
                }
            }
            Spacer(GlanceModifier.height(8.dp))
            Row(GlanceModifier.fillMaxWidth()) {
                st.week.forEachIndexed { i, d ->
                    Column(GlanceModifier.defaultWeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Dot(c, d.mark, if (wide) 18 else 14)
                    }
                    if (i < st.week.size - 1) Spacer(GlanceModifier.width(2.dp))
                }
            }
        }
    }
}

@Composable
private fun Dot(c: WColors, mark: DayMark, size: Int) {
    val color = when (mark) {
        DayMark.DONE -> c.accentP
        DayMark.REST -> c.secondaryP
        DayMark.MISSED -> c.danger
        DayMark.TODAY -> ColorProvider(day = c.accent.copy(alpha = 0.35f), night = c.accent.copy(alpha = 0.45f))
        DayMark.UPCOMING, DayMark.NONE -> ColorProvider(day = Color(0xFFD5D8E0), night = Color(0xFF3A3D46))
    }
    Image(ImageProvider(R.drawable.widget_dot), null, GlanceModifier.size(size.dp), colorFilter = ColorFilter.tint(color))
}

@Composable
fun FoodContent(s: WidgetSnapshot, context: Context) {
    val c = WColors(s.accent, s.secondary)
    val f = s.food
    Card(c, openApp(context, WorkoutNotifier.OPEN_NUTRITION)) {
        Column(GlanceModifier.fillMaxSize()) {
            Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Header(c, R.drawable.ic_restaurant_fill, f.title)
                Spacer(GlanceModifier.defaultWeight())
                Box(
                    GlanceModifier.size(28.dp).background(c.accentP).cornerRadius(14.dp).clickable(openApp(context, WorkoutNotifier.OPEN_NUTRITION)),
                    contentAlignment = Alignment.Center,
                ) { Icon(R.drawable.ic_add, c.white, 18) }
            }
            Spacer(GlanceModifier.height(10.dp))
            Text(f.kcalText, style = TextStyle(color = c.text, fontSize = 14.sp, fontWeight = FontWeight.Bold), maxLines = 1)
            Spacer(GlanceModifier.height(4.dp))
            LinearProgressIndicator(
                progress = (f.kcal.toFloat() / f.kcalTarget.coerceAtLeast(1)).coerceIn(0f, 1f),
                modifier = GlanceModifier.fillMaxWidth().height(8.dp), color = c.accentP, backgroundColor = c.track,
            )
            Spacer(GlanceModifier.height(8.dp))
            Text(f.proteinText, style = TextStyle(color = c.text, fontSize = 13.sp, fontWeight = FontWeight.Medium), maxLines = 1)
            Spacer(GlanceModifier.height(4.dp))
            LinearProgressIndicator(
                progress = (f.protein.toFloat() / f.proteinTarget.coerceAtLeast(1)).coerceIn(0f, 1f),
                modifier = GlanceModifier.fillMaxWidth().height(8.dp), color = c.secondaryP, backgroundColor = c.track,
            )
            if (LocalSize.current.height >= 160.dp) {
                Spacer(GlanceModifier.height(6.dp))
                Text(f.left, style = TextStyle(color = c.muted, fontSize = 11.sp), maxLines = 1)
            }
        }
    }
}

@Composable
fun WeatherContent(s: WidgetSnapshot, context: Context) {
    val c = WColors(s.accent, s.secondary)
    val w = s.weather
    val wide = LocalSize.current.width >= 220.dp
    Card(c, openApp(context, null)) {
        Column(GlanceModifier.fillMaxSize()) {
            Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                val tint = when (w.iconTint) { 0L -> c.secondaryP; 1L -> c.muted; else -> ColorProvider(day = Color(w.iconTint.toInt()), night = Color(w.iconTint.toInt())) }
                Image(ImageProvider(w.icon), null, GlanceModifier.size(if (wide) 44.dp else 36.dp), colorFilter = ColorFilter.tint(tint))
                Spacer(GlanceModifier.width(8.dp))
                Column(GlanceModifier.defaultWeight()) {
                    Text(w.temp, style = TextStyle(color = c.text, fontSize = 28.sp, fontWeight = FontWeight.Bold), maxLines = 1)
                    Text(w.sky, style = TextStyle(color = c.muted, fontSize = 12.sp), maxLines = 1)
                }
            }
            Spacer(GlanceModifier.defaultWeight())
            if (!w.empty) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(R.drawable.ic_water_drop, c.secondaryP, 14)
                    Spacer(GlanceModifier.width(4.dp))
                    Text(w.rain, style = TextStyle(color = c.text, fontSize = 12.sp), maxLines = 1)
                    if (wide && w.city != null) {
                        Spacer(GlanceModifier.width(8.dp))
                        Text("· " + w.city, style = TextStyle(color = c.muted, fontSize = 12.sp), maxLines = 1)
                    }
                }
                w.note?.takeIf { it.isNotBlank() }?.let {
                    Spacer(GlanceModifier.height(4.dp))
                    Text(it, style = TextStyle(color = c.muted, fontSize = 11.sp), maxLines = 2)
                }
            }
        }
    }
}

@Composable
fun WeightContent(s: WidgetSnapshot, context: Context) {
    val c = WColors(s.accent, s.secondary)
    val w = s.weight
    Card(c, openApp(context, WorkoutNotifier.OPEN_WEIGHT)) {
        Column(GlanceModifier.fillMaxSize()) {
            Header(c, R.drawable.ic_monitor_weight, w.title)
            Spacer(GlanceModifier.defaultWeight())
            if (w.value == null) {
                Text(w.empty, style = TextStyle(color = c.muted, fontSize = 13.sp), maxLines = 2)
            } else {
                Text(w.value, style = TextStyle(color = c.text, fontSize = 26.sp, fontWeight = FontWeight.Bold), maxLines = 1)
                w.delta?.let {
                    Text(it, style = TextStyle(color = if (w.trend == 0) c.muted else c.secondaryP, fontSize = 12.sp, fontWeight = FontWeight.Medium), maxLines = 1)
                }
            }
        }
    }
}

/* ------------------------------------------------------------------ widgets */

abstract class HyperWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(DpSize(70.dp, 70.dp), DpSize(110.dp, 110.dp), DpSize(250.dp, 110.dp), DpSize(250.dp, 200.dp)),
    )

    @Composable abstract fun Content(s: WidgetSnapshot, context: Context)

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snap = WidgetLoader.load(context)
        provideContent { Content(snap, context) }
    }
}

class TodayWidget : HyperWidget() { @Composable override fun Content(s: WidgetSnapshot, context: Context) = TodayContent(s, context) }
class StreakWidget : HyperWidget() { @Composable override fun Content(s: WidgetSnapshot, context: Context) = StreakContent(s, context) }
class FoodWidget : HyperWidget() { @Composable override fun Content(s: WidgetSnapshot, context: Context) = FoodContent(s, context) }
class WeatherWidget : HyperWidget() { @Composable override fun Content(s: WidgetSnapshot, context: Context) = WeatherContent(s, context) }
class WeightWidget : HyperWidget() { @Composable override fun Content(s: WidgetSnapshot, context: Context) = WeightContent(s, context) }

class TodayWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = TodayWidget() }
class StreakWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = StreakWidget() }
class FoodWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = FoodWidget() }
class WeatherWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = WeatherWidget() }
class WeightWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = WeightWidget() }

/** Redraws every placed widget (after a workout, a meal, a weigh-in, new weather or settings). Cheap when none are placed. */
object Widgets {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val all: List<() -> HyperWidget> = listOf(::TodayWidget, ::StreakWidget, ::FoodWidget, ::WeatherWidget, ::WeightWidget)

    fun refresh(context: Context) {
        val app = context.applicationContext
        scope.launch {
            runCatching {
                val mgr = GlanceAppWidgetManager(app)
                all.forEach { make ->
                    val w = make()
                    if (mgr.getGlanceIds(w.javaClass).isNotEmpty()) w.updateAll(app)
                }
            }
        }
    }
}
