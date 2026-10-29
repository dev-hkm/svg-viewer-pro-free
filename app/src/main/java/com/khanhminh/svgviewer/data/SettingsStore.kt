package com.khanhminh.svgviewer.data

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.khanhminh.svgviewer.R

enum class ThemeMode(@StringRes val labelRes: Int) {
    SYSTEM(R.string.theme_system),
    LIGHT(R.string.theme_light),
    DARK(R.string.theme_dark)
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

    var background by mutableStateOf(loadBackground(prefs))
        private set

    var hapticsEnabled by mutableStateOf(prefs.getBoolean(KEY_HAPTICS, true))
        private set

    fun updateThemeMode(mode: ThemeMode) {
        themeMode = mode
        prefs.edit().putInt(KEY_THEME, mode.ordinal).apply()
    }

    fun updateBackground(bg: CanvasBackground) {
        background = bg
        prefs.edit().putString(KEY_BG, bg.name).apply()
    }

    fun updateHaptics(enabled: Boolean) {
        hapticsEnabled = enabled
        prefs.edit().putBoolean(KEY_HAPTICS, enabled).apply()
    }

    private companion object {
        const val KEY_THEME = "theme_mode"
        const val KEY_BG = "canvas_background_name"
        const val KEY_BG_LEGACY = "canvas_background"
        const val KEY_HAPTICS = "haptics_enabled"

        fun loadBackground(prefs: android.content.SharedPreferences): CanvasBackground =
            CanvasBackground.fromName(prefs.getString(KEY_BG, null))
                ?: CanvasBackground.fromLegacyOrdinal(prefs.getInt(KEY_BG_LEGACY, 0))
    }
}
