package com.upisoundbox.core.design.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * Custom chat bubble shape with a small tail at the bottom-left.
 * The body has 16dp corner radius and a 6dp tail extending downwards/left.
 */
class ChatBubbleShape(
    val cornerRadius: Dp = 16.dp,
    val tailWidth: Dp = 10.dp,
    val tailHeight: Dp = 6.dp
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val radiusPx = with(density) { cornerRadius.toPx() }
        val tailWPx = with(density) { tailWidth.toPx() }
        val tailHPx = with(density) { tailHeight.toPx() }

        val path = Path().apply {
            val mainHeight = size.height - tailHPx
            // Start at top-left corner after radius
            moveTo(radiusPx, 0f)
            // Top edge to top-right
            lineTo(size.width - radiusPx, 0f)
            arcTo(
                rect = Rect(size.width - 2 * radiusPx, 0f, size.width, 2 * radiusPx),
                startAngleDegrees = -90f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )
            // Right edge to bottom-right
            lineTo(size.width, mainHeight - radiusPx)
            arcTo(
                rect = Rect(size.width - 2 * radiusPx, mainHeight - 2 * radiusPx, size.width, mainHeight),
                startAngleDegrees = 0f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )
            // Bottom edge from right towards bottom-left tail
            lineTo(radiusPx + tailWPx, mainHeight)
            // Tail drawing: curves down to bottom-left point, then up to left edge
            lineTo(0f, size.height)
            lineTo(0f, radiusPx)
            arcTo(
                rect = Rect(0f, 0f, 2 * radiusPx, 2 * radiusPx),
                startAngleDegrees = 180f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )
            close()
        }
        return Outline.Generic(path)
    }
}

object SoundboxShapes {
    val Card = RoundedCornerShape(24.dp)
    val CardLarge = RoundedCornerShape(28.dp)
    val Pill = CircleShape
    val RoundIconButton = CircleShape
    val ChatBubble = ChatBubbleShape(cornerRadius = 16.dp, tailWidth = 10.dp, tailHeight = 6.dp)
    val StatusChip = CircleShape
}

val MaterialShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)
