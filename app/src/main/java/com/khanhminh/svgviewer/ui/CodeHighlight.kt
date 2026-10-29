package com.khanhminh.svgviewer.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight

/** Bảng màu tô sáng mã, chọn theo nền sáng/tối để luôn đủ tương phản. */
data class CodePalette(
    val tag: Color,
    val attr: Color,
    val string: Color,
    val punctuation: Color,
    val comment: Color,
    val number: Color,
    val plain: Color
)

fun codePalette(dark: Boolean): CodePalette = if (dark) {
    CodePalette(
        tag = Color(0xFF7AA2F7),
        attr = Color(0xFFE0AF68),
        string = Color(0xFF9ECE6A),
        punctuation = Color(0xFF7C8296),
        comment = Color(0xFF6E7681),
        number = Color(0xFFBB9AF7),
        plain = Color(0xFFD5D8E0)
    )
} else {
    CodePalette(
        tag = Color(0xFF1F5FD8),
        attr = Color(0xFF9A5B00),
        string = Color(0xFF2E7D32),
        punctuation = Color(0xFF8A8A8A),
        comment = Color(0xFF8C8C8C),
        number = Color(0xFF7A3DB8),
        plain = Color(0xFF1F2228)
    )
}

private val TOKEN = Regex(
    "<!--[\\s\\S]*?-->" +
        "|<!\\[CDATA\\[[\\s\\S]*?\\]\\]>" +
        "|<\\?[\\s\\S]*?\\?>" +
        "|</?[A-Za-z_][\\w:.\\-]*" +
        "|/?>" +
        "|\\s[\\w:.\\-]+\\s*=" +
        "|\"[^\"]*\"" +
        "|'[^']*'"
)

private const val MAX_HIGHLIGHT = 24_000

/**
 * Tô màu markup SVG: tên thẻ, thuộc tính, giá trị, comment, dấu câu.
 * Không đổi nội dung chuỗi nên dùng được trực tiếp trong VisualTransformation.
 */
fun highlightSvg(code: String, dark: Boolean): AnnotatedString {
    val palette = codePalette(dark)
    if (code.isEmpty()) return AnnotatedString("")
    if (code.length > MAX_HIGHLIGHT) {
        return AnnotatedString(code)
    }

    return buildAnnotatedString {
        var cursor = 0
        fun plain(end: Int) {
            if (end > cursor) {
                pushStyle(SpanStyle(color = palette.plain))
                append(code.substring(cursor, end))
                pop()
                cursor = end
            }
        }

        for (match in TOKEN.findAll(code)) {
            plain(match.range.first)
            val token = match.value
            val style = when {
                token.startsWith("<!--") || token.startsWith("<![CDATA[") || token.startsWith("<?") ->
                    SpanStyle(color = palette.comment, fontStyle = FontStyle.Italic)

                token.startsWith("</") || token.startsWith("<") ->
                    SpanStyle(color = palette.tag, fontWeight = FontWeight.Medium)

                token == ">" || token == "/>" || token == "/" ->
                    SpanStyle(color = palette.punctuation)

                token.trimStart().startsWith("\"") || token.trimStart().startsWith("'") ->
                    SpanStyle(color = palette.string)

                token.contains("=") -> SpanStyle(color = palette.attr)

                else -> SpanStyle(color = palette.plain)
            }
            pushStyle(style)
            append(token)
            pop()
            cursor = match.range.last + 1
        }
        plain(code.length)
    }
}
