package com.hyperathlete.app.ui.nutrition

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hyperathlete.app.R
import com.hyperathlete.app.core.locale.Bidi
import com.hyperathlete.app.core.locale.LocalAppLanguage
import com.hyperathlete.app.core.locale.bidiJoin
import com.hyperathlete.app.domain.nutrition.Addon
import com.hyperathlete.app.domain.nutrition.CustomFood
import com.hyperathlete.app.domain.nutrition.Food
import com.hyperathlete.app.domain.nutrition.MealType
import com.hyperathlete.app.ui.components.ConfirmDialog
import com.hyperathlete.app.ui.components.EmptyState
import com.hyperathlete.app.ui.components.HyperTextField
import com.hyperathlete.app.ui.components.PrimaryButton
import com.hyperathlete.app.ui.components.SecondaryButton
import com.hyperathlete.app.ui.components.TextAction
import com.hyperathlete.app.ui.components.hyperClickable
import com.hyperathlete.app.ui.components.rememberHaptics
import com.hyperathlete.app.ui.main.HyperSheet
import com.hyperathlete.app.ui.theme.Hyper
import com.hyperathlete.app.ui.theme.glassBackdrop
import com.hyperathlete.app.ui.theme.glass
import com.hyperathlete.app.ui.theme.HyperShapes
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

/** Selection under the search field: one of the three lists or a food category. */
private const val SEL_RECENT = "@recent"
private const val SEL_FAV = "@fav"
private const val SEL_MINE = "@mine"

@Composable
fun AddFoodRoute(onBack: () -> Unit, onCreateFood: (String?) -> Unit, onEditCustom: (Long) -> Unit, onCreateRecipe: () -> Unit) {
    val vm: AddFoodViewModel = hiltViewModel()
    val ui by vm.ui.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val results by vm.results.collectAsStateWithLifecycle()
    val portion by vm.portion.collectAsStateWithLifecycle()
    val barcode by vm.barcode.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.done.collect { onBack() } }
    val context = LocalContext.current
    val u = ui
    // Google's scanner isn't on the phone: the app's own camera scanner instead.
    var cameraScan by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    if (cameraScan) CameraBarcodeScreen(onCode = { cameraScan = false; vm.lookupBarcode(it) }, onDismiss = { cameraScan = false })
    if (vm.isEditing) {
        // Editing goes straight to the portion sheet over a blank page.
        Box(Modifier.fillMaxSize().glassBackdrop(Hyper.glass))
    } else if (u != null) {
        AddFoodContent(
            meal = vm.meal, date = vm.date, ui = u, query = query, results = results,
            onQuery = { vm.query.value = it }, onBack = onBack, onOpen = vm::open, onFavorite = vm::toggleFavorite,
            onScan = { BarcodeScanner.scan(context, vm::lookupBarcode, onFallback = { cameraScan = true }) },
            onCreateFood = { onCreateFood(null) }, onEditCustom = onEditCustom, onCreateRecipe = onCreateRecipe,
        )
    }
    portion?.let { p ->
        HyperSheet(vm::dismissPortion) {
            PortionSheet(
                p, favorite = p.food.id in (u?.favoriteIds ?: emptySet()),
                onUnit = vm::setUnit, onQty = vm::setQty, onMeal = vm::setMeal, onFavorite = { vm.toggleFavorite(p.food) },
                onToggleAddon = vm::toggleAddon, onAddonQty = vm::setAddonQty, onStep = vm::showAddons, onSave = vm::save,
            )
        }
    }
    when (val b = barcode) {
        BarcodeState.Looking -> HyperSheet(vm::clearBarcode) {
            Row(Modifier.fillMaxWidth().padding(28.dp), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(24.dp), color = Hyper.colors.accent, strokeWidth = 2.6.dp)
                Spacer(Modifier.width(14.dp))
                Text(stringResource(R.string.barcode_looking), style = Hyper.type.bodyMedium, color = Hyper.colors.text)
            }
        }
        is BarcodeState.NotFound -> ConfirmDialog(
            title = stringResource(R.string.barcode_not_found_title),
            message = stringResource(R.string.barcode_not_found_body),
            confirm = stringResource(R.string.barcode_add_own),
            onConfirm = { vm.clearBarcode(); onCreateFood(b.code) },
            onDismiss = vm::clearBarcode,
        )
        BarcodeState.Offline -> ConfirmDialog(
            title = stringResource(R.string.barcode_offline_title),
            message = stringResource(R.string.barcode_offline_body),
            confirm = stringResource(R.string.ok),
            onConfirm = vm::clearBarcode,
            onDismiss = vm::clearBarcode,
        )
        BarcodeState.Idle -> Unit
    }
}

@Composable
fun AddFoodContent(
    meal: MealType,
    date: LocalDate,
    ui: AddFoodUi,
    query: String,
    results: List<Food>,
    onQuery: (String) -> Unit,
    onBack: () -> Unit,
    onOpen: (Food) -> Unit,
    onFavorite: (Food) -> Unit,
    onScan: () -> Unit,
    onCreateFood: () -> Unit,
    onEditCustom: (Long) -> Unit,
    onCreateRecipe: () -> Unit,
    initialSelection: String? = null,
) {
    val c = Hyper.colors
    val locale = LocalConfiguration.current.locales[0]
    val default = initialSelection ?: if (ui.recents.isNotEmpty()) SEL_RECENT else ui.catalog.db.categories.first().id
    var sel by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(default) }
    Column(Modifier.fillMaxSize().glassBackdrop(Hyper.glass).statusBarsPadding().imePadding()) {
        Row(Modifier.padding(start = 8.dp, end = 12.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            CircleButton(R.drawable.ic_arrow_back, stringResource(R.string.back), onBack)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.add_food), style = Hyper.type.h2, color = c.text, maxLines = 1)
                Text(bidiJoin(listOf(mealName(meal), date.format(DateTimeFormatter.ofPattern("EEEE d MMMM", locale)))), style = Hyper.type.small, color = c.textMuted, maxLines = 1)
            }
            CircleButton(R.drawable.ic_barcode, stringResource(R.string.barcode_scan), onScan)
        }
        HyperTextField(
            query, onQuery,
            Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            placeholder = stringResource(R.string.food_search_hint), leadingIcon = R.drawable.ic_search, textStyle = Hyper.type.bodyMedium,
        )
        if (query.isNotBlank()) {
            if (results.isEmpty()) {
                EmptyState(
                    R.drawable.ic_search, stringResource(R.string.food_none_title, query), stringResource(R.string.food_none_body),
                    Modifier.padding(20.dp), action = stringResource(R.string.food_new), onAction = onCreateFood,
                )
            } else {
                FoodList(results, ui, onOpen, onFavorite, onEditCustom)
            }
            return@Column
        }
        val chips = buildList {
            add(SEL_RECENT to stringResource(R.string.food_recent))
            add(SEL_FAV to stringResource(R.string.food_favorites))
            add(SEL_MINE to stringResource(R.string.food_mine))
            ui.catalog.db.categories.forEach { add(it.id to it.label()) }
        }
        val chipState = androidx.compose.foundation.lazy.rememberLazyListState()
        LaunchedEffect(Unit) { chips.indexOfFirst { it.first == sel }.takeIf { it > 1 }?.let { chipState.scrollToItem(it - 1) } }
        LazyRow(state = chipState, contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(chips, key = { it.first }) { (id, label) -> SelectChip(label, id == sel) { sel = id } }
        }
        Spacer(Modifier.height(8.dp))
        when (sel) {
            SEL_RECENT -> if (ui.recents.isEmpty()) EmptyHint(R.drawable.ic_history, R.string.food_recent_empty) else FoodList(ui.recents, ui, onOpen, onFavorite, onEditCustom)
            SEL_FAV -> if (ui.favorites.isEmpty()) EmptyHint(R.drawable.ic_star, R.string.food_fav_empty) else FoodList(ui.favorites, ui, onOpen, onFavorite, onEditCustom)
            SEL_MINE -> FoodList(
                ui.catalog.customFoods, ui, onOpen, onFavorite, onEditCustom,
                header = {
                    Row(Modifier.padding(horizontal = 20.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SecondaryButton(stringResource(R.string.food_new), onCreateFood, Modifier.weight(1f), container = c.accentSoft, content = c.accent)
                        SecondaryButton(stringResource(R.string.recipe_new), onCreateRecipe, Modifier.weight(1f), container = c.accentSoft, content = c.accent)
                    }
                    if (ui.catalog.customFoods.isEmpty()) {
                        Text(stringResource(R.string.food_mine_empty), style = Hyper.type.small, color = c.textMuted, modifier = Modifier.padding(20.dp))
                    }
                },
            )
            else -> FoodList(ui.catalog.db.inCategory(sel), ui, onOpen, onFavorite, onEditCustom)
        }
    }
}

@Composable
private fun EmptyHint(icon: Int, text: Int) {
    EmptyState(icon, stringResource(text), stringResource(R.string.food_browse_hint), Modifier.padding(20.dp))
}

@Composable
private fun CircleButton(icon: Int, description: String, onClick: () -> Unit) {
    val c = Hyper.colors
    Box(Modifier.size(44.dp).clip(CircleShape).hyperClickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(painterResource(icon), description, tint = c.text, modifier = Modifier.size(24.dp))
    }
}

@Composable
fun SelectChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val c = Hyper.colors
    Text(
        label, style = Hyper.type.smallMedium, color = if (selected) c.onAccent else c.textSecondary, maxLines = 1,
        modifier = Modifier.clip(HyperShapes.pill).background(if (selected) c.accent else c.surfaceAlt)
            .hyperClickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 9.dp),
    )
}

@Composable
private fun FoodList(
    foods: List<Food>,
    ui: AddFoodUi,
    onOpen: (Food) -> Unit,
    onFavorite: (Food) -> Unit,
    onEditCustom: (Long) -> Unit,
    header: (@Composable () -> Unit)? = null,
) {
    LazyColumn(contentPadding = PaddingValues(bottom = 32.dp)) {
        if (header != null) item(key = "header") { header() }
        items(foods, key = { it.id }) { f ->
            FoodRow(
                f, f.id in ui.favoriteIds, onClick = { onOpen(f) }, onFavorite = { onFavorite(f) },
                onEdit = if (f.isCustom) ({ f.id.removePrefix(Food.CUSTOM_PREFIX).toLongOrNull()?.let(onEditCustom) }) else null,
            )
        }
    }
}

@Composable
fun FoodRow(f: Food, favorite: Boolean, onClick: () -> Unit, onFavorite: () -> Unit, onEdit: (() -> Unit)? = null) {
    val c = Hyper.colors
    val lang = LocalAppLanguage.current
    val u = f.defaultUnit
    Row(
        Modifier.fillMaxWidth().hyperClickable(scaleOnPress = false, onClick = onClick).padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FoodImage(f.art, size = 50.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(f.label(), style = Hyper.type.bodyMedium, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val qty = f.defaultQty(u)
            val amount = if (u.id == "g") gLabel(qty) else "${qtyText(qty)} ${unitText(u.name(lang), lang)}"
            Text(bidiJoin(listOf(amount, kcalLabel(kcalInt(f.macros(u, qty))))), style = Hyper.type.small, color = c.textMuted, maxLines = 1)
        }
        if (onEdit != null) {
            Box(Modifier.size(40.dp).clip(CircleShape).hyperClickable(onClick = onEdit), contentAlignment = Alignment.Center) {
                Icon(painterResource(R.drawable.ic_edit), stringResource(R.string.action_edit), tint = c.textMuted, modifier = Modifier.size(20.dp))
            }
        }
        Box(Modifier.size(40.dp).clip(CircleShape).hyperClickable(onClick = onFavorite), contentAlignment = Alignment.Center) {
            Icon(
                painterResource(if (favorite) R.drawable.ic_star_fill else R.drawable.ic_star), stringResource(R.string.food_favorite),
                tint = if (favorite) c.accent else c.textMuted, modifier = Modifier.size(22.dp),
            )
        }
    }
}

/* ------------------------------------------------------------------ portion + add-ons sheet */

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PortionSheet(
    p: Portion,
    favorite: Boolean,
    onUnit: (com.hyperathlete.app.domain.nutrition.ServingUnit) -> Unit,
    onQty: (Double) -> Unit,
    onMeal: (MealType) -> Unit,
    onFavorite: () -> Unit,
    onToggleAddon: (Addon) -> Unit,
    onAddonQty: (Addon, Double) -> Unit,
    onStep: (Boolean) -> Unit,
    onSave: () -> Unit,
) {
    AnimatedContent(p.addonStep, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "portionStep") { addons ->
        if (!addons) PortionStep(p, favorite, onUnit, onQty, onMeal, onFavorite, onStep, onSave)
        else AddonStep(p, onToggleAddon, onAddonQty, onBack = { onStep(false) }, onSave = onSave)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PortionStep(
    p: Portion,
    favorite: Boolean,
    onUnit: (com.hyperathlete.app.domain.nutrition.ServingUnit) -> Unit,
    onQty: (Double) -> Unit,
    onMeal: (MealType) -> Unit,
    onFavorite: () -> Unit,
    onStep: (Boolean) -> Unit,
    onSave: () -> Unit,
) {
    val c = Hyper.colors
    val lang = LocalAppLanguage.current
    Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(top = 6.dp, bottom = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FoodImage(p.food.art, size = 72.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(p.food.label(), style = Hyper.type.h2, color = c.text, maxLines = 2)
                Text(
                    stringResource(R.string.per_100g, kcalInt(p.food.macros100)), style = Hyper.type.small, color = c.textMuted,
                )
            }
            Box(Modifier.size(44.dp).clip(CircleShape).hyperClickable(onClick = onFavorite), contentAlignment = Alignment.Center) {
                Icon(painterResource(if (favorite) R.drawable.ic_star_fill else R.drawable.ic_star), stringResource(R.string.food_favorite), tint = if (favorite) c.accent else c.textMuted, modifier = Modifier.size(24.dp))
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.portion_unit), style = Hyper.type.smallMedium, color = c.textSecondary)
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            p.food.allUnits.forEach { u ->
                val label = if (u.id == "g") u.name(lang) else bidiJoin(listOf(u.name(lang), gLabel(u.g)), " ")
                SelectChip(label, u.id == p.unit.id) { onUnit(u) }
            }
        }
        Spacer(Modifier.height(14.dp))
        QtyStepper(p.qty, if (p.unit.g <= 1.0) 10.0 else 0.5, p.unit.name(lang), onQty)
        if (p.unit.g > 1.0) {
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0.5, 1.0, 1.5, 2.0, 3.0).forEach { q -> SelectChip(qtyText(q), p.qty == q) { onQty(q) } }
            }
        }
        Spacer(Modifier.height(16.dp))
        MacroStrip(p)
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.portion_meal), style = Hyper.type.smallMedium, color = c.textSecondary)
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            MealType.entries.forEach { m -> SelectChip(mealName(m), m == p.meal) { onMeal(m) } }
        }
        Spacer(Modifier.height(20.dp))
        if (p.addonOptions.isNotEmpty() || p.keptAddons.isNotEmpty()) {
            val n = p.loggedAddons.size
            if (p.editing != null) {
                PrimaryButton(stringResource(R.string.save), onSave, icon = R.drawable.ic_check)
                Spacer(Modifier.height(10.dp))
                SecondaryButton(if (n > 0) stringResource(R.string.addons_count, n) else stringResource(R.string.addons_question), { onStep(true) }, Modifier.fillMaxWidth())
            } else {
                PrimaryButton(stringResource(R.string.action_next), { onStep(true) }, icon = R.drawable.ic_arrow_forward)
                Spacer(Modifier.height(6.dp))
                TextAction(stringResource(R.string.add_without_addons), onSave, Modifier.align(Alignment.CenterHorizontally))
            }
        } else {
            PrimaryButton(stringResource(if (p.editing != null) R.string.save else R.string.add_food), onSave, icon = R.drawable.ic_check)
        }
    }
}

@Composable
private fun MacroStrip(p: Portion) {
    val c = Hyper.colors
    val m = p.total
    Row(
        Modifier.fillMaxWidth().clip(HyperShapes.cardSmall).background(c.surfaceAlt).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1.3f)) {
            Text(Bidi.ltr(m.kcal.roundToInt().toString()), style = Hyper.type.h1, color = c.accent)
            Text(stringResource(R.string.ring_kcal), style = Hyper.type.micro, color = c.textMuted)
        }
        MacroCell(stringResource(R.string.macro_protein), m.protein, MacroColors.Protein, Modifier.weight(1f))
        MacroCell(stringResource(R.string.macro_carbs), m.carbs, MacroColors.Carbs, Modifier.weight(1f))
        MacroCell(stringResource(R.string.macro_fat), m.fat, MacroColors.Fat, Modifier.weight(1f))
    }
}

@Composable
private fun MacroCell(label: String, v: Double, color: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(gLabel(v), style = Hyper.type.title, color = color, maxLines = 1)
        Text(label, style = Hyper.type.micro, color = Hyper.colors.textMuted, maxLines = 1)
    }
}

/** Amount with − / + and a readable value ("1.5", "250"). */
@Composable
fun QtyStepper(value: Double, step: Double, unit: String, onChange: (Double) -> Unit) {
    val c = Hyper.colors
    val haptics = rememberHaptics()
    Row(
        Modifier.fillMaxWidth().clip(HyperShapes.card).background(c.surface).border(1.dp, c.border, HyperShapes.card).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepCircle(R.drawable.ic_remove, value - step > 0) { haptics.tick(); onChange(((value - step) / step).roundToInt() * step) }
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.Bottom) {
            Text(Bidi.ltr(qtyText(value)), style = Hyper.type.number, color = c.text)
            Spacer(Modifier.width(6.dp))
            Text(unit, style = Hyper.type.bodyMedium, color = c.textSecondary, modifier = Modifier.padding(bottom = 6.dp), maxLines = 1)
        }
        StepCircle(R.drawable.ic_add, true) { haptics.tick(); onChange(((value + step) / step).roundToInt() * step) }
    }
}

@Composable
private fun StepCircle(icon: Int, enabled: Boolean, onClick: () -> Unit) {
    val c = Hyper.colors
    Box(
        Modifier.size(48.dp).clip(CircleShape).background(if (enabled) c.accentSoft else c.lockSoft).hyperClickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(painterResource(icon), null, tint = if (enabled) c.accent else c.lock, modifier = Modifier.size(22.dp)) }
}

@Composable
private fun AddonStep(p: Portion, onToggle: (Addon) -> Unit, onQty: (Addon, Double) -> Unit, onBack: () -> Unit, onSave: () -> Unit) {
    val c = Hyper.colors
    Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(top = 4.dp, bottom = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircleButton(R.drawable.ic_arrow_back, stringResource(R.string.back), onBack)
            Spacer(Modifier.width(6.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.addons_question), style = Hyper.type.h2, color = c.text)
                Text(stringResource(R.string.addons_hint), style = Hyper.type.small, color = c.textMuted)
            }
        }
        Spacer(Modifier.height(14.dp))
        p.addonOptions.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { a -> AddonCard(a, p.addons[a.id] ?: 0.0, { onToggle(a) }, { q -> onQty(a, q) }, Modifier.weight(1f)) }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        Spacer(Modifier.height(6.dp))
        MacroStrip(p)
        Spacer(Modifier.height(16.dp))
        val n = p.loggedAddons.size
        PrimaryButton(
            if (p.editing != null) stringResource(R.string.save) else if (n == 0) stringResource(R.string.add_without_addons) else stringResource(R.string.add_with_addons, n),
            onSave, icon = R.drawable.ic_check,
        )
    }
}

@Composable
private fun AddonCard(a: Addon, qty: Double, onToggle: () -> Unit, onQty: (Double) -> Unit, modifier: Modifier) {
    val c = Hyper.colors
    val lang = LocalAppLanguage.current
    val on = qty > 0
    Column(
        modifier.clip(HyperShapes.cardSmall).background(if (on) c.accentSoft else c.surfaceAlt)
            .border(if (on) 1.6.dp else 0.dp, if (on) c.accent else c.surfaceAlt, HyperShapes.cardSmall)
            .hyperClickable(onClick = onToggle).padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        FoodImage(a.art, size = 60.dp, tile = false)
        Text(a.label(), style = Hyper.type.smallMedium, color = c.text, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        Text(unitText(a.unit.name(lang), lang), style = Hyper.type.micro, color = c.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(kcalLabel(kcalInt(a.macros(1.0))), style = Hyper.type.micro, color = c.textMuted, maxLines = 1)
        if (on) {
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                MiniStep(R.drawable.ic_remove) { onQty(((qty - 0.5) * 2).roundToInt() / 2.0) }
                Text(Bidi.ltr(qtyText(qty)), style = Hyper.type.bodyMedium, color = c.accent, modifier = Modifier.width(34.dp), textAlign = TextAlign.Center)
                MiniStep(R.drawable.ic_add) { onQty(((qty + 0.5) * 2).roundToInt() / 2.0) }
            }
        }
    }
}

@Composable
private fun MiniStep(icon: Int, onClick: () -> Unit) {
    val c = Hyper.colors
    Box(Modifier.size(28.dp).clip(CircleShape).background(c.surface).hyperClickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(painterResource(icon), null, tint = c.accent, modifier = Modifier.size(16.dp))
    }
}
