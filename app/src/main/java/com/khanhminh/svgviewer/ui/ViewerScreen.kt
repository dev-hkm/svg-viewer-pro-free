package com.khanhminh.svgviewer.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.print.PrintAttributes
import android.print.PrintManager
import android.util.Base64
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
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
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.khanhminh.svgviewer.R
import com.khanhminh.svgviewer.data.CanvasBackground
import com.khanhminh.svgviewer.data.SettingsStore
import com.khanhminh.svgviewer.data.SvgDocument
import com.khanhminh.svgviewer.data.SvgHealth
import com.khanhminh.svgviewer.data.SvgLoader
import com.khanhminh.svgviewer.data.SvgOptimizer
import com.khanhminh.svgviewer.data.SvgPalette
import com.khanhminh.svgviewer.render.ExportSession
import com.khanhminh.svgviewer.render.HtmlComposer
import com.khanhminh.svgviewer.render.ImageSaver
import com.khanhminh.svgviewer.render.SvgBridge
import com.khanhminh.svgviewer.render.SvgSharer
import com.khanhminh.svgviewer.ui.haptics.LocalHaptics
import com.khanhminh.svgviewer.ui.theme.AppIcons
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONTokener
import java.io.File
import kotlin.math.roundToInt

private class WebViewHolder {
    var webView: WebView? = null

    /** HTML đã nạp trong WebView, để không nạp lại khi chỉ vì recomposition. */
    var loadedHtml: String? = null
}

private data class ExportUi(
    val running: Boolean = false,
    val progress: Int = 0,
    val index: Int = 0,
    val total: Int = 0
)

/** Định dạng ảnh kết xuất. WebP nhẹ hơn PNG ~30%, JPEG cho ảnh nhiều màu. */
private enum class ExportFormat(val key: String, val mime: String, val ext: String) {
    PNG("png", "image/png", "png"),
    WEBP("webp", "image/webp", "webp"),
    JPEG("jpeg", "image/jpeg", "jpg")
}

private class ExportJob(
    val width: Int,
    val height: Int,
    val background: Color?,
    val padding: Int,
    val format: ExportFormat,
    val quality: Float,
    val album: String,
    val fileName: (Int, Int) -> String
)

private const val MIN_SIDE = 16
private const val MAX_SIDE = 16384

/** Trần số điểm ảnh để canvas không vượt bộ nhớ cho phép (khoảng 8K 16:9 vẫn lọt). */
private const val MAX_PIXELS = 40_000_000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewerScreen(
    document: SvgDocument,
    settings: SettingsStore,
    onThumbnail: (Uri, ByteArray) -> Unit = { _, _ -> },
    onLightBarsChange: (Boolean?) -> Unit = {},
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val clipboard = LocalClipboardManager.current
    val bridge = remember { SvgBridge() }
    val holder = remember { WebViewHolder() }
    val haptics = LocalHaptics.current
    val scheme = MaterialTheme.colorScheme
    val canvasSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val exportSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val speedSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val toolsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val iconSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val paletteSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val optimizeSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val healthSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val snippetsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Bản đang hiển thị: tệp gốc, hoặc markup người dùng vừa sửa trong app.
    var editedDoc by remember(document) { mutableStateOf<SvgDocument?>(null) }
    val activeDoc = editedDoc ?: document
    val activeMarkup = activeDoc.markup
    val html = remember(activeMarkup) { HtmlComposer.compose(context, activeMarkup) }
    var draft by remember(document) { mutableStateOf(document.markup) }
    var showEditor by remember { mutableStateOf(false) }

    // Dock có 8 phần tử: máy hẹp thì thu nhỏ từng nút để không bị cắt mép.
    val compactDock = LocalConfiguration.current.screenWidthDp < 380
    val dockButton = if (compactDock) 42.dp else 48.dp
    val dockIcon = if (compactDock) 19.dp else 21.dp

    val aspect = activeDoc.aspect?.takeIf { it > 0f } ?: 1f

    var zoom by remember { mutableFloatStateOf(1f) }
    var paused by remember { mutableStateOf(false) }
    var showExportSheet by remember { mutableStateOf(false) }
    var showInfo by remember { mutableStateOf(false) }
    var showCanvasSheet by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    var showSpeedSheet by remember { mutableStateOf(false) }
    var showTools by remember { mutableStateOf(false) }
    var showIconSet by remember { mutableStateOf(false) }
    var showPalette by remember { mutableStateOf(false) }
    var showOptimize by remember { mutableStateOf(false) }
    var showHealth by remember { mutableStateOf(false) }
    var showSnippets by remember { mutableStateOf(false) }
    var inverted by remember { mutableStateOf(false) }
    var exportFormat by remember { mutableStateOf(ExportFormat.PNG) }
    var exportQuality by remember { mutableIntStateOf(90) }
    var exportUi by remember { mutableStateOf(ExportUi()) }
    var backProgress by remember { mutableFloatStateOf(0f) }
    val activeSession = remember { mutableStateOf<ExportSession?>(null) }

    // Chế độ soi phần tử.
    var inspecting by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<InspectedElement?>(null) }

    // Đổi màu tại chỗ. Danh sách lấy từ DOM thật nên sau mỗi lần đổi lại được
    // đồng bộ; bộ đếm bước khớp với lịch sử hoàn tác bên trong WebView.
    var recolorSwatches by remember(document) {
        mutableStateOf(SvgPalette.extract(document.markup))
    }
    var recolorSteps by remember(document) { mutableIntStateOf(0) }
    var pendingUndo by remember { mutableStateOf(false) }
    var paletteTarget by remember { mutableStateOf<String?>(null) }

    // Toàn màn hình: ẩn thanh hệ thống cùng thanh nổi của app, chạm để thoát.
    var immersive by remember { mutableStateOf(false) }
    var immersiveHint by remember { mutableStateOf(false) }

    val defaultSize = remember(aspect) { sizeForLongEdge(aspect, 3840) }
    var exportW by remember(activeMarkup) { mutableIntStateOf(defaultSize.first) }
    var exportH by remember(activeMarkup) { mutableIntStateOf(defaultSize.second) }
    var exportBg by remember { mutableStateOf<Color?>(null) }
    var exportPad by remember { mutableIntStateOf(0) }
    var lockAspect by remember { mutableStateOf(true) }

    fun js(code: String) {
        holder.webView?.evaluateJavascript(code, null)
    }

    /** Nhờ WebView liệt kê lại màu đang thật sự có trong DOM sau khi đổi màu. */
    fun requestColors() {
        js(
            "(function(){var l=window.SVGViewer&&window.SVGViewer.collectColors();" +
                "if(l)window.Android.colors(JSON.stringify(l));})()"
        )
    }

    fun enterInspect() {
        selected = null
        inspecting = true
        js("window.SVGViewer && window.SVGViewer.inspect(true)")
    }

    fun exitInspect() {
        if (!inspecting) return
        inspecting = false
        selected = null
        // Trả lại mọi phần tử đã bị ẩn trong lúc soi: thoát ra là bản vẽ nguyên vẹn.
        js("window.SVGViewer && (window.SVGViewer.inspect(false), window.SVGViewer.showAllHidden())")
    }

    fun toggleElementHidden() {
        val element = selected ?: return
        if (element.isRoot) return
        val path = element.path.joinToString(",")
        js("window.SVGViewer.setHidden([$path], ${!element.hidden})")
    }

    fun copyElementCode() {
        val web = holder.webView ?: return
        web.evaluateJavascript(
            "(function(){try{return window.SVGViewer?window.SVGViewer.elementCode():''}" +
                "catch(e){return ''}})()"
        ) { raw ->
            val code = runCatching {
                JSONTokener(raw).nextValue() as? String ?: ""
            }.getOrDefault("")
            if (code.isBlank()) {
                haptics.warning()
                scope.launch { snackbar.showSnackbar(context.getString(R.string.inspector_none)) }
            } else {
                clipboard.setText(AnnotatedString(code))
                haptics.success()
                scope.launch { snackbar.showSnackbar(context.getString(R.string.inspector_copied)) }
            }
        }
    }

    fun applyRecolor(from: String, to: String) {
        js("window.SVGViewer && window.SVGViewer.recolor('$from', '$to')")
    }

    fun undoRecolor() {
        if (recolorSteps <= 0) return
        pendingUndo = true
        js("window.SVGViewer && window.SVGViewer.undoRecolor()")
    }

    fun resetRecolor() {
        if (recolorSteps <= 0) return
        pendingUndo = false
        js("window.SVGViewer && window.SVGViewer.resetRecolor()")
    }

    /** Áp dụng markup vừa sửa; trả về câu báo lỗi nếu không dựng lại được. */
    fun applyEdit(text: String): String? {
        val rebuilt = SvgLoader.recompute(document, text)
            ?: return context.getString(R.string.editor_invalid)
        editedDoc = rebuilt
        draft = rebuilt.markup
        showEditor = false
        haptics.success()
        return null
    }

    fun shareDocument() {
        haptics.click()
        val ok = SvgSharer.share(context, activeMarkup, document.name)
        if (!ok) {
            haptics.warning()
            scope.launch { snackbar.showSnackbar(context.getString(R.string.share_failed)) }
        }
    }

    /**
     * Chạy hàng đợi lần lượt: WebView chỉ kết xuất được một ảnh tại một thời
     * điểm, nên bộ icon nhiều kích thước phải xếp hàng thay vì gọi song song.
     */
    fun runJobs(jobs: List<ExportJob>) {
        val view = holder.webView ?: return
        if (jobs.isEmpty()) return
        activeSession.value?.discard()
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val saved = ArrayList<Uri>()
        var lastW = 0
        var lastH = 0
        exportUi = ExportUi(running = true, progress = 0, index = 0, total = jobs.size)

        fun finish() {
            exportUi = ExportUi()
            scope.launch {
                haptics.success()
                val result = snackbar.showSnackbar(
                    message = if (saved.size > 1) {
                        context.getString(R.string.iconset_done, saved.size)
                    } else {
                        context.getString(R.string.export_saved, lastW, lastH)
                    },
                    actionLabel = if (saved.size == 1) {
                        context.getString(R.string.export_share)
                    } else null,
                    withDismissAction = true
                )
                if (result == SnackbarResult.ActionPerformed && saved.size == 1) {
                    shareImage(context, saved.first())
                }
            }
        }

        fun fail(message: String) {
            exportUi = ExportUi()
            bridge.chunkSink = null
            bridge.onExportDone = null
            bridge.onExportError = null
            scope.launch {
                haptics.warning()
                snackbar.showSnackbar(message)
            }
        }

        fun step(index: Int) {
            if (index >= jobs.size) {
                finish()
                return
            }
            val job = jobs[index]
            exportUi = exportUi.copy(running = true, index = index, total = jobs.size)
            val tmp = File(dir, "svg_${System.currentTimeMillis()}_$index.${job.format.ext}")
            val session = ExportSession(tmp)
            activeSession.value = session

            bridge.chunkSink = { chunk -> session.append(chunk) }
            bridge.onExportDone = { w, h ->
                session.close()
                bridge.chunkSink = null
                bridge.onExportDone = null
                bridge.onExportError = null
                lastW = w
                lastH = h
                scope.launch {
                    val outcome = withContext(Dispatchers.IO) {
                        runCatching {
                            ImageSaver.saveImage(
                                context = context,
                                source = tmp,
                                fileName = job.fileName(w, h),
                                mime = job.format.mime,
                                album = job.album
                            )
                        }
                    }
                    tmp.delete()
                    outcome.onSuccess { saved.add(it) }
                    step(index + 1)
                }
            }
            bridge.onExportError = { code, detail ->
                session.close()
                tmp.delete()
                fail(
                    when (code) {
                        "no-svg" -> context.getString(R.string.error_no_svg_element)
                        else -> context.getString(
                            R.string.error_render_detail,
                            detail.ifBlank { code }
                        )
                    }
                )
            }

            val bgArg = job.background?.let { "\"${toHex(it)}\"" } ?: "null"
            val padArg = (job.padding.coerceIn(0, 45) / 100f).toString()
            val qualityArg = (job.quality.coerceIn(30f, 100f) / 100f).toString()
            view.evaluateJavascript(
                "window.SVGViewer && window.SVGViewer.exportPng(${job.width}, ${job.height}, " +
                    "$bgArg, $padArg, \"${job.format.key}\", $qualityArg);",
                null
            )
        }

        step(0)
    }

    fun performExport(width: Int, height: Int, background: Color?, paddingPercent: Int) {
        val base = ImageSaver.sanitize(document.name)
        val format = exportFormat
        runJobs(
            listOf(
                ExportJob(
                    width = width,
                    height = height,
                    background = background,
                    padding = paddingPercent,
                    format = format,
                    quality = exportQuality.toFloat(),
                    album = "SVG Viewer",
                    fileName = { w, h -> "${base}_${w}x${h}_${ImageSaver.stamp()}.${format.ext}" }
                )
            )
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            performExport(exportW, exportH, exportBg, exportPad)
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
            performExport(exportW, exportH, exportBg, exportPad)
        }
    }

    /** Nạp lại tài liệu: cách duy nhất để cảnh do JavaScript dựng chạy lại từ đầu. */
    fun reloadDocument() {
        paused = false
        zoom = 1f
        // DOM mới sinh ra từ markup gốc nên mọi thay đổi tạm thời đều về mặc định.
        recolorSteps = 0
        recolorSwatches = SvgPalette.extract(activeMarkup)
        selected = null
        inspecting = false
        holder.loadedHtml = html
        holder.webView?.loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
    }

    fun copyCode() {
        clipboard.setText(AnnotatedString(activeMarkup))
        haptics.success()
        scope.launch { snackbar.showSnackbar(context.getString(R.string.info_code_copied)) }
    }

    /**
     * In / lưu PDF qua hộp thoại hệ thống. WebView in theo vector nên tệp PDF
     * giữ được nét sắc ở mọi kích thước, khác hẳn ảnh PNG xuất ra.
     * Đưa về đúng khung trước khi in để bản in không bị lệch theo mức zoom.
     */
    fun printDocument() {
        val web = holder.webView ?: return
        js("window.SVGViewer && window.SVGViewer.fit()")
        web.postDelayed({
            runCatching {
                val manager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
                manager.print(
                    document.name,
                    web.createPrintDocumentAdapter(document.name),
                    PrintAttributes.Builder()
                        .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                        .setMinMargins(PrintAttributes.Margins(0, 0, 0, 0))
                        .build()
                )
            }.onFailure {
                scope.launch { snackbar.showSnackbar(context.getString(R.string.pdf_failed)) }
            }
        }, 420L)
    }

    // Ảnh thu nhỏ luôn đại diện cho markup đang hiển thị; bản đã sửa trong app
    // không ghi đè ảnh của tệp gốc trong danh sách gần đây.
    val editedNow = rememberUpdatedState(editedDoc)

    DisposableEffect(bridge) {
        bridge.onZoom = { value -> zoom = value }
        bridge.onProgress = { percent -> exportUi = exportUi.copy(progress = percent) }
        bridge.onError = { message ->
            exportUi = ExportUi()
            scope.launch { snackbar.showSnackbar(message) }
        }
        bridge.onReload = { reloadDocument() }
        bridge.onTap = { if (immersive) immersive = false }
        bridge.onInspect = { json -> parseInspect(json)?.let { selected = it } }
        bridge.onColors = { json -> parseSwatches(json)?.let { recolorSwatches = it } }
        bridge.onRecolor = { total, _, _ ->
            when {
                total > 0 -> {
                    recolorSteps += 1
                    pendingUndo = false
                }
                pendingUndo -> {
                    recolorSteps = (recolorSteps - 1).coerceAtLeast(0)
                    pendingUndo = false
                }
                else -> scope.launch {
                    snackbar.showSnackbar(context.getString(R.string.palette_none))
                }
            }
            requestColors()
        }
        bridge.onRecolorReset = { _ ->
            recolorSteps = 0
            pendingUndo = false
            requestColors()
        }
        bridge.onHiddenCleared = { count ->
            if (count > 0) {
                scope.launch {
                    snackbar.showSnackbar(context.getString(R.string.inspector_restored, count))
                }
            }
        }
        // Ảnh thu nhỏ: chỉ chụp một lần cho tệp thật, sau khi SVG đã dựng xong
        // và ổn định vài nhịp để cảnh do JS dựng kịp hiện ra.
        bridge.onReady = { _, _ ->
            val uri = document.uri
            if (uri != null && document.origin == SvgDocument.Origin.FILE && editedNow.value == null) {
                holder.webView?.postDelayed({
                    holder.webView?.evaluateJavascript(
                        "window.SVGViewer && window.SVGViewer.snapshot(320);",
                        null
                    )
                }, 700L)
            }
        }
        bridge.onThumbnail = { base64, _, _ ->
            val uri = document.uri
            if (uri != null && base64.isNotBlank()) {
                val bytes = runCatching { Base64.decode(base64, Base64.DEFAULT) }.getOrNull()
                if (bytes != null && bytes.isNotEmpty()) onThumbnail(uri, bytes)
            }
        }
        onDispose {
            bridge.onZoom = null
            bridge.onProgress = null
            bridge.onError = null
            bridge.onReload = null
            bridge.onReady = null
            bridge.onThumbnail = null
            bridge.onTap = null
            bridge.onInspect = null
            bridge.onColors = null
            bridge.onRecolor = null
            bridge.onRecolorReset = null
            bridge.onHiddenCleared = null
            bridge.chunkSink = null
            bridge.onExportDone = null
            bridge.onExportError = null
        }
    }

    // Đổi markup thì nạp lại WebView một lần, và dọn mọi trạng thái gắn với DOM cũ.
    LaunchedEffect(html) {
        val web = holder.webView ?: return@LaunchedEffect
        if (holder.loadedHtml != html) {
            holder.loadedHtml = html
            web.loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
        }
    }
    LaunchedEffect(activeMarkup) {
        paused = false
        zoom = 1f
        recolorSteps = 0
        recolorSwatches = SvgPalette.extract(activeMarkup)
        selected = null
        inspecting = false
    }

    // Toàn màn hình: thanh hệ thống ẩn hẳn, vuốt mép để hiện tạm; thoát là trả lại.
    LaunchedEffect(immersive) {
        val window = context.findActivity()?.window ?: return@LaunchedEffect
        val controller = WindowCompat.getInsetsController(window, view)
        if (immersive) {
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
            immersiveHint = true
            delay(3200)
            immersiveHint = false
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
            immersiveHint = false
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            val window = context.findActivity()?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view)
                    .show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // Trình soạn mã phủ nền màu của theme, nên icon thanh hệ thống phải theo theme
    // thay vì theo màu canvas phía sau khung vẽ.
    LaunchedEffect(showEditor, scheme.background) {
        onLightBarsChange(if (showEditor) scheme.background.isLight() else null)
    }
    DisposableEffect(Unit) {
        onDispose { onLightBarsChange(null) }
    }

    PredictiveBack(enabled = true, onBack = onBack, onProgress = { backProgress = it })
    PredictiveBack(enabled = immersive, onBack = { immersive = false })
    PredictiveBack(enabled = inspecting, onBack = { exitInspect() })
    PredictiveBack(enabled = showEditor, onBack = { showEditor = false })

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
                    // SVG được phép chạy script (nhiều cảnh dựng bằng JS), nhưng bị
                    // khoá trong hộp cát: app không có quyền INTERNET, không đọc được
                    // tệp, không mở cửa sổ mới và không điều hướng đi đâu cả.
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    allowFileAccess = false
                    allowContentAccess = false
                    allowFileAccessFromFileURLs = false
                    allowUniversalAccessFromFileURLs = false
                    setBlockNetworkLoads(true)
                    javaScriptCanOpenWindowsAutomatically = false
                    setSupportMultipleWindows(false)
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
                        view.evaluateJavascript(
                            "window.SVGViewer && (window.SVGViewer.fit()," +
                                " window.SVGViewer.setSpeed(${settings.playbackSpeed})," +
                                " window.SVGViewer.setInvert($inverted));",
                            null
                        )
                    }

                    // Chặn mọi điều hướng: SVG chỉ được vẽ, không được dắt app đi đâu.
                    override fun shouldOverrideUrlLoading(
                        view: WebView,
                        request: WebResourceRequest
                    ): Boolean = true

                    override fun shouldInterceptRequest(
                        view: WebView,
                        request: WebResourceRequest
                    ): WebResourceResponse? {
                        val scheme = request.url.scheme?.lowercase()
                        return if (scheme == "http" || scheme == "https" || scheme == "ftp") {
                            WebResourceResponse("text/plain", "utf-8", null)
                        } else null
                    }
                }
                holder.loadedHtml = html
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

        // Thanh trên — trượt đi hẳn khi vào toàn màn hình.
        AnimatedVisibility(
            visible = !immersive,
            enter = fadeIn(tween(220, easing = Motion.Easing)) +
                slideInVertically(tween(280, easing = Motion.Easing)) { -it / 3 },
            exit = fadeOut(tween(160)) +
                slideOutVertically(tween(220, easing = Motion.Easing)) { -it / 3 },
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier
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
                        text = buildString {
                            append(
                                activeDoc.dimensionLabel.ifBlank {
                                    stringResource(R.string.viewer_unknown_size)
                                }
                            )
                            append(" · ")
                            append(activeDoc.sizeLabel)
                            if (editedDoc != null) {
                                append(" · ")
                                append(stringResource(R.string.viewer_edited))
                            }
                        },
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
                    icon = AppIcons.More,
                    contentDescription = stringResource(R.string.viewer_cd_tools),
                    onClick = {
                        haptics.click()
                        showTools = true
                    },
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
        }

        // Dock dưới — cùng nhịp ẩn/hiện với thanh trên.
        AnimatedVisibility(
            visible = !immersive,
            enter = fadeIn(tween(220, easing = Motion.Easing)) +
                slideInVertically(tween(300, easing = Motion.Easing)) { it / 3 },
            exit = fadeOut(tween(160)) +
                slideOutVertically(tween(220, easing = Motion.Easing)) { it / 3 },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Row(
                modifier = Modifier
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
                tint = scheme.onSurfaceVariant,
                size = dockButton,
                iconSize = dockIcon
            )
            IconAction(
                icon = AppIcons.ZoomOut,
                contentDescription = stringResource(R.string.viewer_cd_zoom_out),
                onClick = { js("window.SVGViewer.zoomBy(0.8)") },
                tint = scheme.onSurfaceVariant,
                size = dockButton,
                iconSize = dockIcon
            )
            Box(
                modifier = Modifier
                    .width(if (compactDock) 50.dp else 56.dp)
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
                tint = scheme.onSurfaceVariant,
                size = dockButton,
                iconSize = dockIcon
            )
            Box(
                Modifier
                    .height(22.dp)
                    .width(1.dp)
                    .background(scheme.outlineVariant)
            )
            // Tốc độ phát: hiện ngay con số đang dùng, chạm để mở bảng chọn.
            SpeedBadge(
                speed = settings.playbackSpeed,
                compact = compactDock,
                onClick = {
                    haptics.click()
                    showSpeedSheet = true
                }
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
                animated = true,
                size = dockButton,
                iconSize = dockIcon
            )
                IconAction(
                    icon = AppIcons.Replay,
                    contentDescription = stringResource(R.string.viewer_cd_replay),
                    onClick = { reloadDocument() },
                    tint = scheme.onSurfaceVariant,
                    size = dockButton,
                    iconSize = dockIcon
                )
            }
        }

        // Nút soi phần tử: nổi bên phải, không chiếm chỗ của thanh trên.
        if (!immersive) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 10.dp)
                    .clip(CircleShape)
                    .background(scheme.surface.copy(alpha = 0.94f))
                    .border(1.dp, scheme.outlineVariant, CircleShape)
            ) {
                IconAction(
                    icon = AppIcons.Inspect,
                    contentDescription = stringResource(
                        if (inspecting) R.string.inspector_cd_exit else R.string.inspector_cd_enter
                    ),
                    onClick = { if (inspecting) exitInspect() else enterInspect() },
                    tint = if (inspecting) null else scheme.onSurface,
                    filled = inspecting,
                    size = 44.dp,
                    iconSize = 19.dp
                )
            }
        }

        // Bảng soi phần tử: nổi phía trên dock để khung vẽ vẫn chạm được.
        AnimatedVisibility(
            visible = inspecting,
            enter = fadeIn(tween(200, easing = Motion.Easing)) +
                slideInVertically(tween(280, easing = Motion.Easing)) { it / 4 },
            exit = fadeOut(tween(150)) +
                slideOutVertically(tween(220, easing = Motion.Easing)) { it / 4 },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(start = 12.dp, end = 12.dp, bottom = 84.dp)
        ) {
            val element = selected
            if (element == null) {
                InspectorHint()
            } else {
                InspectorPanel(
                    element = element,
                    onClose = { exitInspect() },
                    onParent = { js("window.SVGViewer && window.SVGViewer.selectParent()") },
                    onToggleHidden = { toggleElementHidden() },
                    onCopy = { copyElementCode() }
                )
            }
        }

        // Gợi ý thoát toàn màn hình, tự tắt sau vài giây.
        AnimatedVisibility(
            visible = immersive && immersiveHint,
            enter = fadeIn(tween(240, easing = Motion.Easing)),
            exit = fadeOut(tween(260, easing = Motion.Easing)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 34.dp)
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(scheme.surface.copy(alpha = 0.92f))
                    .border(1.dp, scheme.outlineVariant, RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = AppIcons.Fullscreen,
                    contentDescription = null,
                    tint = scheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.immersive_hint),
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.onSurface
                )
            }
        }

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = if (inspecting) 300.dp else 92.dp
                )
        )

        // Trình soạn mã: phủ toàn màn hình nhưng vẫn cùng một cửa sổ, nên
        // edge-to-edge và thanh hệ thống giữ nguyên cách xử lý.
        AnimatedVisibility(
            visible = showEditor,
            enter = fadeIn(tween(200, easing = Motion.Easing)),
            exit = fadeOut(tween(160, easing = Motion.Easing))
        ) {
            CodeEditorScreen(
                text = draft,
                initial = document.markup,
                applied = activeMarkup,
                onTextChange = { draft = it },
                onDismiss = { showEditor = false },
                onCopy = {
                    clipboard.setText(AnnotatedString(draft))
                    haptics.success()
                    scope.launch { snackbar.showSnackbar(context.getString(R.string.info_code_copied)) }
                },
                onApply = { applyEdit(draft) }
            )
        }
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

    if (showSpeedSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSpeedSheet = false },
            sheetState = speedSheetState,
            containerColor = scheme.surface
        ) {
            SpeedSheet(
                current = settings.playbackSpeed,
                onPick = { speed ->
                    haptics.selection()
                    settings.updatePlaybackSpeed(speed)
                    js("window.SVGViewer && window.SVGViewer.setSpeed($speed)")
                }
            )
        }
    }

    if (showTools) {
        ModalBottomSheet(
            onDismissRequest = { showTools = false },
            sheetState = toolsSheetState,
            containerColor = scheme.surface
        ) {
            ToolsSheet(
                inverted = inverted,
                immersive = immersive,
                onInspect = {
                    showTools = false
                    enterInspect()
                },
                onInvert = {
                    inverted = !inverted
                    js("window.SVGViewer && window.SVGViewer.setInvert($inverted)")
                },
                onImmersive = {
                    showTools = false
                    if (!immersive) exitInspect()
                    immersive = !immersive
                },
                onInfo = {
                    showTools = false
                    showInfo = true
                },
                onCopy = {
                    showTools = false
                    copyCode()
                },
                onShare = {
                    showTools = false
                    shareDocument()
                },
                onEditor = {
                    draft = activeMarkup
                    showTools = false
                    showEditor = true
                },
                onIconSet = {
                    showTools = false
                    showIconSet = true
                },
                onOptimize = {
                    showTools = false
                    showOptimize = true
                },
                onPalette = {
                    showTools = false
                    showPalette = true
                    requestColors()
                },
                onHealth = {
                    showTools = false
                    showHealth = true
                },
                onSnippets = {
                    showTools = false
                    showSnippets = true
                },
                onPrint = {
                    showTools = false
                    printDocument()
                }
            )
        }
    }

    if (showIconSet) {
        ModalBottomSheet(
            onDismissRequest = { showIconSet = false },
            sheetState = iconSheetState,
            containerColor = scheme.surface
        ) {
            IconSetSheet(
                onExport = { sizes, background, padding ->
                    showIconSet = false
                    val base = ImageSaver.sanitize(document.name)
                    runJobs(
                        sizes.map { size ->
                            ExportJob(
                                width = size,
                                height = size,
                                background = background,
                                padding = padding,
                                format = ExportFormat.PNG,
                                quality = 100f,
                                album = "SVG Viewer/${base}_icons",
                                fileName = { w, _ -> "${base}_$w.png" }
                            )
                        }
                    )
                }
            )
        }
    }

    if (showPalette) {
        ModalBottomSheet(
            onDismissRequest = { showPalette = false },
            sheetState = paletteSheetState,
            containerColor = scheme.surface
        ) {
            RecolorSheet(
                swatches = recolorSwatches,
                steps = recolorSteps,
                onPick = { hex -> paletteTarget = hex },
                onCopy = { hex ->
                    clipboard.setText(AnnotatedString(hex))
                    haptics.success()
                    scope.launch { snackbar.showSnackbar(context.getString(R.string.palette_copied, hex)) }
                },
                onUndo = {
                    haptics.tick()
                    undoRecolor()
                },
                onReset = {
                    haptics.warning()
                    resetRecolor()
                }
            )
        }
    }

    val recolorTarget = paletteTarget
    if (recolorTarget != null) {
        Dialog(onDismissRequest = { paletteTarget = null }) {
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = scheme.surface
            ) {
                ColorPickerSheet(
                    initial = colorOf(recolorTarget),
                    onDismiss = { paletteTarget = null },
                    onPick = { color ->
                        applyRecolor(recolorTarget, toHex(color))
                        paletteTarget = null
                    }
                )
            }
        }
    }

    if (showHealth) {
        val report by produceState<SvgHealth.Report?>(initialValue = null, activeMarkup) {
            value = withContext(Dispatchers.Default) { SvgHealth.analyze(activeMarkup) }
        }
        ModalBottomSheet(
            onDismissRequest = { showHealth = false },
            sheetState = healthSheetState,
            containerColor = scheme.surface
        ) {
            HealthSheet(
                report = report,
                onOptimize = {
                    showHealth = false
                    showOptimize = true
                }
            )
        }
    }

    if (showSnippets) {
        ModalBottomSheet(
            onDismissRequest = { showSnippets = false },
            sheetState = snippetsSheetState,
            containerColor = scheme.surface
        ) {
            SnippetsSheet(
                markup = activeMarkup,
                onCopy = { snippet ->
                    clipboard.setText(AnnotatedString(snippet))
                    haptics.success()
                    scope.launch { snackbar.showSnackbar(context.getString(R.string.snippets_copied)) }
                }
            )
        }
    }

    if (showOptimize) {
        val optimized = remember(activeMarkup) { SvgOptimizer.optimize(activeMarkup) }
        ModalBottomSheet(
            onDismissRequest = { showOptimize = false },
            sheetState = optimizeSheetState,
            containerColor = scheme.surface
        ) {
            OptimizeSheet(
                result = optimized,
                onCopy = {
                    clipboard.setText(AnnotatedString(optimized.markup))
                    haptics.success()
                    scope.launch { snackbar.showSnackbar(context.getString(R.string.info_code_copied)) }
                },
                onSave = {
                    scope.launch {
                        val outcome = withContext(Dispatchers.IO) {
                            runCatching {
                                ImageSaver.saveText(
                                    context = context,
                                    text = optimized.markup,
                                    fileName = "${ImageSaver.sanitize(document.name)}_optimized.svg",
                                    mime = "image/svg+xml"
                                )
                            }
                        }
                        outcome.onSuccess {
                            haptics.success()
                            showOptimize = false
                            snackbar.showSnackbar(context.getString(R.string.optimize_saved_ok))
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
                document = activeDoc,
                width = exportW,
                height = exportH,
                background = exportBg,
                lockAspect = lockAspect,
                paddingPercent = exportPad,
                onPadding = { exportPad = it },
                format = exportFormat,
                quality = exportQuality,
                onFormat = { exportFormat = it },
                onQuality = { exportQuality = it },
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
        InfoDialog(
            document = activeDoc,
            markup = activeMarkup,
            onCopy = {
                showInfo = false
                copyCode()
            },
            onDismiss = { showInfo = false }
        )
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
                        text = if (exportUi.total > 1) {
                            stringResource(
                                R.string.export_running_batch,
                                exportUi.index + 1,
                                exportUi.total
                            )
                        } else {
                            stringResource(R.string.export_running_size, exportW, exportH)
                        },
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

/** Các mốc tốc độ dùng chung cho dock và bảng chọn. */
private val SPEED_STEPS = listOf(0.25f, 0.5f, 1f, 2f, 4f)

private fun speedLabel(value: Float): String {
    val number = if (value == value.toInt().toFloat()) value.toInt().toString()
    else value.toString().removeSuffix("0").removeSuffix(".")
    return "$number×"
}

/**
 * Huy hiệu tốc độ trong dock: đảo màu khi khác 1× để mắt nhận ra ngay
 * là animation đang chạy không đúng nhịp gốc.
 */
@Composable
private fun SpeedBadge(speed: Float, compact: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val active = speed != 1f
    val shape = RoundedCornerShape(9.dp)
    Box(
        modifier = Modifier
            .size(if (compact) 42.dp else 48.dp)
            .tap(role = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .height(28.dp)
                .widthIn(min = if (compact) 34.dp else 38.dp)
                .clip(shape)
                .then(
                    if (active) Modifier.background(scheme.onBackground)
                    else Modifier.border(1.dp, scheme.outline, shape)
                )
                .padding(horizontal = 5.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = speedLabel(speed),
                style = MaterialTheme.typography.labelSmall,
                color = if (active) scheme.background else scheme.onSurface,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SpeedSheet(current: Float, onPick: (Float) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Column(
        Modifier
            .padding(horizontal = 20.dp)
            .navigationBarsPadding()
    ) {
        Text(
            text = stringResource(R.string.speed_title),
            style = MaterialTheme.typography.titleLarge,
            color = scheme.onSurface
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.speed_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant
        )

        Spacer(Modifier.height(20.dp))
        SectionLabel(stringResource(R.string.speed_current))
        Spacer(Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SPEED_STEPS.forEach { step ->
                ChoicePill(
                    text = speedLabel(step),
                    selected = current == step,
                    onClick = { onPick(step) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(Modifier.height(28.dp))
    }
}

/* ---------------- bảng công cụ chuyên nghiệp ---------------- */

@Composable
private fun ToolsSheet(
    inverted: Boolean,
    immersive: Boolean,
    onInspect: () -> Unit,
    onInvert: () -> Unit,
    onImmersive: () -> Unit,
    onInfo: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onEditor: () -> Unit,
    onIconSet: () -> Unit,
    onOptimize: () -> Unit,
    onPalette: () -> Unit,
    onHealth: () -> Unit,
    onSnippets: () -> Unit,
    onPrint: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        Modifier
            .padding(horizontal = 20.dp)
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = stringResource(R.string.tools_title),
            style = MaterialTheme.typography.titleLarge,
            color = scheme.onSurface
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.tools_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))

        ToolRow(
            icon = AppIcons.Inspect,
            title = stringResource(R.string.tools_inspect),
            subtitle = stringResource(R.string.tools_inspect_desc),
            onClick = onInspect
        )
        ToolRow(
            icon = AppIcons.Info,
            title = stringResource(R.string.tools_info),
            subtitle = stringResource(R.string.tools_info_desc),
            onClick = onInfo
        )
        ToolRow(
            icon = AppIcons.Copy,
            title = stringResource(R.string.tools_copy),
            subtitle = stringResource(R.string.tools_copy_desc),
            onClick = onCopy
        )
        ToolRow(
            icon = AppIcons.Share,
            title = stringResource(R.string.tools_share),
            subtitle = stringResource(R.string.tools_share_desc),
            onClick = onShare
        )
        ToolRow(
            icon = AppIcons.CodeBlock,
            title = stringResource(R.string.tools_editor),
            subtitle = stringResource(R.string.tools_editor_desc),
            onClick = onEditor
        )
        ToolRow(
            icon = AppIcons.Invert,
            title = stringResource(R.string.tools_invert),
            subtitle = stringResource(R.string.tools_invert_desc),
            onClick = onInvert,
            trailing = {
                Switch(
                    checked = inverted,
                    onCheckedChange = { onInvert() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = scheme.background,
                        checkedTrackColor = scheme.onBackground,
                        checkedBorderColor = Color.Transparent,
                        uncheckedThumbColor = scheme.onSurfaceVariant,
                        uncheckedTrackColor = Color.Transparent,
                        uncheckedBorderColor = scheme.outline
                    )
                )
            }
        )
        ToolRow(
            icon = AppIcons.Fullscreen,
            title = stringResource(R.string.tools_fullscreen),
            subtitle = stringResource(R.string.tools_fullscreen_desc),
            onClick = onImmersive,
            trailing = {
                Switch(
                    checked = immersive,
                    onCheckedChange = { onImmersive() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = scheme.background,
                        checkedTrackColor = scheme.onBackground,
                        checkedBorderColor = Color.Transparent,
                        uncheckedThumbColor = scheme.onSurfaceVariant,
                        uncheckedTrackColor = Color.Transparent,
                        uncheckedBorderColor = scheme.outline
                    )
                )
            }
        )

        Hairline(Modifier.padding(vertical = 10.dp))

        ToolRow(
            icon = AppIcons.IconSet,
            title = stringResource(R.string.tools_iconset),
            subtitle = stringResource(R.string.tools_iconset_desc),
            onClick = onIconSet
        )
        ToolRow(
            icon = AppIcons.Optimize,
            title = stringResource(R.string.tools_optimize),
            subtitle = stringResource(R.string.tools_optimize_desc),
            onClick = onOptimize
        )
        ToolRow(
            icon = AppIcons.Palette,
            title = stringResource(R.string.tools_palette),
            subtitle = stringResource(R.string.tools_palette_desc),
            onClick = onPalette
        )
        ToolRow(
            icon = AppIcons.Health,
            title = stringResource(R.string.tools_health),
            subtitle = stringResource(R.string.tools_health_desc),
            onClick = onHealth
        )
        ToolRow(
            icon = AppIcons.Tag,
            title = stringResource(R.string.tools_snippets),
            subtitle = stringResource(R.string.tools_snippets_desc),
            onClick = onSnippets
        )
        ToolRow(
            icon = AppIcons.Printer,
            title = stringResource(R.string.tools_pdf),
            subtitle = stringResource(R.string.tools_pdf_desc),
            onClick = onPrint
        )

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ToolRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    trailing: (@Composable () -> Unit)? = null
) {
    val scheme = MaterialTheme.colorScheme
    val haptics = LocalHaptics.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 62.dp)
            .clip(RoundedCornerShape(14.dp))
            .tap(role = null) {
                haptics.click()
                onClick()
            }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(scheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = scheme.onSurface,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = scheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(10.dp))
        if (trailing != null) {
            trailing()
        } else {
            Icon(
                imageVector = AppIcons.ChevronRight,
                contentDescription = null,
                tint = scheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

private class IconPreset(@StringRes val titleRes: Int, val sizes: List<Int>)

private val ICON_PRESETS = listOf(
    IconPreset(R.string.iconset_android, listOf(48, 72, 96, 144, 192, 512)),
    IconPreset(R.string.iconset_ios, listOf(20, 40, 60, 120, 180, 1024)),
    IconPreset(R.string.iconset_web, listOf(16, 32, 48, 64, 128, 256, 512))
)

@Composable
private fun IconSetSheet(onExport: (List<Int>, Color?, Int) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val haptics = LocalHaptics.current
    var presetIndex by remember { mutableIntStateOf(0) }
    var background by remember { mutableStateOf<Color?>(null) }
    var padding by remember { mutableIntStateOf(8) }
    val preset = ICON_PRESETS[presetIndex]

    Column(
        Modifier
            .padding(horizontal = 20.dp)
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = stringResource(R.string.iconset_title),
            style = MaterialTheme.typography.titleLarge,
            color = scheme.onSurface
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.iconset_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant
        )

        Spacer(Modifier.height(18.dp))
        SectionLabel(stringResource(R.string.iconset_preset))
        Spacer(Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ICON_PRESETS.forEachIndexed { index, item ->
                val active = index == presetIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 62.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (active) scheme.onBackground else Color.Transparent)
                        .border(
                            width = 1.dp,
                            color = if (active) Color.Transparent else scheme.outline,
                            shape = RoundedCornerShape(14.dp)
                        )
                        .tap(role = null) {
                            haptics.selection()
                            presetIndex = index
                        }
                        .padding(vertical = 10.dp, horizontal = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(item.titleRes),
                            style = MaterialTheme.typography.titleSmall,
                            color = if (active) scheme.background else scheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.iconset_count, item.sizes.size),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (active) scheme.background.copy(alpha = 0.7f)
                            else scheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }
        }

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
                    background = null
                }
            )
            BackgroundOption(
                label = stringResource(R.string.iconset_bg_white),
                selected = background == Color.White,
                swatch = Color.White,
                modifier = Modifier.weight(1f),
                onClick = {
                    haptics.selection()
                    background = Color.White
                }
            )
            BackgroundOption(
                label = stringResource(R.string.iconset_bg_black),
                selected = background == Color.Black,
                swatch = Color.Black,
                modifier = Modifier.weight(1f),
                onClick = {
                    haptics.selection()
                    background = Color.Black
                }
            )
        }

        Spacer(Modifier.height(18.dp))
        SectionLabel(stringResource(R.string.export_padding))
        Spacer(Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(0, 8, 16).forEach { percent ->
                ChoicePill(
                    text = if (percent == 0) stringResource(R.string.export_padding_none)
                    else stringResource(R.string.export_padding_value, percent),
                    selected = padding == percent,
                    onClick = { padding = percent },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        PrimaryAction(
            text = stringResource(R.string.iconset_action, preset.sizes.size),
            icon = AppIcons.Download,
            onClick = { onExport(preset.sizes, background, padding) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.iconset_hint),
            style = MaterialTheme.typography.labelSmall,
            color = scheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun OptimizeSheet(
    result: SvgOptimizer.Result,
    onCopy: () -> Unit,
    onSave: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme

    Column(
        Modifier
            .padding(horizontal = 20.dp)
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = stringResource(R.string.optimize_title),
            style = MaterialTheme.typography.titleLarge,
            color = scheme.onSurface
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.optimize_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant
        )

        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard(
                label = stringResource(R.string.optimize_before),
                value = formatBytes(result.before),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = stringResource(R.string.optimize_after),
                value = formatBytes(result.after),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(14.dp))
        Text(
            text = if (result.changed) {
                stringResource(R.string.optimize_saved, result.percent)
            } else {
                stringResource(R.string.optimize_no_change)
            },
            style = MaterialTheme.typography.titleMedium,
            color = scheme.onSurface
        )

        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SecondaryAction(
                text = stringResource(R.string.optimize_copy),
                icon = AppIcons.Copy,
                onClick = onCopy,
                modifier = Modifier.weight(1f)
            )
            PrimaryAction(
                text = stringResource(R.string.optimize_save),
                icon = AppIcons.Download,
                onClick = onSave,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.optimize_hint),
            style = MaterialTheme.typography.labelSmall,
            color = scheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(scheme.surfaceContainerHigh)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = scheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = scheme.onSurface
        )
    }
}

internal fun colorOf(hex: String): Color =
    runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(Color.Gray)

/** Đọc danh sách màu WebView gửi lên: `[{"hex":"#rrggbb","count":n}, …]`. */
internal fun parseSwatches(json: String): List<SvgPalette.Swatch>? = runCatching {
    val array = JSONArray(json)
    buildList {
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            val hex = item.optString("hex").takeIf { it.length >= 4 } ?: continue
            add(SvgPalette.Swatch(hex.lowercase(), item.optInt("count", 1)))
        }
    }
}.getOrNull()

private fun formatBytes(bytes: Int): String = when {
    bytes >= 1024 * 1024 -> String.format(java.util.Locale.US, "%.2f MB", bytes / 1048576.0)
    bytes >= 1024 -> String.format(java.util.Locale.US, "%.1f KB", bytes / 1024.0)
    else -> "$bytes B"
}

@Composable
private fun ExportSheet(
    document: SvgDocument,
    width: Int,
    height: Int,
    background: Color?,
    lockAspect: Boolean,
    paddingPercent: Int,
    format: ExportFormat,
    quality: Int,
    onWidth: (Int) -> Unit,
    onHeight: (Int) -> Unit,
    onPreset: (Int) -> Unit,
    onLockChange: (Boolean) -> Unit,
    onBackground: (Color?) -> Unit,
    onPadding: (Int) -> Unit,
    onFormat: (ExportFormat) -> Unit,
    onQuality: (Int) -> Unit,
    onPickColor: () -> Unit,
    onExport: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val haptics = LocalHaptics.current
    val valid = width in MIN_SIDE..MAX_SIDE &&
        height in MIN_SIDE..MAX_SIDE &&
        width.toLong() * height.toLong() <= MAX_PIXELS

    Column(
        Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
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
        SectionLabel(stringResource(R.string.export_format))
        Spacer(Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ExportFormat.entries.forEach { item ->
                ChoicePill(
                    text = stringResource(
                        when (item) {
                            ExportFormat.PNG -> R.string.export_format_png
                            ExportFormat.WEBP -> R.string.export_format_webp
                            ExportFormat.JPEG -> R.string.export_format_jpeg
                        }
                    ),
                    selected = format == item,
                    onClick = { onFormat(item) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (format != ExportFormat.PNG) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.export_quality, quality),
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(70, 85, 92, 100).forEach { value ->
                    ChoicePill(
                        text = "$value%",
                        selected = quality == value,
                        onClick = { onQuality(value) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

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

        Spacer(Modifier.height(18.dp))
        SectionLabel(stringResource(R.string.export_padding))
        Spacer(Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(0, 4, 8, 12).forEach { percent ->
                ChoicePill(
                    text = if (percent == 0) stringResource(R.string.export_padding_none)
                    else stringResource(R.string.export_padding_value, percent),
                    selected = paddingPercent == percent,
                    onClick = { onPadding(percent) },
                    modifier = Modifier.weight(1f)
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
private fun InfoDialog(
    document: SvgDocument,
    markup: String,
    onCopy: () -> Unit,
    onDismiss: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) }
        },
        dismissButton = {
            TextButton(onClick = onCopy) {
                Icon(
                    imageVector = AppIcons.Copy,
                    contentDescription = null,
                    tint = scheme.onSurface,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.info_copy_code))
            }
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
                        Regex("<[a-zA-Z]").findAll(markup).count()
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
