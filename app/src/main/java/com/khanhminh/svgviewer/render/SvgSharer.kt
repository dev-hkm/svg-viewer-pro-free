package com.khanhminh.svgviewer.render

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.khanhminh.svgviewer.R
import java.io.File

/**
 * Chia sẻ tệp .svg thật qua FileProvider. Nội dung được ghi vào cache của app
 * với đúng tên tệp, nên app nhận (Drive, Gmail, Telegram…) thấy một tệp SVG
 * bình thường thay vì một chuỗi văn bản.
 */
object SvgSharer {

    fun share(context: Context, markup: String, name: String): Boolean = runCatching {
        val dir = File(context.cacheDir, "share").apply { mkdirs() }
        val file = File(dir, fileName(name))
        file.writeText(markup, Charsets.UTF_8)

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/svg+xml"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TITLE, file.name)
            clipData = ClipData.newRawUri(file.name, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(send, context.getString(R.string.share_chooser))
        )
    }.isSuccess

    private fun fileName(name: String): String {
        val base = ImageSaver.sanitize(name)
        return if (base.endsWith(".svg", ignoreCase = true)) base else "$base.svg"
    }
}
