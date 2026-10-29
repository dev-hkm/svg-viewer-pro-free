package com.khanhminh.svgviewer.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.delay
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.rotate
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
 * Logo trong app: cùng hình với icon launcher — 4 trục xoay 45° tạo 8 đỉnh
 * quanh một hồng tâm 3 lớp — vẽ bằng màu chữ của theme để đổi theo Light/Dark.
 */
@Composable
fun AppLogo(modifier: Modifier = Modifier, size: Dp = 32.dp, tint: Color? = null) {
    val color = tint ?: MaterialTheme.colorScheme.onBackground
    val backdrop = MaterialTheme.colorScheme.background
    Canvas(modifier.size(size)) {
        // Ngoài launcher, logo không bị mask nên cho figure lấp gần kín khung:
        // đường kính figure = 64/68 chiều rộng ô, đủ thoáng mà vẫn rõ ở 22dp.
        val u = this.size.minDimension / 68f
        val c = Offset(this.size.width / 2f, this.size.height / 2f)
        val half = 25.6f * u
        val spokeWidth = 3.459f * u
        val nodeRadius = 4.843f * u
        val nodeStroke = 3.114f * u
        val ringOuter = 14.184f * u
        val ringInner = 7.957f * u
        val dot = 3.114f * u

        repeat(4) { index ->
            rotate(index * 45f, pivot = c) {
                drawLine(
                    color = color,
                    start = Offset(c.x, c.y - half),
                    end = Offset(c.x, c.y + half),
                    strokeWidth = spokeWidth,
                    cap = StrokeCap.Round
                )
                listOf(c.y - half, c.y + half).forEach { y ->
                    drawCircle(color = backdrop, radius = nodeRadius, center = Offset(c.x, y))
                    drawCircle(
                        color = color,
                        radius = nodeRadius,
                        center = Offset(c.x, y),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = nodeStroke)
                    )
                }
            }
        }

        drawCircle(color = color, radius = ringOuter, center = c)
        drawCircle(color = backdrop, radius = ringInner, center = c)
        drawCircle(color = color, radius = dot, center = c)
    }
}

/** Nền ca-rô cho vùng canvas trong suốt; [dark] dùng tông than chì thay vì xám sáng. */
@Composable
fun Checkerboard(modifier: Modifier = Modifier, cell: Dp = 12.dp, dark: Boolean = false) {
    val base = if (dark) Color(0xFF171717) else Color(0xFFF3F3F3)
    val alt = if (dark) Color(0xFF232323) else Color(0xFFE2E2E2)
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

/** Nút icon tròn, vùng chạm luôn 48dp (hoặc [size] khi dock cần thu gọn). */
@Composable
fun IconAction(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color? = null,
    filled: Boolean = false,
    animated: Boolean = false,
    size: Dp = 48.dp,
    iconSize: Dp = 21.dp
) {
    val scheme = MaterialTheme.colorScheme
    val haptics = com.khanhminh.svgviewer.ui.haptics.LocalHaptics.current
    val iconTint = tint ?: if (filled) scheme.background else scheme.onSurface
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .then(if (filled) Modifier.background(scheme.onBackground) else Modifier)
            .tap {
                haptics.click()
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        if (animated) {
            AnimatedIcon(
                icon = icon,
                contentDescription = contentDescription,
                tint = iconTint,
                size = iconSize
            )
        } else {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = iconTint,
                modifier = Modifier.size(iconSize)
            )
        }
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

/**
 * Đổi icon có chuyển động: icon mới phóng lên kèm xoay nhẹ, icon cũ mờ đi.
 * Dùng cho nút đổi theme và nút play/pause.
 */
@Composable
fun AnimatedIcon(
    icon: ImageVector,
    contentDescription: String?,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 21.dp
) {
    val spin = remember { Animatable(0f) }
    var first by remember { mutableStateOf(true) }

    LaunchedEffect(icon) {
        if (first) {
            first = false
        } else {
            spin.snapTo(-70f)
            spin.animateTo(0f, tween(380, easing = Motion.Easing))
        }
    }

    AnimatedContent(
        targetState = icon,
        transitionSpec = {
            (fadeIn(tween(200, easing = Motion.Easing)) +
                scaleIn(tween(300, easing = Motion.Easing), initialScale = 0.5f)) togetherWith
                (fadeOut(tween(130)) + scaleOut(tween(160), targetScale = 0.5f))
        },
        label = "icon"
    ) { vector ->
        Icon(
            imageVector = vector,
            contentDescription = contentDescription,
            tint = tint,
            modifier = modifier
                .size(size)
                .graphicsLayer { rotationZ = spin.value }
        )
    }
}

/** Phần tử hiện lên lần lượt từ dưới lên, tạo nhịp khi mở màn. */
@Composable
fun EnterUp(
    index: Int = 0,
    stepMillis: Int = 55,
    content: @Composable () -> Unit
) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(index * stepMillis.toLong())
        shown = true
    }
    AnimatedVisibility(
        visible = shown,
        enter = fadeIn(tween(320, easing = Motion.Easing)) +
            slideInVertically(tween(380, easing = Motion.Easing)) { it / 6 }
    ) {
        content()
    }
}

/** Trạng thái của hiệu ứng lan toả khi đổi theme. */
data class RippleState(val origin: Offset, val key: Long = System.nanoTime())

/**
 * Vòng sáng lan toả từ vị trí vừa chạm — phản hồi trực quan cho việc đổi theme.
 * Vẽ phía trên toàn bộ nội dung nên không phụ thuộc layout của màn hiện tại.
 */
@Composable
fun RippleOverlay(state: RippleState, onFinished: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val progress = remember(state.key) { Animatable(0f) }
    val maxRadius = with(LocalDensity.current) { 2200.dp.toPx() }
    val ringWidth = with(LocalDensity.current) { 2.5.dp.toPx() }

    LaunchedEffect(state.key) {
        progress.animateTo(1f, tween(620, easing = Motion.Easing))
        onFinished()
    }

    Canvas(Modifier.fillMaxSize()) {
        val p = progress.value
        if (p <= 0f) return@Canvas
        val radius = maxRadius * p
        val fade = (1f - p) * (1f - p)

        // Quầng sáng lan dần
        drawCircle(
            color = scheme.onBackground.copy(alpha = 0.07f * fade),
            radius = radius,
            center = state.origin
        )
        // Vành sáng ở mép sóng, tạo cảm giác "lan toả" rõ ràng
        drawCircle(
            color = scheme.onBackground.copy(alpha = 0.22f * fade),
            radius = radius,
            center = state.origin,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = ringWidth * (1f - p * 0.6f)
            )
        )
    }
}
