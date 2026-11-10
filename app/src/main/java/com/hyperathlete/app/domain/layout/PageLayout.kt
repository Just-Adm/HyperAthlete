package com.hyperathlete.app.domain.layout

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.hyperathlete.app.R

enum class PageId { HOME, NUTRITION, PROGRESS }

/**
 * Every movable box in the app. New boxes added in later phases are appended to the user's saved
 * order automatically (at their default position), so a saved layout never hides new features.
 */
enum class BoxId(val page: PageId, @StringRes val label: Int, @DrawableRes val icon: Int) {
    HOME_STREAK(PageId.HOME, R.string.box_streak, R.drawable.ic_local_fire_department),
    HOME_TODAY(PageId.HOME, R.string.box_today, R.drawable.ic_fitness_center),
    HOME_SCHEDULE(PageId.HOME, R.string.box_schedule, R.drawable.ic_calendar_month),
    HOME_WEEK(PageId.HOME, R.string.box_week, R.drawable.ic_flag),
    HOME_NUTRITION(PageId.HOME, R.string.box_home_nutrition, R.drawable.ic_restaurant),
    NUTRITION_SUMMARY(PageId.NUTRITION, R.string.box_nutrition_summary, R.drawable.ic_local_fire_department),
    NUTRITION_MEALS(PageId.NUTRITION, R.string.box_nutrition_meals, R.drawable.ic_restaurant),
    NUTRITION_PROTEIN(PageId.NUTRITION, R.string.box_nutrition_protein, R.drawable.ic_fitness_center),
    PROGRESS_PHASE(PageId.PROGRESS, R.string.box_progress_phase, R.drawable.ic_flag),
    PROGRESS_OVERVIEW(PageId.PROGRESS, R.string.box_progress_overview, R.drawable.ic_monitoring),
    PROGRESS_WEIGHT(PageId.PROGRESS, R.string.box_progress_weight, R.drawable.ic_monitor_weight),
    PROGRESS_NUTRITION(PageId.PROGRESS, R.string.box_progress_nutrition, R.drawable.ic_restaurant),
    PROGRESS_STRENGTH(PageId.PROGRESS, R.string.box_progress_strength, R.drawable.ic_fitness_center),
    PROGRESS_BALANCE(PageId.PROGRESS, R.string.box_progress_balance, R.drawable.ic_sports_gymnastics),
    PROGRESS_KNEE(PageId.PROGRESS, R.string.box_progress_knee, R.drawable.ic_healing),
    PROGRESS_TESTS(PageId.PROGRESS, R.string.box_progress_tests, R.drawable.ic_flag),
    PROGRESS_CONSISTENCY(PageId.PROGRESS, R.string.box_progress_consistency, R.drawable.ic_calendar_month),
    PROGRESS_MEASUREMENTS(PageId.PROGRESS, R.string.box_progress_measurements, R.drawable.ic_height),
    PROGRESS_RECAP(PageId.PROGRESS, R.string.box_progress_recap, R.drawable.ic_monitoring),
    ;

    companion object {
        fun defaults(page: PageId): List<BoxId> = entries.filter { it.page == page }
    }
}

data class PageLayout(val page: PageId, val order: List<BoxId>, val hidden: Set<BoxId>) {
    val visible: List<BoxId> get() = order.filterNot { it in hidden }

    fun move(from: BoxId, to: BoxId): PageLayout {
        val i = order.indexOf(from)
        val j = order.indexOf(to)
        if (i < 0 || j < 0 || i == j) return this
        val list = order.toMutableList()
        list.add(j, list.removeAt(i))
        return copy(order = list)
    }

    fun toggle(box: BoxId): PageLayout = copy(hidden = if (box in hidden) hidden - box else hidden + box)

    /** "HOME_TODAY,HOME_STREAK|HOME_WEEK" = order | hidden. */
    fun encode(): String = order.joinToString(",") { it.name } + "|" + hidden.joinToString(",") { it.name }

    companion object {
        fun default(page: PageId) = PageLayout(page, BoxId.defaults(page), emptySet())

        /** Restores a saved layout, dropping unknown boxes and inserting new ones at their default index. */
        fun decode(page: PageId, raw: String?): PageLayout {
            val defaults = BoxId.defaults(page)
            if (raw.isNullOrBlank()) return default(page)
            val (orderPart, hiddenPart) = raw.split('|').let { it.getOrElse(0) { "" } to it.getOrElse(1) { "" } }
            fun parse(s: String) = s.split(',').mapNotNull { n -> BoxId.entries.firstOrNull { it.name == n && it.page == page } }
            val saved = parse(orderPart).distinct().toMutableList()
            defaults.forEachIndexed { index, box ->
                if (box !in saved) saved.add(index.coerceAtMost(saved.size), box)
            }
            return PageLayout(page, saved, parse(hiddenPart).toSet())
        }
    }
}
