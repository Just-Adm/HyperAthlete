package com.hyperathlete.app.ui.nutrition

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hyperathlete.app.data.nutrition.BarcodeResult
import com.hyperathlete.app.data.nutrition.FoodCatalog
import com.hyperathlete.app.data.nutrition.NutritionRepository
import com.hyperathlete.app.data.nutrition.OpenFoodFacts
import com.hyperathlete.app.domain.nutrition.Addon
import com.hyperathlete.app.domain.nutrition.Food
import com.hyperathlete.app.domain.nutrition.LoggedAddon
import com.hyperathlete.app.domain.nutrition.Macros
import com.hyperathlete.app.domain.nutrition.MealEntry
import com.hyperathlete.app.domain.nutrition.MealType
import com.hyperathlete.app.domain.nutrition.ServingUnit
import com.hyperathlete.app.domain.nutrition.sum
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/** The food being portioned in the sheet: unit, amount, add-ons. */
data class Portion(
    val food: Food,
    val unit: ServingUnit,
    val qty: Double,
    val meal: MealType,
    val addonOptions: List<Addon> = emptyList(),
    val addons: Map<String, Double> = emptyMap(),
    /** Showing the "any add-ons?" step. */
    val addonStep: Boolean = false,
    /** Editing an existing diary line. */
    val editing: MealEntry? = null,
    /** Add-ons from an old entry that are no longer offered (kept as logged). */
    val keptAddons: List<LoggedAddon> = emptyList(),
) {
    val base: Macros get() = food.macros(unit, qty)
    val loggedAddons: List<LoggedAddon>
        get() = addonOptions.mapNotNull { a -> addons[a.id]?.takeIf { it > 0 }?.let { LoggedAddon.of(a, it) } } + keptAddons
    val total: Macros get() = base + loggedAddons.map { it.macros }.sum()
    val grams: Double get() = unit.g * qty
}

/** Where the add-food screen is: search results, a category, recent / favourites / own foods. */
enum class FoodTab { RECENT, FAVORITES, MINE }

data class AddFoodUi(
    val catalog: FoodCatalog,
    val recents: List<Food>,
    val favorites: List<Food>,
    val favoriteIds: Set<String>,
)

@OptIn(FlowPreview::class)
@HiltViewModel
class AddFoodViewModel @Inject constructor(
    saved: SavedStateHandle,
    private val repo: NutritionRepository,
    private val off: OpenFoodFacts,
    private val clock: Clock,
) : ViewModel() {
    val date: LocalDate = LocalDate.ofEpochDay(saved.get<Long>("epochDay") ?: LocalDate.now(clock).toEpochDay())
    val meal: MealType = MealType.entries.firstOrNull { it.name == saved.get<String>("meal") } ?: MealType.SNACKS
    private val entryId: Long = saved.get<Long>("entryId") ?: 0L

    val ui: StateFlow<AddFoodUi?> = combine(repo.catalog, repo.recents(30), repo.favorites) { cat, recentIds, favIds ->
        AddFoodUi(cat, recentIds.mapNotNull(cat::food), favIds.mapNotNull(cat::food), favIds.toSet())
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val query = MutableStateFlow("")
    val category = MutableStateFlow<String?>(null)
    val tab = MutableStateFlow(FoodTab.RECENT)

    val results: StateFlow<List<Food>> = combine(query.debounce(120), ui) { q, u ->
        if (u == null || q.isBlank()) emptyList() else u.catalog.index.search(q)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _portion = MutableStateFlow<Portion?>(null)
    val portion: StateFlow<Portion?> = _portion

    /** One-shot: the entry was saved (close the screen) or the edit sheet was dismissed. */
    private val _done = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val done: SharedFlow<Unit> = _done

    val barcode = MutableStateFlow<BarcodeState>(BarcodeState.Idle)

    init {
        if (entryId > 0) viewModelScope.launch { startEdit() }
    }

    val isEditing: Boolean get() = entryId > 0

    private suspend fun startEdit() {
        val e = repo.entry(entryId) ?: run { _done.tryEmit(Unit); return }
        val cat = repo.catalog.first()
        // A food that left the list (deleted own food) is rebuilt from the diary snapshot.
        val food = cat.food(e.foodId) ?: Food(
            id = e.foodId, cat = "custom", ar = e.ar, en = e.en, art = e.art,
            per100 = listOf(e.per100.kcal, e.per100.protein, e.per100.carbs, e.per100.fat),
            units = listOf(ServingUnit(e.unitId, e.unitAr, e.unitEn, e.unitGrams)), defUnit = e.unitId,
        )
        val options = cat.db.addonsFor(food)
        val ids = options.map { it.id }.toSet()
        _portion.value = Portion(
            food = food,
            unit = food.allUnits.firstOrNull { it.id == e.unitId } ?: ServingUnit(e.unitId, e.unitAr, e.unitEn, e.unitGrams),
            qty = e.qty, meal = e.meal, addonOptions = options,
            addons = e.addons.filter { it.id in ids }.associate { it.id to it.qty },
            editing = e, keptAddons = e.addons.filter { it.id !in ids },
        )
    }

    fun open(food: Food) {
        viewModelScope.launch {
            val last = repo.lastPortion(food.id)
            val unit = last?.let { l -> food.allUnits.firstOrNull { it.id == l.unitId } } ?: food.defaultUnit
            val qty = if (last != null && unit.id == last.unitId) last.qty else food.defaultQty(unit)
            val options = ui.value?.catalog?.db?.addonsFor(food).orEmpty()
            _portion.value = Portion(food, unit, qty, MealType.forFood(food.cat, meal), options)
        }
    }

    fun setUnit(u: ServingUnit) = update { p -> p.copy(unit = u, qty = if (u.id == p.unit.id) p.qty else p.food.defaultQty(u)) }
    fun setQty(q: Double) = update { it.copy(qty = q.coerceIn(0.0, 5000.0)) }
    fun setMeal(m: MealType) = update { it.copy(meal = m) }
    fun toggleAddon(a: Addon) = update { p ->
        val cur = p.addons[a.id]
        p.copy(addons = if (cur != null && cur > 0) p.addons - a.id else p.addons + (a.id to a.defQty))
    }
    fun setAddonQty(a: Addon, q: Double) = update { p ->
        p.copy(addons = if (q <= 0) p.addons - a.id else p.addons + (a.id to q))
    }
    fun showAddons(show: Boolean) = update { it.copy(addonStep = show) }

    fun dismissPortion() {
        _portion.value = null
        if (isEditing) _done.tryEmit(Unit)
    }

    fun toggleFavorite(food: Food) {
        val on = food.id !in (ui.value?.favoriteIds ?: emptySet())
        viewModelScope.launch { repo.setFavorite(food.id, on) }
    }

    fun save() {
        val p = _portion.value ?: return
        if (p.qty <= 0) return
        viewModelScope.launch {
            val now = clock.millis()
            val entry = p.editing?.let { e ->
                e.copy(
                    meal = p.meal, unitId = p.unit.id, unitAr = p.unit.ar, unitEn = p.unit.en, unitGrams = p.unit.g,
                    qty = p.qty, addons = p.loggedAddons,
                )
            } ?: MealEntry.of(date, p.meal, p.food, p.unit, p.qty, p.loggedAddons, now)
            repo.save(entry)
            _portion.value = null
            _done.tryEmit(Unit)
        }
    }

    /* ---- barcode ---- */
    fun lookupBarcode(code: String) {
        barcode.value = BarcodeState.Looking
        viewModelScope.launch {
            when (val r = off.lookup(code)) {
                is BarcodeResult.Found -> {
                    barcode.value = BarcodeState.Idle
                    // The catalog flow picks the new food up; open it right away from the result.
                    open(r.food.toFood())
                }
                BarcodeResult.NotFound -> barcode.value = BarcodeState.NotFound(code)
                BarcodeResult.Offline -> barcode.value = BarcodeState.Offline
            }
        }
    }

    fun clearBarcode() { barcode.value = BarcodeState.Idle }

    private fun update(f: (Portion) -> Portion) { _portion.value = _portion.value?.let(f) }
}

sealed interface BarcodeState {
    data object Idle : BarcodeState
    data object Looking : BarcodeState
    data class NotFound(val code: String) : BarcodeState
    data object Offline : BarcodeState
}
