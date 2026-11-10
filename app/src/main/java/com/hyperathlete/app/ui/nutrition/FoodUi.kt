package com.hyperathlete.app.ui.nutrition

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hyperathlete.app.R
import com.hyperathlete.app.core.locale.Bidi
import com.hyperathlete.app.core.locale.LocalAppLanguage
import com.hyperathlete.app.domain.nutrition.Macros
import com.hyperathlete.app.domain.nutrition.MealType
import com.hyperathlete.app.domain.nutrition.Named
import com.hyperathlete.app.ui.theme.Hyper
import com.hyperathlete.app.ui.theme.HyperShapes
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/** Fixed macro colours, the same in every theme so the rings and bars read alike everywhere. */
object MacroColors {
    val Protein = Color(0xFF8B5CF6)
    val Carbs = Color(0xFF0EA5E9)
    val Fat = Color(0xFFF5B400)
}

/** Food picture on a soft tile; a plate icon for the user's own foods. */
@Composable
fun FoodImage(art: String?, modifier: Modifier = Modifier, size: Dp = 52.dp, tile: Boolean = true) {
    val c = Hyper.colors
    val res = FoodArt.of(art)
    Box(
        modifier.size(size).clip(HyperShapes.cardSmall).then(if (tile) Modifier.background(c.surfaceAlt) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        if (res != null) {
            Image(painterResource(res), null, Modifier.size(size * 0.86f))
        } else {
            Icon(painterResource(R.drawable.ic_restaurant), null, tint = c.accent, modifier = Modifier.size(size * 0.42f))
        }
    }
}

@Composable
@ReadOnlyComposable
fun Named.label(): String = name(LocalAppLanguage.current)

/** 1 → "1", 1.5 → "1.5", 0.25 → "0.25". */
fun qtyText(v: Double): String {
    val r = (v * 100).roundToInt() / 100.0
    return if (abs(r - r.roundToInt()) < 1e-9) r.roundToInt().toString() else String.format(Locale.US, "%.2f", r).trimEnd('0').trimEnd('.')
}

fun kcalInt(m: Macros): Int = m.kcal.roundToInt()

fun gramsText(v: Double): String = if (v >= 10) v.roundToInt().toString() else qtyText((v * 10).roundToInt() / 10.0)

@Composable
@ReadOnlyComposable
fun kcalLabel(v: Int): String = Bidi.isolate(stringResource(R.string.unit_kcal_value, v))

@Composable
@ReadOnlyComposable
fun gLabel(v: Double): String = Bidi.isolate(stringResource(R.string.unit_g_value, gramsText(v)))

/** "110 / 135 g": the numbers stay left-to-right, the unit follows the interface. */
@Composable
@ReadOnlyComposable
fun gRatio(eaten: Int, goal: Int): String = Bidi.isolate(stringResource(R.string.unit_g_value, Bidi.ltr("$eaten / $goal")))

/** "P 30 · C 70 · F 12" (ب / ك / د in Arabic), in the reading order of the interface. */
@Composable
@ReadOnlyComposable
fun macroLine(m: Macros): String = Bidi.isolate(
    "${stringResource(R.string.macro_p_short)} ${m.protein.roundToInt()} · " +
        "${stringResource(R.string.macro_c_short)} ${m.carbs.roundToInt()} · " +
        "${stringResource(R.string.macro_f_short)} ${m.fat.roundToInt()}",
)

@Composable
@ReadOnlyComposable
fun mealName(m: MealType): String = stringResource(
    when (m) {
        MealType.BREAKFAST -> R.string.meal_breakfast
        MealType.LUNCH -> R.string.meal_lunch
        MealType.DINNER -> R.string.meal_dinner
        MealType.SNACKS -> R.string.meal_snacks
        MealType.PRE_WORKOUT -> R.string.meal_pre_workout
        MealType.POST_WORKOUT -> R.string.meal_post_workout
        MealType.SUPPLEMENTS -> R.string.meal_supplements
    },
)

fun mealIcon(m: MealType): Int = when (m) {
    MealType.BREAKFAST -> R.drawable.ic_clear_day_fill
    MealType.LUNCH -> R.drawable.ic_restaurant
    MealType.DINNER -> R.drawable.ic_bedtime
    MealType.SNACKS -> R.drawable.ic_cake
    MealType.PRE_WORKOUT -> R.drawable.ic_bolt
    MealType.POST_WORKOUT -> R.drawable.ic_fitness_center
    MealType.SUPPLEMENTS -> R.drawable.ic_pill
}

/** "1 half plate" in English (units are stored capitalised as labels); Arabic as is. */
fun unitText(name: String, language: com.hyperathlete.app.domain.model.AppLanguage): String =
    if (language == com.hyperathlete.app.domain.model.AppLanguage.ENGLISH && name.length > 1 && !name[1].isUpperCase()) name.replaceFirstChar { it.lowercase() } else name
