package com.khanhminh.svgviewer.data

/**
 * Rút gọn markup SVG mà không đổi hình vẽ.
 *
 * Nguyên tắc an toàn: mọi thứ nằm trong `<script>` được tách ra và giữ nguyên
 * tuyệt đối. Script của các công cụ xuất animation (AI Studio, Figma plugin…)
 * rất dễ vỡ nếu bị regex chạm vào, nên phần còn lại mới là thứ được tối ưu.
 */
object SvgOptimizer {

    data class Result(
        val markup: String,
        val before: Int,
        val after: Int
    ) {
        val saved: Int get() = (before - after).coerceAtLeast(0)
        val percent: Int
            get() = if (before <= 0) 0 else ((before - after) * 100.0 / before).toInt()
        val changed: Boolean get() = after < before
    }

    private val SCRIPT = Regex("""<script\b[\s\S]*?</script>""", RegexOption.IGNORE_CASE)
    private val COMMENT = Regex("""<!--(?!\[if)[\s\S]*?-->""")
    private val METADATA = Regex("""<metadata\b[\s\S]*?</metadata\s*>""", RegexOption.IGNORE_CASE)
    private val EMPTY_GROUP = Regex("""<g\b[^>]*>\s*</g\s*>""", RegexOption.IGNORE_CASE)
    private val BETWEEN_TAGS = Regex(""">\s+<""")
    private val DECIMAL = Regex("""-?\d+\.\d{3,}""")
    private val SPACES = Regex("""[ \t]{2,}""")

    fun optimize(input: String): Result {
        val before = input.toByteArray(Charsets.UTF_8).size
        val scripts = ArrayList<String>()
        var working = SCRIPT.replace(input) { match ->
            scripts.add(match.value)
            "\u0000S${scripts.size - 1}\u0000"
        }

        working = COMMENT.replace(working, "")
        working = METADATA.replace(working, "")
        working = BETWEEN_TAGS.replace(working, "><")
        working = DECIMAL.replace(working) { match -> round(match.value) }
        working = SPACES.replace(working, " ")
        // Nhóm rỗng có thể lộ ra sau khi bỏ metadata/comment, gỡ vài lượt là hết.
        repeat(3) { working = EMPTY_GROUP.replace(working, "") }
        working = working.trim()

        working = Regex("""\u0000S(\d+)\u0000""").replace(working) { match ->
            scripts.getOrElse(match.groupValues[1].toIntOrNull() ?: -1) { "" }
        }

        // Nếu vì lý do nào đó kết quả hỏng, trả lại bản gốc thay vì làm mất hình.
        if (!working.contains("<svg", ignoreCase = true)) {
            return Result(input, before, before)
        }
        return Result(working, before, working.toByteArray(Charsets.UTF_8).size)
    }

    /** 3.14159 → 3.14, 12.500 → 12.5, 4.000 → 4 (bỏ số 0 vô nghĩa). */
    private fun round(raw: String): String {
        val value = raw.toDoubleOrNull() ?: return raw
        val text = String.format(java.util.Locale.US, "%.2f", value)
        return text.trimEnd('0').trimEnd('.')
    }
}
