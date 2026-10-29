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
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import com.khanhminh.svgviewer.R
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

private class WebViewHolder {
    var webView: WebView? = null
}

private data class ExportUi(val running: Boolean = false, val progress: Int = 0)

private const val MIN_SIDE = 16
private const val MAX_SIDE = 16384

/** Trần số điểm ảnh để canvas không vượt bộ nhớ cho phép (khoảng 8K 16:9 vẫn lọt). */
private const val MAX_PIXELS = 40_000_000L

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
    val canvasSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val exportSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val aspect = document.aspect?.takeIf { it > 0f } ?: 1f

    var zoom by remember { mutableFloatStateOf(1f) }
    var paused by remember { mutableStateOf(false) }
    var showExportSheet by remember { mutableStateOf(false) }
    var showInfo by remember { mutableStateOf(false) }
    var showCanvasSheet by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    var exportUi by remember { mutableStateOf(ExportUi()) }
    var backProgress by remember { mutableFloatStateOf(0f) }
    val activeSession = remember { mutableStateOf<ExportSession?>(null) }

    val defaultSize = remember(aspect) { sizeForLongEdge(aspect, 3840) }
    var exportW by remember(document.markup) { mutableIntStateOf(defaultSize.first) }
    var exportH by remember(document.markup) { mutableIntStateOf(defaultSize.second) }
    var exportBg by remember { mutableStateOf<Color?>(null) }
    var lockAspect by remember { mutableStateOf(true) }

    fun js(code: String) {
        holder.webView?.evaluateJavascript(code, null)
    }

    fun performExport(width: Int, height: Int, background: Color?) {
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
            bridge.onExportError = null
            scope.launch {
                val saved = withContext(Dispatchers.IO) {
                    runCatching { ImageSaver.savePng(context, tmp, document.name, w, h) }
                }
                tmp.delete()
                exportUi = ExportUi()
                saved.onSuccess { uri ->
                    haptics.success()
                    val result = snackbar.showSnackbar(
                        message = context.getString(R.string.export_saved, w, h),
                        actionLabel = context.getString(R.string.export_share),
                        withDismissAction = true
                    )
                    if (result == SnackbarResult.ActionPerformed) shareImage(context, uri)
                }.onFailure { error ->
                    haptics.warning()
                    snackbar.showSnackbar(
                        context.getString(
                            R.string.export_failed,
                            error.message ?: context.getString(R.string.error_generic)
                        )
                    )
                }
            }
        }
        bridge.onExportError = { code, detail ->
            exportUi = ExportUi()
            bridge.chunkSink = null
            bridge.onExportDone = null
            bridge.onExportError = null
            session.close()
            tmp.delete()
            val message = when (code) {
                "no-svg" -> context.getString(R.string.error_no_svg_element)
                else -> context.getString(
                    R.string.error_render_detail,
                    detail.ifBlank { code }
                )
            }
            scope.launch { snackbar.showSnackbar(message) }
        }

        val bgArg = background?.let { "\"${toHex(it)}\"" } ?: "null"
        view.evaluateJavascript(
            "window.SVGViewer && window.SVGViewer.exportPng($width, $height, $bgArg);",
            null
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            performExport(exportW, exportH, exportBg)
        } else {
            scope.launch {
                snackbar.showSnackbar(context.getString(R.string.export_permission))
            }
        }
    }

    fun requestExport() {
        val needsPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
            PackageManager.PERMISSION_GRANTED
        if (needsPermission) {
            permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            performExport(exportW, exportH, exportBg)
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
            bridge.onExportError = null
        }
    }

    PredictiveBack(enabled = true, onBack = onBack, onProgress = { backProgress = it })

    // Thanh trên trượt xuống, dock trượt lên — màn xem không còn "bật" ra tức thì.
    val barIn = remember { Animatable(0f) }
    val dockIn = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        barIn.animateTo(1f, tween(360, easing = Motion.Easing))
    }
    LaunchedEffect(Unit) {
        delay(80)
        dockIn.animateTo(1f, tween(420, easing = Motion.Easing))
    }

    Box(
        Modifier
            .fillMaxSize()
            .predictiveBackVisual(backProgress)
    ) {
        // Nền canvas đổi mềm, kể cả khi chuyển giữa ca-rô và màu đặc.
        Crossfade(
            targetState = settings.background,
            animationSpec = tween(280, easing = Motion.Easing),
            label = "canvas"
        ) { bg ->
            CanvasLayer(bg)
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
                .padding(horizontal = 4.dp, vertical = 4.dp)
                .graphicsLayer {
                    alpha = barIn.value
                    translationY = (1f - barIn.value) * -36.dp.toPx()
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconAction(
                icon = AppIcons.Back,
                contentDescription = stringResource(R.string.viewer_cd_back),
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
                    text = "${document.dimensionLabel.ifBlank { stringResource(R.string.viewer_unknown_size) }} · ${document.sizeLabel}",
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconAction(
                icon = if (settings.background.checker) AppIcons.Checker else AppIcons.Grid,
                contentDescription = stringResource(R.string.viewer_cd_canvas),
                onClick = {
                    haptics.click()
                    showCanvasSheet = true
                },
                tint = scheme.onSurfaceVariant
            )
            IconAction(
                icon = AppIcons.Info,
                contentDescription = stringResource(R.string.viewer_cd_info),
                onClick = { showInfo = true },
                tint = scheme.onSurfaceVariant
            )
            IconAction(
                icon = AppIcons.Export,
                contentDescription = stringResource(R.string.viewer_cd_export),
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
                .padding(horizontal = 4.dp, vertical = 4.dp)
                .graphicsLayer {
                    alpha = dockIn.value
                    translationY = (1f - dockIn.value) * 44.dp.toPx()
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            IconAction(
                icon = AppIcons.Fit,
                contentDescription = stringResource(R.string.viewer_cd_fit),
                onClick = { js("window.SVGViewer.fit()") },
                tint = scheme.onSurfaceVariant
            )
            IconAction(
                icon = AppIcons.ZoomOut,
                contentDescription = stringResource(R.string.viewer_cd_zoom_out),
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
                contentDescription = stringResource(R.string.viewer_cd_zoom_in),
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
                contentDescription = stringResource(
                    if (paused) R.string.viewer_cd_play else R.string.viewer_cd_pause
                ),
                onClick = {
                    paused = !paused
                    js("window.SVGViewer.setPaused($paused)")
                },
                animated = true
            )
            IconAction(
                icon = AppIcons.Replay,
                contentDescription = stringResource(R.string.viewer_cd_replay),
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

    if (showCanvasSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCanvasSheet = false },
            sheetState = canvasSheetState,
            containerColor = scheme.surface
        ) {
            CanvasPickerSheet(
                selected = settings.background,
                onSelect = { bg ->
                    haptics.selection()
                    settings.updateBackground(bg)
                },
                onDismiss = { showCanvasSheet = false }
            )
        }
    }

    if (showExportSheet) {
        ModalBottomSheet(
            onDismissRequest = { showExportSheet = false },
            sheetState = exportSheetState,
            containerColor = scheme.surface
        ) {
            ExportSheet(
                document = document,
                width = exportW,
                height = exportH,
                background = exportBg,
                lockAspect = lockAspect,
                onWidth = { value ->
                    exportW = value
                    if (lockAspect) exportH = (value / aspect).roundToInt().coerceIn(MIN_SIDE, MAX_SIDE)
                },
                onHeight = { value ->
                    exportH = value
                    if (lockAspect) exportW = (value * aspect).roundToInt().coerceIn(MIN_SIDE, MAX_SIDE)
                },
                onPreset = { longEdge ->
                    val size = sizeForLongEdge(aspect, longEdge)
                    exportW = size.first
                    exportH = size.second
                },
                onLockChange = { lockAspect = it },
                onBackground = { exportBg = it },
                onPickColor = { showColorPicker = true },
                onExport = {
                    showExportSheet = false
                    requestExport()
                }
            )
        }
    }

    if (showColorPicker) {
        Dialog(onDismissRequest = { showColorPicker = false }) {
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = scheme.surface
            ) {
                ColorPickerSheet(
                    initial = exportBg ?: Color.White,
                    onDismiss = { showColorPicker = false },
                    onPick = { color ->
                        exportBg = color
                        showColorPicker = false
                    }
                )
            }
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
                        text = stringResource(R.string.export_running),
                        style = MaterialTheme.typography.titleMedium,
                        color = scheme.onSurface
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.export_running_size, exportW, exportH),
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

/** Lớp nền canvas: ca-rô trong suốt (sáng/tối) hoặc một màu đặc. */
@Composable
private fun CanvasLayer(bg: CanvasBackground) {
    if (bg.checker) {
        Checkerboard(Modifier.fillMaxSize(), dark = !bg.isLight)
    } else {
        Box(Modifier.fillMaxSize().background(Color(bg.argb)))
    }
}

@Composable
private fun ExportSheet(
    document: SvgDocument,
    width: Int,
    height: Int,
    background: Color?,
    lockAspect: Boolean,
    onWidth: (Int) -> Unit,
    onHeight: (Int) -> Unit,
    onPreset: (Int) -> Unit,
    onLockChange: (Boolean) -> Unit,
    onBackground: (Color?) -> Unit,
    onPickColor: () -> Unit,
    onExport: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val haptics = LocalHaptics.current
    val valid = width in MIN_SIDE..MAX_SIDE &&
        height in MIN_SIDE..MAX_SIDE &&
        width.toLong() * height.toLong() <= MAX_PIXELS

    Column(Modifier.padding(horizontal = 20.dp)) {
        Text(
            text = stringResource(R.string.export_title),
            style = MaterialTheme.typography.titleLarge,
            color = scheme.onSurface
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(
                R.string.export_subtitle,
                document.dimensionLabel.ifBlank { stringResource(R.string.viewer_unknown_size) }
            ),
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant
        )

        Spacer(Modifier.height(18.dp))
        SectionLabel(stringResource(R.string.export_presets))
        Spacer(Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(1920 to "1080p", 3840 to "4K", 7680 to "8K").forEach { (edge, label) ->
                val size = sizeForLongEdge(document.aspect ?: 1f, edge)
                val active = size.first == width && size.second == height
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (active) scheme.onBackground else Color.Transparent)
                        .border(
                            width = 1.dp,
                            color = if (active) Color.Transparent else scheme.outline,
                            shape = RoundedCornerShape(14.dp)
                        )
                        .tap(role = null) {
                            haptics.selection()
                            onPreset(edge)
                        }
                        .padding(vertical = 10.dp, horizontal = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.titleSmall,
                            color = if (active) scheme.background else scheme.onSurface
                        )
                        Text(
                            text = "${size.first}×${size.second}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (active) scheme.background.copy(alpha = 0.7f)
                            else scheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        SectionLabel(stringResource(R.string.export_size))
        Spacer(Modifier.height(10.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            NumberField(
                label = stringResource(R.string.export_width),
                value = width,
                onValue = onWidth,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (lockAspect) scheme.onBackground else Color.Transparent
                    )
                    .border(
                        1.dp,
                        if (lockAspect) Color.Transparent else scheme.outline,
                        CircleShape
                    )
                    .tap(role = null) {
                        haptics.selection()
                        onLockChange(!lockAspect)
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (lockAspect) AppIcons.Lock else AppIcons.LockOpen,
                    contentDescription = stringResource(R.string.export_lock),
                    tint = if (lockAspect) scheme.background else scheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
            NumberField(
                label = stringResource(R.string.export_height),
                value = height,
                onValue = onHeight,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = if (valid) stringResource(R.string.export_limit)
            else stringResource(R.string.export_invalid_size),
            style = MaterialTheme.typography.labelSmall,
            color = if (valid) scheme.onSurfaceVariant else scheme.error
        )

        Spacer(Modifier.height(18.dp))
        SectionLabel(stringResource(R.string.export_background))
        Spacer(Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BackgroundOption(
                label = stringResource(R.string.export_bg_transparent),
                selected = background == null,
                swatch = null,
                modifier = Modifier.weight(1f),
                onClick = {
                    haptics.selection()
                    onBackground(null)
                }
            )
            BackgroundOption(
                label = stringResource(R.string.export_custom_color),
                selected = background != null,
                swatch = background ?: Color.White,
                modifier = Modifier.weight(1f),
                onClick = {
                    haptics.selection()
                    if (background == null) onBackground(Color.White)
                    onPickColor()
                }
            )
        }

        if (background != null) {
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, scheme.outlineVariant, RoundedCornerShape(12.dp))
                    .tap(role = null) { onPickColor() }
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(background)
                        .border(1.dp, scheme.outlineVariant, RoundedCornerShape(8.dp))
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = toHex(background),
                    style = MaterialTheme.typography.labelLarge.copy(fontFamily = FontFamily.Monospace),
                    color = scheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = stringResource(R.string.export_bg_pick),
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        PrimaryAction(
            text = stringResource(R.string.export_action_size, width, height),
            icon = AppIcons.Export,
            enabled = valid,
            onClick = onExport,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.export_saved_hint),
            style = MaterialTheme.typography.labelSmall,
            color = scheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun BackgroundOption(
    label: String,
    selected: Boolean,
    swatch: Color?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .heightIn(min = 52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) scheme.onBackground else Color.Transparent)
            .border(
                1.dp,
                if (selected) Color.Transparent else scheme.outline,
                RoundedCornerShape(14.dp)
            )
            .tap(role = null, onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (swatch == null) {
            Box(
                Modifier
                    .size(22.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .border(1.dp, if (selected) scheme.background else scheme.outline, RoundedCornerShape(6.dp))
            ) {
                Checkerboard(Modifier.fillMaxSize(), cell = 5.dp)
            }
        } else {
            Box(
                Modifier
                    .size(22.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(swatch)
                    .border(1.dp, scheme.outlineVariant, RoundedCornerShape(6.dp))
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) scheme.background else scheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Ô nhập số nguyên có nhãn nhỏ phía trên, chỉ nhận chữ số. */
@Composable
private fun NumberField(
    label: String,
    value: Int,
    onValue: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    var text by remember(value) { mutableStateOf(value.toString()) }

    Column(modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = scheme.onSurfaceVariant
        )
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, scheme.outlineVariant, RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = text,
                onValueChange = { raw ->
                    val digits = raw.filter { it.isDigit() }.take(5)
                    text = digits
                    digits.toIntOrNull()?.let(onValue)
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle = TextStyle(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = scheme.onSurface
                ),
                cursorBrush = SolidColor(scheme.onSurface),
                modifier = Modifier.weight(1f)
            )
            Text(
                text = stringResource(R.string.export_px),
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun InfoDialog(document: SvgDocument, onDismiss: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) }
        },
        title = {
            Text(
                text = stringResource(R.string.info_title),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                InfoRow(stringResource(R.string.info_name), document.name)
                InfoRow(
                    stringResource(R.string.info_viewbox),
                    document.viewBox ?: stringResource(R.string.info_unknown)
                )
                InfoRow(
                    stringResource(R.string.info_size),
                    document.dimensionLabel.ifBlank { stringResource(R.string.viewer_unknown_size) }
                )
                InfoRow(stringResource(R.string.info_bytes), document.sizeLabel)
                InfoRow(
                    stringResource(R.string.info_source),
                    stringResource(
                        when (document.origin) {
                            SvgDocument.Origin.FILE -> R.string.info_source_file
                            SvgDocument.Origin.SAMPLE -> R.string.info_source_sample
                            SvgDocument.Origin.CLIPBOARD -> R.string.info_source_clipboard
                        }
                    )
                )
                InfoRow(
                    stringResource(R.string.info_elements),
                    stringResource(
                        R.string.info_elements_count,
                        Regex("<[a-zA-Z]").findAll(document.markup).count()
                    )
                )
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

private fun sizeForLongEdge(aspect: Float, longEdge: Int): Pair<Int, Int> {
    val ratio = if (aspect <= 0f) 1f else aspect
    return if (ratio >= 1f) {
        longEdge to (longEdge / ratio).roundToInt().coerceIn(MIN_SIDE, MAX_SIDE)
    } else {
        (longEdge * ratio).roundToInt().coerceIn(MIN_SIDE, MAX_SIDE) to longEdge
    }
}

private fun shareImage(context: Context, uri: Uri) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    runCatching {
        context.startActivity(
            Intent.createChooser(intent, context.getString(R.string.export_share_chooser))
        )
    }
}
