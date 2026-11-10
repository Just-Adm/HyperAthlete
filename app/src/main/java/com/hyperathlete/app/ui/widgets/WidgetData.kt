package com.hyperathlete.app.ui.widgets

import android.content.Context
import androidx.annotation.DrawableRes
import com.hyperathlete.app.R
import com.hyperathlete.app.core.locale.Localizer
import com.hyperathlete.app.data.nutrition.NutritionRepository
import com.hyperathlete.app.data.profile.ProfileRepository
import com.hyperathlete.app.data.program.ProgramRepository
import com.hyperathlete.app.data.progress.ProgressRepository
import com.hyperathlete.app.data.settings.SettingsRepository
import com.hyperathlete.app.data.weather.WeatherRepository
import com.hyperathlete.app.data.workout.WorkoutRepository
import com.hyperathlete.app.domain.model.AppLanguage
import com.hyperathlete.app.domain.nutrition.Macros
import com.hyperathlete.app.domain.nutrition.TargetCalculator
import com.hyperathlete.app.domain.program.Focus
import com.hyperathlete.app.domain.program.Phase
import com.hyperathlete.app.domain.program.Planner
import com.hyperathlete.app.domain.program.SessionKind
import com.hyperathlete.app.domain.program.WeekPlan
import com.hyperathlete.app.domain.progress.Metric
import com.hyperathlete.app.domain.streak.DayMark
import com.hyperathlete.app.domain.streak.StreakCalculator
import com.hyperathlete.app.domain.weather.Sky
import com.hyperathlete.app.domain.weather.skyOf
import com.hyperathlete.app.ui.main.skyIcon
import com.hyperathlete.app.ui.main.skyLabel
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/* Everything the widgets show, already worded in the app's language (widgets can't use the app's locale). */

enum class TodayStatus { PENDING, DONE, REST, REST_DONE }

data class TodayData(
    val dayLabel: String,
    val title: String,
    val sub: String,
    val status: TodayStatus,
    val statusText: String,
    val exercises: List<String>,
    val action: String,
)

data class WeekDot(val label: String, val mark: DayMark, val bonus: Boolean)

data class StreakData(val count: Int, val unit: String, val best: String, val week: List<WeekDot>, val title: String)

data class FoodData(
    val title: String,
    val kcal: Int, val kcalTarget: Int, val kcalText: String,
    val protein: Int, val proteinTarget: Int, val proteinText: String,
    val left: String,
    val add: String,
)

data class WeatherData(
    val temp: String, val sky: String, @DrawableRes val icon: Int, val city: String?, val rain: String,
    /** Training heads-up (outdoor Thursday) or the next days, short. */
    val note: String?, val empty: Boolean,
    /** Icon colour: sun yellow, rain in the app's blue (0 = the secondary colour), clouds grey (1). */
    val iconTint: Long = 0xFFF5A300,
)

data class WeightData(val title: String, val value: String?, val delta: String?, val trend: Int, val empty: String)

data class WidgetSnapshot(
    val rtl: Boolean,
    val accent: Long,
    val secondary: Long,
    val today: TodayData,
    val streak: StreakData,
    val food: FoodData,
    val weather: WeatherData,
    val weight: WeightData,
)

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun settings(): SettingsRepository
    fun profiles(): ProfileRepository
    fun programs(): ProgramRepository
    fun workouts(): WorkoutRepository
    fun nutrition(): NutritionRepository
    fun progress(): ProgressRepository
    fun weather(): WeatherRepository
    fun clock(): Clock
}

/** Localizer whose sentences start with a right-to-left mark in Arabic, so "مطر 10%" never comes out as "10 مطر%". */
class RtlWords(private val l: Localizer, private val rtl: Boolean) {
    val ui get() = l.ui
    val terms get() = l.terms
    fun string(id: Int, vararg args: Any): String = mark(l.string(id, *args))
    fun mark(s: String) = if (rtl && s.isNotEmpty()) "\u200F" + s else s
    /** Keeps "1540 / 2650" or "10%" in one left-to-right piece inside Arabic text. */
    fun iso(s: String) = if (rtl) "\u2066" + s + "\u2069" else s
}

object WidgetLoader {

    suspend fun load(context: Context): WidgetSnapshot {
        val e = EntryPointAccessors.fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
        val s = e.settings().current()
        val l0 = Localizer(context, s.language)
        val rtl = s.language != AppLanguage.ENGLISH
        val l = RtlWords(l0, rtl)
        val today = LocalDate.now(e.clock())
        val pos = s.position(today)
        val program = e.programs().program

        // Today
        val plan = Planner.plan(program, today.dayOfWeek, pos.week)
        val days = runCatching { e.workouts().days(today.minusDays(400)).first() }.getOrDefault(emptyMap())
        val rec = days[today]
        val rest = plan.template.kind == SessionKind.REST
        val status = when {
            rest && rec?.mobilityDone == true -> TodayStatus.REST_DONE
            rest -> TodayStatus.REST
            rec?.sessionDone == true -> TodayStatus.DONE
            else -> TodayStatus.PENDING
        }
        val minutes = plan.template.minutes.let { if (it.first == it.last) "${it.first}" else "${it.first}-${it.last}" }
        val todayData = TodayData(
            dayLabel = l.string(R.string.day_today) + " · " + dayName(l, today.dayOfWeek).removePrefix("\u200F"),
            title = l.mark(plan.template.focus.joinToString(" + ") { focus(l, it).removePrefix("\u200F") }),
            sub = l.string(R.string.w_today_sub, plan.slots.size, minutes, pos.week),
            status = status,
            statusText = l.string(
                when (status) {
                    TodayStatus.DONE -> R.string.w_status_done
                    TodayStatus.REST -> R.string.w_status_rest
                    TodayStatus.REST_DONE -> R.string.w_status_rest_done
                    TodayStatus.PENDING -> if (pos.phase == Phase.DELOAD) R.string.w_status_deload else R.string.w_status_pending
                },
            ),
            exercises = plan.slots.map { it.exercise.name.term(s.language) }.distinct().take(4),
            action = l.string(if (status == TodayStatus.PENDING) R.string.w_start else R.string.w_open),
        )

        // Streak + week
        val first = runCatching { e.workouts().firstCompletedDay().first() }.getOrNull()
        val since = listOfNotNull(s.streakSince, first).minOrNull()
        val streak = StreakCalculator.compute(days, { WeekPlan.of(it).kind }, since, today)
        val streakData = StreakData(
            count = streak.current,
            unit = l.mark(l.ui.resources.getQuantityString(R.plurals.w_days, streak.current, streak.current).replace("${streak.current}", "").trim()),
            best = l.string(R.string.w_best, streak.best),
            week = streak.week.map { WeekDot(dayShort(l, it.date.dayOfWeek), it.mark, it.bonus) },
            title = l.string(R.string.w_streak_title),
        )

        // Food
        val profile = e.profiles().current()
        val targets = TargetCalculator.targets(profile, s.nutrition, today)
        val eaten = runCatching { e.nutrition().dayOnce(today) }.getOrDefault(emptyList()).fold(Macros()) { acc, m -> acc + m.total }
        val kcal = eaten.kcal.roundToInt()
        val protein = eaten.protein.roundToInt()
        val food = FoodData(
            title = l.string(R.string.w_food_title),
            kcal = kcal, kcalTarget = targets.kcal, kcalText = l.string(R.string.w_kcal, l.iso("$kcal / ${targets.kcal}")),
            protein = protein, proteinTarget = targets.protein, proteinText = l.string(R.string.w_protein, l.iso("$protein / ${targets.protein}")),
            left = (targets.kcal - kcal).let { if (it >= 0) l.string(R.string.w_left, it) else l.string(R.string.w_over, -it) },
            add = l.string(R.string.w_add_food),
        )

        // Weather
        val w = e.weather().cached()
        val weather = if (w == null) {
            WeatherData("--", l.string(R.string.w_no_weather), R.drawable.ic_partly_cloudy_day, null, "", null, empty = true)
        } else {
            val sky = skyOf(w.code)
            val thu = w.daily.firstOrNull { it.localDate.dayOfWeek == DayOfWeek.THURSDAY && !it.localDate.isBefore(today) }
            val note = when {
                thu != null && (thu.rainChance >= 50 || thu.maxC >= 35) ->
                    if (thu.rainChance >= 50) l.string(R.string.w_thu_rain, l.iso("${thu.rainChance}%")) else l.string(R.string.w_thu_hot, thu.maxC.roundToInt())
                else -> l.mark(w.daily.drop(1).take(3).joinToString("  ") { dayShort(l, it.localDate.dayOfWeek).removePrefix("\u200F") + " " + it.maxC.roundToInt() + "°" })
            }
            WeatherData(
                temp = "${w.tempC.roundToInt()}°", sky = l.string(skyLabel(sky)), icon = skyIcon(sky, w.isDay), city = w.city,
                iconTint = when (sky) {
                    Sky.CLEAR, Sky.PARTLY_CLOUDY -> if (w.isDay) 0xFFF5A300 else 0xFF7C83DB
                    Sky.RAIN, Sky.DRIZZLE, Sky.THUNDER -> 0L
                    else -> 1L
                },
                rain = l.string(R.string.w_rain, l.iso("${w.today?.rainChance ?: 0}%")), note = note, empty = false,
            )
        }

        // Weight
        val weights = runCatching { e.progress().metrics.first() }.getOrDefault(emptyList())
            .filter { it.metric == Metric.WEIGHT }.sortedBy { it.date }
        val latest = weights.lastOrNull()
        val weekAgo = latest?.let { lt -> weights.lastOrNull { !it.date.isAfter(lt.date.minusDays(7)) } }
        val delta = if (latest != null && weekAgo != null) latest.value - weekAgo.value else null
        val weight = WeightData(
            title = l.string(R.string.w_weight_title),
            value = latest?.let { com.hyperathlete.app.domain.model.Weights.text(it.value, s.weightUnit) },
            delta = delta?.let { "\u2066" + com.hyperathlete.app.domain.model.Weights.signed(it, s.weightUnit) + "\u2069 · " + l.string(R.string.w_week).removePrefix("\u200F") }?.let(l::mark),
            trend = when { delta == null || abs(delta) < 0.05 -> 0; delta > 0 -> 1; else -> -1 },
            empty = l.string(R.string.w_weight_empty),
        )

        return WidgetSnapshot(
            rtl = s.language != AppLanguage.ENGLISH,
            accent = s.accentArgb, secondary = s.secondaryArgb,
            today = todayData, streak = streakData, food = food, weather = weather, weight = weight,
        )
    }

    private fun fmt(v: Double) = String.format(Locale.US, "%.1f", v).removeSuffix(".0")

    private fun focus(l: RtlWords, f: Focus) = if (f.isTerm) l.terms.getString(f.label) else l.string(f.label)

    private fun dayName(l: RtlWords, d: DayOfWeek) = l.string(
        when (d) {
            DayOfWeek.SUNDAY -> R.string.day_sun; DayOfWeek.MONDAY -> R.string.day_mon; DayOfWeek.TUESDAY -> R.string.day_tue
            DayOfWeek.WEDNESDAY -> R.string.day_wed; DayOfWeek.THURSDAY -> R.string.day_thu; DayOfWeek.FRIDAY -> R.string.day_fri
            DayOfWeek.SATURDAY -> R.string.day_sat
        },
    )

    private fun dayShort(l: RtlWords, d: DayOfWeek) = l.string(
        when (d) {
            DayOfWeek.SUNDAY -> R.string.day_short_sun; DayOfWeek.MONDAY -> R.string.day_short_mon; DayOfWeek.TUESDAY -> R.string.day_short_tue
            DayOfWeek.WEDNESDAY -> R.string.day_short_wed; DayOfWeek.THURSDAY -> R.string.day_short_thu; DayOfWeek.FRIDAY -> R.string.day_short_fri
            DayOfWeek.SATURDAY -> R.string.day_short_sat
        },
    )
}
