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
 * Ghi ảnh đã kết xuất vào thư mục ảnh của máy (Pictures/SVG Viewer) và ghi
 * tệp văn bản (SVG đã tối ưu) vào Download/SVG Viewer.
 */
object ImageSaver {

    private const val ALBUM = "SVG Viewer"

    /** Lưu một tệp ảnh bất kỳ (png / webp / jpg) vào thư viện ảnh. */
    fun saveImage(
        context: Context,
        source: File,
        fileName: String,
        mime: String,
        album: String = ALBUM
    ): Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        saveImageMediaStore(context, source, fileName, mime, album)
    } else {
        saveImageLegacy(context, source, fileName, mime, album)
    }

    fun savePng(context: Context, source: File, baseName: String, width: Int, height: Int): Uri =
        saveImage(
            context = context,
            source = source,
            fileName = "${sanitize(baseName)}_${width}x${height}_${stamp()}.png",
            mime = "image/png"
        )

    /** Lưu một tệp văn bản (thường là .svg) vào Download/SVG Viewer. */
    fun saveText(context: Context, text: String, fileName: String, mime: String): Uri =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, mime)
                put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/$ALBUM")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: throw IOException("Không tạo được tệp trong thư mục Download")
            try {
                resolver.openOutputStream(uri)?.use { out ->
                    out.write(text.toByteArray(Charsets.UTF_8))
                } ?: throw IOException("Không ghi được vào thư mục Download")
                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            } catch (t: Throwable) {
                runCatching { resolver.delete(uri, null, null) }
                throw t
            }
            uri
        } else {
            @Suppress("DEPRECATION")
            val dir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                ALBUM
            )
            if (!dir.exists() && !dir.mkdirs()) throw IOException("Không tạo được thư mục Download")
            val dest = File(dir, fileName)
            dest.writeText(text, Charsets.UTF_8)
            MediaScannerConnection.scanFile(context, arrayOf(dest.absolutePath), arrayOf(mime), null)
            Uri.fromFile(dest)
        }

    private fun saveImageMediaStore(
        context: Context,
        source: File,
        fileName: String,
        mime: String,
        album: String
    ): Uri {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, mime)
            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/$album")
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
    private fun saveImageLegacy(
        context: Context,
        source: File,
        fileName: String,
        mime: String,
        album: String
    ): Uri {
        val dir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            album
        )
        if (!dir.exists() && !dir.mkdirs()) throw IOException("Không tạo được thư mục ảnh")
        val dest = File(dir, fileName)
        source.inputStream().use { input ->
            dest.outputStream().use { output -> input.copyTo(output, 256 * 1024) }
        }
        MediaScannerConnection.scanFile(context, arrayOf(dest.absolutePath), arrayOf(mime), null)
        return Uri.fromFile(dest)
    }

    fun sanitize(name: String): String {
        val cleaned = name.substringBeforeLast('.').replace(Regex("[^\\p{L}\\p{N}_-]+"), "_").trim('_')
        return cleaned.ifBlank { "svg" }.take(48)
    }

    fun stamp(): String {
        val now = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US)
        return now.format(java.util.Date())
    }
}
