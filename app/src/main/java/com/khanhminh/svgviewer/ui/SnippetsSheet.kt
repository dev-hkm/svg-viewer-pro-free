package com.khanhminh.svgviewer.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.khanhminh.svgviewer.R
import com.khanhminh.svgviewer.data.SvgSnippets
import com.khanhminh.svgviewer.ui.theme.AppIcons
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Chép hình sang đúng dạng mà nơi nhận cần: data URI, CSS, thẻ `<img>` hoặc
 * component React. Mỗi dạng chỉ dựng khi được chọn, nên tệp lớn không tốn
 * bốn bản sao trong bộ nhớ cùng lúc.
 */
@Composable
fun SnippetsSheet(markup: String, onCopy: (String) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val dark = scheme.background.luminance() < 0.5f
    var kind by remember(markup) { mutableStateOf(SvgSnippets.Kind.DATA_URI) }
    val cache = remember(markup) { mutableStateMapOf<SvgSnippets.Kind, String>() }

    val snippet by produceState<String?>(
        initialValue = null,
        markup,
        kind
    ) {
        val cached = cache[kind]
        if (cached != null) {
            value = cached
            return@produceState
        }
        value = null
        value = withContext(Dispatchers.Default) {
            runCatching { SvgSnippets.build(markup, kind) }.getOrDefault("")
        }.also { built -> cache[kind] = built }
    }

    Column(
        Modifier
            .padding(horizontal = 20.dp)
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = stringResource(R.string.snippets_title),
            style = MaterialTheme.typography.titleLarge,
            color = scheme.onSurface
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.snippets_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant
        )

        Spacer(Modifier.height(18.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SvgSnippets.Kind.entries.forEach { item ->
                ChoicePill(
                    text = stringResource(kindLabel(item)),
                    selected = item == kind,
                    onClick = { kind = item }
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(kindDescription(kind)),
            style = MaterialTheme.typography.labelSmall,
            color = scheme.onSurfaceVariant
        )

        Spacer(Modifier.height(10.dp))

        val current = snippet
        if (current == null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 96.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = scheme.onSurface,
                    trackColor = scheme.surfaceContainerHighest,
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.snippets_building),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant
                )
            }
        } else {
            CodePreview(
                text = previewOf(current),
                dark = dark,
                maxLines = if (kind == SvgSnippets.Kind.REACT) 12 else 7
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.snippets_chars, current.length),
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(16.dp))
        PrimaryAction(
            text = stringResource(R.string.snippets_copy),
            icon = AppIcons.Copy,
            enabled = !current.isNullOrBlank(),
            onClick = { current?.let(onCopy) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.snippets_note),
            style = MaterialTheme.typography.labelSmall,
            color = scheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
    }
}

/** Bản xem trước ngắn: data URI dài cả chục nghìn ký tự nên chỉ giữ phần đầu. */
private fun previewOf(snippet: String): String =
    if (snippet.length <= 620) snippet else snippet.take(620) + "\n…"

private fun kindLabel(kind: SvgSnippets.Kind): Int = when (kind) {
    SvgSnippets.Kind.DATA_URI -> R.string.snippets_data_uri
    SvgSnippets.Kind.CSS -> R.string.snippets_css
    SvgSnippets.Kind.IMG -> R.string.snippets_img
    SvgSnippets.Kind.REACT -> R.string.snippets_react
}

private fun kindDescription(kind: SvgSnippets.Kind): Int = when (kind) {
    SvgSnippets.Kind.DATA_URI -> R.string.snippets_data_uri_desc
    SvgSnippets.Kind.CSS -> R.string.snippets_css_desc
    SvgSnippets.Kind.IMG -> R.string.snippets_img_desc
    SvgSnippets.Kind.REACT -> R.string.snippets_react_desc
}
