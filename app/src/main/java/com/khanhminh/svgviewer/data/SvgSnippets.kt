package com.khanhminh.svgviewer.data

/**
 * Dựng sẵn các dạng "dán được ngay" của một hình SVG: data URI, CSS,
 * thẻ `<img>` và component React. Tất cả đều giữ nguyên nội dung hình vẽ.
 */
object SvgSnippets {

    enum class Kind { DATA_URI, CSS, IMG, REACT }

    fun build(markup: String, kind: Kind): String = when (kind) {
        Kind.DATA_URI -> "data:image/svg+xml," + encode(compact(markup))
        Kind.CSS -> "background-image: url(\"data:image/svg+xml," + encode(compact(markup)) + "\");"
        Kind.IMG -> "<img src=\"data:image/svg+xml," + encode(compact(markup)) + "\" alt=\"\" />"
        Kind.REACT -> react(markup)
    }

    /** Bỏ khai báo XML và comment — không ảnh hưởng hình vẽ — rồi cắt khoảng trắng thừa hai đầu. */
    fun compact(markup: String): String {
        var text = markup.trim()
        text = XML_DECL.replace(text, "")
        text = DOCTYPE.replace(text, "")
        text = HTML_COMMENT.replace(text, "")
        return text.trim()
    }

    private const val SAFE =
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~!$&'()*+,;=:@/?"
    private const val HEX = "0123456789ABCDEF"

    private fun encode(svg: String): String {
        val bytes = svg.toByteArray(Charsets.UTF_8)
        val out = StringBuilder(bytes.size + bytes.size / 3)
        for (byte in bytes) {
            val value = byte.toInt() and 0xFF
            val char = value.toChar()
            if (value < 128 && SAFE.indexOf(char) >= 0) {
                out.append(char)
            } else {
                out.append('%').append(HEX[value shr 4]).append(HEX[value and 0xF])
            }
        }
        return out.toString()
    }

    /* ---------------- React / JSX ---------------- */

    private val XML_DECL = Regex("""<\?xml[^>]*\?>""", RegexOption.IGNORE_CASE)
    private val DOCTYPE = Regex("""<!DOCTYPE[^>]*>""", RegexOption.IGNORE_CASE)
    private val HTML_COMMENT = Regex("""<!--([\s\S]*?)-->""")
    private val STYLE_BLOCK = Regex("""<style\b([^>]*)>([\s\S]*?)</style\s*>""", RegexOption.IGNORE_CASE)
    private val SCRIPT_BLOCK = Regex("""<script\b([^>]*)>([\s\S]*?)</script\s*>""", RegexOption.IGNORE_CASE)
    private val TAG = Regex("""<([A-Za-z][\w:.-]*)((?:[^>"']|"[^"]*"|'[^']*')*)>""")
    private val ATTR = Regex("""(\s+)([A-Za-z_:][\w:.-]*)(\s*=\s*)("[^"]*"|'[^']*')""")
    private val PLACEHOLDER = Regex("""\u0000S(\d+)\u0000""")
    private val NUMBER = Regex("""-?\d+(?:\.\d+)?""")

    /**
     * Component React dùng được ngay: thuộc tính đổi sang camelCase, `style`
     * thành object, `<style>`/`<script>` thành chuỗi template literal, và
     * phần tử gốc nhận `{...props}` để đổi kích thước từ bên ngoài.
     */
    fun react(markup: String): String {
        if (!markup.contains("<svg", ignoreCase = true)) return markup.trim()
        val blocks = ArrayList<String>()
        var body = markup.trim()

        body = STYLE_BLOCK.replace(body) { match ->
            blocks += "<style${match.groupValues[1]}>{" + template(match.groupValues[2]) + "}</style>"
            "\u0000S${blocks.size - 1}\u0000"
        }
        body = SCRIPT_BLOCK.replace(body) { match ->
            blocks += "<script${match.groupValues[1]}>{" + template(match.groupValues[2]) + "}</script>"
            "\u0000S${blocks.size - 1}\u0000"
        }
        body = HTML_COMMENT.replace(body) { match ->
            "{/*" + match.groupValues[1].replace("*/", "*\\/") + "*/}"
        }
        body = TAG.replace(body) { match ->
            val attrs = ATTR.replace(match.groupValues[2]) { attr ->
                val name = attr.groupValues[2]
                val raw = attr.groupValues[4]
                if (name.equals("style", ignoreCase = true)) {
                    " style={{ ${styleObject(raw)} }}"
                } else {
                    " ${reactName(name)}=${quote(raw)}"
                }
            }
            "<${match.groupValues[1]}$attrs>"
        }
        body = body.replaceFirst(Regex("""<svg\b"""), "<svg {...props}")
        body = PLACEHOLDER.replace(body) { match ->
            blocks.getOrElse(match.groupValues[1].toIntOrNull() ?: -1) { "" }
        }

        // Xuống dòng giữa hai thẻ liền nhau: không chạm vào text nằm trong thẻ.
        val lines = body.replace("><", ">\n<")
            .lineSequence()
            .filter { it.isNotBlank() }
            .joinToString("\n") { "    $it" }

        return "import type { SVGProps } from \"react\";\n\n" +
            "export function Icon(props: SVGProps<SVGSVGElement>) {\n" +
            "  return (\n" +
            lines +
            "\n  );\n}\n"
    }

    private fun template(text: String): String {
        val escaped = text
            .replace("\\", "\\\\")
            .replace("`", "\\`")
            .replace("\${", "\\\${")
        return "`$escaped`"
    }

    private fun quote(raw: String): String {
        val inner = raw.substring(1, raw.length - 1)
        return if (inner.contains('"')) {
            "{\"" + inner.replace("\"", "\\\"") + "\"}"
        } else {
            "\"$inner\""
        }
    }

    private fun reactName(name: String): String {
        if (name.startsWith("aria-") || name.startsWith("data-")) return name
        if (name == "class") return "className"
        if (name == "for") return "htmlFor"
        if (name.contains(':')) {
            val parts = name.split(':')
            return parts.first() + parts.drop(1)
                .joinToString("") { it.replaceFirstChar { ch -> ch.uppercaseChar() } }
        }
        if (!name.contains('-')) return name
        return name.split('-')
            .filter { it.isNotEmpty() }
            .mapIndexed { index, part ->
                if (index == 0) part else part.replaceFirstChar { ch -> ch.uppercaseChar() }
            }
            .joinToString("")
    }

    private fun styleObject(raw: String): String {
        val inner = raw.substring(1, raw.length - 1)
        val declarations = inner.split(';').mapNotNull { segment ->
            val colon = segment.indexOf(':')
            if (colon <= 0) return@mapNotNull null
            val key = segment.substring(0, colon).trim()
            val value = segment.substring(colon + 1).trim()
            if (key.isEmpty() || value.isEmpty()) return@mapNotNull null
            val rendered = if (NUMBER.matches(value)) value
            else "\"" + value.replace("\"", "\\\"") + "\""
            reactName(key) to rendered
        }
        return declarations.joinToString(", ") { "${it.first}: ${it.second}" }
    }
}
