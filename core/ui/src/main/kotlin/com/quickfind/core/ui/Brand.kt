package com.quickfind.core.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Brand tokens from docs/brand. */
object Brand {
    val Night = Color(0xFF120D09)
    val Cocoa = Color(0xFF2A1A10)
    val Cream = Color(0xFFFFE8C2)
    val Peach = Color(0xFFFFC48A)
    val PalOrange = Color(0xFFF07A2E)
    val Ember = Color(0xFFD9572A)
    val Ink = Color(0xFF1D140E)

    /** Baloo 2 ExtraBold (Ek Type, SIL OFL): logo, Pal's speech, big numbers. */
    val Display = FontFamily(Font(R.font.baloo2_extrabold, FontWeight.ExtraBold))
}

/** Pal's moods, matching docs/brand/mascot. */
enum class PalMood(@DrawableRes val res: Int) {
    HELLO(R.drawable.pal_hello),
    SEARCHING(R.drawable.pal_searching),
    FOUND(R.drawable.pal_found),
    RESTING(R.drawable.pal_resting),
    PRO(R.drawable.pal_pro),
    OOPS(R.drawable.pal_oops),
}

@Composable
fun Pal(mood: PalMood, size: Dp, modifier: Modifier = Modifier) {
    Image(painterResource(mood.res), contentDescription = null, modifier = modifier.size(size))
}

/** "Find" in the current text colour, "Pal" in Pal Orange. */
@Composable
fun Wordmark(fontSize: TextUnit = 28.sp, color: Color = MaterialTheme.colorScheme.onSurface, modifier: Modifier = Modifier) {
    Text(
        buildAnnotatedString {
            append("Find")
            withStyle(SpanStyle(color = Brand.PalOrange)) { append("Pal") }
        },
        style = TextStyle(fontFamily = Brand.Display, fontWeight = FontWeight.ExtraBold, fontSize = fontSize, color = color),
        modifier = modifier,
    )
}

/** Pal with a cream speech bubble above it. */
@Composable
fun PalWithBubble(mood: PalMood, text: String, size: Dp, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text,
            style = TextStyle(fontFamily = Brand.Display, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = Brand.Ink),
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(Brand.Cream)
                .padding(horizontal = 18.dp, vertical = 8.dp),
        )
        Pal(mood, size)
    }
}
