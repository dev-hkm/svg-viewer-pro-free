package com.khanhminh.svgviewer.render

import android.util.Base64
import android.util.Log
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream

/**
 * Nhận dữ liệu base64 theo từng khối từ WebView và ghi thẳng ra tệp PNG tạm.
 * Tránh phải giữ chuỗi base64 khổng lồ (ảnh 8K) trong bộ nhớ Java.
 */
class ExportSession(val target: File) {

    private val stream = BufferedOutputStream(FileOutputStream(target), 1 shl 16)
    private var closed = false
    private var written = 0L

    @Synchronized
    fun append(chunk: String) {
        if (closed) return
        try {
            val bytes = Base64.decode(chunk, Base64.DEFAULT)
            stream.write(bytes)
            written += bytes.size
        } catch (t: Throwable) {
            Log.w(TAG, "Bỏ qua một khối dữ liệu ảnh không hợp lệ", t)
        }
    }

    @Synchronized
    fun close() {
        if (closed) return
        closed = true
        runCatching { stream.flush() }
        runCatching { stream.close() }
    }

    fun byteCount(): Long = written

    fun discard() {
        close()
        target.delete()
    }

    private companion object {
        const val TAG = "ExportSession"
    }
}
