package com.hyperathlete.app.domain

import com.hyperathlete.app.domain.model.UserProfile
import com.hyperathlete.app.domain.nutrition.Adherence
import com.hyperathlete.app.domain.nutrition.AdherenceRule
import com.hyperathlete.app.domain.nutrition.ArabicText
import com.hyperathlete.app.domain.nutrition.CustomFood
import com.hyperathlete.app.domain.nutrition.DayStatus
import com.hyperathlete.app.domain.nutrition.FoodDb
import com.hyperathlete.app.domain.nutrition.FoodIndex
import com.hyperathlete.app.domain.nutrition.LoggedAddon
import com.hyperathlete.app.domain.nutrition.Macros
import com.hyperathlete.app.domain.nutrition.MealEntry
import com.hyperathlete.app.domain.nutrition.MealType
import com.hyperathlete.app.domain.nutrition.NutritionPrefs
import com.hyperathlete.app.domain.nutrition.NutritionTargets
import com.hyperathlete.app.domain.nutrition.RecipeIngredient
import com.hyperathlete.app.domain.nutrition.ServingUnit
import com.hyperathlete.app.domain.nutrition.TargetCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.abs

class NutritionDomainTest {
    private val db: FoodDb by lazy { FoodDb.parse(File("src/main/assets/food/foods.json").readText()) }
    private val index by lazy { FoodIndex(db.foods) }

    @Test fun `food list covers the brief and every entry is well formed`() {
        assertTrue(db.foods.size >= 1000)
        assertTrue(db.addons.size >= 130)
        assertEquals(db.foods.size, db.foods.map { it.id }.toSet().size)
        val cats = db.categories.map { it.id }.toSet()
        for (f in db.foods) {
            assertTrue("${f.id} category", f.cat in cats)
            assertEquals("${f.id} per100", 4, f.per100.size)
            assertTrue("${f.id} default unit", f.units.any { it.id == f.defUnit })
            assertTrue("${f.id} units", f.units.all { it.g > 0 })
            assertEquals("${f.id} unit ids", f.units.size, f.units.map { it.id }.toSet().size)
            assertTrue("${f.id} addons group", f.addons == null || f.addons in db.groups)
            val m = f.macros100
            val fromMacros = m.protein * 4 + m.carbs * 4 + m.fat * 9
            if (m.kcal > 40) assertTrue("${f.id} energy ${m.kcal} vs $fromMacros", abs(fromMacros - m.kcal) / m.kcal < 0.3)
        }
        for ((group, ids) in db.groups) for (id in ids) assertTrue("$group → $id", db.addon(id) != null)
    }

    @Test fun `every food and add-on has its picture`() {
        val drawables = File("src/main/res/drawable")
        for (art in db.foods.mapNotNull { it.art } + db.addons.mapNotNull { it.art }) {
            assertTrue("food_$art.xml", File(drawables, "food_$art.xml").exists())
        }
    }

    @Test fun `arabic search folds hamza, taa marbuta, tashkeel and the article`() {
        assertEquals("ارز", ArabicText.normalize("أرز"))
        assertEquals("شوربه", ArabicText.normalize("شوربة"))
        assertEquals("منسف", ArabicText.normalize("مَنْسَف"))
        assertEquals("hummus", index.search("حمص").first().id)
        assertEquals("mansaf", index.search("مَنسف").first().id)
        assertTrue(index.search("أرز").any { it.id == "rice_white" })
        assertTrue(index.search("شوربه عدس").any { it.id == "soup_lentil" })
        assertTrue(index.search("الكنافة").any { it.id == "knafeh" })
        assertTrue(index.search("shawarma").size >= 2)
        assertTrue(index.search("   ").isEmpty())
    }

    @Test fun `targets follow Mifflin-St Jeor with the surplus and carb days`() {
        val p = UserProfile(weightKg = 67.0, heightCm = 176.0, birthDate = LocalDate.of(2001, 1, 1))
        val prefs = NutritionPrefs()
        val sunday = LocalDate.of(2026, 10, 18)
        val age = 25
        assertEquals(1650.0, TargetCalculator.bmr(67.0, 176.0, age, true), 0.01)
        val t = TargetCalculator.targets(p, prefs, sunday)
        assertEquals(2910, t.kcal)
        assertEquals(134, t.protein)
        assertEquals(81, t.fat)
        assertEquals(411, t.carbs)
        val thursday = TargetCalculator.targets(p, prefs, sunday.plusDays(4))
        assertTrue(thursday.carbDay)
        assertEquals(3060, thursday.kcal)
        assertEquals(t.protein, thursday.protein)
        assertEquals(t.fat, thursday.fat)
        assertEquals(449, thursday.carbs)
        val off = TargetCalculator.targets(p, prefs.copy(carbCycling = false), sunday.plusDays(4))
        assertEquals(2910, off.kcal)
    }

    @Test fun `day status reads remaining, protein only, over and done`() {
        val t = NutritionTargets(2900, 134, 411, 81, 1650, 2557, false)
        assertEquals(DayStatus.Remaining(650, 42), DayStatus.of(Macros(2250.0, 92.0), t))
        assertEquals(DayStatus.Over(400), DayStatus.of(Macros(3300.0, 150.0), t))
        assertEquals(DayStatus.ProteinOnly(30), DayStatus.of(Macros(2880.0, 104.0), t))
        assertEquals(DayStatus.Done, DayStatus.of(Macros(2950.0, 140.0), t))
        assertEquals(Adherence.NONE, AdherenceRule.of(Macros.ZERO, t))
        assertEquals(Adherence.GOOD, AdherenceRule.of(Macros(2800.0, 130.0), t))
        assertEquals(Adherence.CLOSE, AdherenceRule.of(Macros(2400.0, 110.0), t))
        assertEquals(Adherence.MISSED, AdherenceRule.of(Macros(1500.0, 60.0), t))
    }

    @Test fun `a logged food sums its add-ons`() {
        val rice = db.food("rice_white")!!
        val plate = rice.unit("plate")
        val chicken = LoggedAddon.of(db.addon("chicken")!!, 1.5)
        val e = MealEntry.of(LocalDate.of(2026, 10, 15), MealType.LUNCH, rice, plate, 1.0, listOf(chicken), 1L)
        val riceOnly = rice.macros(plate, 1.0)
        assertEquals(riceOnly.kcal + 190 * 1.5, e.total.kcal, 0.001)
        assertEquals(riceOnly.protein + 27 * 1.5, e.total.protein, 0.001)
        // grams are always offered, with a sensible default amount
        assertEquals(ServingUnit.GRAM, rice.allUnits.last())
        assertEquals(250.0, rice.defaultQty(ServingUnit.GRAM), 0.0)
        assertEquals(MealType.BREAKFAST, MealType.forTime(LocalTime.of(8, 0)))
        assertEquals(5, MealType.visible(false).size)  // four meals + supplements & medicines
    }

    @Test fun `custom foods and recipes scale to 100 g`() {
        val bar = CustomFood.fromServing("Bar", "bar", 60.0, Macros(240.0, 20.0, 24.0, 8.0))
        assertEquals(400.0, bar.per100.kcal, 0.001)
        assertEquals(240.0, bar.toFood().macros(bar.toFood().defaultUnit, 1.0).kcal, 0.001)
        val oats = db.food("oats")!!
        val milk = db.food("milk_full")!!
        val r = CustomFood.recipe(
            "Overnight oats",
            listOf(RecipeIngredient.of(oats, ServingUnit.GRAM, 80.0), RecipeIngredient.of(milk, ServingUnit.GRAM, 250.0)),
            servings = 2.0, servingName = "jar",
        )
        assertEquals(165.0, r.servingGrams, 0.001)
        val whole = oats.macros(ServingUnit.GRAM, 80.0) + milk.macros(ServingUnit.GRAM, 250.0)
        assertEquals(whole.kcal / 2, r.toFood().macros(r.toFood().defaultUnit, 1.0).kcal, 0.01)
    }

    @Test fun `big list has drinks, supplements, vitamins and medicines with add-ons`() {
        val cats = db.foods.groupBy { it.cat }
        for (c in listOf("hot_drinks", "juices", "soft_drinks", "supplements", "vitamins", "restaurants", "pastries", "sauces")) {
            assertTrue("$c has foods", cats[c].orEmpty().size >= 10)
        }
        for (q in listOf("اوميجا", "omega", "فيتامين د", "زنك", "مغنيسيوم", "بنادول", "كرياتين", "لاتيه", "شاورما", "قهوة")) {
            assertTrue("search $q", index.search(q).isNotEmpty())
        }
        assertEquals("vitamins", index.search("اوميجا 3").first().cat)
        // Coffee, burgers and shawarma come with their extras.
        for (id in listOf("latte", "chicken_burger", "pizza_margherita")) {
            val f = db.food(id) ?: continue
            assertTrue("$id add-ons", db.addonsFor(f).isNotEmpty())
        }
        assertTrue(db.foods.count { db.addonsFor(it).isNotEmpty() } >= 300)
    }

    @Test fun `vitamins and medicines go to their own section`() {
        assertEquals(MealType.SUPPLEMENTS, MealType.forFood("vitamins", MealType.LUNCH))
        assertEquals(MealType.LUNCH, MealType.forFood("supplements", MealType.LUNCH))
        assertTrue(MealType.SUPPLEMENTS in MealType.visible(false))
        assertEquals(MealType.SUPPLEMENTS, MealType.entries.last())
    }

    @Test fun `own food from ingredients adds them up, free ones too`() {
        val bread = db.food("bread_arabic") ?: db.foods.first { it.cat == "bread" }
        val sauce = RecipeIngredient.free("House sauce", 30.0, Macros(120.0, 1.0, 3.0, 12.0))
        assertTrue(sauce.isFree)
        assertEquals(120.0, sauce.macros.kcal, 0.001)
        val ings = listOf(RecipeIngredient.of(bread, ServingUnit.GRAM, 90.0), sauce)
        val one = CustomFood.fromIngredients("Sandwich", ings, servings = 1.0, servingName = "sandwich")
        assertEquals(false, one.recipe)
        assertEquals(120.0, one.servingGrams, 0.001)
        val whole = bread.macros(ServingUnit.GRAM, 90.0) + sauce.macros
        val food = one.toFood()
        assertEquals(whole.kcal, food.macros(food.defaultUnit, 1.0).kcal, 0.01)
        assertEquals(whole.protein, food.macros(food.defaultUnit, 1.0).protein, 0.01)
        assertEquals(CustomFood.CAT_CUSTOM, food.cat)
        val half = CustomFood.fromIngredients("Sandwich", ings, servings = 2.0, servingName = "half")
        assertEquals(whole.kcal / 2, half.toFood().macros(half.toFood().defaultUnit, 1.0).kcal, 0.01)
    }
}
