package com.hyperathlete.app.ui.nutrition

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hyperathlete.app.R
import com.hyperathlete.app.core.locale.Bidi
import com.hyperathlete.app.core.locale.LocalAppLanguage
import com.hyperathlete.app.core.locale.bidiJoin
import com.hyperathlete.app.domain.layout.BoxId
import com.hyperathlete.app.domain.layout.PageLayout
import com.hyperathlete.app.domain.nutrition.Adherence
import com.hyperathlete.app.domain.nutrition.DayStatus
import com.hyperathlete.app.domain.nutrition.MealEntry
import com.hyperathlete.app.domain.nutrition.MealType
import com.hyperathlete.app.domain.nutrition.ProteinSpread
import com.hyperathlete.app.domain.nutrition.sum
import com.hyperathlete.app.ui.common.dayName
import com.hyperathlete.app.ui.common.dayShort
import com.hyperathlete.app.ui.components.HyperCard
import com.hyperathlete.app.ui.components.HyperChip
import com.hyperathlete.app.ui.components.IconTile
import com.hyperathlete.app.ui.components.hyperClickable
import com.hyperathlete.app.ui.components.rememberHaptics
import com.hyperathlete.app.ui.main.EditableBoxList
import com.hyperathlete.app.ui.main.HyperSheet
import com.hyperathlete.app.ui.theme.Hyper
import com.hyperathlete.app.ui.theme.glassBackdrop
import com.hyperathlete.app.ui.theme.glass
import com.hyperathlete.app.ui.theme.HyperShapes
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

/** Everything the nutrition page needs from outside; stateless so it renders in screenshot tests. */
class NutritionDeps(
    val layout: PageLayout,
    val editMode: Boolean = false,
    val onLayoutChange: (PageLayout) -> Unit = {},
    val month: (YearMonth) -> Flow<Map<LocalDate, Adherence>> = { emptyFlow() },
    val onSelect: (LocalDate) -> Unit = {},
    val onShift: (Long) -> Unit = {},
    val onAdd: (MealType) -> Unit = {},
    val onEdit: (MealEntry) -> Unit = {},
    val onDelete: (MealEntry) -> Unit = {},
    val undo: MealEntry? = null,
    val onUndo: () -> Unit = {},
    val onSettings: () -> Unit = {},
    val ringPage: Int = 0,
)

@Composable
fun NutritionScreen(day: NutritionDay, deps: NutritionDeps) {
    val c = Hyper.colors
    var calendar by rememberSaveable { mutableStateOf(false) }
    Box(Modifier.fillMaxSize().glassBackdrop(Hyper.glass)) {
        Column(Modifier.fillMaxSize()) {
            if (!deps.editMode) {
                Column(Modifier.statusBarsPadding().padding(start = 20.dp, end = 12.dp, top = 12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.nav_nutrition), style = Hyper.type.h1, color = c.text, modifier = Modifier.weight(1f))
                        RoundIcon(R.drawable.ic_tune, stringResource(R.string.nutrition_settings), deps.onSettings)
                    }
                    Spacer(Modifier.height(10.dp))
                    DayBar(day, onShift = deps.onShift, onOpenCalendar = { calendar = true }, modifier = Modifier.padding(end = 8.dp))
                }
            }
            EditableBoxList(
                layout = deps.layout,
                editMode = deps.editMode,
                onLayoutChange = deps.onLayoutChange,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 96.dp),
            ) { id, interactive ->
                when (id) {
                    BoxId.NUTRITION_SUMMARY -> SummaryBox(day, deps.ringPage, onAdd = { deps.onAdd(defaultMeal(day)) }, interactive = interactive)
                    BoxId.NUTRITION_MEALS -> MealsBox(day, deps, interactive)
                    BoxId.NUTRITION_PROTEIN -> ProteinBox(day)
                    else -> Unit
                }
            }
        }
        UndoBar(deps.undo, deps.onUndo, Modifier.align(Alignment.BottomCenter))
    }
    if (calendar) {
        HyperSheet({ calendar = false }) {
            MonthCalendar(day.date, day.today, deps.month) { d -> deps.onSelect(d); calendar = false }
        }
    }
}

private fun defaultMeal(day: NutritionDay): MealType =
    if (day.date == day.today) MealType.forTime(java.time.LocalTime.now()) else MealType.SNACKS

@Composable
private fun RoundIcon(icon: Int, description: String, onClick: () -> Unit, enabled: Boolean = true) {
    val c = Hyper.colors
    Box(
        Modifier.size(42.dp).clip(CircleShape).background(c.surfaceAlt).hyperClickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(icon), description, tint = if (enabled) c.textSecondary else c.lock, modifier = Modifier.size(20.dp))
    }
}

/* ------------------------------------------------------------------ day bar */

@Composable
private fun DayBar(day: NutritionDay, onShift: (Long) -> Unit, onOpenCalendar: () -> Unit, modifier: Modifier = Modifier) {
    val c = Hyper.colors
    val haptics = rememberHaptics()
    val locale = LocalConfiguration.current.locales[0]
    val rel = when (day.date) {
        day.today -> stringResource(R.string.day_today)
        day.today.minusDays(1) -> stringResource(R.string.day_yesterday)
        day.today.plusDays(1) -> stringResource(R.string.day_tomorrow)
        else -> dayName(day.date.dayOfWeek)
    }
    Row(
        modifier.fillMaxWidth().clip(HyperShapes.pill).background(c.surfaceAlt).padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepArrow(R.drawable.ic_chevron_left, stringResource(R.string.day_previous)) { haptics.tick(); onShift(-1) }
        Column(
            Modifier.weight(1f).clip(HyperShapes.pill).hyperClickable(scaleOnPress = false, onClick = onOpenCalendar).padding(vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(rel, style = Hyper.type.title, color = c.text)
                Spacer(Modifier.width(6.dp))
                Icon(painterResource(R.drawable.ic_calendar_month), null, tint = c.accent, modifier = Modifier.size(16.dp))
            }
            Text(day.date.format(DateTimeFormatter.ofPattern("d MMMM yyyy", locale)), style = Hyper.type.micro, color = c.textMuted)
        }
        StepArrow(R.drawable.ic_chevron_right, stringResource(R.string.day_next)) { haptics.tick(); onShift(1) }
    }
}

@Composable
private fun StepArrow(icon: Int, description: String, onClick: () -> Unit) {
    val c = Hyper.colors
    Box(
        Modifier.size(44.dp).clip(CircleShape).background(c.surface).hyperClickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(painterResource(icon), description, tint = c.text, modifier = Modifier.size(22.dp)) }
}

/* ------------------------------------------------------------------ summary ring */

private enum class RingPage { TOTAL, PROTEIN, CARBS, FAT }

@Composable
private fun SummaryBox(day: NutritionDay, initialPage: Int, onAdd: () -> Unit, interactive: Boolean) {
    val c = Hyper.colors
    val pager = rememberPagerState(initialPage) { RingPage.entries.size }
    HyperCard(Modifier.fillMaxWidth(), padding = PaddingValues(top = 16.dp, bottom = 18.dp)) {
        Row(Modifier.padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.box_nutrition_summary), style = Hyper.type.title, color = c.text, modifier = Modifier.weight(1f))
            if (day.targets.carbDay) HyperChip(stringResource(R.string.carb_day), icon = R.drawable.ic_bolt, color = MacroColors.Carbs, background = MacroColors.Carbs.copy(alpha = 0.12f))
        }
        HorizontalPager(pager, Modifier.fillMaxWidth().height(206.dp), userScrollEnabled = interactive) { p ->
            val page = RingPage.entries[p]
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Ring(day, page) }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            RingPage.entries.forEachIndexed { i, _ ->
                val on = pager.currentPage == i
                val w by animateFloatAsState(if (on) 18f else 6f, tween(220), label = "dot")
                Box(Modifier.padding(horizontal = 3.dp).size(w.dp, 6.dp).clip(HyperShapes.pill).background(if (on) c.accent else c.borderStrong))
            }
        }
        if (pager.currentPage == 0) {
            Spacer(Modifier.height(14.dp))
            Row(Modifier.padding(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MacroMini(stringResource(R.string.macro_protein), day.eaten.protein, day.targets.protein, MacroColors.Protein, Modifier.weight(1f))
                MacroMini(stringResource(R.string.macro_carbs), day.eaten.carbs, day.targets.carbs, MacroColors.Carbs, Modifier.weight(1f))
                MacroMini(stringResource(R.string.macro_fat), day.eaten.fat, day.targets.fat, MacroColors.Fat, Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(
            statusText(day), style = Hyper.type.bodyMedium, color = statusColor(day.status),
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
        )
        Spacer(Modifier.height(2.dp))
        Text(
            stringResource(R.string.nutri_weight, com.hyperathlete.app.ui.common.wt(day.weightKg)),
            style = Hyper.type.small, color = c.textMuted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(14.dp))
        Box(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
            com.hyperathlete.app.ui.components.PrimaryButton(stringResource(R.string.add_food), onAdd, icon = R.drawable.ic_add)
        }
    }
}

@Composable
private fun statusText(day: NutritionDay): String = when (val s = day.status) {
    is DayStatus.Remaining -> stringResource(R.string.nutri_left, kcalLabel(s.kcal), gLabel(s.protein.toDouble()))
    is DayStatus.ProteinOnly -> stringResource(R.string.nutri_protein_left, gLabel(s.protein.toDouble()))
    is DayStatus.Over -> stringResource(R.string.nutri_over, kcalLabel(s.kcal))
    DayStatus.Done -> stringResource(R.string.nutri_done)
}

@Composable
private fun statusColor(s: DayStatus): Color = when (s) {
    is DayStatus.Over -> Hyper.colors.warn
    DayStatus.Done -> Hyper.colors.success
    else -> Hyper.colors.text
}

@Composable
private fun Ring(day: NutritionDay, page: RingPage) {
    val c = Hyper.colors
    val (eaten, goal, color, label) = when (page) {
        RingPage.TOTAL -> Quad(day.eaten.kcal, day.targets.kcal.toDouble(), c.accent, stringResource(R.string.ring_kcal))
        RingPage.PROTEIN -> Quad(day.eaten.protein, day.targets.protein.toDouble(), MacroColors.Protein, stringResource(R.string.macro_protein))
        RingPage.CARBS -> Quad(day.eaten.carbs, day.targets.carbs.toDouble(), MacroColors.Carbs, stringResource(R.string.macro_carbs))
        RingPage.FAT -> Quad(day.eaten.fat, day.targets.fat.toDouble(), MacroColors.Fat, stringResource(R.string.macro_fat))
    }
    val fraction = if (goal <= 0) 0f else (eaten / goal).toFloat()
    val sweep by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(700), label = "ring")
    val over = fraction > 1.05f
    val track = c.surfaceAlt
    Box(Modifier.size(184.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 16.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(track, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
            drawArc(color, -90f, 360f * sweep, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            if (over) {
                val extra = ((fraction - 1f).coerceAtMost(1f)) * 360f
                drawArc(color.copy(alpha = 0.45f), -90f, extra, false, Offset(inset + stroke * 0.9f, inset + stroke * 0.9f),
                    Size(arcSize.width - stroke * 1.8f, arcSize.height - stroke * 1.8f), style = Stroke(stroke * 0.35f, cap = StrokeCap.Round))
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = Hyper.type.smallMedium, color = c.textMuted)
            Text(
                Bidi.ltr(eaten.roundToInt().toString()), style = Hyper.type.number, color = c.text,
            )
            val unit = if (page == RingPage.TOTAL) stringResource(R.string.unit_kcal_of, goal.roundToInt())
            else stringResource(R.string.unit_g_of, goal.roundToInt())
            Text(Bidi.isolate(unit), style = Hyper.type.small, color = c.textSecondary)
            val left = (goal - eaten).roundToInt()
            Spacer(Modifier.height(4.dp))
            HyperChip(
                if (left >= 0) stringResource(R.string.ring_left, left) else stringResource(R.string.ring_over, -left),
                color = if (left >= 0) color else c.warn, background = (if (left >= 0) color else c.warn).copy(alpha = 0.12f),
            )
        }
    }
}

private data class Quad(val eaten: Double, val goal: Double, val color: Color, val label: String)

@Composable
private fun MacroMini(label: String, eaten: Double, goal: Int, color: Color, modifier: Modifier) {
    val c = Hyper.colors
    val f by animateFloatAsState(if (goal <= 0) 0f else (eaten / goal).toFloat().coerceIn(0f, 1f), tween(600), label = "macro")
    Column(modifier) {
        Text(label, style = Hyper.type.micro, color = c.textSecondary, maxLines = 1)
        Spacer(Modifier.height(4.dp))
        Box(Modifier.fillMaxWidth().height(7.dp).clip(HyperShapes.pill).background(c.surfaceAlt)) {
            Box(Modifier.fillMaxWidth(f).height(7.dp).clip(HyperShapes.pill).background(color))
        }
        Spacer(Modifier.height(4.dp))
        Text(gRatio(eaten.roundToInt(), goal), style = Hyper.type.micro, color = c.text, maxLines = 1)
    }
}

/* ------------------------------------------------------------------ meals */

@Composable
private fun MealsBox(day: NutritionDay, deps: NutritionDeps, interactive: Boolean) {
    val c = Hyper.colors
    HyperCard(Modifier.fillMaxWidth(), padding = PaddingValues(top = 16.dp, bottom = 8.dp)) {
        Row(Modifier.padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.box_nutrition_meals), style = Hyper.type.title, color = c.text, modifier = Modifier.weight(1f))
            HyperChip(kcalLabel(kcalInt(day.eaten)), icon = R.drawable.ic_local_fire_department)
        }
        if (day.entries.isEmpty()) {
            Text(stringResource(R.string.meals_empty_hint), style = Hyper.type.small, color = c.textMuted, modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp))
        }
        day.meals.forEachIndexed { i, meal ->
            if (i > 0) Box(Modifier.padding(horizontal = 18.dp).fillMaxWidth().height(0.8.dp).background(c.border))
            MealSection(meal, day.inMeal(meal), deps, interactive)
        }
    }
}

@Composable
private fun MealSection(meal: MealType, entries: List<MealEntry>, deps: NutritionDeps, interactive: Boolean) {
    val c = Hyper.colors
    val total = entries.map { it.total }.sum()
    Row(Modifier.fillMaxWidth().padding(start = 18.dp, end = 10.dp, top = 12.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        IconTile(mealIcon(meal), size = 34.dp, iconSize = 18.dp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(mealName(meal), style = Hyper.type.bodyMedium, color = c.text)
            if (entries.isNotEmpty()) Text(bidiJoin(listOf(kcalLabel(kcalInt(total)), macroLine(total))), style = Hyper.type.micro, color = c.textMuted, maxLines = 1)
        }
        Box(
            Modifier.size(38.dp).clip(CircleShape).background(c.accentSoft).hyperClickable(enabled = interactive) { deps.onAdd(meal) },
            contentAlignment = Alignment.Center,
        ) { Icon(painterResource(R.drawable.ic_add), stringResource(R.string.add_to_meal, mealName(meal)), tint = c.accent, modifier = Modifier.size(20.dp)) }
    }
    entries.forEach { e ->
        androidx.compose.runtime.key(e.id) {
            SwipeRow(e, enabled = interactive, onDelete = { deps.onDelete(e) }) {
                EntryRow(e, onClick = { if (interactive) deps.onEdit(e) })
            }
        }
    }
}

@Composable
private fun SwipeRow(e: MealEntry, enabled: Boolean, onDelete: () -> Unit, content: @Composable () -> Unit) {
    val c = Hyper.colors
    val state = rememberSwipeToDismissBoxState()
    SwipeToDismissBox(
        state = state,
        gesturesEnabled = enabled,
        onDismiss = { if (it != SwipeToDismissBoxValue.Settled) onDelete() },
        backgroundContent = {
            val active = state.dismissDirection != SwipeToDismissBoxValue.Settled
            val bg by animateColorAsState(if (active) c.danger else Color.Transparent, tween(150), label = "swipeBg")
            Row(
                Modifier.fillMaxSize().background(bg).padding(horizontal = 22.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (state.dismissDirection == SwipeToDismissBoxValue.StartToEnd) Arrangement.Start else Arrangement.End,
            ) { Icon(painterResource(R.drawable.ic_delete), stringResource(R.string.delete), tint = Color.White, modifier = Modifier.size(22.dp)) }
        },
    ) { Box(Modifier.background(c.surface)) { content() } }
}

@Composable
fun EntryRow(e: MealEntry, onClick: () -> Unit) {
    val c = Hyper.colors
    val lang = LocalAppLanguage.current
    Row(
        Modifier.fillMaxWidth().hyperClickable(scaleOnPress = false, onClick = onClick).padding(horizontal = 18.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FoodImage(e.art, size = 48.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(e.name(lang), style = Hyper.type.bodyMedium, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val amount = if (e.unitId == "g") gLabel(e.grams) else bidiJoin(listOf("${qtyText(e.qty)} ${unitText(e.unitName(lang), lang)}", gLabel(e.grams)))
            Text(amount, style = Hyper.type.small, color = c.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (e.addons.isNotEmpty()) {
                Text(
                    "+ " + e.addons.joinToString("، ") { it.name(lang) + if (it.qty != 1.0) " ×${qtyText(it.qty)}" else "" },
                    style = Hyper.type.micro, color = c.accent, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(kcalLabel(kcalInt(e.total)), style = Hyper.type.smallMedium, color = c.text)
            Text(macroLine(e.total), style = Hyper.type.micro, color = c.textMuted)
        }
    }
}

/* ------------------------------------------------------------------ protein spread */

@Composable
private fun ProteinBox(day: NutritionDay) {
    val c = Hyper.colors
    val per = ProteinSpread.perMeal(day.entries)
    val target = ProteinSpread.mealTarget(day.targets)
    HyperCard(Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.box_nutrition_protein), style = Hyper.type.title, color = c.text)
        Text(stringResource(R.string.protein_spread_hint, gLabel(target.toDouble())), style = Hyper.type.small, color = c.textMuted)
        Spacer(Modifier.height(12.dp))
        ProteinSpread.Main.forEach { m ->
            val v = per[m] ?: 0.0
            val f by animateFloatAsState(if (target <= 0) 0f else (v / target).toFloat().coerceIn(0f, 1f), tween(600), label = "pm")
            Row(Modifier.padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(mealName(m), style = Hyper.type.small, color = c.textSecondary, modifier = Modifier.width(78.dp), maxLines = 1)
                Box(Modifier.weight(1f).height(10.dp).clip(HyperShapes.pill).background(c.surfaceAlt)) {
                    Box(Modifier.fillMaxWidth(f).height(10.dp).clip(HyperShapes.pill).background(if (v >= target * 0.9) c.success else MacroColors.Protein))
                }
                Spacer(Modifier.width(10.dp))
                Text(gRatio(v.roundToInt(), target), style = Hyper.type.micro, color = c.text, modifier = Modifier.width(62.dp), textAlign = TextAlign.End)
            }
        }
    }
}

/* ------------------------------------------------------------------ undo */

@Composable
private fun UndoBar(entry: MealEntry?, onUndo: () -> Unit, modifier: Modifier) {
    val c = Hyper.colors
    val lang = LocalAppLanguage.current
    AnimatedVisibility(entry != null, modifier, enter = slideInVertically { it } + fadeIn(), exit = slideOutVertically { it } + fadeOut()) {
        val e = remember(entry?.id) { entry } ?: return@AnimatedVisibility
        Row(
            Modifier.then(
                if (com.hyperathlete.app.ui.theme.LocalFloatingBarInset.current > 0.dp) Modifier.padding(bottom = com.hyperathlete.app.ui.theme.LocalFloatingBarInset.current)
                else Modifier.navigationBarsPadding(),
            ).padding(16.dp).fillMaxWidth().clip(HyperShapes.cardSmall).background(c.text).padding(start = 18.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.entry_deleted, e.name(lang)), style = Hyper.type.small, color = c.bg, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                stringResource(R.string.undo_set), style = Hyper.type.bodyMedium, color = c.accentLight,
                modifier = Modifier.clip(HyperShapes.pill).hyperClickable(onClick = onUndo).padding(horizontal = 14.dp, vertical = 10.dp),
            )
        }
    }
}

/* ------------------------------------------------------------------ month calendar */

@Composable
fun MonthCalendar(selected: LocalDate, today: LocalDate, month: (YearMonth) -> Flow<Map<LocalDate, Adherence>>, onPick: (LocalDate) -> Unit) {
    val c = Hyper.colors
    val locale = LocalConfiguration.current.locales[0]
    var shown by remember { mutableStateOf(YearMonth.from(selected)) }
    val flow = remember(shown) { month(shown) }
    val marks by flow.collectAsState(initial = emptyMap())
    Column(Modifier.padding(horizontal = 20.dp).padding(top = 8.dp, bottom = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StepArrow(R.drawable.ic_chevron_left, stringResource(R.string.month_previous)) { shown = shown.minusMonths(1) }
            Text(
                shown.atDay(1).format(DateTimeFormatter.ofPattern("MMMM yyyy", locale)), style = Hyper.type.h2, color = c.text,
                textAlign = TextAlign.Center, modifier = Modifier.weight(1f),
            )
            StepArrow(R.drawable.ic_chevron_right, stringResource(R.string.month_next)) { shown = shown.plusMonths(1) }
        }
        Spacer(Modifier.height(14.dp))
        val week = listOf(DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY)
        Row(Modifier.fillMaxWidth()) {
            week.forEach { d -> Text(dayShort(d), style = Hyper.type.micro, color = c.textMuted, textAlign = TextAlign.Center, modifier = Modifier.weight(1f)) }
        }
        Spacer(Modifier.height(6.dp))
        val first = shown.atDay(1)
        val lead = (first.dayOfWeek.value % 7) // Sunday = 0
        val cells = lead + shown.lengthOfMonth()
        val rows = (cells + 6) / 7
        for (r in 0 until rows) {
            Row(Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val n = r * 7 + col - lead + 1
                    Box(Modifier.weight(1f).height(46.dp), contentAlignment = Alignment.Center) {
                        if (n in 1..shown.lengthOfMonth()) {
                            val d = shown.atDay(n)
                            DayCell(d, marks[d] ?: Adherence.NONE, d == selected, d == today) { onPick(d) }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Legend(c.success, stringResource(R.string.adherence_good))
            Legend(c.warn, stringResource(R.string.adherence_close))
            Legend(c.danger, stringResource(R.string.adherence_missed))
        }
    }
}

@Composable
private fun DayCell(d: LocalDate, mark: Adherence, selected: Boolean, today: Boolean, onClick: () -> Unit) {
    val c = Hyper.colors
    val (bg, fg) = when {
        selected -> c.accent to c.onAccent
        mark == Adherence.GOOD -> c.successSoft to c.success
        mark == Adherence.CLOSE -> c.warnSoft to c.warn
        mark == Adherence.MISSED -> c.dangerSoft to c.danger
        else -> Color.Transparent to c.text
    }
    Box(
        Modifier.size(40.dp).clip(CircleShape).background(bg)
            .then(if (today && !selected) Modifier.border(1.6.dp, c.accent, CircleShape) else Modifier)
            .hyperClickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(Bidi.ltr(d.dayOfMonth.toString()), style = Hyper.type.smallMedium, color = fg) }
}

@Composable
private fun Legend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(label, style = Hyper.type.micro, color = Hyper.colors.textSecondary)
    }
}
