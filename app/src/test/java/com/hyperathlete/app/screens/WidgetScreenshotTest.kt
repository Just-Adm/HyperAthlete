package com.hyperathlete.app.screens

import android.app.Application
import android.content.Context
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.ExperimentalGlanceApi
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.compose
import androidx.glance.appwidget.provideContent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.hyperathlete.app.R
import com.hyperathlete.app.domain.streak.DayMark
import com.hyperathlete.app.ui.widgets.FoodContent
import com.hyperathlete.app.ui.widgets.FoodData
import com.hyperathlete.app.ui.widgets.StreakContent
import com.hyperathlete.app.ui.widgets.StreakData
import com.hyperathlete.app.ui.widgets.TodayContent
import com.hyperathlete.app.ui.widgets.TodayData
import com.hyperathlete.app.ui.widgets.TodayStatus
import com.hyperathlete.app.ui.widgets.WeatherContent
import com.hyperathlete.app.ui.widgets.WeatherData
import com.hyperathlete.app.ui.widgets.WeekDot
import com.hyperathlete.app.ui.widgets.WeightContent
import com.hyperathlete.app.ui.widgets.WeightData
import com.hyperathlete.app.ui.widgets.WidgetSnapshot
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Renders each widget through Glance → RemoteViews → real views, like a home screen would. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w360dp-h802dp-xhdpi", application = Application::class)
class WidgetScreenshotTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    private fun r(s: String) = "\u200F" + s

    private val snap0 = WidgetSnapshot(
        rtl = true, accent = 0xFFFF6B00, secondary = 0xFF2F6BFF,
        today = TodayData(
            "اليوم · الخميس", "جسم كامل خارجي + تحمّل + Core", "14 تمارين · 85-90 د · أسبوع 3",
            TodayStatus.PENDING, "لسا ما خلص", listOf("Snap Down", "Pull-up", "Sled Push", "Farmer's Carry"), "ابدأ",
        ),
        streak = StreakData(
            12, "يوم", "الأفضل 21",
            listOf(
                WeekDot("أحد", DayMark.DONE, false), WeekDot("اثنين", DayMark.DONE, false), WeekDot("ثلاثاء", DayMark.REST, true),
                WeekDot("أربعاء", DayMark.DONE, false), WeekDot("خميس", DayMark.TODAY, false), WeekDot("جمعة", DayMark.UPCOMING, false),
                WeekDot("سبت", DayMark.UPCOMING, false),
            ),
            "الستريك",
        ),
        food = FoodData("أكل اليوم", 1540, 2650, "\u20661540 / 2650\u2069 سعرة", 96, 156, "بروتين \u206696 / 156\u2069 غ", "ضايل 1110 سعرة", "إضافة"),
        weather = WeatherData("24°", "غائم جزئياً", R.drawable.ic_partly_cloudy_day_fill, "عمّان", "مطر \u206610%\u2069", "الخميس برا: مطر \u206670%\u2069", false),
        weight = WeightData("الوزن", "78.4 kg", "−0.3 kg · أسبوع", -1, "سجّل وزنك"),
    )

    private val snap = snap0.copy(
        today = snap0.today.copy(dayLabel = r(snap0.today.dayLabel), title = r(snap0.today.title), sub = r(snap0.today.sub), statusText = r(snap0.today.statusText)),
        food = snap0.food.copy(kcalText = r(snap0.food.kcalText), proteinText = r(snap0.food.proteinText), left = r(snap0.food.left)),
        weather = snap0.weather.copy(rain = r(snap0.weather.rain), note = r(snap0.weather.note!!), sky = r(snap0.weather.sky)),
        weight = snap0.weight.copy(delta = r("\u2066−0.3 kg\u2069 · أسبوع")),
        streak = snap0.streak.copy(best = r(snap0.streak.best)),
    )

    private class Fixed(val body: @Composable (Context) -> Unit) : GlanceAppWidget() {
        override suspend fun provideGlance(context: Context, id: GlanceId) = provideContent { body(context) }
    }

    @OptIn(ExperimentalGlanceApi::class)
    private fun render(w: Int, h: Int, body: @Composable (Context) -> Unit): View {
        val rv = runBlocking { Fixed(body).compose(context, size = DpSize(w.dp, h.dp)) }
        val d = context.resources.displayMetrics.density
        val host = FrameLayout(context).apply { layoutParams = ViewGroup.LayoutParams((w * d).toInt(), (h * d).toInt()) }
        host.addView(rv.apply(context, host), FrameLayout.LayoutParams((w * d).toInt(), (h * d).toInt()))
        return host
    }

    private fun board(name: String, vararg views: View) {
        val d = context.resources.displayMetrics.density
        val col = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#FFB9C3D6"))
            setPadding((16 * d).toInt(), (16 * d).toInt(), (16 * d).toInt(), (16 * d).toInt())
        }
        views.forEach { v ->
            col.addView(v)
            col.addView(View(context), LinearLayout.LayoutParams(1, (12 * d).toInt()))
        }
        col.measure(View.MeasureSpec.makeMeasureSpec((360 * d).toInt(), View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
        col.layout(0, 0, col.measuredWidth, col.measuredHeight)
        val bmp = android.graphics.Bitmap.createBitmap(col.measuredWidth, col.measuredHeight, android.graphics.Bitmap.Config.ARGB_8888)
        col.draw(android.graphics.Canvas(bmp))
        bmp.captureRoboImage("screenshots/$name.png")
    }

    @Test fun widgets_training() = board(
        "widgets_training",
        render(320, 230) { TodayContent(snap, it) },
        render(320, 110) { TodayContent(snap, it) },
        render(160, 160) { TodayContent(snap, it) },
        render(160, 160) { StreakContent(snap, it) },
        render(320, 120) { StreakContent(snap, it) },
    )

    @Test fun widgets_daily() = board(
        "widgets_daily",
        render(320, 170) { FoodContent(snap, it) },
        render(160, 160) { WeatherContent(snap, it) },
        render(320, 130) { WeatherContent(snap, it) },
        render(160, 100) { WeightContent(snap, it) },
    )

    @Config(qualifiers = "w360dp-h802dp-night-xhdpi")
    @Test fun widgets_dark() = board(
        "widgets_dark",
        render(320, 200) { TodayContent(snap.copy(today = snap.today.copy(status = TodayStatus.DONE, statusText = "خلصت ✓", action = "افتح")), it) },
        render(320, 170) { FoodContent(snap, it) },
        render(160, 160) { StreakContent(snap, it) },
    )
}
