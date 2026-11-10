package com.hyperathlete.app.ui.nutrition

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.hyperathlete.app.R
import com.hyperathlete.app.core.locale.LocalAppLanguage
import com.hyperathlete.app.core.locale.bidiJoin
import com.hyperathlete.app.data.nutrition.FoodCatalog
import com.hyperathlete.app.data.nutrition.NutritionRepository
import com.hyperathlete.app.domain.nutrition.CustomFood
import com.hyperathlete.app.domain.nutrition.Food
import com.hyperathlete.app.domain.nutrition.Macros
import com.hyperathlete.app.domain.nutrition.RecipeIngredient
import com.hyperathlete.app.domain.nutrition.ServingUnit
import com.hyperathlete.app.domain.nutrition.sum
import com.hyperathlete.app.ui.components.ConfirmDialog
import com.hyperathlete.app.ui.components.HyperTextField
import com.hyperathlete.app.ui.components.PrimaryButton
import com.hyperathlete.app.ui.components.Segmented
import com.hyperathlete.app.ui.components.TextAction
import com.hyperathlete.app.ui.components.hyperClickable
import com.hyperathlete.app.ui.main.HyperSheet
import com.hyperathlete.app.ui.theme.Hyper
import com.hyperathlete.app.ui.theme.glassBackdrop
import com.hyperathlete.app.ui.theme.glass
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Form for an own food or a recipe. An own food is filled in one of two ways: the values printed on a
 * label (per serving), or every ingredient that goes into it, with the app adding up the numbers.
 */
data class OwnFoodForm(
    val name: String = "",
    val servingName: String = "",
    val grams: String = "100",
    val kcal: String = "",
    val protein: String = "",
    val carbs: String = "",
    val fat: String = "",
    val servings: Double = 4.0,
    val ingredients: List<RecipeIngredient> = emptyList(),
    val fromIngredients: Boolean = false,
) {
    private fun num(s: String) = s.replace(',', '.').toDoubleOrNull()
    val perServing: Macros get() = Macros(num(kcal) ?: 0.0, num(protein) ?: 0.0, num(carbs) ?: 0.0, num(fat) ?: 0.0)
    fun gramsOrNull(): Double? = num(grams)?.takeIf { it > 0 }
    fun validFood(): Boolean = name.isNotBlank() && gramsOrNull() != null && num(kcal) != null
    fun validRecipe(): Boolean = name.isNotBlank() && ingredients.isNotEmpty() && servings >= 1
    val total: Macros get() = ingredients.map { it.macros }.sum()
    val totalGrams: Double get() = ingredients.sumOf { it.grams }
    val recipeServing: Macros get() = total * (1.0 / servings.coerceAtLeast(1.0))
    fun valid(recipe: Boolean): Boolean = when {
        recipe || fromIngredients -> validRecipe()
        else -> validFood()
    }
}

@HiltViewModel
class OwnFoodViewModel @Inject constructor(saved: SavedStateHandle, private val repo: NutritionRepository) : ViewModel() {
    val id: Long = saved.get<Long>("id") ?: 0L
    val recipe: Boolean = saved.get<Boolean>("recipe") ?: false
    private val barcode: String? = saved.get<String>("barcode")?.takeIf { it.isNotBlank() }

    val form = MutableStateFlow(OwnFoodForm(servings = if (recipe) 4.0 else 1.0))
    val catalog: StateFlow<FoodCatalog?> = repo.catalog.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    private val _done = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val done = _done

    init {
        if (id > 0) viewModelScope.launch {
            val f = repo.catalog.first().custom.firstOrNull { it.id == id } ?: return@launch
            val per = f.per100.forGrams(f.servingGrams)
            form.value = OwnFoodForm(
                name = f.name, servingName = f.servingName, grams = qtyText(f.servingGrams),
                kcal = qtyText(per.kcal), protein = qtyText(per.protein), carbs = qtyText(per.carbs), fat = qtyText(per.fat),
                servings = f.servings ?: 1.0, ingredients = f.ingredients,
                fromIngredients = !f.recipe && f.ingredients.isNotEmpty(),
            )
        }
    }

    fun update(f: (OwnFoodForm) -> OwnFoodForm) { form.value = f(form.value) }

    fun save(defaultServing: String) {
        val f = form.value
        viewModelScope.launch {
            val serving = f.servingName.ifBlank { defaultServing }
            val food = when {
                recipe -> CustomFood.recipe(f.name, f.ingredients, f.servings, serving)
                f.fromIngredients -> CustomFood.fromIngredients(f.name, f.ingredients, f.servings, serving).copy(barcode = barcode)
                else -> CustomFood.fromServing(f.name, serving, f.gramsOrNull() ?: 100.0, f.perServing, barcode)
            }
            repo.saveCustom(food.copy(id = id))
            _done.tryEmit(Unit)
        }
    }

    fun delete() {
        viewModelScope.launch { repo.deleteCustom(id); _done.tryEmit(Unit) }
    }
}

@Composable
fun OwnFoodRoute(onBack: () -> Unit) {
    val vm: OwnFoodViewModel = hiltViewModel()
    val form by vm.form.collectAsStateWithLifecycle()
    val catalog by vm.catalog.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.done.collect { onBack() } }
    OwnFoodScreen(vm.recipe, vm.id > 0, form, catalog, vm::update, vm::save, vm::delete, onBack)
}

@Composable
fun OwnFoodScreen(
    recipe: Boolean,
    editing: Boolean,
    form: OwnFoodForm,
    catalog: FoodCatalog?,
    onChange: ((OwnFoodForm) -> OwnFoodForm) -> Unit,
    onSave: (String) -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit,
) {
    val c = Hyper.colors
    var confirmDelete by remember { mutableStateOf(false) }
    var picking by remember { mutableStateOf(false) }
    /** Index of the ingredient being changed, or -1 while adding a new one. */
    var editIndex by remember { mutableStateOf(-1) }
    val defaultServing = stringResource(if (recipe) R.string.serving_portion else R.string.serving_default)
    Column(Modifier.fillMaxSize().glassBackdrop(Hyper.glass).statusBarsPadding().imePadding()) {
        Row(Modifier.padding(start = 8.dp, end = 12.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(CircleShape).hyperClickable(onClick = onBack), contentAlignment = Alignment.Center) {
                Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.back), tint = c.text, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(if (recipe) (if (editing) R.string.recipe_edit else R.string.recipe_new) else (if (editing) R.string.food_edit else R.string.food_new)),
                style = Hyper.type.h2, color = c.text, modifier = Modifier.weight(1f),
            )
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp)) {
            Label(R.string.own_name)
            HyperTextField(form.name, { v -> onChange { it.copy(name = v) } }, placeholder = stringResource(if (recipe) R.string.recipe_name_hint else R.string.own_name_hint), textStyle = Hyper.type.bodyMedium)
            Spacer(Modifier.height(14.dp))
            val addIngredient = { editIndex = -1; picking = true }
            val editIngredient = { i: Int -> editIndex = i; picking = true }
            if (!recipe) {
                Segmented(
                    listOf(stringResource(R.string.own_mode_label), stringResource(R.string.own_mode_ingredients)),
                    if (form.fromIngredients) 1 else 0,
                    { i -> onChange { it.copy(fromIngredients = i == 1) } },
                )
                Spacer(Modifier.height(16.dp))
            }
            if (!recipe && !form.fromIngredients) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(Modifier.weight(1.4f)) {
                        Label(R.string.own_serving_name)
                        HyperTextField(form.servingName, { v -> onChange { it.copy(servingName = v) } }, placeholder = defaultServing, textStyle = Hyper.type.bodyMedium)
                    }
                    Column(Modifier.weight(1f)) {
                        Label(R.string.own_serving_grams)
                        NumField(form.grams) { v -> onChange { it.copy(grams = v) } }
                    }
                }
                Spacer(Modifier.height(18.dp))
                Text(stringResource(R.string.own_per_serving), style = Hyper.type.title, color = c.text)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(Modifier.weight(1f)) { Label(R.string.ring_kcal); NumField(form.kcal) { v -> onChange { it.copy(kcal = v) } } }
                    Column(Modifier.weight(1f)) { Label(R.string.macro_protein_g); NumField(form.protein) { v -> onChange { it.copy(protein = v) } } }
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(Modifier.weight(1f)) { Label(R.string.macro_carbs_g); NumField(form.carbs) { v -> onChange { it.copy(carbs = v) } } }
                    Column(Modifier.weight(1f)) { Label(R.string.macro_fat_g); NumField(form.fat) { v -> onChange { it.copy(fat = v) } } }
                }
            } else if (!recipe) {
                // Own food from its ingredients: list first, then how many servings it makes.
                IngredientList(form, R.string.own_ingredients_hint, addIngredient, editIngredient, onChange)
                Spacer(Modifier.height(18.dp))
                Label(R.string.own_split)
                QtyStepper(form.servings, 1.0, form.servingName.ifBlank { defaultServing }) { v -> onChange { it.copy(servings = v.coerceAtLeast(1.0)) } }
                Spacer(Modifier.height(14.dp))
                Label(R.string.own_serving_name)
                HyperTextField(form.servingName, { v -> onChange { it.copy(servingName = v) } }, placeholder = defaultServing, textStyle = Hyper.type.bodyMedium)
                Totals(form)
            } else {
                Label(R.string.recipe_servings)
                QtyStepper(form.servings, 1.0, form.servingName.ifBlank { defaultServing }) { v -> onChange { it.copy(servings = v.coerceAtLeast(1.0)) } }
                Spacer(Modifier.height(14.dp))
                Label(R.string.own_serving_name)
                HyperTextField(form.servingName, { v -> onChange { it.copy(servingName = v) } }, placeholder = defaultServing, textStyle = Hyper.type.bodyMedium)
                Spacer(Modifier.height(18.dp))
                IngredientList(form, R.string.recipe_empty, addIngredient, editIngredient, onChange)
                Totals(form)
            }
            if (editing) {
                Spacer(Modifier.height(20.dp))
                TextAction(stringResource(R.string.delete), { confirmDelete = true }, color = c.danger)
            }
        }
        Box(Modifier.navigationBarsPadding().padding(20.dp)) {
            PrimaryButton(stringResource(R.string.save), { onSave(defaultServing) }, enabled = form.valid(recipe), icon = R.drawable.ic_check)
        }
    }
    if (confirmDelete) {
        ConfirmDialog(stringResource(R.string.own_delete_title), stringResource(R.string.own_delete_body), stringResource(R.string.delete), { confirmDelete = false }, onDelete, destructive = true)
    }
    if (picking && catalog != null) {
        val i = editIndex
        HyperSheet({ picking = false }) {
            IngredientPicker(catalog, form.ingredients.getOrNull(i)) { ing ->
                onChange { f -> f.copy(ingredients = if (i in f.ingredients.indices) f.ingredients.mapIndexed { j, o -> if (j == i) ing else o } else f.ingredients + ing) }
                picking = false
            }
        }
    }
}

@Composable
private fun IngredientList(
    form: OwnFoodForm,
    emptyText: Int,
    onAdd: () -> Unit,
    onEdit: (Int) -> Unit,
    onChange: ((OwnFoodForm) -> OwnFoodForm) -> Unit,
) {
    val c = Hyper.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.recipe_ingredients), style = Hyper.type.title, color = c.text, modifier = Modifier.weight(1f))
        TextAction(stringResource(R.string.recipe_add_ingredient), onAdd, color = c.accent)
    }
    if (form.ingredients.isEmpty()) {
        Text(stringResource(emptyText), style = Hyper.type.small, color = c.textMuted, modifier = Modifier.padding(vertical = 8.dp))
    }
    form.ingredients.forEachIndexed { i, ing ->
        IngredientRow(ing, onClick = { onEdit(i) }) { onChange { f -> f.copy(ingredients = f.ingredients.filterIndexed { j, _ -> j != i }) } }
    }
}

/** Whole-food total, and per serving when it is split. */
@Composable
private fun Totals(form: OwnFoodForm) {
    if (form.ingredients.isEmpty()) return
    val c = Hyper.colors
    Spacer(Modifier.height(14.dp))
    Column(Modifier.fillMaxWidth().clip(com.hyperathlete.app.ui.theme.HyperShapes.cardSmall).background(c.accentSoft).padding(14.dp)) {
        val t = form.total
        Text(
            bidiJoin(listOf(stringResource(R.string.own_total), gLabel(form.totalGrams), kcalLabel(kcalInt(t)))),
            style = Hyper.type.bodyMedium, color = c.text,
        )
        Text(macroLine(t), style = Hyper.type.small, color = c.textSecondary)
        if (form.servings > 1.0) {
            Spacer(Modifier.height(8.dp))
            val m = form.recipeServing
            Text(
                bidiJoin(listOf(stringResource(R.string.recipe_per_serving), gLabel(form.totalGrams / form.servings), kcalLabel(kcalInt(m)))),
                style = Hyper.type.bodyMedium, color = c.accent,
            )
            Text(macroLine(m), style = Hyper.type.small, color = c.textSecondary)
        }
    }
}

@Composable
private fun Label(text: Int) {
    Text(stringResource(text), style = Hyper.type.smallMedium, color = Hyper.colors.textSecondary, modifier = Modifier.padding(bottom = 6.dp))
}

@Composable
private fun NumField(value: String, onChange: (String) -> Unit) {
    HyperTextField(
        value, { raw -> onChange(raw.replace(',', '.').filter { it.isDigit() || it == '.' }.take(7)) },
        keyboardType = KeyboardType.Decimal, textStyle = Hyper.type.bodyMedium, placeholder = "0",
    )
}

@Composable
private fun IngredientRow(ing: RecipeIngredient, onClick: () -> Unit, onRemove: () -> Unit) {
    val c = Hyper.colors
    val lang = LocalAppLanguage.current
    Row(Modifier.fillMaxWidth().hyperClickable(scaleOnPress = false, onClick = onClick).padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        FoodImage(ing.art, size = 42.dp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(if (lang == com.hyperathlete.app.domain.model.AppLanguage.ENGLISH) ing.en else ing.ar, style = Hyper.type.bodyMedium, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val unit = if (lang == com.hyperathlete.app.domain.model.AppLanguage.ENGLISH) ing.unitEn else ing.unitAr
            Text(
                bidiJoin(listOf(if (ing.unitId == "g") gLabel(ing.grams) else "${qtyText(ing.qty)} $unit", kcalLabel(kcalInt(ing.macros)))),
                style = Hyper.type.small, color = c.textMuted, maxLines = 1,
            )
        }
        Box(Modifier.size(40.dp).clip(CircleShape).hyperClickable(onClick = onRemove), contentAlignment = Alignment.Center) {
            Icon(painterResource(R.drawable.ic_close), stringResource(R.string.delete), tint = c.textMuted, modifier = Modifier.size(20.dp))
        }
    }
}

/**
 * Search a food and pick an amount, or type an ingredient that is not in the list with its own values.
 * [initial] opens straight on an ingredient already added, to change its amount.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IngredientPicker(catalog: FoodCatalog, initial: RecipeIngredient?, onPick: (RecipeIngredient) -> Unit) {
    val c = Hyper.colors
    val lang = LocalAppLanguage.current
    val start = initial?.takeIf { !it.isFree }?.let { catalog.food(it.foodId) }
    var query by remember { mutableStateOf("") }
    var food by remember { mutableStateOf(start) }
    var unit by remember { mutableStateOf(start?.allUnits?.firstOrNull { it.id == initial?.unitId } ?: start?.defaultUnit ?: ServingUnit.GRAM) }
    var qty by remember { mutableStateOf(if (start != null) initial.qty else 100.0) }
    var free by remember { mutableStateOf(initial?.let { it.isFree || start == null } ?: false) }
    Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 16.dp)) {
        val f = food
        if (free) {
            FreeIngredient(initial?.takeIf { start == null }, onPick) { free = false }
        } else if (f == null) {
            HyperTextField(query, { query = it }, placeholder = stringResource(R.string.food_search_hint), leadingIcon = R.drawable.ic_search, textStyle = Hyper.type.bodyMedium)
            Spacer(Modifier.height(6.dp))
            Row(
                Modifier.fillMaxWidth().hyperClickable(scaleOnPress = false) { free = true }.padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(42.dp).clip(CircleShape).background(c.accentSoft), contentAlignment = Alignment.Center) {
                    Icon(painterResource(R.drawable.ic_edit), null, tint = c.accent, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.ingredient_free), style = Hyper.type.bodyMedium, color = c.accent)
                    Text(stringResource(R.string.ingredient_free_desc), style = Hyper.type.small, color = c.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            val results = remember(query, catalog) { if (query.isBlank()) emptyList() else catalog.index.search(query, 40) }
            LazyColumn(Modifier.fillMaxWidth().height(340.dp)) {
                items(results, key = { it.id }) { r ->
                    Row(
                        Modifier.fillMaxWidth().hyperClickable(scaleOnPress = false) { food = r; unit = r.defaultUnit; qty = r.defaultQty(r.defaultUnit) }.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FoodImage(r.art, size = 42.dp)
                        Spacer(Modifier.width(10.dp))
                        Text(r.label(), style = Hyper.type.bodyMedium, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FoodImage(f.art, size = 56.dp)
                Spacer(Modifier.width(12.dp))
                Text(f.label(), style = Hyper.type.h2, color = c.text, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                f.allUnits.forEach { u -> SelectChip(u.name(lang), u.id == unit.id) { unit = u; qty = f.defaultQty(u) } }
            }
            Spacer(Modifier.height(12.dp))
            QtyStepper(qty, if (unit.g <= 1.0) 10.0 else 0.5, unit.name(lang)) { qty = it }
            Spacer(Modifier.height(10.dp))
            val m = f.macros(unit, qty)
            Text(bidiJoin(listOf(kcalLabel(kcalInt(m)), macroLine(m))), style = Hyper.type.smallMedium, color = c.accent)
            Spacer(Modifier.height(16.dp))
            PrimaryButton(
                stringResource(if (initial != null) R.string.save else R.string.recipe_add_ingredient),
                { onPick(RecipeIngredient.of(f, unit, qty)) }, enabled = qty > 0, icon = if (initial != null) R.drawable.ic_check else R.drawable.ic_add,
            )
            if (initial == null) {
                Spacer(Modifier.height(6.dp))
                TextAction(stringResource(R.string.back), { food = null }, Modifier.align(Alignment.CenterHorizontally))
            }
        }
    }
}

/** An ingredient typed by hand: name, how much, and the values for that amount. */
@Composable
private fun FreeIngredient(initial: RecipeIngredient?, onPick: (RecipeIngredient) -> Unit, onBack: () -> Unit) {
    val c = Hyper.colors
    val m0 = initial?.macros
    var name by remember { mutableStateOf(initial?.ar.orEmpty()) }
    var grams by remember { mutableStateOf(initial?.let { qtyText(it.grams) } ?: "") }
    var kcal by remember { mutableStateOf(m0?.let { qtyText(it.kcal) } ?: "") }
    var protein by remember { mutableStateOf(m0?.let { qtyText(it.protein) } ?: "") }
    var carbs by remember { mutableStateOf(m0?.let { qtyText(it.carbs) } ?: "") }
    var fat by remember { mutableStateOf(m0?.let { qtyText(it.fat) } ?: "") }
    fun num(s: String) = s.toDoubleOrNull()
    Text(stringResource(R.string.ingredient_free), style = Hyper.type.h2, color = c.text)
    Spacer(Modifier.height(12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(Modifier.weight(1.4f)) {
            Label(R.string.own_name)
            HyperTextField(name, { name = it }, placeholder = stringResource(R.string.ingredient_name_hint), textStyle = Hyper.type.bodyMedium)
        }
        Column(Modifier.weight(1f)) { Label(R.string.own_serving_grams); NumField(grams) { grams = it } }
    }
    Spacer(Modifier.height(12.dp))
    Text(stringResource(R.string.ingredient_values_for), style = Hyper.type.smallMedium, color = c.textSecondary)
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(Modifier.weight(1f)) { Label(R.string.ring_kcal); NumField(kcal) { kcal = it } }
        Column(Modifier.weight(1f)) { Label(R.string.macro_protein_g); NumField(protein) { protein = it } }
    }
    Spacer(Modifier.height(10.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(Modifier.weight(1f)) { Label(R.string.macro_carbs_g); NumField(carbs) { carbs = it } }
        Column(Modifier.weight(1f)) { Label(R.string.macro_fat_g); NumField(fat) { fat = it } }
    }
    Spacer(Modifier.height(16.dp))
    val g = num(grams)?.takeIf { it > 0 }
    PrimaryButton(
        stringResource(if (initial != null) R.string.save else R.string.recipe_add_ingredient),
        { onPick(RecipeIngredient.free(name, g ?: 1.0, Macros(num(kcal) ?: 0.0, num(protein) ?: 0.0, num(carbs) ?: 0.0, num(fat) ?: 0.0))) },
        enabled = name.isNotBlank() && g != null && num(kcal) != null, icon = if (initial != null) R.drawable.ic_check else R.drawable.ic_add,
    )
    if (initial == null) {
        Spacer(Modifier.height(6.dp))
        TextAction(stringResource(R.string.back), onBack, Modifier.fillMaxWidth())
    }
}
