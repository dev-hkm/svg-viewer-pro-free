package com.khanhminh.svgviewer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.khanhminh.svgviewer.R
import com.khanhminh.svgviewer.data.CanvasBackground
import com.khanhminh.svgviewer.ui.haptics.LocalHaptics
import com.khanhminh.svgviewer.ui.theme.AppIcons

/** Chọn nền canvas: lưới swatch, có bản xem trước caro cho 2 mục trong suốt. */
@Composable
fun CanvasPickerSheet(
    selected: CanvasBackground,
    onSelect: (CanvasBackground) -> Unit,
    onDismiss: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme

    Column(
        Modifier
            .padding(horizontal = 20.dp)
            .navigationBarsPadding()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.canvas_title),
                style = MaterialTheme.typography.titleLarge,
                color = scheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            IconAction(
                icon = AppIcons.Close,
                contentDescription = stringResource(R.string.action_close),
                onClick = onDismiss,
                tint = scheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(14.dp))

        CanvasBackground.entries.chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { item ->
                    Swatch(
                        item = item,
                        selected = item == selected,
                        onClick = { onSelect(item) },
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
            Spacer(Modifier.height(14.dp))
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun Swatch(
    item: CanvasBackground,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val haptics = LocalHaptics.current

    Column(
        modifier = modifier.tap(role = null) {
            haptics.selection()
            onClick()
        },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp)
                .clip(RoundedCornerShape(14.dp))
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) scheme.onBackground else scheme.outlineVariant,
                    shape = RoundedCornerShape(14.dp)
                )
        ) {
            if (item.checker) {
                Checkerboard(Modifier.fillMaxSize(), cell = 9.dp, dark = !item.isLight)
            } else {
                Box(Modifier.fillMaxSize().background(Color(item.argb)))
            }
            if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(scheme.onBackground),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = AppIcons.Check,
                        contentDescription = null,
                        tint = scheme.background,
                        modifier = Modifier.size(11.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(item.labelRes),
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) scheme.onSurface else scheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

/** Bộ chọn màu: ô bão hoà/độ sáng + thanh hue + nhập HEX. */
@Composable
fun ColorPickerSheet(
    initial: Color,
    onDismiss: () -> Unit,
    onPick: (Color) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val haptics = LocalHaptics.current

    val hsv = remember(initial) {
        FloatArray(3).also { android.graphics.Color.colorToHSV(initial.toArgb(), it) }
    }
    var hue by remember { mutableFloatStateOf(hsv[0]) }
    var sat by remember { mutableFloatStateOf(hsv[1]) }
    var value by remember { mutableFloatStateOf(hsv[2]) }
    var hex by remember { mutableStateOf(toHex(Color.hsv(hue, sat, value))) }
    var editing by remember { mutableStateOf(false) }
    var invalid by remember { mutableStateOf(false) }

    val current = Color.hsv(hue, sat, value)

    LaunchedEffect(hue, sat, value) {
        if (!editing) hex = toHex(current)
    }

    Column(
        Modifier
            .padding(horizontal = 20.dp)
            .navigationBarsPadding()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.color_title),
                style = MaterialTheme.typography.titleLarge,
                color = scheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            IconAction(
                icon = AppIcons.Close,
                contentDescription = stringResource(R.string.action_close),
                onClick = onDismiss,
                tint = scheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(14.dp))

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(168.dp)
                .clip(RoundedCornerShape(16.dp))
                .pointerInput(hue) {
                    detectDragGestures(
                        onDragStart = { o -> onSv(o, size.width.toFloat(), size.height.toFloat()) { s, v -> sat = s; value = v } },
                        onDrag = { change, _ -> onSv(change.position, size.width.toFloat(), size.height.toFloat()) { s, v -> sat = s; value = v } }
                    )
                }
                .pointerInput(hue) {
                    detectTapGestures { o -> onSv(o, size.width.toFloat(), size.height.toFloat()) { s, v -> sat = s; value = v } }
                }
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Brush.horizontalGradient(listOf(Color.White, Color.hsv(hue, 1f, 1f))))
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
            )
            val thumbX = maxWidth * sat
            val thumbY = maxHeight * (1f - value)
            Box(
                Modifier
                    .offset(x = thumbX - 9.dp, y = thumbY - 9.dp)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(current)
                    .border(2.dp, Color.White, CircleShape)
                    .border(3.dp, Color.Black.copy(alpha = 0.35f), CircleShape)
            )
        }

        Spacer(Modifier.height(16.dp))

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .clip(CircleShape)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.hsv(0f, 1f, 1f), Color.hsv(60f, 1f, 1f), Color.hsv(120f, 1f, 1f),
                            Color.hsv(180f, 1f, 1f), Color.hsv(240f, 1f, 1f), Color.hsv(300f, 1f, 1f),
                            Color.hsv(360f, 1f, 1f)
                        )
                    )
                )
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { o -> hue = (o.x / size.width).coerceIn(0f, 1f) * 360f },
                        onDrag = { c, _ -> hue = (c.position.x / size.width).coerceIn(0f, 1f) * 360f }
                    )
                }
                .pointerInput(Unit) {
                    detectTapGestures { o -> hue = (o.x / size.width).coerceIn(0f, 1f) * 360f }
                }
        ) {
            Box(
                modifier = Modifier
                    .offset(x = (maxWidth * (hue / 360f)) - 10.dp, y = 2.dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color.hsv(hue, 1f, 1f))
                    .border(2.dp, Color.White, CircleShape)
                    .border(3.dp, Color.Black.copy(alpha = 0.25f), CircleShape)
            )
        }

        Spacer(Modifier.height(18.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(current)
                    .border(1.dp, scheme.outlineVariant, RoundedCornerShape(12.dp))
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.color_hex),
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant
                )
                Box(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(
                            1.dp,
                            if (invalid) scheme.error else scheme.outlineVariant,
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    BasicTextField(
                        value = hex,
                        onValueChange = { raw ->
                            editing = true
                            val cleaned = raw.uppercase()
                                .filter { it.isDigit() || it in 'A'..'F' || it == '#' }
                                .take(7)
                            hex = if (cleaned.startsWith("#")) cleaned else "#$cleaned"
                            val parsed = parseHex(hex)
                            invalid = parsed == null
                            if (parsed != null) {
                                val arr = FloatArray(3)
                                android.graphics.Color.colorToHSV(parsed.toArgb(), arr)
                                hue = arr[0]; sat = arr[1]; value = arr[2]
                            }
                        },
                        singleLine = true,
                        textStyle = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = scheme.onSurface
                        ),
                        cursorBrush = SolidColor(scheme.onSurface),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        if (invalid) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.color_invalid_hex),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.error
            )
        }

        Spacer(Modifier.height(18.dp))

        PrimaryAction(
            text = stringResource(R.string.action_apply),
            icon = AppIcons.Check,
            enabled = !invalid,
            onClick = {
                haptics.success()
                onPick(current)
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(20.dp))
    }
}

private fun onSv(offset: Offset, width: Float, height: Float, apply: (Float, Float) -> Unit) {
    if (width <= 0f || height <= 0f) return
    apply(
        (offset.x / width).coerceIn(0f, 1f),
        1f - (offset.y / height).coerceIn(0f, 1f)
    )
}

fun toHex(color: Color): String {
    val argb = color.toArgb()
    return String.format("#%02X%02X%02X", (argb shr 16) and 0xFF, (argb shr 8) and 0xFF, argb and 0xFF)
}

private fun parseHex(text: String): Color? {
    val body = text.removePrefix("#")
    if (body.length != 6 || body.any { !it.isDigit() && it.lowercaseChar() !in 'a'..'f' }) return null
    val value = body.toLongOrNull(16) ?: return null
    return Color(0xFF000000 or value)
}
