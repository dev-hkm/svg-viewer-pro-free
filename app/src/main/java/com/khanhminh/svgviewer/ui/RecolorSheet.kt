package com.khanhminh.svgviewer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.khanhminh.svgviewer.R
import com.khanhminh.svgviewer.data.SvgPalette
import com.khanhminh.svgviewer.ui.haptics.LocalHaptics
import com.khanhminh.svgviewer.ui.theme.AppIcons

/**
 * Bảng màu có thể đổi tại chỗ: chạm một ô để thay màu đó trong toàn bản vẽ,
 * nhấn giữ để chép mã HEX. Mọi thay đổi đều hoàn tác được.
 */
@Composable
fun RecolorSheet(
    swatches: List<SvgPalette.Swatch>,
    steps: Int,
    onPick: (String) -> Unit,
    onCopy: (String) -> Unit,
    onUndo: () -> Unit,
    onReset: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val haptics = LocalHaptics.current

    Column(
        Modifier
            .padding(horizontal = 20.dp)
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = stringResource(R.string.palette_title),
            style = MaterialTheme.typography.titleLarge,
            color = scheme.onSurface
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.palette_recolor_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant
        )

        Spacer(Modifier.height(14.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GhostAction(
                text = stringResource(R.string.palette_undo),
                icon = AppIcons.Parent,
                onClick = onUndo,
                enabled = steps > 0
            )
            GhostAction(
                text = stringResource(R.string.palette_reset),
                icon = AppIcons.Refresh,
                onClick = onReset,
                enabled = steps > 0
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = stringResource(R.string.palette_steps, steps),
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                color = scheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(14.dp))

        if (swatches.isEmpty()) {
            Text(
                text = stringResource(R.string.palette_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant
            )
        } else {
            swatches.chunked(4).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { swatch ->
                        SwatchCell(
                            swatch = swatch,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                haptics.selection()
                                onPick(swatch.hex)
                            },
                            onLongPress = {
                                haptics.click()
                                onCopy(swatch.hex)
                            }
                        )
                    }
                    repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                }
                Spacer(Modifier.height(6.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.palette_recolor_note),
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SwatchCell(
    swatch: SvgPalette.Swatch,
    modifier: Modifier,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .pointerInput(swatch.hex) {
                detectTapGestures(onTap = { onClick() }, onLongPress = { onLongPress() })
            }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(46.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(colorOf(swatch.hex))
                .border(1.dp, scheme.outlineVariant, RoundedCornerShape(10.dp))
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = swatch.hex.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
            color = scheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = stringResource(R.string.palette_places, swatch.count),
            style = MaterialTheme.typography.labelSmall,
            color = scheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}
