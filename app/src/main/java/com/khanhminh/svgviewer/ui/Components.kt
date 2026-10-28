package com.khanhminh.svgviewer.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.khanhminh.svgviewer.ui.theme.LocalAccent

/**
 * Nền ứng dụng: gradient dọc + hai quầng sáng màu để tạo chiều sâu.
 */
@Composable
fun AppBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val accent = LocalAccent.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        scheme.background,
                        scheme.background,
                        scheme.surface.copy(alpha = 0.65f)
                    )
                )
            )
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(accent.violet.copy(alpha = 0.30f), Color.Transparent),
                    center = Offset(size.width * 0.15f, size.height * 0.05f),
                    radius = size.minDimension * 0.85f
                ),
                radius = size.minDimension * 0.85f,
                center = Offset(size.width * 0.15f, size.height * 0.05f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(accent.mint.copy(alpha = 0.20f), Color.Transparent),
                    center = Offset(size.width * 0.95f, size.height * 0.35f),
                    radius = size.minDimension * 0.7f
                ),
                radius = size.minDimension * 0.7f,
                center = Offset(size.width * 0.95f, size.height * 0.35f)
            )
        }
        content()
    }
}

/**
 * Biểu trưng vector: đường bezier kèm hai điểm neo.
 */
@Composable
fun AppLogo(modifier: Modifier = Modifier, size: Dp = 40.dp) {
    val accent = LocalAccent.current
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val s = w / 108f
        val path = Path().apply {
            moveTo(34 * s, 76 * s)
            cubicTo(34 * s, 52 * s, 74 * s, 58 * s, 74 * s, 32 * s)
        }
        drawPath(
            path = path,
            brush = Brush.linearGradient(
                colors = listOf(accent.violet, accent.cyan, accent.mint),
                start = Offset(0f, this.size.height),
                end = Offset(this.size.width, 0f)
            ),
            style = Stroke(width = 7 * s, cap = StrokeCap.Round)
        )
        drawCircle(color = Color.White, radius = 8.5f * s, center = Offset(34 * s, 76 * s))
        drawCircle(color = accent.indigo, radius = 4f * s, center = Offset(34 * s, 76 * s))
        drawCircle(color = accent.mint, radius = 8.5f * s, center = Offset(74 * s, 32 * s))
        drawCircle(color = Color.White.copy(alpha = 0.9f), radius = 3.2f * s, center = Offset(50 * s, 64 * s))
        drawCircle(color = Color.White.copy(alpha = 0.9f), radius = 3.2f * s, center = Offset(58 * s, 44 * s))
    }
}

/** Nền ca-rô cho vùng canvas trong suốt. */
@Composable
fun Checkerboard(modifier: Modifier = Modifier, cell: Dp = 13.dp) {
    val scheme = MaterialTheme.colorScheme
    val a = scheme.surfaceVariant
    val b = scheme.surface
    Canvas(modifier) {
        drawRect(a)
        val c = cell.toPx().coerceAtLeast(2f)
        var y = 0f
        var row = 0
        while (y < size.height) {
            var x = 0f
            var col = 0
            while (x < size.width) {
                if ((row + col) % 2 == 0) {
                    drawRect(b, topLeft = Offset(x, y), size = Size(c, c))
                }
                x += c
                col++
            }
            y += c
            row++
        }
    }
}

/** Nền trơn cho canvas. */
@Composable
fun SolidCanvas(color: Color, modifier: Modifier = Modifier) {
    Box(modifier.background(color))
}

val CardShape = RoundedCornerShape(22.dp)
