package com.hyperathlete.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hyperathlete.app.core.AppEntryPoint
import com.hyperathlete.app.core.workout.ExerciseVideo
import com.hyperathlete.app.domain.nutrition.CustomFood
import com.hyperathlete.app.domain.nutrition.LoggedAddon
import com.hyperathlete.app.domain.nutrition.Macros
import com.hyperathlete.app.domain.nutrition.MealEntry
import com.hyperathlete.app.domain.nutrition.MealType
import com.hyperathlete.app.domain.nutrition.NutritionPrefs
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalTime

/** The food diary on the real Room database and DataStore: log, edit, sum, favourites, recents, own foods. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class NutritionFlowTest {

    @Test fun `a day of eating is stored, summed and edited`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val app = EntryPointAccessors.fromApplication(context, AppEntryPoint::class.java)
        val repo = app.nutrition()
        val db = repo.foodDb
        val day = LocalDate.of(2026, 10, 15)

        val rice = db.food("rice_white")!!
        val hummus = db.food("hummus")!!
        val withChicken = MealEntry.of(day, MealType.LUNCH, rice, rice.unit("plate"), 1.0, listOf(LoggedAddon.of(db.addon("chicken")!!, 1.0)), 10L)
        val id = repo.save(withChicken)
        repo.save(MealEntry.of(day, MealType.BREAKFAST, hummus, hummus.defaultUnit, 1.0, emptyList(), 20L))
        repo.save(MealEntry.of(day.plusDays(1), MealType.DINNER, hummus, hummus.defaultUnit, 2.0, emptyList(), 30L))

        val entries = repo.day(day).first()
        assertEquals(2, entries.size)
        val back = entries.first { it.id == id }
        assertEquals(1, back.addons.size)
        assertEquals(withChicken.total.kcal, back.total.kcal, 0.001)

        val totals = repo.totals(day, day.plusDays(1)).first()
        assertEquals(entries.sumOf { it.total.kcal }, totals[day]!!.kcal, 0.001)
        assertEquals(2, totals.size)

        // Edit the amount, then delete
        repo.save(back.copy(qty = 2.0))
        assertEquals(rice.macros(rice.unit("plate"), 2.0).kcal + 190, repo.entry(id)!!.total.kcal, 0.01)
        assertEquals(2.0, repo.lastPortion("rice_white")!!.qty, 0.0)
        repo.delete(id)
        assertEquals(1, repo.countOn(day))

        // Recents (newest first) and favourites
        assertEquals(listOf("hummus", "rice_white").take(1), repo.recents().first().take(1))
        repo.setFavorite("knafeh", true)
        assertEquals(listOf("knafeh"), repo.favorites.first())
        repo.setFavorite("knafeh", false)
        assertTrue(repo.favorites.first().isEmpty())

        // Own food: searchable and loggable like any other
        val cid = repo.saveCustom(CustomFood.fromServing("Protein pancake", "piece", 90.0, Macros(210.0, 18.0, 20.0, 6.0)))
        val catalog = repo.catalog.first()
        val mine = catalog.food("custom:$cid")
        assertNotNull(mine)
        assertEquals("custom:$cid", catalog.index.search("protein pancake").first().id)
        assertTrue(catalog.index.search("pancake").any { it.id == "custom:$cid" })
        repo.deleteCustom(cid)
        assertFalse(repo.catalog.first().custom.any { it.id == cid })
    }

    @Test fun `nutrition settings round-trip`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val settings = EntryPointAccessors.fromApplication(context, AppEntryPoint::class.java).settings()
        val prefs = NutritionPrefs(activity = 1.725, surplus = 400, proteinPerKg = 1.8, fatPercent = 30, carbCycling = false, workoutMeals = false, reminderTime = LocalTime.of(20, 30))
        settings.setNutritionPrefs(prefs)
        assertEquals(prefs, settings.current().nutrition)
    }

    @Test fun `how-to search uses a clean english query`() {
        assertEquals("Incline Dumbbell Press 30 exercise form", ExerciseVideo.query("Incline Dumbbell Press (30°)"))
        assertEquals("Wall Slides Thread the Needle exercise form", ExerciseVideo.query("Wall Slides + Thread the Needle"))
        assertTrue(ExerciseVideo.searchUrl("Pull-up").toString().startsWith("https://www.youtube.com/results?search_query=Pull-up"))
    }
}
