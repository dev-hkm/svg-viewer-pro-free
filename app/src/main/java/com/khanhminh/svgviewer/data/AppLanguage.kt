package com.khanhminh.svgviewer.data

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale

/** Ngôn ngữ app hỗ trợ. SYSTEM = bám theo ngôn ngữ máy. */
enum class AppLanguage(val tag: String) {
    SYSTEM(""),
    VIETNAMESE("vi"),
    ENGLISH("en")
}

object LocaleHelper {

    /** Bọc Context với locale đã chọn, dùng trong Activity.attachBaseContext. */
    fun wrap(base: Context, language: AppLanguage): Context {
        if (language == AppLanguage.SYSTEM) return base
        val locale = Locale.forLanguageTag(language.tag)
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return base.createConfigurationContext(config)
    }
}

/**
 * Ngôn ngữ hiển thị, lưu cùng file prefs với SettingsStore.
 * Có hàm đọc tĩnh để dùng trước khi composition chạy (attachBaseContext).
 */
class LanguageStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var language by mutableStateOf(read(prefs))
        private set

    fun update(value: AppLanguage) {
        language = value
        prefs.edit().putString(KEY, value.name).apply()
    }

    companion object {
        private const val PREFS = "settings"
        private const val KEY = "app_language"

        fun read(context: Context): AppLanguage =
            read(context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE))

        private fun read(prefs: android.content.SharedPreferences): AppLanguage =
            runCatching { AppLanguage.valueOf(prefs.getString(KEY, null) ?: "SYSTEM") }
                .getOrDefault(AppLanguage.SYSTEM)
    }
}
