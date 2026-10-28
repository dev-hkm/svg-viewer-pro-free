package com.khanhminh.svgviewer.data

import android.content.Context
import android.net.Uri

data class RecentFile(val uri: Uri, val name: String, val timestamp: Long)

/**
 * Danh sách tệp gần đây, lưu trong SharedPreferences dưới dạng văn bản
 * để không cần thêm thư viện serialize.
 */
class RecentStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("recents", Context.MODE_PRIVATE)

    fun list(): List<RecentFile> =
        prefs.getString(KEY, null)
            ?.lineSequence()
            ?.mapNotNull { line ->
                val parts = line.split('\t')
                if (parts.size < 3) null
                else runCatching {
                    RecentFile(Uri.parse(parts[0]), parts[1], parts[2].toLongOrNull() ?: 0L)
                }.getOrNull()
            }
            ?.take(MAX)
            ?.toList()
            .orEmpty()

    fun add(uri: Uri, name: String) {
        val existing = list().filterNot { it.uri == uri }
        val updated = (listOf(RecentFile(uri, name, System.currentTimeMillis())) + existing).take(MAX)
        prefs.edit().putString(KEY, serialize(updated)).apply()
    }

    fun remove(uri: Uri) {
        val updated = list().filterNot { it.uri == uri }
        prefs.edit().putString(KEY, serialize(updated)).apply()
    }

    fun clear() {
        prefs.edit().remove(KEY).apply()
    }

    private fun serialize(items: List<RecentFile>): String =
        items.joinToString("\n") { "${it.uri}\t${it.name}\t${it.timestamp}" }

    private companion object {
        const val KEY = "files"
        const val MAX = 12
    }
}
