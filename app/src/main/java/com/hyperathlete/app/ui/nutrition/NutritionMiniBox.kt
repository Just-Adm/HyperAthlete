package com.hyperathlete.app.ui.nutrition

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hyperathlete.app.R
import com.hyperathlete.app.core.locale.Bidi
import com.hyperathlete.app.domain.nutrition.DayStatus
import com.hyperathlete.app.ui.components.HyperCard
import com.hyperathlete.app.ui.components.IconTile
import com.hyperathlete.app.ui.theme.Hyper
import com.hyperathlete.app.ui.theme.HyperShapes
import kotlin.math.roundToInt

/** Home: today's calories and protein left at a glance; tap opens the nutrition page. */
@Composable
fun NutritionMiniBox(day: NutritionDay, onOpen: () -> Unit) {
    val c = Hyper.colors
    HyperCard(Modifier.fillMaxWidth(), onClick = onOpen) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconTile(R.drawable.ic_restaurant)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.box_home_nutrition), style = Hyper.type.title, color = c.text)
                val s = day.status
                Text(
                    when (s) {
                        is DayStatus.Remaining -> stringResource(R.string.nutri_left, kcalLabel(s.kcal), gLabel(s.protein.toDouble()))
                        is DayStatus.ProteinOnly -> stringResource(R.string.nutri_protein_left, gLabel(s.protein.toDouble()))
                        is DayStatus.Over -> stringResource(R.string.nutri_over, kcalLabel(s.kcal))
                        DayStatus.Done -> stringResource(R.string.nutri_done)
                    },
                    style = Hyper.type.small, color = if (s is DayStatus.Over) c.warn else c.textMuted, maxLines = 2,
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            MiniBar(stringResource(R.string.ring_kcal), day.eaten.kcal, day.targets.kcal, c.accent, Modifier.weight(1f))
            MiniBar(stringResource(R.string.macro_protein), day.eaten.protein, day.targets.protein, MacroColors.Protein, Modifier.weight(1f), grams = true)
        }
    }
}

@Composable
private fun MiniBar(label: String, eaten: Double, goal: Int, color: Color, modifier: Modifier, grams: Boolean = false) {
    val c = Hyper.colors
    val f by animateFloatAsState(if (goal <= 0) 0f else (eaten / goal).toFloat().coerceIn(0f, 1f), tween(600), label = "mini")
    Column(modifier) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(label, style = Hyper.type.micro, color = c.textSecondary, modifier = Modifier.weight(1f), maxLines = 1)
            Text(
                if (grams) gRatio(eaten.roundToInt(), goal) else Bidi.ltr("${eaten.roundToInt()} / $goal"),
                style = Hyper.type.micro, color = c.text, maxLines = 1,
            )
        }
        Spacer(Modifier.height(5.dp))
        Box(Modifier.fillMaxWidth().height(8.dp).clip(HyperShapes.pill).background(c.surfaceAlt)) {
            Box(Modifier.fillMaxWidth(f).height(8.dp).clip(HyperShapes.pill).background(color))
        }
    }
}
