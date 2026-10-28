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
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.FitScreen
import androidx.compose.material.icons.rounded.GridOn
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material.icons.rounded.ZoomOut
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
import com.khanhminh.svgviewer.ui.theme.LocalAccent
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
    val accent = LocalAccent.current
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
                    val result = snackbar.showSnackbar(
                        message = "Đã lưu PNG ${w} × ${h} vào Thư viện ảnh",
                        actionLabel = "Chia sẻ",
                        withDismissAction = true
                    )
                    if (result == SnackbarResult.ActionPerformed) shareImage(context, uri)
                }.onFailure { error ->
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

    BackHandler { onBack() }

    Box(Modifier.fillMaxSize()) {
        when (settings.background) {
            CanvasBackground.CHECKER -> Checkerboard(Modifier.fillMaxSize())
            CanvasBackground.LIGHT -> SolidCanvas(Color(0xFFFDFDFF), Modifier.fillMaxSize())
            CanvasBackground.DARK -> SolidCanvas(Color(0xFF0A0A10), Modifier.fillMaxSize())
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
                .clip(RoundedCornerShape(20.dp))
                .background(scheme.surface.copy(alpha = 0.92f))
                .border(1.dp, scheme.outlineVariant.copy(alpha = 0.7f), RoundedCornerShape(20.dp))
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = "Quay lại", tint = scheme.onSurface)
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = document.name,
                    style = MaterialTheme.typography.titleMedium,
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
            IconButton(onClick = { showInfo = true }) {
                Icon(Icons.Rounded.Info, contentDescription = "Thông tin", tint = scheme.onSurfaceVariant)
            }
            IconButton(
                onClick = { showExportSheet = true },
                modifier = Modifier
                    .padding(end = 4.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(accent.violet, accent.cyan)))
            ) {
                Icon(
                    Icons.Rounded.IosShare,
                    contentDescription = "Xuất ảnh",
                    tint = Color.White,
                    modifier = Modifier.size(19.dp)
                )
            }
        }

        // Dock dưới
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 18.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(scheme.surface.copy(alpha = 0.94f))
                .border(1.dp, scheme.outlineVariant.copy(alpha = 0.7f), RoundedCornerShape(24.dp))
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            IconButton(onClick = { js("window.SVGViewer.fit()") }) {
                Icon(Icons.Rounded.FitScreen, contentDescription = "Vừa khung", tint = scheme.onSurfaceVariant)
            }
            IconButton(onClick = { js("window.SVGViewer.zoomBy(0.8)") }) {
                Icon(Icons.Rounded.ZoomOut, contentDescription = "Thu nhỏ", tint = scheme.onSurfaceVariant)
            }
            Box(
                modifier = Modifier
                    .width(54.dp)
                    .height(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { js("window.SVGViewer.actualSize()") },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${(zoom * 100).roundToInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                    color = scheme.primary,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
            IconButton(onClick = { js("window.SVGViewer.zoomBy(1.25)") }) {
                Icon(Icons.Rounded.ZoomIn, contentDescription = "Phóng to", tint = scheme.onSurfaceVariant)
            }
            HorizontalDivider(
                modifier = Modifier
                    .height(22.dp)
                    .width(1.dp),
                color = scheme.outlineVariant
            )
            IconButton(
                onClick = {
                    paused = !paused
                    js("window.SVGViewer.setPaused($paused)")
                }
            ) {
                Icon(
                    imageVector = if (paused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                    contentDescription = if (paused) "Phát" else "Tạm dừng",
                    tint = scheme.onSurface
                )
            }
            IconButton(onClick = { js("window.SVGViewer.restart()"); paused = false }) {
                Icon(Icons.Rounded.Replay, contentDescription = "Chạy lại", tint = scheme.onSurfaceVariant)
            }
            IconButton(onClick = {
                settings.updateBackground(
                    when (settings.background) {
                        CanvasBackground.CHECKER -> CanvasBackground.LIGHT
                        CanvasBackground.LIGHT -> CanvasBackground.DARK
                        CanvasBackground.DARK -> CanvasBackground.CHECKER
                    }
                )
            }) {
                Icon(
                    imageVector = if (settings.background == CanvasBackground.CHECKER) Icons.Rounded.GridOn else Icons.Rounded.LightMode,
                    contentDescription = "Đổi nền",
                    tint = scheme.onSurfaceVariant
                )
            }
        }

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 90.dp, start = 16.dp, end = 16.dp)
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
                color = scheme.surface,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        progress = { if (exportUi.progress <= 0) 0.05f else exportUi.progress / 100f },
                        modifier = Modifier.size(46.dp),
                        color = accent.violet,
                        trackColor = scheme.surfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "Đang kết xuất PNG",
                        style = MaterialTheme.typography.titleMedium,
                        color = scheme.onSurface
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "${preset.label} · ${outputSize(document.aspect, preset.longEdge).let { "${it.first} × ${it.second}" }}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(14.dp))
                    LinearProgressIndicator(
                        progress = { if (exportUi.progress <= 0) 0.05f else exportUi.progress / 100f },
                        modifier = Modifier.fillMaxWidth(),
                        color = accent.cyan,
                        trackColor = scheme.surfaceVariant
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
    val accent = LocalAccent.current

    Column(Modifier.padding(horizontal = 22.dp)) {
        Text(
            text = "Xuất ảnh PNG",
            style = MaterialTheme.typography.titleLarge,
            color = scheme.onSurface
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Khung nhìn ${document.dimensionLabel} · giữ nguyên tỉ lệ gốc",
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurfaceVariant
        )
        Spacer(Modifier.height(18.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ExportPreset.entries.forEach { item ->
                val dims = outputSize(document.aspect, item.longEdge)
                val active = item == selected
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            if (active) accent.violet.copy(alpha = 0.16f) else scheme.surfaceVariant.copy(alpha = 0.6f)
                        )
                        .border(
                            width = if (active) 1.5.dp else 1.dp,
                            color = if (active) accent.violet else scheme.outlineVariant,
                            shape = RoundedCornerShape(18.dp)
                        )
                        .clickable { onSelect(item) }
                        .padding(vertical = 14.dp, horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (active) accent.violet else scheme.onSurface
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "${dims.first}×${dims.second}",
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(scheme.surfaceVariant.copy(alpha = 0.45f))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Nền trong suốt",
                    style = MaterialTheme.typography.titleMedium,
                    color = scheme.onSurface
                )
                Text(
                    text = "Tắt để có nền trắng",
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant
                )
            }
            Switch(checked = transparent, onCheckedChange = onTransparentChange)
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = onExport,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = accent.violet,
                contentColor = Color.White
            )
        ) {
            Text(
                text = "Xuất PNG ${selected.label}",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(10.dp))
        Text(
            text = "Ảnh được lưu vào Thư viện ảnh › Pictures/SVG Viewer",
            style = MaterialTheme.typography.labelSmall,
            color = scheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(26.dp))
    }
}

@Composable
private fun InfoDialog(document: SvgDocument, onDismiss: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Đóng") }
        },
        title = {
            Text("Thông tin tệp", style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                InfoRow("Tên", document.name)
                InfoRow("Khung nhìn", document.viewBox ?: "Không khai báo")
                InfoRow("Kích thước", document.dimensionLabel)
                InfoRow("Dung lượng", document.sizeLabel)
                InfoRow("Nguồn", when (document.origin) {
                    SvgDocument.Origin.FILE -> "Tệp trên máy"
                    SvgDocument.Origin.SAMPLE -> "Mẫu có sẵn"
                    SvgDocument.Origin.CLIPBOARD -> "Dán từ bộ nhớ tạm"
                })
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
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurfaceVariant,
            modifier = Modifier.width(96.dp)
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
