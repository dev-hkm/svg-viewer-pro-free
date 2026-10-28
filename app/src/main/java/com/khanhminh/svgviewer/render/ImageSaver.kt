package com.khanhminh.svgviewer.render

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.IOException

/**
 * Lưu PNG đã kết xuất vào thư mục ảnh của máy
 * (Pictures/SVG Viewer) để hiện ngay trong thư viện ảnh.
 */
object ImageSaver {

    private const val ALBUM = "SVG Viewer"

    fun savePng(context: Context, source: File, baseName: String, width: Int, height: Int): Uri {
        val fileName = "${sanitize(baseName)}_${width}x${height}_${stamp()}.png"
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveViaMediaStore(context, source, fileName)
        } else {
            saveLegacy(context, source, fileName)
        }
    }

    private fun saveViaMediaStore(context: Context, source: File, fileName: String): Uri {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/$ALBUM")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: throw IOException("Không tạo được mục trong thư viện ảnh")

        try {
            resolver.openOutputStream(uri)?.use { output ->
                source.inputStream().use { input -> input.copyTo(output, 256 * 1024) }
            } ?: throw IOException("Không ghi được vào thư viện ảnh")
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        } catch (t: Throwable) {
            runCatching { resolver.delete(uri, null, null) }
            throw t
        }
        return uri
    }

    @Suppress("DEPRECATION")
    private fun saveLegacy(context: Context, source: File, fileName: String): Uri {
        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), ALBUM)
        if (!dir.exists() && !dir.mkdirs()) throw IOException("Không tạo được thư mục ảnh")
        val dest = File(dir, fileName)
        source.inputStream().use { input ->
            dest.outputStream().use { output -> input.copyTo(output, 256 * 1024) }
        }
        MediaScannerConnection.scanFile(context, arrayOf(dest.absolutePath), arrayOf("image/png"), null)
        return Uri.fromFile(dest)
    }

    private fun sanitize(name: String): String {
        val cleaned = name.substringBeforeLast('.').replace(Regex("[^\\p{L}\\p{N}_-]+"), "_").trim('_')
        return cleaned.ifBlank { "svg" }.take(48)
    }

    private fun stamp(): String {
        val now = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US)
        return now.format(java.util.Date())
    }
}
