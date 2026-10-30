package com.khanhminh.svgviewer.render

import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface

/**
 * Cầu nối JS -> Kotlin. Mọi callback UI được đẩy về main thread,
 * riêng dữ liệu ảnh được ghi trực tiếp trên thread của WebView.
 */
class SvgBridge {

    var onReady: ((Float, Float) -> Unit)? = null
    var onZoom: ((Float) -> Unit)? = null
    var onError: ((String) -> Unit)? = null
    var onProgress: ((Int) -> Unit)? = null
    var onExportDone: ((Int, Int) -> Unit)? = null
    var onExportError: ((String, String) -> Unit)? = null
    var onReload: (() -> Unit)? = null
    var onThumbnail: ((String, Int, Int) -> Unit)? = null
    var onTap: (() -> Unit)? = null
    var onInspect: ((String) -> Unit)? = null
    var onColors: ((String) -> Unit)? = null
    var onRecolor: ((Int, String, String) -> Unit)? = null
    var onRecolorReset: ((Int) -> Unit)? = null
    var onHiddenCleared: ((Int) -> Unit)? = null
    var chunkSink: ((String) -> Unit)? = null

    private val main = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun ready(width: Float, height: Float) {
        main.post { onReady?.invoke(width, height) }
    }

    @JavascriptInterface
    fun zoom(scale: Float) {
        main.post { onZoom?.invoke(scale) }
    }

    @JavascriptInterface
    fun error(message: String) {
        main.post { onError?.invoke(message) }
    }

    @JavascriptInterface
    fun progress(percent: Int) {
        main.post { onProgress?.invoke(percent) }
    }

    @JavascriptInterface
    fun chunk(data: String) {
        chunkSink?.invoke(data)
    }

    @JavascriptInterface
    fun exportDone(width: Int, height: Int) {
        main.post { onExportDone?.invoke(width, height) }
    }

    @JavascriptInterface
    fun exportError(code: String, detail: String) {
        main.post { onExportError?.invoke(code, detail) }
    }

    /** Ảnh thu nhỏ dạng base64 (rỗng nếu không dựng được) kèm kích thước thật. */
    @JavascriptInterface
    fun thumbnail(base64: String, width: Int, height: Int) {
        main.post { onThumbnail?.invoke(base64, width, height) }
    }

    /** SVG yêu cầu chạy lại từ đầu: nạp lại tài liệu để script khởi tạo lại sạch. */
    @JavascriptInterface
    fun reload() {
        main.post { onReload?.invoke() }
    }

    /** Một cú chạm đơn trên khung vẽ (đã tách khỏi chạm đôi để phóng to). */
    @JavascriptInterface
    fun tap() {
        main.post { onTap?.invoke() }
    }

    /** Phần tử đang được chọn trong chế độ soi, dạng JSON. */
    @JavascriptInterface
    fun inspect(json: String) {
        main.post { onInspect?.invoke(json) }
    }

    /** Danh sách màu đang dùng trong hình, dạng JSON [{hex,count}]. */
    @JavascriptInterface
    fun colors(json: String) {
        main.post { onColors?.invoke(json) }
    }

    /** Kết quả đổi màu; total = 0 nghĩa là vừa hoàn tác. */
    @JavascriptInterface
    fun recolor(total: Int, from: String, to: String) {
        main.post { onRecolor?.invoke(total, from, to) }
    }

    @JavascriptInterface
    fun recolorReset(steps: Int) {
        main.post { onRecolorReset?.invoke(steps) }
    }

    @JavascriptInterface
    fun hiddenCleared(count: Int) {
        main.post { onHiddenCleared?.invoke(count) }
    }
}
