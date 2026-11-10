package com.hyperathlete.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.hyperathlete.app.R

/** Tajawal, the Calisthenics typeface: clean Arabic with matching Latin glyphs. */
val Tajawal = FontFamily(
    Font(R.font.tajawal_regular, FontWeight.Normal),
    Font(R.font.tajawal_medium, FontWeight.Medium),
    Font(R.font.tajawal_bold, FontWeight.Bold),
)

/** Type scale from the Calisthenics design system (display 40 / h1 26 / h2 21 / title 17 / body 15 / small 13.5 / micro 11.5). */
@Immutable
data class HyperType(
    val display: TextStyle,
    val h1: TextStyle,
    val h2: TextStyle,
    val title: TextStyle,
    val body: TextStyle,
    val bodyMedium: TextStyle,
    val small: TextStyle,
    val smallMedium: TextStyle,
    val micro: TextStyle,
    val button: TextStyle,
    val number: TextStyle,
)

private fun style(size: TextUnit, weight: FontWeight, lineHeight: TextUnit, tracking: TextUnit = 0.sp) = TextStyle(
    fontFamily = Tajawal,
    fontWeight = weight,
    fontSize = size,
    lineHeight = lineHeight,
    letterSpacing = tracking,
    // Matches includeFontPadding(false) from the reference app: tight, predictable boxes.
    platformStyle = PlatformTextStyle(includeFontPadding = false),
    lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
)

fun hyperType(): HyperType = HyperType(
    // Large titles tighten slightly, like iOS display text.
    display = style(40.sp, FontWeight.Bold, 46.sp, (-0.6).sp),
    h1 = style(26.sp, FontWeight.Bold, 34.sp, (-0.3).sp),
    h2 = style(21.sp, FontWeight.Bold, 28.sp, (-0.2).sp),
    title = style(17.sp, FontWeight.Bold, 24.sp),
    body = style(15.sp, FontWeight.Normal, 22.sp),
    bodyMedium = style(15.sp, FontWeight.Medium, 22.sp),
    small = style(13.5.sp, FontWeight.Normal, 19.sp),
    smallMedium = style(13.5.sp, FontWeight.Medium, 19.sp),
    micro = style(11.5.sp, FontWeight.Medium, 15.sp),
    button = style(17.sp, FontWeight.Bold, 22.sp),
    number = style(40.sp, FontWeight.Bold, 44.sp),
)
