package com.khanhminh.svgviewer.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import com.khanhminh.svgviewer.data.CanvasBackground
import com.khanhminh.svgviewer.data.SettingsStore
import com.khanhminh.svgviewer.data.SvgDocument
import com.khanhminh.svgviewer.render.ExportSession
import com.khanhminh.svgviewer.render.HtmlComposer
import com.khanhminh.svgviewer.render.ImageSaver
import com.khanhminh.svgviewer.render.SvgBridge
import com.khanhminh.svgviewer.ui.haptics.LocalHaptics
import com.khanhminh.svgviewer.ui.theme.AppIcons
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

enum class ExportPreset(val label: String, val longEdge: Int, val note: String) {
    FHD("1080p", 1920, "Full HD"),
    UHD("4K", 3840, "Ultra HD"),
    UHD8("8K", 7680, "Siêu nét")
}

private class WebViewHolder {
    var webView: WebView? = null
}

private data class ExportUi(val running: Boolean = false, val progress: Int = 0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewerScreen(
    document: SvgDocument,
    settings: SettingsStore,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val bridge = remember { SvgBridge() }
    val holder = remember { WebViewHolder() }
    val haptics = LocalHaptics.current
    val scheme = MaterialTheme.colorScheme
    val html = remember(document.markup) { HtmlComposer.compose(context, document.markup) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var zoom by remember { mutableFloatStateOf(1f) }
    var paused by remember { mutableStateOf(false) }
    var showExportSheet by remember { mutableStateOf(false) }
    var showInfo by remember { mutableStateOf(false) }
    var exportUi by remember { mutableStateOf(ExportUi()) }
    var preset by remember { mutableStateOf(ExportPreset.UHD) }
    var transparent by remember { mutableStateOf(false) }
    var pendingExport by remember { mutableStateOf<Pair<ExportPreset, Boolean>?>(null) }
    var backProgress by remember { mutableFloatStateOf(0f) }
    val activeSession = remember { mutableStateOf<ExportSession?>(null) }

    fun js(code: String) {
        holder.webView?.evaluateJavascript(code, null)
    }

    fun performExport(selected: ExportPreset, withAlpha: Boolean) {
        val view = holder.webView ?: return
        activeSession.value?.discard()
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val tmp = File(dir, "svg_${System.currentTimeMillis()}.png")
        val session = ExportSession(tmp)
        activeSession.value = session

        exportUi = ExportUi(running = true, progress = 0)
        bridge.chunkSink = { chunk -> session.append(chunk) }
        bridge.onExportDone = { w, h ->
            session.close()
            bridge.chunkSink = null
            bridge.onExportDone = null
            scope.launch {
                val saved = withContext(Dispatchers.IO) {
                    runCatching { ImageSaver.savePng(context, tmp, document.name, w, h) }
                }
                tmp.delete()
                exportUi = ExportUi()
                saved.onSuccess { uri ->
                    haptics.success()
                    val result = snackbar.showSnackbar(
                        message = "Đã lưu PNG ${w} × ${h} vào Thư viện ảnh",
                        actionLabel = "Chia sẻ",
                        withDismissAction = true
                    )
                    if (result == SnackbarResult.ActionPerformed) shareImage(context, uri)
                }.onFailure { error ->
                    haptics.warning()
                    snackbar.showSnackbar("Không lưu được ảnh: ${error.message ?: "lỗi không xác định"}")
                }
            }
        }

        view.evaluateJavascript(
            "window.SVGViewer && window.SVGViewer.exportPng(${selected.longEdge}, $withAlpha);",
            null
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val queued = pendingExport
        pendingExport = null
        if (granted && queued != null) {
            performExport(queued.first, queued.second)
        } else if (!granted) {
            scope.launch { snackbar.showSnackbar("Cần quyền ghi bộ nhớ để lưu ảnh PNG") }
        }
    }

    fun requestExport(selected: ExportPreset, withAlpha: Boolean) {
        val needsPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
            PackageManager.PERMISSION_GRANTED
        if (needsPermission) {
            pendingExport = selected to withAlpha
            permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            performExport(selected, withAlpha)
        }
    }

    DisposableEffect(bridge) {
        bridge.onZoom = { value -> zoom = value }
        bridge.onProgress = { percent -> exportUi = exportUi.copy(progress = percent) }
        bridge.onError = { message ->
            exportUi = ExportUi()
            scope.launch { snackbar.showSnackbar(message) }
        }
        onDispose {
            bridge.onZoom = null
            bridge.onProgress = null
            bridge.onError = null
            bridge.chunkSink = null
            bridge.onExportDone = null
        }
    }

    PredictiveBack(enabled = true, onBack = onBack, onProgress = { backProgress = it })

    Box(
        Modifier
            .fillMaxSize()
            .predictiveBackVisual(backProgress)
    ) {
        when (settings.background) {
            CanvasBackground.CHECKER -> Checkerboard(Modifier.fillMaxSize())
            CanvasBackground.LIGHT -> Box(Modifier.fillMaxSize().background(Color.White))
            CanvasBackground.DARK -> Box(Modifier.fillMaxSize().background(Color(0xFF0B0B0B)))
        }

        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val web = WebView(ctx)
                web.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                with(web.settings) {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    allowFileAccess = false
                    allowContentAccess = false
                    setSupportZoom(false)
                    builtInZoomControls = false
                    displayZoomControls = false
                    mediaPlaybackRequiresUserGesture = false
                }
                web.isVerticalScrollBarEnabled = false
                web.isHorizontalScrollBarEnabled = false
                web.overScrollMode = View.OVER_SCROLL_NEVER
                web.addJavascriptInterface(bridge, "Android")
                web.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String?) {
                        super.onPageFinished(view, url)
                        view.evaluateJavascript("window.SVGViewer && window.SVGViewer.fit();", null)
                    }
                }
                web.loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
                holder.webView = web
                web
            },
            onRelease = { view ->
                holder.webView = null
                view.removeJavascriptInterface("Android")
                view.destroy()
            }
        )

        // Thanh trên
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(scheme.surface.copy(alpha = 0.94f))
                .border(1.dp, scheme.outlineVariant, RoundedCornerShape(22.dp))
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconAction(
                icon = AppIcons.Back,
                contentDescription = "Quay lại",
                onClick = onBack
            )
            Column(Modifier.weight(1f)) {
                Text(
                    text = document.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = scheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${document.dimensionLabel} · ${document.sizeLabel}",
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconAction(
                icon = if (settings.background == CanvasBackground.CHECKER) AppIcons.Checker else AppIcons.Grid,
                contentDescription = "Đổi nền canvas",
                onClick = {
                    haptics.selection()
                    settings.updateBackground(
                        when (settings.background) {
                            CanvasBackground.CHECKER -> CanvasBackground.LIGHT
                            CanvasBackground.LIGHT -> CanvasBackground.DARK
                            CanvasBackground.DARK -> CanvasBackground.CHECKER
                        }
                    )
                },
                tint = scheme.onSurfaceVariant
            )
            IconAction(
                icon = AppIcons.Info,
                contentDescription = "Thông tin tệp",
                onClick = { showInfo = true },
                tint = scheme.onSurfaceVariant
            )
            IconAction(
                icon = AppIcons.Export,
                contentDescription = "Xuất ảnh PNG",
                onClick = {
                    haptics.impact()
                    showExportSheet = true
                },
                filled = true
            )
        }

        // Dock dưới
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 18.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(scheme.surface.copy(alpha = 0.95f))
                .border(1.dp, scheme.outlineVariant, RoundedCornerShape(26.dp))
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            IconAction(
                icon = AppIcons.Fit,
                contentDescription = "Vừa khung",
                onClick = { js("window.SVGViewer.fit()") },
                tint = scheme.onSurfaceVariant
            )
            IconAction(
                icon = AppIcons.ZoomOut,
                contentDescription = "Thu nhỏ",
                onClick = { js("window.SVGViewer.zoomBy(0.8)") },
                tint = scheme.onSurfaceVariant
            )
            Box(
                modifier = Modifier
                    .width(56.dp)
                    .height(48.dp)
                    .tap(role = null) {
                        haptics.tick()
                        js("window.SVGViewer.actualSize()")
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${(zoom * 100).roundToInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.onSurface,
                    textAlign = TextAlign.Center
                )
            }
            IconAction(
                icon = AppIcons.ZoomIn,
                contentDescription = "Phóng to",
                onClick = { js("window.SVGViewer.zoomBy(1.25)") },
                tint = scheme.onSurfaceVariant
            )
            Box(
                Modifier
                    .height(22.dp)
                    .width(1.dp)
                    .background(scheme.outlineVariant)
            )
            IconAction(
                icon = if (paused) AppIcons.Play else AppIcons.Pause,
                contentDescription = if (paused) "Phát chuyển động" else "Tạm dừng chuyển động",
                onClick = {
                    paused = !paused
                    js("window.SVGViewer.setPaused($paused)")
                }
            )
            IconAction(
                icon = AppIcons.Replay,
                contentDescription = "Chạy lại từ đầu",
                onClick = {
                    js("window.SVGViewer.restart()")
                    paused = false
                },
                tint = scheme.onSurfaceVariant
            )
        }

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 92.dp, start = 16.dp, end = 16.dp)
        )
    }

    if (showExportSheet) {
        ModalBottomSheet(
            onDismissRequest = { showExportSheet = false },
            sheetState = sheetState,
            containerColor = scheme.surface
        ) {
            ExportSheet(
                document = document,
                selected = preset,
                transparent = transparent,
                onSelect = { preset = it },
                onTransparentChange = { transparent = it },
                onExport = {
                    showExportSheet = false
                    requestExport(preset, transparent)
                }
            )
        }
    }

    if (showInfo) {
        InfoDialog(document = document, onDismiss = { showInfo = false })
    }

    if (exportUi.running) {
        Dialog(onDismissRequest = { }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = scheme.surface
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        progress = { if (exportUi.progress <= 0) 0.05f else exportUi.progress / 100f },
                        modifier = Modifier.size(44.dp),
                        color = scheme.onSurface,
                        trackColor = scheme.surfaceContainerHighest,
                        strokeWidth = 3.dp
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "Đang kết xuất PNG",
                        style = MaterialTheme.typography.titleMedium,
                        color = scheme.onSurface
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${preset.label} · " + outputSize(document.aspect, preset.longEdge)
                            .let { "${it.first} × ${it.second}" },
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    LinearProgressIndicator(
                        progress = { if (exportUi.progress <= 0) 0.05f else exportUi.progress / 100f },
                        modifier = Modifier.fillMaxWidth(),
                        color = scheme.onSurface,
                        trackColor = scheme.surfaceContainerHighest
                    )
                }
            }
        }
    }
}

@Composable
private fun ExportSheet(
    document: SvgDocument,
    selected: ExportPreset,
    transparent: Boolean,
    onSelect: (ExportPreset) -> Unit,
    onTransparentChange: (Boolean) -> Unit,
    onExport: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val haptics = LocalHaptics.current

    Column(Modifier.padding(horizontal = 20.dp)) {
        Text(
            text = "Xuất ảnh PNG",
            style = MaterialTheme.typography.titleLarge,
            color = scheme.onSurface
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Khung nhìn ${document.dimensionLabel} · giữ nguyên tỉ lệ gốc",
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant
        )
        Spacer(Modifier.height(18.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ExportPreset.entries.forEach { item ->
                val dims = outputSize(document.aspect, item.longEdge)
                val active = item == selected
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (active) scheme.onBackground else Color.Transparent)
                        .border(
                            width = 1.dp,
                            color = if (active) Color.Transparent else scheme.outline,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .tap(role = null) {
                            haptics.selection()
                            onSelect(item)
                        }
                        .padding(vertical = 14.dp, horizontal = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (active) scheme.background else scheme.onSurface
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "${dims.first}×${dims.second}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (active) scheme.background.copy(alpha = 0.7f) else scheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Hairline()
        SettingRow(
            title = "Nền trong suốt",
            subtitle = "Tắt để xuất nền trắng"
        ) {
            Switch(
                checked = transparent,
                onCheckedChange = {
                    haptics.selection()
                    onTransparentChange(it)
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = scheme.background,
                    checkedTrackColor = scheme.onBackground,
                    checkedBorderColor = Color.Transparent
                )
            )
        }
        Hairline()

        Spacer(Modifier.height(18.dp))

        PrimaryAction(
            text = "Xuất PNG ${selected.label}",
            icon = AppIcons.Export,
            onClick = onExport,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(10.dp))
        Text(
            text = "Ảnh lưu vào Thư viện ảnh › Pictures/SVG Viewer",
            style = MaterialTheme.typography.labelSmall,
            color = scheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun InfoDialog(document: SvgDocument, onDismiss: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Đóng") }
        },
        title = { Text("Thông tin tệp", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                InfoRow("Tên", document.name)
                InfoRow("Khung nhìn", document.viewBox ?: "Không khai báo")
                InfoRow("Kích thước", document.dimensionLabel)
                InfoRow("Dung lượng", document.sizeLabel)
                InfoRow(
                    "Nguồn",
                    when (document.origin) {
                        SvgDocument.Origin.FILE -> "Tệp trên máy"
                        SvgDocument.Origin.SAMPLE -> "Mẫu có sẵn"
                        SvgDocument.Origin.CLIPBOARD -> "Dán từ bộ nhớ tạm"
                    }
                )
                InfoRow("Phần tử", "${Regex("<[a-zA-Z]").findAll(document.markup).count()} thẻ")
            }
        },
        containerColor = scheme.surface
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    val scheme = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant,
            modifier = Modifier.width(92.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}

private fun outputSize(aspect: Float?, longEdge: Int): Pair<Int, Int> {
    val ratio = if (aspect == null || aspect <= 0f) 1f else aspect
    return if (ratio >= 1f) {
        longEdge to (longEdge / ratio).roundToInt().coerceAtLeast(1)
    } else {
        (longEdge * ratio).roundToInt().coerceAtLeast(1) to longEdge
    }
}

private fun shareImage(context: Context, uri: Uri) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    runCatching {
        context.startActivity(Intent.createChooser(intent, "Chia sẻ ảnh PNG"))
    }
}
