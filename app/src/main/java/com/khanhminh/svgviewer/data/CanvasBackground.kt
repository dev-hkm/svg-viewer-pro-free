package com.khanhminh.svgviewer.data

import androidx.annotation.StringRes
import com.khanhminh.svgviewer.R

/**
 * Các loại nền canvas của màn xem.
 * [isLight] dùng để quyết định màu icon status bar cho tương phản.
 */
enum class CanvasBackground(
    @StringRes val labelRes: Int,
    val checker: Boolean,
    val argb: Long,
    val isLight: Boolean
) {
    CHECKER_LIGHT(R.string.canvas_checker_light, checker = true, argb = 0xFFF3F3F3, isLight = true),
    CHECKER_DARK(R.string.canvas_checker_dark, checker = true, argb = 0xFF171717, isLight = false),
    WHITE(R.string.canvas_white, checker = false, argb = 0xFFFFFFFF, isLight = true),
    CREAM(R.string.canvas_cream, checker = false, argb = 0xFFF4EFE6, isLight = true),
    GRAY(R.string.canvas_gray, checker = false, argb = 0xFF9A9A9A, isLight = true),
    BLACK(R.string.canvas_black, checker = false, argb = 0xFF000000, isLight = false),
    SLATE(R.string.canvas_slate, checker = false, argb = 0xFF141821, isLight = false),
    INK(R.string.canvas_ink, checker = false, argb = 0xFF0F172A, isLight = false);

    companion object {
        fun fromName(name: String?): CanvasBackground? =
            entries.firstOrNull { it.name == name }

        /** Giữ tương thích với bản cũ lưu theo ordinal: 0 caro, 1 nền sáng, 2 nền tối. */
        fun fromLegacyOrdinal(ordinal: Int): CanvasBackground = when (ordinal) {
            1 -> WHITE
            2 -> BLACK
            else -> CHECKER_LIGHT
        }
    }
}
