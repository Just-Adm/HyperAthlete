package com.hyperathlete.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/** Radii (Liquid Glass, after Saja `--r`): cards 26dp, small cards 18dp, sheets 34dp, buttons and chips fully rounded. */
object HyperShapes {
    val card = RoundedCornerShape(26.dp)
    val cardSmall = RoundedCornerShape(18.dp)
    val tile = RoundedCornerShape(14.dp)
    val sheet = RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp)
    val dialog = RoundedCornerShape(30.dp)
    val pill = RoundedCornerShape(percent = 50)
}

/** Spacing scale used across screens. */
object Space {
    val xs = 4.dp
    val s = 8.dp
    val m = 12.dp
    val l = 16.dp
    val xl = 22.dp
    val xxl = 28.dp
    val screen = 20.dp
}
