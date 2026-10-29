package com.khanhminh.svgviewer.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.zip.GZIPInputStream

/**
 * Nạp SVG từ Uri, assets hoặc clipboard và chuẩn hoá lại markup
 * để nhúng an toàn vào WebView.
 */
object SvgLoader {

    private const val MAX_BYTES = 24 * 1024 * 1024

    private val XML_DECL = Regex("""<\?xml[^>]*\?>""", RegexOption.IGNORE_CASE)
    private val DOCTYPE = Regex("""<!DOCTYPE[^>]*>""", RegexOption.IGNORE_CASE)
    private val SCRIPT = Regex("""<script[\s\S]*?</script>""", RegexOption.IGNORE_CASE)
    private val EVENT_ATTR = Regex("""\son[a-zA-Z]+\s*=\s*("[^"]*"|'[^']*')""")
    private val VIEWBOX = Regex("""viewBox\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
    private val WIDTH = Regex("""\swidth\s*=\s*["']([\d.]+)""", RegexOption.IGNORE_CASE)
    private val HEIGHT = Regex("""\sheight\s*=\s*["']([\d.]+)""", RegexOption.IGNORE_CASE)

    fun fromUri(context: Context, uri: Uri): SvgDocument {
        val name = queryDisplayName(context, uri) ?: uri.lastPathSegment ?: "image.svg"
        val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
            val buffer = ByteArrayOutputStream()
            val chunk = ByteArray(64 * 1024)
            var total = 0L
            while (true) {
                val read = input.read(chunk)
                if (read <= 0) break
                total += read
                if (total > MAX_BYTES) throw IOException("Tệp quá lớn (giới hạn 24 MB)")
                buffer.write(chunk, 0, read)
            }
            buffer.toByteArray()
        } ?: throw IOException("Không mở được tệp")

        val payload = if (isGzip(bytes) || name.endsWith(".svgz", true)) gunzip(bytes) else bytes
        val markup = payload.toString(Charsets.UTF_8)
        return build(uri, name, markup, payload.size.toLong(), SvgDocument.Origin.FILE)
    }

    fun fromAsset(context: Context, assetName: String, displayName: String): SvgDocument {
        val markup = context.assets.open(assetName).use { it.readBytes().toString(Charsets.UTF_8) }
        return build(null, displayName, markup, markup.toByteArray().size.toLong(), SvgDocument.Origin.SAMPLE)
    }

    fun fromText(text: String, displayName: String): SvgDocument? {
        if (!text.contains("<svg", ignoreCase = true)) return null
        return build(null, displayName, text, text.toByteArray().size.toLong(), SvgDocument.Origin.CLIPBOARD)
    }

    private fun build(
        uri: Uri?,
        name: String,
        rawMarkup: String,
        byteSize: Long,
        origin: SvgDocument.Origin
    ): SvgDocument {
        val markup = sanitize(rawMarkup)
        if (!markup.contains("<svg", ignoreCase = true)) {
            throw IOException("Tệp này không chứa nội dung SVG")
        }
        val viewBox = VIEWBOX.find(markup)?.groupValues?.get(1)?.trim()
        val w = WIDTH.find(markup)?.groupValues?.get(1)?.toFloatOrNull()
        val h = HEIGHT.find(markup)?.groupValues?.get(1)?.toFloatOrNull()
        return SvgDocument(
            uri = uri,
            name = name,
            markup = markup,
            viewBox = viewBox,
            width = w,
            height = h,
            byteSize = byteSize,
            origin = origin
        )
    }

    private fun sanitize(input: String): String {
        var markup = input.removePrefix("\uFEFF").trim()
        markup = XML_DECL.replace(markup, "")
        markup = DOCTYPE.replace(markup, "")
        markup = SCRIPT.replace(markup, "")
        markup = EVENT_ATTR.replace(markup, "")
        return markup.trim()
    }

    private fun isGzip(bytes: ByteArray): Boolean =
        bytes.size > 2 && bytes[0] == 0x1f.toByte() && bytes[1] == 0x8b.toByte()

    private fun gunzip(bytes: ByteArray): ByteArray =
        GZIPInputStream(bytes.inputStream()).use { it.readBytes() }

    private fun queryDisplayName(context: Context, uri: Uri): String? {
        if (uri.scheme == "file") return uri.lastPathSegment
        return runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (index >= 0) cursor.getString(index) else null
                    } else null
                }
        }.getOrNull()
    }
}
