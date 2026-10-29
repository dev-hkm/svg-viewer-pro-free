package com.khanhminh.svgviewer.data

import android.net.Uri

/**
 * Một tài liệu SVG đã được nạp vào bộ nhớ, sẵn sàng để render.
 */
data class SvgDocument(
    val uri: Uri?,
    val name: String,
    val markup: String,
    val viewBox: String?,
    val width: Float?,
    val height: Float?,
    val byteSize: Long,
    val origin: Origin
) {
    enum class Origin { FILE, SAMPLE, CLIPBOARD }

    /** Tỉ lệ rộng / cao của khung nhìn, null nếu không xác định được. */
    val aspect: Float?
        get() {
            val vb = viewBox?.trim()?.split(Regex("[\\s,]+"))?.mapNotNull { it.toFloatOrNull() }
            if (vb != null && vb.size == 4 && vb[2] > 0f && vb[3] > 0f) return vb[2] / vb[3]
            if (width != null && height != null && width > 0f && height > 0f) return width / height
            return null
        }

    val dimensionLabel: String
        get() {
            val vb = viewBox?.trim()?.split(Regex("[\\s,]+"))?.mapNotNull { it.toFloatOrNull() }
            if (vb != null && vb.size == 4) {
                return "${fmt(vb[2])} × ${fmt(vb[3])}"
            }
            if (width != null && height != null) return "${fmt(width)} × ${fmt(height)}"
            return ""
        }

    val sizeLabel: String
        get() = when {
            byteSize <= 0 -> "—"
            byteSize < 1024 -> "$byteSize B"
            byteSize < 1024 * 1024 -> String.format("%.1f KB", byteSize / 1024f)
            else -> String.format("%.2f MB", byteSize / (1024f * 1024f))
        }

    private fun fmt(v: Float): String =
        if (v == v.toInt().toFloat()) v.toInt().toString() else String.format("%.1f", v)
}
