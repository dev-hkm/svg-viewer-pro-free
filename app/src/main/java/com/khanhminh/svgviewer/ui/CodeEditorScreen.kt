package com.khanhminh.svgviewer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.khanhminh.svgviewer.R
import com.khanhminh.svgviewer.ui.theme.AppIcons

/**
 * Trình soạn markup toàn màn hình: gõ, xem cú pháp tô màu ngay trong lúc gõ,
 * rồi áp dụng để khung vẽ dựng lại. Nội dung được giữ ở màn hình cha nên lỡ
 * đóng rồi mở lại vẫn còn nguyên bản nháp.
 *
 * Đây là lớp phủ ngay trong khung vẽ (không dùng Dialog) để dùng chung hệ
 * thống thanh hệ thống và vùng an toàn với màn hình chính.
 */
@Composable
fun CodeEditorScreen(
    text: String,
    initial: String,
    applied: String,
    onTextChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onCopy: () -> Unit,
    onApply: () -> String?
) {
    val scheme = MaterialTheme.colorScheme
    val dark = scheme.background.luminance() < 0.5f
    val scroll = rememberScrollState()
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .background(scheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconAction(
                icon = AppIcons.Close,
                contentDescription = stringResource(R.string.editor_close),
                onClick = onDismiss,
                tint = scheme.onSurfaceVariant
            )
            Spacer(Modifier.width(4.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.editor_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = scheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.editor_chars, text.length),
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 12.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(scheme.surfaceContainer)
                .border(1.dp, scheme.outlineVariant, RoundedCornerShape(18.dp))
                .padding(horizontal = 12.dp, vertical = 12.dp)
        ) {
            BasicTextField(
                value = text,
                onValueChange = {
                    error = null
                    onTextChange(it)
                },
                textStyle = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    color = scheme.onSurface
                ),
                cursorBrush = SolidColor(scheme.onSurface),
                visualTransformation = VisualTransformation { input ->
                    TransformedText(highlightSvg(input.text, dark), OffsetMapping.Identity)
                },
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scroll)
            )
        }

        val message = error
        if (message != null) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = scheme.error,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GhostAction(
                text = stringResource(R.string.editor_reset),
                icon = AppIcons.Refresh,
                onClick = {
                    error = null
                    onTextChange(initial)
                },
                enabled = text != initial
            )
            GhostAction(
                text = stringResource(R.string.editor_copy),
                icon = AppIcons.Copy,
                onClick = onCopy,
                enabled = text.isNotBlank()
            )
        }

        PrimaryAction(
            text = stringResource(R.string.editor_apply),
            icon = AppIcons.Check,
            enabled = text != applied && text.isNotBlank(),
            onClick = { error = onApply() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .padding(bottom = 12.dp)
        )
    }
}

/** Ô hiển thị nội dung chỉ đọc cho bản xem trước snippet. */
@Composable
fun CodePreview(
    text: String,
    dark: Boolean,
    modifier: Modifier = Modifier,
    maxLines: Int = 8
) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(scheme.surfaceContainer)
            .border(1.dp, scheme.outlineVariant, RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Text(
            text = highlightSvg(text, dark),
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                lineHeight = 18.sp
            ),
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis
        )
    }
}
