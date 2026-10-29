package com.khanhminh.svgviewer.ui

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.khanhminh.svgviewer.data.CanvasBackground
import com.khanhminh.svgviewer.data.IncomingSvg
import com.khanhminh.svgviewer.data.RecentStore
import com.khanhminh.svgviewer.data.SettingsStore
import com.khanhminh.svgviewer.data.SvgDocument
import com.khanhminh.svgviewer.data.SvgLoader
import com.khanhminh.svgviewer.data.ThemeMode
import com.khanhminh.svgviewer.ui.haptics.AndroidHaptics
import com.khanhminh.svgviewer.ui.haptics.LocalHaptics
import com.khanhminh.svgviewer.ui.theme.AppIcons
import com.khanhminh.svgviewer.ui.theme.SvgViewerTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun SvgViewerApp(incoming: IncomingSvg? = null) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val store = remember { SettingsStore(context) }
    val recentStore = remember { RecentStore(context) }
    val snackbar = remember { SnackbarHostState() }

    val haptics = remember(view) {
        AndroidHaptics(context, view) { store.hapticsEnabled }
    }

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

    fun openIncoming(payload: IncomingSvg) {
        val uri = payload.uri
        val text = payload.text
        when {
            uri != null -> openUri(uri)
            text != null -> {
                val doc = SvgLoader.fromText(text, "chia-se.svg")
                if (doc != null) {
                    document = doc
                } else {
                    errorMessage = "Nội dung được chia sẻ không chứa mã SVG"
                }
            }
        }
    }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) openUri(uri)
    }

    LaunchedEffect(incoming?.token) {
        if (incoming != null) openIncoming(incoming)
    }

    SvgViewerTheme(darkTheme = darkTheme) {
        CompositionLocalProvider(LocalHaptics provides haptics) {
            Surface(
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier.fillMaxSize()
            ) {
                Box(Modifier.fillMaxSize()) {
                    AnimatedContent(
                        targetState = document,
                        transitionSpec = {
                            if (targetState != null) screenTransition(1) else screenTransition(-1)
                        },
                        label = "home-viewer"
                    ) { current ->
                        if (current == null) {
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
                                            "application/octet-stream",
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
                                document = current,
                                settings = store,
                                onBack = { document = null }
                            )
                        }
                    }

                    if (loading) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.4f)),
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
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(30.dp),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                        strokeWidth = 2.5.dp
                                    )
                                    Spacer(Modifier.height(14.dp))
                                    Text(
                                        text = "Đang nạp SVG…",
                                        style = MaterialTheme.typography.bodySmall,
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
                AlertDialog(
                    onDismissRequest = { errorMessage = null },
                    confirmButton = {
                        TextButton(onClick = { errorMessage = null }) { Text("Đã hiểu") }
                    },
                    title = { Text("Không mở được tệp") },
                    text = { Text(message) },
                    containerColor = MaterialTheme.colorScheme.surface
                )
            }
        }
    }
}

@Composable
private fun SettingsSheet(store: SettingsStore, onClose: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val haptics = LocalHaptics.current

    Column(
        Modifier
            .padding(horizontal = 20.dp)
            .navigationBarsPadding()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Cài đặt",
                style = MaterialTheme.typography.titleLarge,
                color = scheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            IconAction(
                icon = AppIcons.Close,
                contentDescription = "Đóng cài đặt",
                onClick = onClose,
                tint = scheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(20.dp))
        SectionLabel("Giao diện")
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeMode.entries.forEach { mode ->
                ChoicePill(
                    text = mode.label,
                    selected = store.themeMode == mode,
                    onClick = { store.updateThemeMode(mode) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(Modifier.height(22.dp))
        SectionLabel("Nền canvas mặc định")
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CanvasBackground.entries.forEach { bg ->
                ChoicePill(
                    text = bg.label,
                    selected = store.background == bg,
                    onClick = { store.updateBackground(bg) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(Modifier.height(22.dp))
        Hairline()
        SettingRow(
            title = "Phản hồi rung",
            subtitle = "Rung nhẹ ở mỗi thao tác"
        ) {
            Switch(
                checked = store.hapticsEnabled,
                onCheckedChange = {
                    haptics.selection()
                    store.updateHaptics(it)
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = scheme.background,
                    checkedTrackColor = scheme.onBackground,
                    checkedBorderColor = Color.Transparent
                )
            )
        }
        Hairline()

        Spacer(Modifier.height(24.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            AppLogo(size = 24.dp)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = "SVG Viewer",
                    style = MaterialTheme.typography.titleSmall,
                    color = scheme.onSurface
                )
                Text(
                    text = "Phiên bản 1.1.0",
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = "Giao diện đen trắng, font Urbanist, icon Phosphor.",
            style = MaterialTheme.typography.labelSmall,
            color = scheme.onSurfaceVariant
        )

        Spacer(Modifier.height(28.dp))
    }
}
