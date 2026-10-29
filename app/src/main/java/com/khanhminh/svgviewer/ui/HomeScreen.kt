package com.khanhminh.svgviewer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.khanhminh.svgviewer.data.RecentFile
import com.khanhminh.svgviewer.data.ThemeMode
import com.khanhminh.svgviewer.ui.haptics.LocalHaptics
import com.khanhminh.svgviewer.ui.theme.AppIcons
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
    onRemoveRecent: (RecentFile) -> Unit,
    onClearRecents: () -> Unit,
    onToggleTheme: () -> Unit,
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
                    text = "SVG VIEWER",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.8.sp),
                    color = scheme.onBackground,
                    modifier = Modifier.weight(1f)
                )
                IconAction(
                    icon = if (themeMode == ThemeMode.DARK) AppIcons.Sun else AppIcons.Moon,
                    contentDescription = if (themeMode == ThemeMode.DARK)
                        "Chuyển sang giao diện sáng" else "Chuyển sang giao diện tối",
                    onClick = onToggleTheme,
                    tint = scheme.onSurfaceVariant
                )
                IconAction(
                    icon = AppIcons.Gear,
                    contentDescription = "Cài đặt",
                    onClick = onOpenSettings,
                    tint = scheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(26.dp))

            Text(
                text = "Vector sống động.\nẢnh xuất tới 8K.",
                style = MaterialTheme.typography.displaySmall,
                color = scheme.onBackground
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = "Xem SMIL và CSS animation, zoom mượt, xuất PNG nền trong suốt.",
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant
            )

            Spacer(Modifier.height(22.dp))

            PrimaryAction(
                text = "Mở tệp SVG",
                icon = AppIcons.FolderOpen,
                onClick = onOpenFile,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(10.dp))

            SecondaryAction(
                text = "Dán mã SVG",
                icon = AppIcons.Clipboard,
                onClick = onPaste,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(26.dp))
            Hairline()
            SpecStrip()
            Hairline()

            Spacer(Modifier.height(26.dp))

            SectionLabel(
                text = "Mẫu có chuyển động",
                trailing = "${SampleSvgs.size} mẫu"
            )
            Spacer(Modifier.height(12.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(end = 4.dp)
            ) {
                items(SampleSvgs, key = { it.id }) { sample ->
                    SampleCard(sample = sample, onClick = { onOpenSample(sample) })
                }
            }

            Spacer(Modifier.height(28.dp))

            SectionLabel(
                text = "Gần đây",
                trailing = if (recents.isEmpty()) null else "${recents.size} tệp"
            )
            Spacer(Modifier.height(6.dp))

            if (recents.isEmpty()) {
                EmptyRecents()
            } else {
                Column {
                    recents.forEachIndexed { index, recent ->
                        if (index > 0) {
                            Hairline(Modifier.padding(start = 56.dp))
                        }
                        RecentRow(
                            recent = recent,
                            onClick = { onOpenRecent(recent) },
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
                            text = "Xoá danh sách gần đây",
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
    val items = listOf("SMIL · CSS", "1080p · 4K · 8K", "PNG alpha")
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
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
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
            text = sample.title,
            style = MaterialTheme.typography.titleSmall,
            color = scheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = sample.subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = scheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun RecentRow(recent: RecentFile, onClick: () -> Unit, onRemove: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val haptics = LocalHaptics.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .tap(role = null) {
                haptics.click()
                onClick()
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(scheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = AppIcons.FileSvg,
                contentDescription = null,
                tint = scheme.onSurface,
                modifier = Modifier.size(19.dp)
            )
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = recent.name,
                style = MaterialTheme.typography.titleSmall,
                color = scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = relativeTime(recent.timestamp),
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant
            )
        }
        IconAction(
            icon = AppIcons.Close,
            contentDescription = "Xoá ${recent.name} khỏi danh sách",
            onClick = {
                haptics.warning()
                onRemove()
            },
            tint = scheme.onSurfaceVariant
        )
    }
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
            text = "Tệp bạn mở sẽ được ghim ở đây để mở lại nhanh.",
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
            text = "Chạm 2 lần để phóng to, kéo 2 ngón để zoom, giữ 1 ngón để di chuyển. " +
                "Chia sẻ tệp SVG từ app khác và chọn SVG Viewer để mở ngay tại đây.",
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant
        )
    }
}

private fun relativeTime(ts: Long): String {
    val diff = System.currentTimeMillis() - ts
    return when {
        diff < 60_000L -> "Vừa xong"
        diff < 3_600_000L -> "${diff / 60_000L} phút trước"
        diff < 86_400_000L -> "${diff / 3_600_000L} giờ trước"
        diff < 7 * 86_400_000L -> "${diff / 86_400_000L} ngày trước"
        else -> SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(ts))
    }
}
