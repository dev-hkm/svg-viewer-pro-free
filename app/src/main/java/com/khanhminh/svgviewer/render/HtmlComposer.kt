package com.khanhminh.svgviewer.render

import android.content.Context

/**
 * Ghép markup SVG vào khung HTML của WebView.
 * Dùng chèn trực tiếp vào DOM thay vì nối chuỗi JavaScript
 * để không phải escape nội dung SVG.
 */
object HtmlComposer {

    private const val PLACEHOLDER = "<!--SVG_CONTENT-->"
    private var cachedTemplate: String? = null

    fun compose(context: Context, markup: String): String {
        val template = cachedTemplate ?: context.assets
            .open("viewer.html")
            .use { it.readBytes().toString(Charsets.UTF_8) }
            .also { cachedTemplate = it }
        return template.replace(PLACEHOLDER, markup)
    }
}
