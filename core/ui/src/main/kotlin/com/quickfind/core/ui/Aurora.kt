package com.quickfind.core.ui

import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/** The brand gradient: violet → magenta → amber → cyan, the "aurora". */
object Aurora {
    val Violet = Color(0xFF7C4DFF)
    val Magenta = Color(0xFFFF4FD8)
    val Amber = Color(0xFFFFB547)
    val Cyan = Color(0xFF22D3EE)

    /** Closed loop (first colour repeated) so sweep gradients have no seam. */
    val ring = listOf(Violet, Magenta, Amber, Cyan, Violet)
    val linear = listOf(Violet, Magenta, Cyan)

    fun horizontal(): Brush = Brush.linearGradient(linear)
}

/** True when the user turned animations off (Developer options or Accessibility "Remove animations"). */
@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}

/**
 * Slowly drifting aurora blobs. Animated values are read only inside the draw lambda, so the
 * animation redraws but never recomposes (keeps typing latency inside the spec 9 budget).
 */
@Composable
fun AuroraBackground(modifier: Modifier = Modifier, animated: Boolean = true) {
    val t = if (animated) {
        val transition = rememberInfiniteTransition(label = "aurora")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = (2 * Math.PI).toFloat(),
            animationSpec = infiniteRepeatable(tween(18_000, easing = LinearEasing)),
            label = "aurora-phase",
        )
    } else {
        null
    }
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val alpha = if (dark) 0.55f else 0.35f
    Canvas(modifier) {
        val phase = t?.value ?: 0.6f
        val r = size.maxDimension * 0.65f
        fun blob(color: Color, cx: Float, cy: Float, speed: Float, offset: Float) {
            val a = phase * speed + offset
            val center = Offset(
                x = size.width * (cx + 0.18f * cos(a)),
                y = size.height * (cy + 0.12f * sin(a * 1.3f)),
            )
            drawCircle(Brush.radialGradient(listOf(color.copy(alpha = alpha), Color.Transparent), center, r), r, center)
        }
        blob(Aurora.Violet, 0.15f, 0.12f, 1f, 0f)
        blob(Aurora.Magenta, 0.9f, 0.3f, 1f, 2f)
        blob(Aurora.Cyan, 0.3f, 0.85f, 1f, 4f)
        blob(Aurora.Amber, 0.85f, 0.95f, 1f, 1f)
    }
}

/**
 * A container with an animated, rotating gradient ring: the "AI is here" glow.
 * [intense] speeds the ring up and widens it (used while results are loading).
 */
@Composable
fun GlowBorderBox(
    shape: Shape,
    modifier: Modifier = Modifier,
    borderWidth: Dp = 2.dp,
    intense: Boolean = false,
    animated: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    content: @Composable BoxScope.() -> Unit,
) {
    val angle = if (animated) {
        val transition = rememberInfiniteTransition(label = "glow")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(if (intense) 1_400 else 6_000, easing = LinearEasing), RepeatMode.Restart),
            label = "glow-angle",
        )
    } else {
        null
    }
    val ringBrush = remember { Brush.sweepGradient(Aurora.ring) }
    Box(
        modifier
            .clip(shape)
            .drawBehind {
                rotate(angle?.value ?: 30f) {
                    drawCircle(ringBrush, radius = size.maxDimension, center = center)
                }
            }
            .padding(if (intense) borderWidth + 1.dp else borderWidth),
    ) {
        // Explicit content colour: translucent containers defeat contentColorFor(), which
        // otherwise falls back to black text on our dark glass.
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
            Box(Modifier.clip(shape).background(containerColor), content = content)
        }
    }
}

private fun Color.luminance(): Float = 0.2126f * red + 0.7152f * green + 0.0722f * blue
