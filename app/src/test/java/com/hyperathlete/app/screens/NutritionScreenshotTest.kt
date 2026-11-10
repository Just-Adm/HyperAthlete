package com.hyperathlete.app.screens

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.hyperathlete.app.core.locale.ProvideAppLanguage
import com.hyperathlete.app.data.nutrition.FoodCatalog
import com.hyperathlete.app.domain.layout.PageId
import com.hyperathlete.app.domain.layout.PageLayout
import com.hyperathlete.app.domain.model.AppLanguage
import com.hyperathlete.app.domain.model.AppSettings
import com.hyperathlete.app.domain.model.ThemeMode
import com.hyperathlete.app.domain.model.UserProfile
import com.hyperathlete.app.domain.nutrition.Adherence
import com.hyperathlete.app.domain.nutrition.CustomFood
import com.hyperathlete.app.domain.nutrition.FoodDb
import com.hyperathlete.app.domain.nutrition.LoggedAddon
import com.hyperathlete.app.domain.nutrition.Macros
import com.hyperathlete.app.domain.nutrition.MealEntry
import com.hyperathlete.app.domain.nutrition.MealType
import com.hyperathlete.app.domain.nutrition.NutritionPrefs
import com.hyperathlete.app.domain.nutrition.RecipeIngredient
import com.hyperathlete.app.domain.nutrition.ServingUnit
import com.hyperathlete.app.ui.AppState
import com.hyperathlete.app.ui.nutrition.AddFoodContent
import com.hyperathlete.app.ui.nutrition.AddFoodUi
import com.hyperathlete.app.ui.nutrition.MonthCalendar
import com.hyperathlete.app.ui.nutrition.NutritionDay
import com.hyperathlete.app.ui.nutrition.NutritionDeps
import com.hyperathlete.app.ui.nutrition.NutritionMiniBox
import com.hyperathlete.app.ui.nutrition.NutritionScreen
import com.hyperathlete.app.ui.nutrition.OwnFoodForm
import com.hyperathlete.app.ui.nutrition.OwnFoodScreen
import com.hyperathlete.app.ui.nutrition.Portion
import com.hyperathlete.app.ui.nutrition.PortionSheet
import com.hyperathlete.app.ui.settings.NutritionSettingsScreen
import com.hyperathlete.app.ui.theme.Hyper
import com.hyperathlete.app.ui.theme.HyperTheme
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.LocalDate

/** Phase 3 screens: nutrition page, month calendar, add food, portion + add-ons, own foods, settings. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w360dp-h802dp-xhdpi", application = Application::class)
class NutritionScreenshotTest {

    @get:Rule val compose = createComposeRule()

    private val today = LocalDate.of(2026, 10, 15) // Thursday → carb day
    private val db: FoodDb by lazy { FoodDb.parse(File("src/main/assets/food/foods.json").readText()) }
    private val profile = UserProfile("آدم", LocalDate.of(2001, 3, 20), 67.4, 176.0)
    private val prefs = NutritionPrefs()

    private fun state(language: AppLanguage = AppLanguage.MIXED) =
        AppState(AppSettings(onboardingDone = true, language = language), profile, today)

    private fun shot(name: String, language: AppLanguage = AppLanguage.MIXED, theme: ThemeMode = ThemeMode.LIGHT, content: @Composable () -> Unit) {
        compose.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                ProvideAppLanguage(language) {
                    HyperTheme(themeMode = theme) {
                        Box(Modifier.fillMaxSize().background(Hyper.colors.bg)) { content() }
                    }
                }
            }
        }
        compose.onRoot().captureRoboImage("screenshots/$name.png")
    }

    private fun entry(id: Long, food: String, unit: String, qty: Double, meal: MealType, addons: List<Pair<String, Double>> = emptyList()): MealEntry {
        val f = db.food(food)!!
        return MealEntry.of(today, meal, f, f.unit(unit), qty, addons.map { (a, q) -> LoggedAddon.of(db.addon(a)!!, q) }, id).copy(id = id)
    }

    private val entries by lazy {
        listOf(
            entry(1, "hummus", "half_plate", 1.0, MealType.BREAKFAST, listOf("olive_oil" to 1.0)),
            entry(2, "falafel", "piece", 4.0, MealType.BREAKFAST),
            entry(3, "bread_arabic", "loaf", 1.0, MealType.BREAKFAST),
            entry(4, "rice_white", "plate", 1.0, MealType.LUNCH, listOf("chicken" to 1.5, "laban" to 1.0)),
            entry(5, "salad_arabic", "bowl", 1.0, MealType.LUNCH),
            entry(6, "whey", "scoop", 1.0, MealType.POST_WORKOUT, listOf("milk" to 1.0)),
            entry(7, "dates", "piece", 3.0, MealType.SNACKS),
            entry(8, "vitamin_d3_1000", "softgel", 1.0, MealType.SUPPLEMENTS),
            entry(9, "omega3", "softgel", 2.0, MealType.SUPPLEMENTS),
        )
    }

    private fun day(list: List<MealEntry> = entries) = NutritionDay.of(today, today, list, profile, prefs)

    private fun deps(ring: Int = 0) = NutritionDeps(PageLayout.default(PageId.NUTRITION), ringPage = ring)

    @Test fun nutrition_page() = shot("nutrition_page") { NutritionScreen(day(), deps()) }

    @Test fun nutrition_page_dark() = shot("nutrition_page_dark", theme = ThemeMode.DARK) { NutritionScreen(day(), deps()) }

    @Test fun nutrition_page_english() = shot("nutrition_page_english", AppLanguage.ENGLISH) { NutritionScreen(day(), deps()) }

    @Test fun nutrition_page_empty() = shot("nutrition_page_empty") { NutritionScreen(day(emptyList()), deps()) }

    @Test fun nutrition_ring_protein() = shot("nutrition_ring_protein") { NutritionScreen(day(), deps(ring = 1)) }

    @Test fun nutrition_meals_scrolled() = shot("nutrition_meals_box") {
        val layout = PageLayout.default(PageId.NUTRITION).let { it.copy(order = listOf(com.hyperathlete.app.domain.layout.BoxId.NUTRITION_MEALS, com.hyperathlete.app.domain.layout.BoxId.NUTRITION_PROTEIN, com.hyperathlete.app.domain.layout.BoxId.NUTRITION_SUMMARY)) }
        NutritionScreen(day(), NutritionDeps(layout))
    }

    @Test fun nutrition_month_calendar() = shot("nutrition_month_calendar") {
        val marks = (1..15).associate { d ->
            today.withDayOfMonth(d) to when (d % 5) { 0 -> Adherence.MISSED; 1, 2 -> Adherence.GOOD; 3 -> Adherence.CLOSE; else -> Adherence.NONE }
        }
        Column(Modifier.statusBarsPadding().padding(top = 20.dp)) { MonthCalendar(today, today, { flowOf(marks) }) {} }
    }

    private fun ui(withRecents: Boolean = true): AddFoodUi {
        val mine = listOf(
            CustomFood(1, "شوفان بروتين", "جار", 320.0, Macros(150.0, 12.0, 18.0, 4.0), recipe = true),
            CustomFood(2, "Protein bar · Brand", "bar", 60.0, Macros(390.0, 33.0, 36.0, 12.0), barcode = "123"),
        )
        val cat = FoodCatalog(db, mine)
        val recents = if (withRecents) listOf("hummus", "rice_white", "shawarma_chicken", "whey", "dates", "knafeh", "banana").mapNotNull(cat::food) else emptyList()
        return AddFoodUi(cat, recents, listOf("hummus", "whey").mapNotNull(cat::food), setOf("hummus", "whey"))
    }

    @Composable
    private fun addFood(sel: String? = null, query: String = "", results: List<com.hyperathlete.app.domain.nutrition.Food> = emptyList(), u: AddFoodUi = ui()) {
        AddFoodContent(
            MealType.LUNCH, today, u, query, results, {}, {}, {}, {}, {}, {}, {}, {}, initialSelection = sel,
        )
    }

    @Test fun add_food_recent() = shot("add_food_recent") { addFood() }

    @Test fun add_food_category() = shot("add_food_category") { addFood("dishes") }

    @Test fun add_food_mine() = shot("add_food_mine") { addFood("@mine") }

    @Test fun add_food_search() = shot("add_food_search") {
        val u = ui()
        addFood(query = "شاورما", results = u.catalog.index.search("شاورما"), u = u)
    }

    @Test fun add_food_not_found() = shot("add_food_not_found") { addFood(query = "سوشي", results = emptyList()) }

    private fun portion(addonStep: Boolean): Portion {
        val rice = db.food("rice_white")!!
        return Portion(
            rice, rice.unit("plate"), 1.0, MealType.LUNCH, db.addonsFor(rice),
            addons = mapOf("chicken" to 1.5, "nuts_pine" to 1.0), addonStep = addonStep,
        )
    }

    @Test fun portion_sheet() = shot("portion_sheet") {
        Column(Modifier.statusBarsPadding().padding(top = 12.dp)) {
            PortionSheet(portion(false), true, {}, {}, {}, {}, {}, { _, _ -> }, {}, {})
        }
    }

    @Test fun portion_addons() = shot("portion_addons") {
        Column(Modifier.statusBarsPadding().padding(top = 12.dp)) {
            PortionSheet(portion(true), false, {}, {}, {}, {}, {}, { _, _ -> }, {}, {})
        }
    }

    @Test fun portion_addons_sweet() = shot("portion_addons_sweet") {
        val p = db.food("pancakes")!!
        Column(Modifier.statusBarsPadding().padding(top = 12.dp)) {
            PortionSheet(Portion(p, p.defaultUnit, 2.0, MealType.BREAKFAST, db.addonsFor(p), mapOf("honey" to 1.0, "banana" to 1.0), addonStep = true), false, {}, {}, {}, {}, {}, { _, _ -> }, {}, {})
        }
    }

    @Test fun own_food_form() = shot("own_food_form") {
        OwnFoodScreen(false, false, OwnFoodForm("Protein bar", "بار", "60", "240", "20", "24", "8"), null, {}, {}, {}, {})
    }

    @Test fun own_food_ingredients() = shot("own_food_ingredients") {
        val bun = db.food("bread_burger_bun")!!
        val chicken = db.food("chicken_breast")!!
        val form = OwnFoodForm(
            name = "برجر دجاج البيت", fromIngredients = true, servings = 1.0,
            ingredients = listOf(
                RecipeIngredient.of(bun, bun.defaultUnit, 1.0), RecipeIngredient.of(chicken, ServingUnit.GRAM, 150.0),
                RecipeIngredient.of(db.food("tomato")!!, db.food("tomato")!!.defaultUnit, 0.5),
                RecipeIngredient.of(db.food("mayo")!!, db.food("mayo")!!.defaultUnit, 1.0),
                RecipeIngredient.free("صوص البيت", 30.0, Macros(95.0, 0.5, 4.0, 8.5)),
            ),
        )
        OwnFoodScreen(false, false, form, null, {}, {}, {}, {})
    }

    @Test fun own_recipe_form() = shot("own_recipe_form") {
        val oats = db.food("oats")!!
        val milk = db.food("milk_full")!!
        val banana = db.food("banana")!!
        val form = OwnFoodForm(
            name = "شوفان بروتين", servings = 2.0,
            ingredients = listOf(
                RecipeIngredient.of(oats, ServingUnit.GRAM, 80.0), RecipeIngredient.of(milk, milk.defaultUnit, 1.0),
                RecipeIngredient.of(banana, banana.defaultUnit, 1.0),
            ),
        )
        OwnFoodScreen(true, false, form, null, {}, {}, {}, {})
    }

    @Test fun nutrition_settings() = shot("nutrition_settings") { NutritionSettingsScreen(state(), {}, {}) }

    @Test fun home_nutrition_box() = shot("home_nutrition_box") {
        Column(Modifier.statusBarsPadding().padding(20.dp)) {
            NutritionMiniBox(day(), {})
            androidx.compose.foundation.layout.Spacer(Modifier.padding(8.dp))
            NutritionMiniBox(day(entries + entry(9, "knafeh", "piece", 3.0, MealType.SNACKS) + entry(10, "mansaf", "plate", 1.0, MealType.DINNER)), {})
        }
    }
}
