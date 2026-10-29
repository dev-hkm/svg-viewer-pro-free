package com.khanhminh.svgviewer.data

import android.content.Context
import android.net.Uri
import java.io.File

data class RecentFile(
    val uri: Uri,
    val name: String,
    val timestamp: Long,
    val cachedPath: String? = null,
    val thumbPath: String? = null,
    val pinned: Boolean = false
)

/**
 * Danh sách tệp gần đây + bản sao nội dung SVG trong bộ nhớ app.
 *
 * Vì quyền đọc Uri có thể mất sau khi tiến trình bị huỷ (hoặc tệp gốc bị xoá/đổi tên),
 * nội dung SVG được lưu lại một bản nên mở lại từ danh sách luôn thành công.
 *
 * Mỗi mục còn có thể kèm:
 *  - thumbPath: ảnh thu nhỏ PNG (chụp từ chính khung hình đang render) để danh sách
 *    hiện đúng nội dung tệp thay vì một icon chung chung;
 *  - pinned: ghim lên đầu danh sách và không bị đẩy ra khi danh sách đầy.
 */
class RecentStore(context: Context) {

    private val app = context.applicationContext
    private val prefs = app.getSharedPreferences("recents", Context.MODE_PRIVATE)
    private val dir = File(app.filesDir, "recents").apply { mkdirs() }

    /** Đã ghim trước, sau đó mới tới thứ tự mở gần nhất. */
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
                        cachedPath = parts.getOrNull(3)?.takeIf { it.isNotBlank() },
                        thumbPath = parts.getOrNull(4)?.takeIf { it.isNotBlank() },
                        pinned = parts.getOrNull(5) == "1"
                    )
                }.getOrNull()
            }
            ?.toList()
            .orEmpty()
            .sortedWith(ORDER)

    /** Thêm tệp vào đầu danh sách, kèm bản sao nội dung để mở lại chắc chắn được. */
    fun add(uri: Uri, name: String, markup: String?) {
        val current = list()
        val old = current.firstOrNull { it.uri == uri }
        val cachePath = markup?.let { writeCache(uri, name, it) } ?: old?.cachedPath
        val existing = current.filterNot { it.uri == uri }
        val fresh = RecentFile(
            uri = uri,
            name = name,
            timestamp = System.currentTimeMillis(),
            cachedPath = cachePath,
            // Giữ ảnh thu nhỏ cũ để danh sách không nhấp nháy trong lúc chờ ảnh mới.
            thumbPath = old?.thumbPath,
            pinned = old?.pinned ?: false
        )
        commit(trim(listOf(fresh) + existing))
    }

    /** Lưu ảnh thu nhỏ (bytes PNG) cho một tệp đã có trong danh sách. */
    fun writeThumb(uri: Uri, png: ByteArray) {
        val current = list()
        val target = current.firstOrNull { it.uri == uri } ?: return
        val path = runCatching {
            val file = File(dir, hashName(uri, target.name) + ".png")
            file.writeBytes(png)
            file.absolutePath
        }.getOrNull() ?: return
        if (path == target.thumbPath) return
        commit(current.map { if (it.uri == uri) it.copy(thumbPath = path) else it })
    }

    fun thumbFile(file: RecentFile): File? {
        val path = file.thumbPath ?: return null
        val f = File(path)
        return if (f.exists() && f.length() > 0L) f else null
    }

    /** Ghim / bỏ ghim. Tệp đã ghim không bị loại khi danh sách vượt hạn mức. */
    fun setPinned(uri: Uri, pinned: Boolean) {
        val current = list()
        if (current.none { it.uri == uri }) return
        commit(trim(current.map { if (it.uri == uri) it.copy(pinned = pinned) else it }))
    }

    /** Đọc lại nội dung SVG đã lưu; null nếu không còn. */
    fun readCached(file: RecentFile): String? {
        val path = file.cachedPath ?: return null
        val f = File(path)
        if (!f.exists()) return null
        return runCatching { f.readText() }.getOrNull()
    }

    fun remove(uri: Uri) {
        commit(list().filterNot { it.uri == uri })
    }

    fun clear() {
        commit(emptyList())
    }

    private fun commit(items: List<RecentFile>) {
        prefs.edit().putString(KEY, serialize(items)).apply()
        pruneCache(items)
    }

    /** Giữ toàn bộ mục đã ghim, phần còn lại cắt theo [MAX]. */
    private fun trim(items: List<RecentFile>): List<RecentFile> {
        val ordered = items.sortedWith(ORDER)
        val pinned = ordered.filter { it.pinned }
        val rest = ordered.filterNot { it.pinned }.take(MAX)
        return (pinned + rest).sortedWith(ORDER)
    }

    private fun hashName(uri: Uri, name: String): String =
        Integer.toHexString((uri.toString() + name).hashCode())

    private fun writeCache(uri: Uri, name: String, markup: String): String? = runCatching {
        val file = File(dir, hashName(uri, name) + ".svg")
        file.writeText(markup)
        file.absolutePath
    }.getOrNull()

    private fun pruneCache(keep: List<RecentFile>) {
        val keepPaths = (keep.mapNotNull { it.cachedPath } + keep.mapNotNull { it.thumbPath }).toSet()
        dir.listFiles()?.forEach { f ->
            if (f.absolutePath !in keepPaths) runCatching { f.delete() }
        }
    }

    private fun serialize(items: List<RecentFile>): String =
        items.joinToString("\n") {
            "${it.uri}\t${it.name}\t${it.timestamp}\t${it.cachedPath.orEmpty()}\t" +
                "${it.thumbPath.orEmpty()}\t${if (it.pinned) "1" else "0"}"
        }

    private companion object {
        const val KEY = "files"
        const val MAX = 12
        val ORDER: Comparator<RecentFile> =
            compareByDescending<RecentFile> { it.pinned }.thenByDescending { it.timestamp }
    }
}
