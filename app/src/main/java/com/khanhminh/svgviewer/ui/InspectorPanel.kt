package com.khanhminh.svgviewer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.khanhminh.svgviewer.R
import com.khanhminh.svgviewer.ui.theme.AppIcons
import org.json.JSONObject

/** Một phần tử trong bản vẽ, do viewer.html mô tả lại bằng JSON. */
data class InspectedElement(
    val tag: String,
    val id: String,
    val cls: String,
    val width: Float,
    val height: Float,
    val fill: String,
    val stroke: String,
    val opacity: String,
    val strokeWidth: Float,
    val hidden: Boolean,
    val children: Int,
    val path: List<Int>
) {
    val isRoot: Boolean get() = path.isEmpty()

    /** `<g#icon.hero>` — đúng cách người viết SVG đọc lại phần tử của mình. */
    val label: String
        get() = buildString {
            append('<').append(tag)
            if (id.isNotBlank()) append('#').append(id)
            cls.split(' ').filter { it.isNotBlank() }.forEach { append('.').append(it) }
            append('>')
        }

    val sizeLabel: String
        get() = "${trim(width)} × ${trim(height)}"

    private fun trim(value: Float): String =
        if (value == value.toInt().toFloat()) value.toInt().toString()
        else String.format(java.util.Locale.US, "%.1f", value)
}

fun parseInspect(json: String): InspectedElement? = runCatching {
    val obj = JSONObject(json)
    val array = obj.optJSONArray("path")
    val path = buildList {
        if (array != null) for (i in 0 until array.length()) add(array.getInt(i))
    }
    InspectedElement(
        tag = obj.optString("tag"),
        id = obj.optString("id"),
        cls = obj.optString("cls"),
        width = obj.optDouble("w", 0.0).toFloat(),
        height = obj.optDouble("h", 0.0).toFloat(),
        fill = obj.optString("fill"),
        stroke = obj.optString("stroke"),
        opacity = obj.optString("opacity"),
        strokeWidth = obj.optDouble("strokeWidth", 0.0).toFloat(),
        hidden = obj.optBoolean("hidden"),
        children = obj.optInt("children"),
        path = path
    )
}.getOrNull()

/** Gợi ý ngắn khi đang soi nhưng chưa chạm vào phần tử nào. */
@Composable
fun InspectorHint(modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(scheme.surface.copy(alpha = 0.94f))
            .border(1.dp, scheme.outlineVariant, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = AppIcons.Inspect,
            contentDescription = null,
            tint = scheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.inspector_hint),
            style = MaterialTheme.typography.labelMedium,
            color = scheme.onSurfaceVariant
        )
    }
}

/**
 * Bảng chi tiết phần tử đang chọn. Không phải modal: khung vẽ phía trên vẫn
 * chạm được để chọn phần tử khác, đúng cảm giác của công cụ soi.
 */
@Composable
fun InspectorPanel(
    element: InspectedElement,
    onClose: () -> Unit,
    onParent: () -> Unit,
    onToggleHidden: () -> Unit,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme

    Column(
        modifier = modifier
            .widthIn(max = 560.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(scheme.surface.copy(alpha = 0.97f))
            .border(1.dp, scheme.outlineVariant, RoundedCornerShape(22.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(scheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = AppIcons.Bounding,
                    contentDescription = null,
                    tint = scheme.onSurface,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = element.label,
                    style = MaterialTheme.typography.titleSmall.copy(fontFamily = FontFamily.Monospace),
                    color = scheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (element.isRoot) stringResource(R.string.inspector_root)
                    else stringResource(R.string.inspector_size, element.sizeLabel),
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            IconAction(
                icon = AppIcons.Close,
                contentDescription = stringResource(R.string.inspector_close),
                onClick = onClose,
                tint = scheme.onSurfaceVariant,
                size = 40.dp,
                iconSize = 18.dp
            )
        }

        Spacer(Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ColorChip(
                label = stringResource(R.string.inspector_fill),
                hex = element.fill,
                modifier = Modifier.weight(1f)
            )
            ColorChip(
                label = stringResource(R.string.inspector_stroke),
                hex = element.stroke,
                modifier = Modifier.weight(1f)
            )
            CountChip(
                label = stringResource(R.string.inspector_children),
                value = element.children.toString(),
                modifier = Modifier.weight(0.72f)
            )
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GhostAction(
                text = stringResource(R.string.inspector_parent),
                icon = AppIcons.Parent,
                onClick = onParent,
                enabled = !element.isRoot
            )
            GhostAction(
                text = stringResource(
                    if (element.hidden) R.string.inspector_show else R.string.inspector_hide
                ),
                icon = if (element.hidden) AppIcons.Eye else AppIcons.EyeSlash,
                onClick = onToggleHidden,
                enabled = !element.isRoot
            )
            GhostAction(
                text = stringResource(R.string.inspector_copy),
                icon = AppIcons.Copy,
                onClick = onCopy
            )
        }
    }
}

@Composable
private fun ColorChip(label: String, hex: String, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, scheme.outlineVariant, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (hex.isBlank()) {
            Box(
                Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .border(1.dp, scheme.outline, CircleShape)
            )
        } else {
            Box(
                Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(colorOf(hex))
                    .border(1.dp, scheme.outlineVariant, CircleShape)
            )
        }
        Spacer(Modifier.width(7.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant,
                maxLines = 1
            )
            Text(
                text = hex.ifBlank { stringResource(R.string.inspector_none) }.uppercase(),
                style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
                color = scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CountChip(label: String, value: String, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, scheme.outlineVariant, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = scheme.onSurfaceVariant,
            maxLines = 1
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            color = scheme.onSurface,
            maxLines = 1
        )
    }
}
