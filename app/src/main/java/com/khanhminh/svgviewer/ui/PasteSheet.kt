package com.khanhminh.svgviewer.ui

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.khanhminh.svgviewer.R
import com.khanhminh.svgviewer.data.SvgLoader
import com.khanhminh.svgviewer.ui.haptics.LocalHaptics
import com.khanhminh.svgviewer.ui.theme.AppIcons

/**
 * Trình soạn mã SVG: dán từ bộ nhớ tạm hoặc gõ tay, tô màu cú pháp ngay khi nhập,
 * rồi mở luôn vào màn xem.
 */
@Composable
fun PasteSheet(
    onDismiss: () -> Unit,
    onOpen: (String) -> Unit
) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val haptics = LocalHaptics.current
    val palette = codePalette(scheme.background.luminance() < 0.5f)

    var text by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val scroll = rememberScrollState()

    fun readClipboard(): String? {
        val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        return manager?.primaryClip
            ?.takeIf { it.itemCount > 0 }
            ?.getItemAt(0)
            ?.coerceToText(context)
            ?.toString()
    }

    // Nếu bộ nhớ tạm đang có sẵn SVG thì điền luôn, người dùng chỉ cần bấm "Xem SVG".
    LaunchedEffect(Unit) {
        val clip = readClipboard()?.trim()
        if (text.isEmpty() && !clip.isNullOrEmpty() && clip.contains("<svg", ignoreCase = true)) {
            text = clip
        }
    }

    Column(
        Modifier
            .padding(horizontal = 20.dp)
            .navigationBarsPadding()
            .imePadding()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.paste_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = scheme.onSurface
                )
                Text(
                    text = stringResource(R.string.paste_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant
                )
            }
            IconAction(
                icon = AppIcons.Close,
                contentDescription = stringResource(R.string.paste_cd_close),
                onClick = onDismiss,
                tint = scheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(14.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 220.dp, max = 320.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(scheme.surfaceContainer)
                .border(1.dp, scheme.outlineVariant, RoundedCornerShape(16.dp))
        ) {
            BasicTextField(
                value = text,
                onValueChange = {
                    text = it
                    if (error != null) error = null
                },
                textStyle = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.5.sp,
                    lineHeight = 19.sp,
                    color = palette.plain
                ),
                cursorBrush = SolidColor(scheme.onSurface),
                visualTransformation = VisualTransformation { input ->
                    TransformedText(
                        highlightSvg(input.text, scheme.background.luminance() < 0.5f),
                        OffsetMapping.Identity
                    )
                },
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scroll)
                    .padding(14.dp)
            )

            if (text.isEmpty()) {
                Text(
                    text = stringResource(R.string.paste_hint),
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = scheme.onSurfaceVariant.copy(alpha = 0.65f),
                    modifier = Modifier.padding(14.dp)
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            GhostAction(
                text = stringResource(R.string.paste_from_clipboard),
                icon = AppIcons.Clipboard,
                onClick = {
                    val clip = readClipboard()
                    if (clip.isNullOrBlank()) {
                        haptics.warning()
                        error = context.getString(R.string.paste_clipboard_empty)
                    } else {
                        haptics.click()
                        text = clip.trim()
                        error = null
                    }
                }
            )
            Spacer(Modifier.width(8.dp))
            GhostAction(
                text = stringResource(R.string.paste_clear),
                icon = AppIcons.Close,
                enabled = text.isNotEmpty(),
                onClick = {
                    haptics.warning()
                    text = ""
                    error = null
                }
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = stringResource(R.string.paste_chars, text.length),
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant
            )
        }

        val message = error
        if (message != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = scheme.error
            )
        }

        Spacer(Modifier.height(14.dp))

        PrimaryAction(
            text = stringResource(R.string.paste_preview),
            icon = AppIcons.Play,
            enabled = text.isNotBlank(),
            onClick = {
                val doc = SvgLoader.fromText(text, "clipboard.svg")
                if (doc == null) {
                    haptics.warning()
                    error = context.getString(R.string.paste_invalid)
                } else {
                    onOpen(text)
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(20.dp))
    }
}

/** Nút nhỏ dạng chữ + icon cho thanh công cụ. */
@Composable
fun GhostAction(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val scheme = MaterialTheme.colorScheme
    val haptics = LocalHaptics.current
    Row(
        modifier = modifier
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, scheme.outlineVariant, RoundedCornerShape(12.dp))
            .tap(enabled = enabled, role = null) {
                haptics.click()
                onClick()
            }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        androidx.compose.material3.Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) scheme.onSurface else scheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = if (enabled) scheme.onSurface else scheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    }
}
