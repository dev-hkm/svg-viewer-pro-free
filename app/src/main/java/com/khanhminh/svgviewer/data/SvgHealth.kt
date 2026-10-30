package com.khanhminh.svgviewer.data

/**
 * Soi markup SVG để chỉ ra những điểm thường làm tệp hiển thị sai khi gửi sang
 * máy khác, in ấn hoặc đưa lên web. Chỉ đọc và đếm — không sửa nội dung.
 */
object SvgHealth {

    enum class Level { WARN, INFO }

    enum class Kind {
        VIEWBOX, DIMENSIONS, ACCESSIBILITY, SCRIPT, EXTERNAL,
        EMBEDDED, TEXT, FILTER, DUPLICATE_IDS, FOREIGN, SIZE, ANIMATION
    }

    data class Check(val kind: Kind, val level: Level, val count: Int = 0)

    data class Report(val score: Int, val checks: List<Check>) {
        val warnings: Int get() = checks.count { it.level == Level.WARN }
        val passed: Boolean get() = checks.isEmpty()
    }

    private val ROOT_SVG = Regex("""<svg\b[^>]*>""", RegexOption.IGNORE_CASE)
    private val SIZE_ATTR = Regex("""\s(?:width|height)\s*=""", RegexOption.IGNORE_CASE)
    private val ARIA = Regex("""\baria-(?:label|labelledby|hidden)\s*=""", RegexOption.IGNORE_CASE)
    private val TITLE_TAG = Regex("""<title\b""", RegexOption.IGNORE_CASE)
    private val SCRIPT_TAG = Regex("""<script\b""", RegexOption.IGNORE_CASE)
    private val IMAGE_TAG = Regex("""<image\b""", RegexOption.IGNORE_CASE)
    private val TEXT_TAG = Regex("""<text\b""", RegexOption.IGNORE_CASE)
    private val FILTER_TAG = Regex("""<filter\b""", RegexOption.IGNORE_CASE)
    private val FOREIGN_TAG = Regex("""<foreignObject\b""", RegexOption.IGNORE_CASE)
    private val ANIM_TAG = Regex("""<animate(?:Motion|Transform|Color)?\b""", RegexOption.IGNORE_CASE)
    private val KEYFRAMES = Regex("""@keyframes\b""", RegexOption.IGNORE_CASE)
    private val ID_ATTR = Regex("""\sid\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
    private val FONT_FAMILY = Regex("""font-family\s*[:=]""", RegexOption.IGNORE_CASE)

    /** Chỉ tính là "ra ngoài" khi thật sự trỏ URL, không bắt nhầm xmlns. */
    private val EXTERNAL = Regex(
        """(?:\b(?:xlink:)?href\s*=\s*["']\s*(?:https?:)?//|url\(\s*["']?(?:https?:)?//)""",
        RegexOption.IGNORE_CASE
    )

    fun analyze(markup: String): Report {
        val checks = ArrayList<Check>(10)
        val root = ROOT_SVG.find(markup)?.value.orEmpty()

        if (!root.contains("viewBox", ignoreCase = true)) {
            checks += Check(Kind.VIEWBOX, Level.WARN)
        }
        if (!SIZE_ATTR.containsMatchIn(root)) {
            checks += Check(Kind.DIMENSIONS, Level.INFO)
        }
        if (!ARIA.containsMatchIn(root) && !TITLE_TAG.containsMatchIn(markup)) {
            checks += Check(Kind.ACCESSIBILITY, Level.WARN)
        }

        count(SCRIPT_TAG, markup).takeIf { it > 0 }?.let {
            checks += Check(Kind.SCRIPT, Level.INFO, it)
        }
        count(EXTERNAL, markup).takeIf { it > 0 }?.let {
            checks += Check(Kind.EXTERNAL, Level.WARN, it)
        }
        count(IMAGE_TAG, markup).takeIf { it > 0 }?.let {
            checks += Check(Kind.EMBEDDED, Level.INFO, it)
        }
        val textCount = count(TEXT_TAG, markup)
        if (textCount > 0 && !FONT_FAMILY.containsMatchIn(markup)) {
            checks += Check(Kind.TEXT, Level.WARN, textCount)
        }
        count(FILTER_TAG, markup).takeIf { it > 0 }?.let {
            checks += Check(Kind.FILTER, Level.INFO, it)
        }
        duplicateIds(markup).takeIf { it > 0 }?.let {
            checks += Check(Kind.DUPLICATE_IDS, Level.WARN, it)
        }
        count(FOREIGN_TAG, markup).takeIf { it > 0 }?.let {
            checks += Check(Kind.FOREIGN, Level.WARN, it)
        }
        val animations = count(ANIM_TAG, markup) + if (KEYFRAMES.containsMatchIn(markup)) 1 else 0
        if (animations > 0) {
            checks += Check(Kind.ANIMATION, Level.INFO, animations)
        }

        val bytes = if (markup.length > 1_000_000) {
            markup.toByteArray(Charsets.UTF_8).size.toLong()
        } else {
            markup.length.toLong()
        }
        if (bytes > 4L * 1024 * 1024) {
            checks += Check(Kind.SIZE, Level.WARN)
        } else if (bytes > 1024L * 1024) {
            checks += Check(Kind.SIZE, Level.INFO)
        }

        val ordered = checks.sortedWith(compareBy<Check> { it.level.ordinal }.thenBy { it.kind.ordinal })
        val warns = ordered.count { it.level == Level.WARN }
        val infos = ordered.count { it.level == Level.INFO }
        val score = (100 - warns * 14 - infos * 4).coerceIn(0, 100)
        return Report(score, ordered)
    }

    private fun count(pattern: Regex, text: String): Int = pattern.findAll(text).count()

    private fun duplicateIds(markup: String): Int {
        val seen = HashSet<String>()
        val repeated = HashSet<String>()
        ID_ATTR.findAll(markup).forEach { match ->
            val id = match.groupValues[1]
            if (!seen.add(id)) repeated.add(id)
        }
        return repeated.size
    }
}
