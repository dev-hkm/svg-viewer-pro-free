package com.khanhminh.svgviewer.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.khanhminh.svgviewer.R
import com.khanhminh.svgviewer.data.RecentFile
import com.khanhminh.svgviewer.data.ThemeMode
import com.khanhminh.svgviewer.ui.haptics.LocalHaptics
import com.khanhminh.svgviewer.ui.theme.AppIcons
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    recents: List<RecentFile>,
    themeMode: ThemeMode,
    onOpenFile: () -> Unit,
    onPaste: () -> Unit,
    onOpenSample: (SampleSvg) -> Unit,
    onOpenRecent: (RecentFile) -> Unit,
    onTogglePin: (RecentFile) -> Unit,
    onRename: (RecentFile) -> Unit,
    onSaveCopy: (RecentFile) -> Unit,
    onRemoveRecent: (RecentFile) -> Unit,
    onClearRecents: () -> Unit,
    onToggleTheme: () -> Unit,
    onThemeAnchorChange: (Offset) -> Unit,
    onOpenSettings: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val haptics = LocalHaptics.current

    AppBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(10.dp))

            // Đầu trang: wordmark + hành động nhanh
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppLogo(size = 22.dp)
                Spacer(Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.app_name).uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.8.sp),
                    color = scheme.onBackground,
                    modifier = Modifier.weight(1f)
                )
                IconAction(
                    icon = if (themeMode == ThemeMode.DARK) AppIcons.Sun else AppIcons.Moon,
                    contentDescription = stringResource(
                        if (themeMode == ThemeMode.DARK) R.string.home_cd_theme_light
                        else R.string.home_cd_theme_dark
                    ),
                    onClick = onToggleTheme,
                    tint = scheme.onSurfaceVariant,
                    animated = true,
                    modifier = Modifier.onGloballyPositioned { coords ->
                        val topLeft = coords.positionInRoot()
                        onThemeAnchorChange(
                            Offset(
                                topLeft.x + coords.size.width / 2f,
                                topLeft.y + coords.size.height / 2f
                            )
                        )
                    }
                )
                IconAction(
                    icon = AppIcons.Gear,
                    contentDescription = stringResource(R.string.home_cd_settings),
                    onClick = onOpenSettings,
                    tint = scheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(26.dp))

            EnterUp(index = 0) {
                Text(
                    text = stringResource(R.string.home_title),
                    style = MaterialTheme.typography.displaySmall,
                    color = scheme.onBackground
                )
            }

            Spacer(Modifier.height(10.dp))

            EnterUp(index = 1) {
                Text(
                    text = stringResource(R.string.home_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(22.dp))

            EnterUp(index = 2) {
                PrimaryAction(
                    text = stringResource(R.string.home_open_file),
                    icon = AppIcons.FolderOpen,
                    onClick = onOpenFile,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(10.dp))

            EnterUp(index = 3) {
                SecondaryAction(
                    text = stringResource(R.string.home_paste_code),
                    icon = AppIcons.Clipboard,
                    onClick = onPaste,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(26.dp))
            EnterUp(index = 4) {
                Column {
                    Hairline()
                    SpecStrip()
                    Hairline()
                }
            }

            Spacer(Modifier.height(26.dp))

            EnterUp(index = 5) {
                SectionLabel(
                    text = stringResource(R.string.home_samples),
                    trailing = "${SampleSvgs.size}"
                )
            }
            Spacer(Modifier.height(12.dp))

            EnterUp(index = 6) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(end = 4.dp)
                ) {
                    items(SampleSvgs, key = { it.id }) { sample ->
                        SampleCard(sample = sample, onClick = { onOpenSample(sample) })
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            EnterUp(index = 7) {
                SectionLabel(
                    text = stringResource(R.string.home_recents),
                    trailing = if (recents.isEmpty()) null
                    else stringResource(R.string.home_recents_count, recents.size)
                )
            }
            Spacer(Modifier.height(6.dp))

            if (recents.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.home_recents_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Spacer(Modifier.height(2.dp))
            }

            if (recents.isEmpty()) {
                EmptyRecents()
            } else {
                Column {
                    recents.forEachIndexed { index, recent ->
                        if (index > 0) {
                            Hairline(Modifier.padding(start = 58.dp))
                        }
                        RecentRow(
                            recent = recent,
                            onClick = { onOpenRecent(recent) },
                            onTogglePin = { onTogglePin(recent) },
                            onRename = { onRename(recent) },
                            onSaveCopy = { onSaveCopy(recent) },
                            onRemove = { onRemoveRecent(recent) }
                        )
                    }
                    Hairline()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .tap(role = null) {
                                haptics.warning()
                                onClearRecents()
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.home_clear_recents),
                            style = MaterialTheme.typography.labelLarge,
                            color = scheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(26.dp))

            TipBlock()

            Spacer(Modifier.height(36.dp))
        }
    }
}

/** Dải thông số mảnh, chia bằng hairline dọc. */
@Composable
private fun SpecStrip() {
    val scheme = MaterialTheme.colorScheme
    val items = listOf(
        stringResource(R.string.home_spec_formats),
        stringResource(R.string.home_spec_sizes),
        stringResource(R.string.home_spec_alpha)
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEachIndexed { index, item ->
            if (index > 0) {
                Box(
                    Modifier
                        .height(14.dp)
                        .width(1.dp)
                        .background(scheme.outlineVariant)
                )
            }
            Text(
                text = item,
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant,
                maxLines = 1,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SampleCard(sample: SampleSvg, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val haptics = LocalHaptics.current

    Column(
        modifier = Modifier
            .width(150.dp)
            .tap(role = null) {
                haptics.click()
                onClick()
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(scheme.surfaceContainerHigh)
                .border(1.dp, scheme.outlineVariant, RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = sample.icon,
                contentDescription = null,
                tint = scheme.onSurface,
                modifier = Modifier.size(30.dp)
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(sample.titleRes),
            style = MaterialTheme.typography.titleSmall,
            color = scheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = stringResource(sample.subtitleRes),
            style = MaterialTheme.typography.labelSmall,
            color = scheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun RecentRow(
    recent: RecentFile,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
    onRename: () -> Unit,
    onSaveCopy: () -> Unit,
    onRemove: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val haptics = LocalHaptics.current
    val thumb = rememberThumb(recent.thumbPath)
    var menu by remember { mutableStateOf(false) }

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 68.dp)
                .pointerInput(recent.uri) {
                    detectTapGestures(
                        onTap = {
                            haptics.click()
                            onClick()
                        },
                        onLongPress = {
                            haptics.impact()
                            menu = true
                        }
                    )
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(scheme.surfaceContainerHigh)
                    .border(1.dp, scheme.outlineVariant, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (thumb != null) {
                    Image(
                        bitmap = thumb,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = AppIcons.FileSvg,
                        contentDescription = null,
                        tint = scheme.onSurface,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (recent.pinned) {
                        Icon(
                            imageVector = AppIcons.Pin,
                            contentDescription = null,
                            tint = scheme.onSurfaceVariant,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(5.dp))
                    }
                    Text(
                        text = recent.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = scheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = if (recent.pinned) {
                        stringResource(R.string.home_recent_pinned) + " · " +
                            relativeTime(recent.timestamp)
                    } else relativeTime(recent.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant
                )
            }
            IconAction(
                icon = AppIcons.Pin,
                contentDescription = stringResource(
                    if (recent.pinned) R.string.home_cd_unpin_recent
                    else R.string.home_cd_pin_recent,
                    recent.name
                ),
                onClick = {
                    haptics.selection()
                    onTogglePin()
                },
                tint = if (recent.pinned) scheme.onSurface
                else scheme.onSurfaceVariant.copy(alpha = 0.45f),
                size = 44.dp,
                iconSize = 18.dp
            )
            IconAction(
                icon = AppIcons.Close,
                contentDescription = stringResource(R.string.home_cd_remove_recent, recent.name),
                onClick = {
                    haptics.warning()
                    onRemove()
                },
                tint = scheme.onSurfaceVariant,
                size = 44.dp,
                iconSize = 18.dp
            )
        }

        // Nhấn giữ để mở: đổi tên, lưu bản .svg, xoá khỏi danh sách.
        DropdownMenu(
            expanded = menu,
            onDismissRequest = { menu = false },
            containerColor = scheme.surface
        ) {
            RecentMenuItem(AppIcons.Rename, stringResource(R.string.recent_rename)) {
                menu = false
                onRename()
            }
            RecentMenuItem(AppIcons.Download, stringResource(R.string.recent_save_copy)) {
                menu = false
                onSaveCopy()
            }
            RecentMenuItem(AppIcons.Trash, stringResource(R.string.recent_remove)) {
                menu = false
                haptics.warning()
                onRemove()
            }
        }
    }
}

@Composable
private fun RecentMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    DropdownMenuItem(
        text = {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurface
            )
        },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = scheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        },
        onClick = onClick
    )
}

/**
 * Ảnh thu nhỏ chụp từ khung hình SVG. Giải mã ở luồng nền để danh sách
 * không bị khựng khi cuộn qua nhiều tệp.
 */
@Composable
private fun rememberThumb(path: String?): ImageBitmap? {
    return produceState<ImageBitmap?>(initialValue = null, key1 = path) {
        value = if (path == null) {
            null
        } else {
            withContext(Dispatchers.IO) {
                runCatching { BitmapFactory.decodeFile(path)?.asImageBitmap() }.getOrNull()
            }
        }
    }.value
}

@Composable
private fun EmptyRecents() {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = AppIcons.History,
            contentDescription = null,
            tint = scheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(14.dp))
        Text(
            text = stringResource(R.string.home_recents_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TipBlock() {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(scheme.surfaceContainer)
            .padding(16.dp)
    ) {
        Icon(
            imageVector = AppIcons.Info,
            contentDescription = null,
            tint = scheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = stringResource(R.string.home_tip),
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant
        )
    }
}

@Composable
private fun relativeTime(ts: Long): String {
    val diff = System.currentTimeMillis() - ts
    return when {
        diff < 60_000L -> stringResource(R.string.time_now)
        diff < 3_600_000L -> stringResource(R.string.time_minutes, (diff / 60_000L).toInt())
        diff < 86_400_000L -> stringResource(R.string.time_hours, (diff / 3_600_000L).toInt())
        diff < 7 * 86_400_000L -> stringResource(R.string.time_days, (diff / 86_400_000L).toInt())
        else -> SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(ts))
    }
}
