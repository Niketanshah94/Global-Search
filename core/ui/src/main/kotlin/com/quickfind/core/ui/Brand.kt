package com.quickfind.core.ui

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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

/** Pal's moods, matching docs/brand/mascot. [res] is the static PNG (widgets, notifications). */
enum class PalMood(@DrawableRes val res: Int, val face: PalFace, val arm: PalArm) {
    HELLO(R.drawable.pal_hello, PalFace.HAPPY, PalArm.WAVE),
    HAPPY(R.drawable.pal_hello, PalFace.HAPPY, PalArm.NONE),
    SEARCHING(R.drawable.pal_searching, PalFace.LOOK, PalArm.NONE),
    FOUND(R.drawable.pal_found, PalFace.STARS, PalArm.CHEER),
    RESTING(R.drawable.pal_resting, PalFace.SLEEPY, PalArm.NONE),
    PRO(R.drawable.pal_pro, PalFace.COOL, PalArm.POINT),
    OOPS(R.drawable.pal_oops, PalFace.WORRIED, PalArm.NONE),
}

/** Live Pal: floats, blinks, waves and glances (see [AnimatedPal]). */
@Composable
fun Pal(mood: PalMood, size: Dp, modifier: Modifier = Modifier, lookAt: Offset = Offset.Zero) {
    AnimatedPal(mood, size, modifier, lookAt = lookAt)
}

/** The brand's cream pill button with a 3D ember edge, as on the website. */
@Composable
fun BrandButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val drop = if (pressed) 2.dp else 6.dp
    Box(
        modifier
            .padding(top = 6.dp - drop)
            .clip(RoundedCornerShape(999.dp))
            .background(Brand.Ember)
            .padding(bottom = drop),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(999.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFFFFF3DC), Color(0xFFFFD9A0))))
                .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(text, style = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Brand.Ink))
        }
    }
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

/** Pal with a cream speech bubble above it; the bubble pops in. */
@Composable
fun PalWithBubble(mood: PalMood, text: String, size: Dp, modifier: Modifier = Modifier) {
    val pop = remember(text) { Animatable(0.6f) }
    LaunchedEffect(text) { pop.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium)) }
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text,
            style = TextStyle(fontFamily = Brand.Display, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = Brand.Ink),
            modifier = Modifier
                .graphicsLayer { scaleX = pop.value; scaleY = pop.value; alpha = pop.value.coerceIn(0f, 1f) }
                .clip(RoundedCornerShape(999.dp))
                .background(Brand.Cream)
                .padding(horizontal = 18.dp, vertical = 8.dp),
        )
        Pal(mood, size)
    }
}
