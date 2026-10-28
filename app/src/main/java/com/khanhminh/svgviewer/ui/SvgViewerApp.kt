package com.khanhminh.svgviewer.ui

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.khanhminh.svgviewer.data.CanvasBackground
import com.khanhminh.svgviewer.data.RecentStore
import com.khanhminh.svgviewer.data.SettingsStore
import com.khanhminh.svgviewer.data.SvgDocument
import com.khanhminh.svgviewer.data.SvgLoader
import com.khanhminh.svgviewer.data.ThemeMode
import com.khanhminh.svgviewer.ui.theme.LocalAccent
import com.khanhminh.svgviewer.ui.theme.SvgViewerTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SvgViewerApp(initialUri: Uri? = null) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember { SettingsStore(context) }
    val recentStore = remember { RecentStore(context) }
    val snackbar = remember { SnackbarHostState() }

    var recents by remember { mutableStateOf(recentStore.list()) }
    var document by remember { mutableStateOf<SvgDocument?>(null) }
    var loading by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val darkTheme = when (store.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    fun openUri(uri: Uri) {
        scope.launch {
            loading = true
            val result = withContext(Dispatchers.IO) {
                runCatching { SvgLoader.fromUri(context, uri) }
            }
            loading = false
            result.onSuccess { doc ->
                runCatching {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
                recentStore.add(uri, doc.name)
                recents = recentStore.list()
                document = doc
            }.onFailure { error ->
                errorMessage = error.message ?: "Không đọc được tệp SVG"
            }
        }
    }

    fun openSample(sample: SampleSvg) {
        scope.launch {
            loading = true
            val doc = withContext(Dispatchers.IO) {
                runCatching {
                    SvgLoader.fromAsset(context, sample.asset, "${sample.title}.svg")
                }.getOrNull()
            }
            loading = false
            if (doc != null) document = doc
            else errorMessage = "Không nạp được mẫu ${sample.title}"
        }
    }

    fun pasteFromClipboard() {
        val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val text = manager?.primaryClip
            ?.takeIf { it.itemCount > 0 }
            ?.getItemAt(0)
            ?.coerceToText(context)
            ?.toString()
        val doc = text?.let { SvgLoader.fromText(it, "clipboard.svg") }
        if (doc != null) {
            document = doc
        } else {
            scope.launch { snackbar.showSnackbar("Bộ nhớ tạm không chứa mã SVG hợp lệ") }
        }
    }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) openUri(uri)
    }

    LaunchedEffect(initialUri) {
        if (initialUri != null) openUri(initialUri)
    }

    SvgViewerTheme(darkTheme = darkTheme, dynamicColor = store.dynamicColor) {
        Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize()) {
                val doc = document
                if (doc == null) {
                    HomeScreen(
                        recents = recents,
                        themeMode = store.themeMode,
                        onOpenFile = {
                            picker.launch(
                                arrayOf(
                                    "image/svg+xml",
                                    "image/svg",
                                    "text/xml",
                                    "application/xml",
                                    "text/plain",
                                    "*/*"
                                )
                            )
                        },
                        onPaste = { pasteFromClipboard() },
                        onOpenSample = { openSample(it) },
                        onOpenRecent = { openUri(it.uri) },
                        onRemoveRecent = {
                            recentStore.remove(it.uri)
                            recents = recentStore.list()
                        },
                        onClearRecents = {
                            recentStore.clear()
                            recents = emptyList()
                        },
                        onToggleTheme = {
                            store.updateThemeMode(if (darkTheme) ThemeMode.LIGHT else ThemeMode.DARK)
                        },
                        onOpenSettings = { showSettings = true }
                    )
                } else {
                    ViewerScreen(
                        document = doc,
                        settings = store,
                        onBack = { document = null }
                    )
                }

                if (loading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.35f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Column(
                                modifier = Modifier.padding(26.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(color = LocalAccent.current.violet)
                                Spacer(Modifier.height(14.dp))
                                Text(
                                    text = "Đang nạp SVG…",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                SnackbarHost(
                    hostState = snackbar,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(16.dp)
                )
            }
        }

        if (showSettings) {
            ModalBottomSheet(
                onDismissRequest = { showSettings = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                SettingsSheet(store = store, onClose = { showSettings = false })
            }
        }

        errorMessage?.let { message ->
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { errorMessage = null },
                confirmButton = {
                    TextButton(onClick = { errorMessage = null }) { Text("Đã hiểu") }
                },
                title = { Text("Không mở được tệp") },
                text = { Text(message) }
            )
        }
    }
}

@Composable
private fun SettingsSheet(store: SettingsStore, onClose: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val accent = LocalAccent.current

    Column(
        Modifier
            .padding(horizontal = 22.dp)
            .navigationBarsPadding()
    ) {
        Text("Cài đặt", style = MaterialTheme.typography.titleLarge, color = scheme.onSurface)
        Spacer(Modifier.height(18.dp))

        Text(
            "Giao diện",
            style = MaterialTheme.typography.labelSmall,
            color = scheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeMode.entries.forEach { mode ->
                val active = store.themeMode == mode
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (active) accent.violet.copy(alpha = 0.16f)
                            else scheme.surfaceVariant.copy(alpha = 0.55f)
                        )
                        .border(
                            width = if (active) 1.5.dp else 1.dp,
                            color = if (active) accent.violet else scheme.outlineVariant,
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clickable { store.updateThemeMode(mode) }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = mode.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (active) accent.violet else scheme.onSurface
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        SettingSwitchRow(
            title = "Màu theo hệ thống",
            subtitle = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                "Dùng bảng màu Material You của thiết bị"
            else "Chỉ hỗ trợ từ Android 12",
            checked = store.dynamicColor,
            enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
            onCheckedChange = { store.updateDynamicColor(it) }
        )

        Spacer(Modifier.height(20.dp))

        Text(
            "Nền canvas mặc định",
            style = MaterialTheme.typography.labelSmall,
            color = scheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CanvasBackground.entries.forEach { bg ->
                val active = store.background == bg
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (active) accent.cyan.copy(alpha = 0.18f)
                            else scheme.surfaceVariant.copy(alpha = 0.55f)
                        )
                        .border(
                            width = if (active) 1.5.dp else 1.dp,
                            color = if (active) accent.cyan else scheme.outlineVariant,
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clickable { store.updateBackground(bg) }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = bg.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (active) accent.cyan else scheme.onSurface
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            AppLogo(size = 26.dp)
            Spacer(Modifier.size(10.dp))
            Column {
                Text(
                    "SVG Viewer",
                    style = MaterialTheme.typography.titleMedium,
                    color = scheme.onSurface
                )
                Text(
                    "Phiên bản 1.0.0 · Jetpack Compose",
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(18.dp))
        TextButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
            Text("Đóng", fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(scheme.surfaceVariant.copy(alpha = 0.45f))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = scheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}
