package com.khanhminh.svgviewer.data

import android.content.Intent
import android.net.Uri

/**
 * Nội dung được đưa vào app từ bên ngoài (mở tệp hoặc chia sẻ).
 * [token] đổi mỗi lần nhận intent mới để Compose biết cần nạp lại.
 */
data class IncomingSvg(
    val uri: Uri? = null,
    val text: String? = null,
    val token: Long = System.nanoTime()
) {
    companion object {
        fun fromIntent(intent: Intent?): IncomingSvg? {
            if (intent == null) return null
            return when (intent.action) {
                Intent.ACTION_VIEW -> intent.data?.let { IncomingSvg(uri = it) }

                Intent.ACTION_SEND -> {
                    @Suppress("DEPRECATION")
                    val stream = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                        ?: intent.clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.uri
                    val text = intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()
                    when {
                        stream != null -> IncomingSvg(uri = stream)
                        !text.isNullOrBlank() -> IncomingSvg(text = text)
                        else -> null
                    }
                }

                Intent.ACTION_SEND_MULTIPLE -> {
                    @Suppress("DEPRECATION")
                    val streams = intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
                    streams?.firstOrNull()?.let { IncomingSvg(uri = it) }
                        ?: intent.clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.uri
                            ?.let { IncomingSvg(uri = it) }
                }

                else -> null
            }
        }
    }
}
