package com.khanhminh.svgviewer.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Nền app: một sắc phẳng, chỉ thêm quầng sáng rất nhẹ ở đỉnh để có chiều sâu. */
@Composable
fun AppBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(scheme.background)
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val radius = size.width * 0.9f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        scheme.onBackground.copy(alpha = 0.045f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.5f, 0f),
                    radius = radius
                ),
                radius = radius,
                center = Offset(size.width * 0.5f, 0f)
            )
        }
        content()
    }
}

/**
 * Logo trong app: cùng một glyph với icon launcher (chữ S dựng từ hai cung bezier),
 * vẽ bằng màu chữ của theme để đổi màu theo Light/Dark.
 */
@Composable
fun AppLogo(modifier: Modifier = Modifier, size: Dp = 32.dp, tint: Color? = null) {
    val color = tint ?: MaterialTheme.colorScheme.onBackground
    Canvas(modifier.size(size)) {
        val s = this.size.width / 108f
        val path = Path().apply {
            moveTo(64.32f * s, 45.4f * s)
            cubicTo(64.32f * s, 36.8f * s, 43.68f * s, 36.8f * s, 43.68f * s, 45.4f * s)
            cubicTo(43.68f * s, 54f * s, 64.32f * s, 54f * s, 64.32f * s, 62.6f * s)
            cubicTo(64.32f * s, 71.2f * s, 43.68f * s, 71.2f * s, 43.68f * s, 62.6f * s)
        }
        drawPath(
            path = path,
            color = color,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = 7.8f * s,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}

/** Nền ca-rô cho vùng canvas trong suốt (xám trung tính, không màu). */
@Composable
fun Checkerboard(modifier: Modifier = Modifier, cell: Dp = 12.dp) {
    val scheme = MaterialTheme.colorScheme
    val base = scheme.surfaceContainer
    val alt = scheme.onBackground.copy(alpha = 0.05f)
    Canvas(modifier) {
        drawRect(base)
        val c = cell.toPx().coerceAtLeast(2f)
        var y = 0f
        var row = 0
        while (y < size.height) {
            var x = 0f
            var col = 0
            while (x < size.width) {
                if ((row + col) % 2 == 0) {
                    drawRect(alt, topLeft = Offset(x, y), size = Size(c, c))
                }
                x += c
                col++
            }
            y += c
            row++
        }
    }
}

/** Nhãn nhóm nội dung: chữ hoa, giãn chữ, màu phụ. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, trailing: String? = null) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        if (trailing != null) {
            Text(
                text = trailing,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Đường phân cách mảnh, đúng một hairline. */
@Composable
fun Hairline(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant)
    )
}

/** Nút hành động chính: nền đảo màu với nền app, chữ tương phản. */
@Composable
fun PrimaryAction(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val scheme = MaterialTheme.colorScheme
    val haptics = com.khanhminh.svgviewer.ui.haptics.LocalHaptics.current
    Row(
        modifier = modifier
            .heightIn(min = 54.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (enabled) scheme.onBackground else scheme.outlineVariant)
            .tap(enabled = enabled) {
                haptics.impact()
                onClick()
            }
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) scheme.background else scheme.onSurfaceVariant,
            modifier = Modifier.size(19.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (enabled) scheme.background else scheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Nút hành động phụ: chỉ viền, không tô. */
@Composable
fun SecondaryAction(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val haptics = com.khanhminh.svgviewer.ui.haptics.LocalHaptics.current
    Row(
        modifier = modifier
            .heightIn(min = 54.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, scheme.outline, RoundedCornerShape(16.dp))
            .tap {
                haptics.click()
                onClick()
            }
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = scheme.onBackground,
            modifier = Modifier.size(19.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = scheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Nút icon tròn, vùng chạm luôn 48dp. */
@Composable
fun IconAction(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color? = null,
    filled: Boolean = false
) {
    val scheme = MaterialTheme.colorScheme
    val haptics = com.khanhminh.svgviewer.ui.haptics.LocalHaptics.current
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .then(if (filled) Modifier.background(scheme.onBackground) else Modifier)
            .tap {
                haptics.click()
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint ?: if (filled) scheme.background else scheme.onSurface,
            modifier = Modifier.size(21.dp)
        )
    }
}

/** Lựa chọn dạng viên thuốc, dùng cho nhóm 2-3 tuỳ chọn ngắn. */
@Composable
fun ChoicePill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val haptics = com.khanhminh.svgviewer.ui.haptics.LocalHaptics.current
    Box(
        modifier = modifier
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) scheme.onBackground else Color.Transparent)
            .border(
                width = 1.dp,
                color = if (selected) Color.Transparent else scheme.outline,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(
                interactionSource = null,
                indication = null,
                role = Role.RadioButton
            ) {
                haptics.selection()
                onClick()
            }
            .padding(horizontal = 14.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) scheme.background else scheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun SettingRow(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = scheme.onSurface)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(12.dp))
        content()
    }
}
