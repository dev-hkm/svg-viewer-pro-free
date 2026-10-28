package com.khanhminh.svgviewer.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class ThemeMode(val label: String) {
    SYSTEM("Theo hệ thống"),
    LIGHT("Sáng"),
    DARK("Tối")
}

enum class CanvasBackground(val label: String) {
    CHECKER("Trong suốt"),
    LIGHT("Nền sáng"),
    DARK("Nền tối")
}

/**
 * Tuỳ chọn hiển thị của ứng dụng, lưu bền qua SharedPreferences
 * nhưng expose dưới dạng State để Compose tự cập nhật.
 */
class SettingsStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var themeMode by mutableStateOf(
        ThemeMode.entries.getOrElse(prefs.getInt(KEY_THEME, 0)) { ThemeMode.SYSTEM }
    )
        private set

    var dynamicColor by mutableStateOf(prefs.getBoolean(KEY_DYNAMIC, false))
        private set

    var background by mutableStateOf(
        CanvasBackground.entries.getOrElse(prefs.getInt(KEY_BG, 0)) { CanvasBackground.CHECKER }
    )
        private set

    fun updateThemeMode(mode: ThemeMode) {
        themeMode = mode
        prefs.edit().putInt(KEY_THEME, mode.ordinal).apply()
    }

    fun updateDynamicColor(enabled: Boolean) {
        dynamicColor = enabled
        prefs.edit().putBoolean(KEY_DYNAMIC, enabled).apply()
    }

    fun updateBackground(bg: CanvasBackground) {
        background = bg
        prefs.edit().putInt(KEY_BG, bg.ordinal).apply()
    }

    private companion object {
        const val KEY_THEME = "theme_mode"
        const val KEY_DYNAMIC = "dynamic_color"
        const val KEY_BG = "canvas_background"
    }
}
