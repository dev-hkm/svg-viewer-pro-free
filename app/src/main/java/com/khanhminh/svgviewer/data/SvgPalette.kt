package com.khanhminh.svgviewer.data

/**
 * Bóc các màu đang dùng trong bản vẽ để người thiết kế chép lại mã HEX.
 *
 * Chỉ đọc những chỗ thực sự mang màu (`fill`, `stroke`, `stop-color`, `color`,
 * `flood-color`, `lighting-color`, thuộc tính `style`), nhờ vậy bảng màu không
 * bị nhiễu bởi các chuỗi tình cờ trông giống mã màu trong `<text>` hay `<script>`.
 */
object SvgPalette {

    data class Swatch(val hex: String, val count: Int)

    private val COLOR_ATTR = Regex(
        """(?:fill|stroke|stop-color|color|flood-color|lighting-color|solid-color)\s*[:=]\s*["']?\s*(#[0-9a-fA-F]{3,8}|rgba?\([^)]*\))""",
        RegexOption.IGNORE_CASE
    )

    fun extract(markup: String, limit: Int = 24): List<Swatch> {
        val body = markup.replace(Regex("""<script\b[\s\S]*?</script>""", RegexOption.IGNORE_CASE), "")
        val counts = LinkedHashMap<String, Int>()
        COLOR_ATTR.findAll(body).forEach { match ->
            val hex = normalize(match.groupValues[1]) ?: return@forEach
            counts[hex] = (counts[hex] ?: 0) + 1
        }
        return counts.entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .take(limit)
            .map { Swatch(it.key, it.value) }
    }

    /** #abc → #aabbcc, rgb(r,g,b) → #rrggbb; giá trị không hợp lệ trả về null. */
    private fun normalize(raw: String): String? {
        val value = raw.trim()
        if (value.startsWith("#")) {
            val digits = value.drop(1)
            return when (digits.length) {
                3 -> "#" + digits.map { "$it$it" }.joinToString("")
                4 -> "#" + digits.take(3).map { "$it$it" }.joinToString("")
                6 -> "#${digits.lowercase()}"
                8 -> "#${digits.take(6).lowercase()}"
                else -> null
            }
        }
        val parts = value.substringAfter("(").substringBefore(")").split(",")
        if (parts.size < 3) return null
        val rgb = parts.take(3).map { it.trim().toFloatOrNull()?.toInt()?.coerceIn(0, 255) ?: return null }
        return String.format(java.util.Locale.US, "#%02x%02x%02x", rgb[0], rgb[1], rgb[2])
    }
}
