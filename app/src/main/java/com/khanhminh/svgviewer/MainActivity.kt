package com.khanhminh.svgviewer

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import com.khanhminh.svgviewer.data.IncomingSvg
import com.khanhminh.svgviewer.data.LanguageStore
import com.khanhminh.svgviewer.data.LocaleHelper
import com.khanhminh.svgviewer.data.SettingsStore
import com.khanhminh.svgviewer.data.ThemeMode
import com.khanhminh.svgviewer.ui.SvgViewerApp
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {

    private val incoming = MutableStateFlow<IncomingSvg?>(null)

    /**
     * Ngôn ngữ đã lưu được áp vào Context gốc trước khi resource được nạp,
     * nên mọi chuỗi (kể cả tên app, dialog hệ thống) đúng ngôn ngữ ngay từ đầu.
     */
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase, LanguageStore.read(newBase)))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        applyStartupTheme()
        incoming.value = IncomingSvg.fromIntent(intent)

        setContent {
            val payload by incoming.collectAsState()
            SvgViewerApp(incoming = payload)
        }
    }

    /**
     * Nền cửa sổ và màu icon thanh hệ thống được đặt ngay khi Activity tạo,
     * dựa trên lựa chọn theme của app (không phải theme của máy) — nhờ vậy
     * không có nháy trắng/đen ở frame đầu tiên khi người dùng ép Light hoặc Dark.
     */
    private fun applyStartupTheme() {
        val mode = SettingsStore(this).themeMode
        val systemDark = (resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val dark = when (mode) {
            ThemeMode.SYSTEM -> systemDark
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
        }
        window.setBackgroundDrawable(
            ColorDrawable(if (dark) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
        )
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        incoming.value = IncomingSvg.fromIntent(intent)
    }
}
