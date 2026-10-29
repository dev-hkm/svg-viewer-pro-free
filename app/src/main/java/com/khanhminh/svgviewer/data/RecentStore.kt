package com.khanhminh.svgviewer.data

import android.content.Context
import android.net.Uri
import java.io.File

data class RecentFile(
    val uri: Uri,
    val name: String,
    val timestamp: Long,
    val cachedPath: String? = null
)

/**
 * Danh sách tệp gần đây + bản sao nội dung SVG trong bộ nhớ app.
 *
 * Vì quyền đọc Uri có thể mất sau khi tiến trình bị huỷ (hoặc tệp gốc bị xoá/đổi tên),
 * nội dung SVG được lưu lại một bản nên mở lại từ danh sách luôn thành công.
 */
class RecentStore(context: Context) {

    private val app = context.applicationContext
    private val prefs = app.getSharedPreferences("recents", Context.MODE_PRIVATE)
    private val dir = File(app.filesDir, "recents").apply { mkdirs() }

    fun list(): List<RecentFile> =
        prefs.getString(KEY, null)
            ?.lineSequence()
            ?.mapNotNull { line ->
                val parts = line.split('\t')
                if (parts.size < 3) null
                else runCatching {
                    RecentFile(
                        uri = Uri.parse(parts[0]),
                        name = parts[1],
                        timestamp = parts[2].toLongOrNull() ?: 0L,
                        cachedPath = parts.getOrNull(3)?.takeIf { it.isNotBlank() }
                    )
                }.getOrNull()
            }
            ?.take(MAX)
            ?.toList()
            .orEmpty()

    /** Thêm tệp vào đầu danh sách, kèm bản sao nội dung để mở lại chắc chắn được. */
    fun add(uri: Uri, name: String, markup: String?) {
        val cachePath = markup?.let { writeCache(uri, name, it) }
        val existing = list().filterNot { it.uri == uri }
        val updated = (listOf(RecentFile(uri, name, System.currentTimeMillis(), cachePath)) + existing)
            .take(MAX)
        prefs.edit().putString(KEY, serialize(updated)).apply()
        pruneCache(updated)
    }

    /** Đọc lại nội dung SVG đã lưu; null nếu không còn. */
    fun readCached(file: RecentFile): String? {
        val path = file.cachedPath ?: return null
        val f = File(path)
        if (!f.exists()) return null
        return runCatching { f.readText() }.getOrNull()
    }

    fun remove(uri: Uri) {
        val updated = list().filterNot { it.uri == uri }
        prefs.edit().putString(KEY, serialize(updated)).apply()
        pruneCache(updated)
    }

    fun clear() {
        prefs.edit().remove(KEY).apply()
        pruneCache(emptyList())
    }

    private fun writeCache(uri: Uri, name: String, markup: String): String? = runCatching {
        val file = File(dir, Integer.toHexString((uri.toString() + name).hashCode()) + ".svg")
        file.writeText(markup)
        file.absolutePath
    }.getOrNull()

    private fun pruneCache(keep: List<RecentFile>) {
        val keepPaths = keep.mapNotNull { it.cachedPath }.toSet()
        dir.listFiles()?.forEach { f ->
            if (f.absolutePath !in keepPaths) runCatching { f.delete() }
        }
    }

    private fun serialize(items: List<RecentFile>): String =
        items.joinToString("\n") { "${it.uri}\t${it.name}\t${it.timestamp}\t${it.cachedPath.orEmpty()}" }

    private companion object {
        const val KEY = "files"
        const val MAX = 12
    }
}
