package com.khanhminh.svgviewer.ui

import android.annotation.SuppressLint
import android.util.Base64
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.khanhminh.svgviewer.data.RecentFile
import com.khanhminh.svgviewer.render.HtmlComposer
import com.khanhminh.svgviewer.render.SvgBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Bù ảnh thu nhỏ cho những mục trong "Gần đây" chưa có ảnh.
 *
 * Các mục được thêm trước khi có tính năng này (hoặc lần mở trước bị huỷ giữa
 * chừng) sẽ vĩnh viễn trơ ra icon tệp chung chung, vì ảnh chỉ được chụp lúc
 * đang xem. Ở đây dựng lại một WebView ẩn, nạp bản sao SVG đã lưu trong app và
 * chụp từng tệp một — danh sách tự có ảnh mà người dùng không phải mở lại.
 *
 * Chạy một lượt cho mỗi lần vào màn chính, tối đa [MAX_PER_PASS] tệp, để không
 * dựng WebView liên tục khi danh sách dài.
 */
@Composable
fun ThumbnailBackfill(
    recents: List<RecentFile>,
    readCached: (RecentFile) -> String?,
    onThumb: (RecentFile, ByteArray) -> Unit
) {
    val snapshot = remember { recents }

    val jobs by produceState(initialValue = null as List<Job>?, key1 = Unit) {
        value = withContext(Dispatchers.IO) {
            snapshot
                .filter { it.thumbPath.isNullOrBlank() }
                .take(MAX_PER_PASS)
                .mapNotNull { file -> readCached(file)?.let { Job(file, it) } }
        }
    }

    var index by remember { mutableIntStateOf(0) }
    val list = jobs ?: return
    val job = list.getOrNull(index) ?: return

    // key theo uri: mỗi tệp một WebView mới, không lo di sản trạng thái cũ.
    androidx.compose.runtime.key(job.file.uri.toString()) {
        ThumbJob(
            job = job,
            onDone = { bytes ->
                if (bytes != null && bytes.isNotEmpty()) onThumb(job.file, bytes)
                index += 1
            }
        )
    }
}

private class Job(val file: RecentFile, val markup: String)

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun ThumbJob(job: Job, onDone: (ByteArray?) -> Unit) {
    val context = LocalContext.current
    val html = remember(job.markup) { HtmlComposer.compose(context, job.markup) }
    val bridge = remember { SvgBridge() }
    var finished by remember { mutableStateOf(false) }

    fun finish(bytes: ByteArray?) {
        if (finished) return
        finished = true
        onDone(bytes)
    }

    DisposableEffect(bridge) {
        bridge.onThumbnail = { base64, _, _ ->
            val bytes = if (base64.isBlank()) {
                null
            } else {
                runCatching { Base64.decode(base64, Base64.DEFAULT) }.getOrNull()
            }
            finish(bytes)
        }
        onDispose { bridge.onThumbnail = null }
    }

    // Cảnh nặng có thể không bao giờ trả ảnh: bỏ qua để lượt sau thử tệp khác.
    LaunchedEffect(job) {
        delay(9000)
        finish(null)
    }

    Box(
        Modifier
            .size(1.dp)
            .clipToBounds()
    ) {
        AndroidView(
            modifier = Modifier
                .requiredSize(320.dp)
                .graphicsLayer { alpha = 0f },
            factory = { ctx ->
                WebView(ctx).apply {
                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    settings.javaScriptEnabled = true
                    settings.blockNetworkLoads = true
                    settings.allowFileAccess = false
                    settings.allowContentAccess = false
                    settings.javaScriptCanOpenWindowsAutomatically = false
                    addJavascriptInterface(bridge, "Android")
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView, url: String?) {
                            // Chờ vài nhịp cho cảnh do JS dựng kịp vẽ khung đầu.
                            view.postDelayed({
                                view.evaluateJavascript(
                                    "window.SVGViewer && window.SVGViewer.snapshot(320);",
                                    null
                                )
                            }, 900L)
                        }
                    }
                    loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
                }
            },
            onRelease = { web -> runCatching { web.destroy() } }
        )
    }
}

private const val MAX_PER_PASS = 6
