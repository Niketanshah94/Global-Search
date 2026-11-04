package com.quickfind.core.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp

/**
 * Pal drawn with Canvas, so its parts can move: it floats, blinks, waves, its pupils follow
 * [lookAt] (a direction in -1..1), and switching to [PalMood.FOUND] makes it hop.
 * Geometry matches docs/brand/source/mascot.js (viewBox -26 -18 252 246).
 */
@Composable
fun AnimatedPal(
    mood: PalMood,
    size: Dp,
    modifier: Modifier = Modifier,
    lookAt: Offset = Offset.Zero,
    animated: Boolean = !rememberReducedMotion(),
    description: String? = null,
) {
    val t = rememberInfiniteTransition(label = "pal")
    val float = if (animated) t.animateFloat(0f, 1f, infiniteRepeatable(tween(2200), RepeatMode.Reverse), label = "float").value else 0.5f
    val blink = if (animated) {
        t.animateFloat(
            initialValue = 1f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                keyframes {
                    durationMillis = 4200
                    1f at 3800
                    0.08f at 3900
                    1f at 4000
                },
            ),
            label = "blink",
        ).value
    } else {
        1f
    }
    val wave = if (animated && mood.arm == PalArm.WAVE) {
        t.animateFloat(
            initialValue = 0f,
            targetValue = 0f,
            animationSpec = infiniteRepeatable(
                keyframes {
                    durationMillis = 2400
                    -14f at 360
                    10f at 720
                    -12f at 1080
                    0f at 1440
                },
            ),
            label = "wave",
        ).value
    } else {
        0f
    }
    val sparkle = if (animated) t.animateFloat(0.7f, 1.1f, infiniteRepeatable(tween(800, easing = LinearEasing), RepeatMode.Reverse), label = "sparkle").value else 1f
    // When idle, Pal glances around now and then; an explicit lookAt wins.
    val idleGlance = if (animated) {
        t.animateFloat(
            initialValue = 0f,
            targetValue = 0f,
            animationSpec = infiniteRepeatable(
                keyframes {
                    durationMillis = 7000
                    0f at 2500
                    -1f at 2800
                    -1f at 3800
                    1f at 4200
                    1f at 5000
                    0f at 5300
                },
            ),
            label = "glance",
        ).value
    } else {
        0f
    }
    val target = if (lookAt == Offset.Zero) Offset(idleGlance, 0f) else lookAt
    val pupil by animateOffsetAsState(target, spring(stiffness = Spring.StiffnessMediumLow), label = "pupil")

    val hop = remember { Animatable(0f) }
    LaunchedEffect(mood) {
        if (animated && mood == PalMood.FOUND) {
            hop.animateTo(-1f, tween(160))
            hop.animateTo(0f, spring(dampingRatio = 0.35f, stiffness = Spring.StiffnessMediumLow))
        }
    }

    Canvas(
        modifier
            .size(size)
            .graphicsLayer {
                translationY = (float - 0.5f) * size.toPx() * 0.05f + hop.value * size.toPx() * 0.14f
            }
            .semantics { if (description != null) contentDescription = description },
    ) {
        val s = this.size.minDimension / 252f
        scale(s, s, pivot = Offset.Zero) {
            translate(26f, 18f) {
                drawPal(mood, blink, wave, Offset(pupil.x.coerceIn(-1f, 1f), pupil.y.coerceIn(-1f, 1f)), sparkle)
            }
        }
    }
}

enum class PalFace { HAPPY, LOOK, STARS, SLEEPY, COOL, WORRIED }

enum class PalArm { NONE, WAVE, POINT, CHEER }

private val InkLine = Color(0xFF2B1A10)
private val RimLight = Color(0xFFFFA35C)
private val RimMid = Color(0xFFF07A2E)
private val RimDark = Color(0xFFD9572A)
private val Hand = Color(0xFFFFA35C)
private val Blush = Color(0x8CFF8F6B)

private fun DrawScope.drawPal(mood: PalMood, blink: Float, wave: Float, look: Offset, sparkle: Float) {
    // Arms sit behind the lens.
    when (mood.arm) {
        PalArm.WAVE -> rotate(wave, pivot = Offset(30f, 132f)) { leftArm() }
        PalArm.CHEER -> { leftArm(); rightArm(raised = false) }
        PalArm.POINT -> rightArm(raised = true)
        PalArm.NONE -> Unit
    }
    // Handle = body.
    rotate(-38f, pivot = Offset(132f, 168f)) {
        drawRoundRect(
            Brush.linearGradient(listOf(Color(0xFF5A3420), InkLine), Offset(114f, 140f), Offset(150f, 214f)),
            topLeft = Offset(114f, 140f), size = Size(36f, 74f), cornerRadius = CornerRadius(18f),
        )
        drawRoundRect(InkLine, Offset(114f, 140f), Size(36f, 74f), CornerRadius(18f), style = Stroke(4f))
        drawLine(Color(0xFF7A4A2C), Offset(116f, 168f), Offset(148f, 168f), 4f)
        drawLine(Color(0xFF7A4A2C), Offset(116f, 182f), Offset(148f, 182f), 4f)
        drawRoundRect(RimMid, Offset(112f, 132f), Size(40f, 18f), CornerRadius(7f))
        drawRoundRect(InkLine, Offset(112f, 132f), Size(40f, 18f), CornerRadius(7f), style = Stroke(4f))
    }
    val c = Offset(95f, 96f)
    drawCircle(Brush.linearGradient(listOf(RimLight, RimMid, RimDark), Offset(15f, 16f), Offset(175f, 176f)), 80f, c)
    drawCircle(InkLine, 80f, c, style = Stroke(5f))
    drawCircle(
        Brush.radialGradient(listOf(Color(0xFFFFFDF8), Color(0xFFFFF1DC), Color(0xFFFFE0B8)), Offset(71f, 76f), 100f),
        62f, c,
    )
    drawCircle(InkLine, 62f, c, style = Stroke(4f))
    drawPath(Path().apply { moveTo(52f, 62f); quadraticTo(62f, 46f, 82f, 40f) }, Color.White.copy(alpha = .9f), style = Stroke(8f, cap = StrokeCap.Round))
    if (mood.face != PalFace.COOL) {
        drawOval(Blush, Offset(46f, 106f), Size(20f, 12f))
        drawOval(Blush, Offset(124f, 106f), Size(20f, 12f))
    }
    drawFace(mood.face, blink, look, sparkle)
    when (mood) {
        PalMood.FOUND -> {
            sparkleStar(Offset(176f, 40f), 12f * sparkle)
            sparkleStar(Offset(14f, 38f), 8f * sparkle)
        }
        PalMood.SEARCHING -> {
            val drop = Path().apply { moveTo(156f, 52f); quadraticTo(164f, 64f, 156f, 70f); quadraticTo(148f, 64f, 156f, 52f); close() }
            drawPath(drop, Color(0xFF8FD3FF))
            drawPath(drop, InkLine, style = Stroke(2.5f))
        }
        else -> Unit
    }
}

private fun DrawScope.leftArm() {
    val p = Path().apply { moveTo(30f, 132f); cubicTo(6f, 128f, -10f, 108f, -8f, 84f) }
    drawPath(p, InkLine, style = Stroke(17f, cap = StrokeCap.Round))
    drawPath(p, RimMid, style = Stroke(10f, cap = StrokeCap.Round))
    drawCircle(Hand, 12f, Offset(-8f, 78f))
    drawCircle(InkLine, 12f, Offset(-8f, 78f), style = Stroke(3.5f))
}

private fun DrawScope.rightArm(raised: Boolean) {
    val p = Path().apply { moveTo(162f, 132f); cubicTo(186f, 128f, 202f, 108f, 200f, 86f) }
    drawPath(p, InkLine, style = Stroke(17f, cap = StrokeCap.Round))
    drawPath(p, RimMid, style = Stroke(10f, cap = StrokeCap.Round))
    if (raised) {
        drawLine(InkLine, Offset(200f, 74f), Offset(200f, 54f), 11f, StrokeCap.Round)
        drawLine(Hand, Offset(200f, 74f), Offset(200f, 54f), 5f, StrokeCap.Round)
    }
    drawCircle(Hand, 12f, Offset(200f, 80f))
    drawCircle(InkLine, 12f, Offset(200f, 80f), style = Stroke(3.5f))
}

private fun DrawScope.drawFace(face: PalFace, blink: Float, look: Offset, sparkle: Float) {
    val eyeY = if (face == PalFace.WORRIED) 88f else 86f
    when (face) {
        PalFace.HAPPY, PalFace.LOOK, PalFace.WORRIED -> {
            // Blink squashes the eyes vertically around their centre line.
            scale(1f, blink, pivot = Offset(95f, eyeY)) {
                for (x in listOf(72f, 118f)) {
                    drawOval(Color.White, Offset(x - 17f, eyeY - 20f), Size(34f, 40f))
                    drawOval(InkLine, Offset(x - 17f, eyeY - 20f), Size(34f, 40f), style = Stroke(3.5f))
                }
                val base = when (face) {
                    PalFace.LOOK -> Offset(9f, -2f)
                    PalFace.WORRIED -> Offset(0f, 8f)
                    else -> Offset(4f, 4f)
                }
                val r = if (face == PalFace.WORRIED) 10f else 9f
                for (x in listOf(72f, 118f)) {
                    val pc = Offset(x, eyeY) + base + Offset(look.x * 5f, look.y * 6f)
                    drawCircle(InkLine, r, pc)
                    drawCircle(Color.White, r * .39f, pc + Offset(3f, -4f))
                }
            }
            if (face == PalFace.LOOK) {
                drawPath(Path().apply { moveTo(58f, 64f); quadraticTo(72f, 56f, 86f, 64f) }, InkLine, style = Stroke(4f, cap = StrokeCap.Round))
                drawPath(Path().apply { moveTo(106f, 60f); quadraticTo(120f, 54f, 132f, 64f) }, InkLine, style = Stroke(4f, cap = StrokeCap.Round))
            }
            if (face == PalFace.WORRIED) {
                drawLine(InkLine, Offset(58f, 70f), Offset(84f, 62f), 4f, StrokeCap.Round)
                drawLine(InkLine, Offset(132f, 70f), Offset(106f, 62f), 4f, StrokeCap.Round)
            }
        }
        PalFace.STARS -> for (dx in listOf(0f, 46f)) {
            scale(sparkle.coerceIn(.9f, 1.05f), pivot = Offset(72f + dx, 86f)) {
                val star = Path().apply {
                    val pts = listOf(72f to 66f, 78f to 79f, 92f to 81f, 82f to 91f, 85f to 105f, 72f to 98f, 59f to 105f, 62f to 91f, 52f to 81f, 66f to 79f)
                    pts.forEachIndexed { i, (x, y) -> if (i == 0) moveTo(x + dx, y) else lineTo(x + dx, y) }
                    close()
                }
                drawPath(star, Color(0xFFFFC93C))
                drawPath(star, InkLine, style = Stroke(3f))
            }
        }
        PalFace.SLEEPY -> for (x in listOf(58f, 104f)) {
            drawPath(Path().apply { moveTo(x, 90f); quadraticTo(x + 14f, 102f, x + 28f, 90f) }, InkLine, style = Stroke(4.5f, cap = StrokeCap.Round))
        }
        PalFace.COOL -> {
            drawRoundRect(Color(0xFF1B120C), Offset(50f, 76f), Size(48f, 28f), CornerRadius(14f))
            drawRoundRect(Color(0xFF1B120C), Offset(100f, 76f), Size(48f, 28f), CornerRadius(14f))
            drawLine(Color(0xFF1B120C), Offset(94f, 82f), Offset(104f, 82f), 5f)
            drawLine(Color.White.copy(alpha = .4f), Offset(58f, 82f), Offset(68f, 80f), 4f, StrokeCap.Round)
        }
    }
    // Mouth.
    val mouth = Path()
    when (face) {
        PalFace.HAPPY -> { mouth.moveTo(82f, 118f); mouth.quadraticTo(95f, 130f, 108f, 118f) }
        PalFace.LOOK -> { mouth.moveTo(86f, 120f); mouth.quadraticTo(96f, 124f, 106f, 118f) }
        PalFace.SLEEPY -> { mouth.moveTo(86f, 118f); mouth.quadraticTo(95f, 125f, 104f, 118f) }
        PalFace.COOL -> { mouth.moveTo(80f, 118f); mouth.quadraticTo(95f, 130f, 110f, 116f) }
        PalFace.WORRIED -> { mouth.moveTo(84f, 126f); mouth.quadraticTo(95f, 117f, 106f, 126f) }
        PalFace.STARS -> {
            drawOval(Color(0xFF7A2E14), Offset(86f, 113f), Size(18f, 22f))
            drawOval(InkLine, Offset(86f, 113f), Size(18f, 22f), style = Stroke(3.5f))
            drawOval(Color(0xFFFF8A7A), Offset(90f, 125f), Size(10f, 8f))
        }
    }
    if (face != PalFace.STARS) drawPath(mouth, InkLine, style = Stroke(4.5f, cap = StrokeCap.Round))
}

private fun DrawScope.sparkleStar(c: Offset, r: Float) {
    val p = Path().apply {
        moveTo(c.x, c.y - r); lineTo(c.x + r * .3f, c.y - r * .3f); lineTo(c.x + r, c.y); lineTo(c.x + r * .3f, c.y + r * .3f)
        lineTo(c.x, c.y + r); lineTo(c.x - r * .3f, c.y + r * .3f); lineTo(c.x - r, c.y); lineTo(c.x - r * .3f, c.y - r * .3f); close()
    }
    drawPath(p, Color(0xFFFFC93C))
}
