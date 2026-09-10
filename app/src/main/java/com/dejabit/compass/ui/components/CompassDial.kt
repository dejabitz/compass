package com.dejabit.compass.ui.components

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import com.dejabit.compass.model.CompassState
import com.dejabit.compass.ui.theme.AmbientGrey
import com.dejabit.compass.ui.theme.AmbientWhite
import com.dejabit.compass.ui.theme.DialCardinalText
import com.dejabit.compass.ui.theme.DialPrimaryTicks
import com.dejabit.compass.ui.theme.DialSecondaryTicks
import com.dejabit.compass.ui.theme.GhostMagText
import com.dejabit.compass.ui.theme.MagneticNorthAccent
import com.dejabit.compass.ui.theme.SouthAccent
import com.dejabit.compass.ui.theme.TrueNorthAccent
import kotlin.math.cos
import kotlin.math.sin

/**
 * High-performance, battery-efficient custom Canvas compass dial.
 *
 * Designed for round Wear OS displays. Rotates the compass card according to current heading.
 *
 * User Customization:
 * - True North (common case): Solid Safety Orange needle, clean and uncrowded.
 * - Magnetic North: Distinct cyan needle with dashed/hollow accents and subtle ghost 'MAG' indicator.
 */
@Composable
fun CompassDial(
    state: CompassState,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = (size.minDimension / 2f) - 6.dp.toPx()

            if (state.isAmbient) {
                drawAmbientDial(center, radius, state)
            } else {
                drawInteractiveDial(center, radius, state)
            }
        }
    }
}

private fun DrawScope.drawInteractiveDial(
    center: Offset,
    radius: Float,
    state: CompassState
) {
    // 1. Top Fixed Lubber Line (12 o'clock indicator)
    val lubberColor = if (state.isTrueNorth) TrueNorthAccent else MagneticNorthAccent
    drawLine(
        color = lubberColor,
        start = Offset(center.x, center.y - radius),
        end = Offset(center.x, center.y - radius + 12.dp.toPx()),
        strokeWidth = 3.dp.toPx()
    )

    // 2. Rotating Compass Rose (-azimuth brings current heading to 12 o'clock)
    rotate(degrees = -state.azimuth, pivot = center) {
        // Outer tick marks
        for (deg in 0 until 360 step 5) {
            val rad = Math.toRadians(deg.toDouble() - 90.0)
            val isMajor = deg % 30 == 0
            val isCardinal = deg % 90 == 0

            val tickLength = when {
                isCardinal -> 14.dp.toPx()
                isMajor -> 10.dp.toPx()
                else -> 5.dp.toPx()
            }

            val strokeWidth = when {
                isCardinal -> 2.5.dp.toPx()
                isMajor -> 2.0.dp.toPx()
                else -> 1.0.dp.toPx()
            }

            val tickColor = when {
                deg == 0 -> if (state.isTrueNorth) TrueNorthAccent else MagneticNorthAccent
                isCardinal -> DialCardinalText
                isMajor -> DialPrimaryTicks
                else -> DialSecondaryTicks
            }

            val startX = (center.x + (radius - tickLength) * cos(rad)).toFloat()
            val startY = (center.y + (radius - tickLength) * sin(rad)).toFloat()
            val endX = (center.x + radius * cos(rad)).toFloat()
            val endY = (center.y + radius * sin(rad)).toFloat()

            drawLine(
                color = tickColor,
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = strokeWidth
            )
        }

        // Cardinal Letters (N, E, S, W)
        drawCardinalLetters(center, radius - 26.dp.toPx(), state)

        // Center Needle (Pointing to North and South)
        drawCenterNeedle(center, radius * 0.48f, state)
    }

    // 3. Central Degree & Cardinal Readout
    drawCentralReadout(center, state)
}

private fun DrawScope.drawCenterNeedle(
    center: Offset,
    length: Float,
    state: CompassState
) {
    val halfWidth = 10.dp.toPx()

    // North Needle
    val northPath = Path().apply {
        moveTo(center.x, center.y - length)           // Tip
        lineTo(center.x + halfWidth, center.y)        // Right base
        lineTo(center.x, center.y - (length * 0.25f)) // Inner notch
        lineTo(center.x - halfWidth, center.y)        // Left base
        close()
    }

    // South Needle (Muted Grey)
    val southPath = Path().apply {
        moveTo(center.x, center.y + length)           // South tip
        lineTo(center.x + halfWidth, center.y)        // Right base
        lineTo(center.x, center.y + (length * 0.25f)) // Inner notch
        lineTo(center.x - halfWidth, center.y)        // Left base
        close()
    }

    if (state.isTrueNorth) {
        // True North: Solid vibrant safety orange
        drawPath(path = northPath, color = TrueNorthAccent)
        drawPath(path = southPath, color = SouthAccent)
    } else {
        // Magnetic North: Distinct cyan with dashed stroke
        drawPath(
            path = northPath,
            color = MagneticNorthAccent,
            style = Stroke(
                width = 2.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 4f), 0f)
            )
        )
        drawPath(
            path = southPath,
            color = SouthAccent,
            style = Stroke(width = 2.dp.toPx())
        )
    }

    // Pivot center dot
    drawCircle(
        color = DialCardinalText,
        radius = 4.dp.toPx(),
        center = center
    )
}

private fun DrawScope.drawCardinalLetters(
    center: Offset,
    textRadius: Float,
    state: CompassState
) {
    val paint = Paint().apply {
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        textSize = 14.dp.toPx()
        isFakeBoldText = true
    }

    val cardinals = listOf("N", "E", "S", "W")
    cardinals.forEachIndexed { index, letter ->
        val deg = index * 90.0
        val rad = Math.toRadians(deg - 90.0)

        paint.color = when (index) {
            0 -> if (state.isTrueNorth) android.graphics.Color.parseColor("#FF5722")
                 else android.graphics.Color.parseColor("#00E5FF")
            2 -> android.graphics.Color.parseColor("#9E9E9E")
            else -> android.graphics.Color.WHITE
        }

        val x = (center.x + textRadius * cos(rad)).toFloat()
        val y = (center.y + textRadius * sin(rad) - ((paint.descent() + paint.ascent()) / 2f)).toFloat()

        drawContext.canvas.nativeCanvas.drawText(letter, x, y, paint)
    }
}

private fun DrawScope.drawCentralReadout(
    center: Offset,
    state: CompassState
) {
    val degreesPaint = Paint().apply {
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        textSize = 28.dp.toPx()
        color = android.graphics.Color.WHITE
        isFakeBoldText = true
    }

    val cardinalPaint = Paint().apply {
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        textSize = 14.dp.toPx()
        color = if (state.isTrueNorth) android.graphics.Color.parseColor("#FF5722")
                else android.graphics.Color.parseColor("#00E5FF")
        isFakeBoldText = true
    }

    // Large degree text above center
    val degreesY = center.y - 28.dp.toPx()
    drawContext.canvas.nativeCanvas.drawText(
        state.formattedDegrees,
        center.x,
        degreesY,
        degreesPaint
    )

    // Direction text (e.g., "NW")
    val cardinalY = center.y + 44.dp.toPx()
    drawContext.canvas.nativeCanvas.drawText(
        state.cardinalDirection,
        center.x,
        cardinalY,
        cardinalPaint
    )

    // Ghost 'MAG' text only when in Magnetic North mode
    if (!state.isTrueNorth) {
        val magPaint = Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            textSize = 11.dp.toPx()
            color = android.graphics.Color.argb(100, 255, 255, 255)
            letterSpacing = 0.15f
        }
        drawContext.canvas.nativeCanvas.drawText(
            "MAG",
            center.x,
            center.y + 58.dp.toPx(),
            magPaint
        )
    }
}

private fun DrawScope.drawAmbientDial(
    center: Offset,
    radius: Float,
    state: CompassState
) {
    // High-contrast monochrome AOD rendering for OLED power saving
    rotate(degrees = -state.azimuth, pivot = center) {
        // Draw 4 cardinal ticks only
        for (deg in 0 until 360 step 90) {
            val rad = Math.toRadians(deg.toDouble() - 90.0)
            val length = 12.dp.toPx()
            val startX = (center.x + (radius - length) * cos(rad)).toFloat()
            val startY = (center.y + (radius - length) * sin(rad)).toFloat()
            val endX = (center.x + radius * cos(rad)).toFloat()
            val endY = (center.y + radius * sin(rad)).toFloat()

            drawLine(
                color = if (deg == 0) AmbientWhite else AmbientGrey,
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = 2.dp.toPx()
            )
        }
    }

    val ambientTextPaint = Paint().apply {
        isAntiAlias = false // Save GPU power in ambient
        textAlign = Paint.Align.CENTER
        textSize = 26.dp.toPx()
        color = android.graphics.Color.WHITE
    }

    drawContext.canvas.nativeCanvas.drawText(
        state.formattedDegrees,
        center.x,
        center.y,
        ambientTextPaint
    )
}
