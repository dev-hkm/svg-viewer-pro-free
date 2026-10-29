package com.khanhminh.svgviewer.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Status bar / navigation bar luôn trong suốt và đổi màu icon theo **nền thật**
 * phía sau chúng, nên không bao giờ chìm vào nền ở bất kỳ màn nào — kể cả khi
 * canvas trong viewer đang là trắng, đen hay ca-rô.
 *
 * @param lightIcons true khi nền phía sau là màu sáng (icon phải sẫm màu).
 */
@Composable
fun SystemBars(lightIcons: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return

    SideEffect {
        val window = view.context.findActivity()?.window ?: return@SideEffect
        @Suppress("DEPRECATION")
        window.statusBarColor = Color.Transparent.toArgb()
        @Suppress("DEPRECATION")
        window.navigationBarColor = Color.Transparent.toArgb()

        // Tắt lớp scrim tự thêm của hệ thống: nếu không, thanh điều hướng 3 nút
        // sẽ bị phủ một dải xám và mất tương phản với nền app.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
        }

        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = lightIcons
            isAppearanceLightNavigationBars = lightIcons
        }
    }
}

/** Nền sáng hay tối, dùng để chọn màu icon hệ thống. */
fun Color.isLight(): Boolean = luminance() > 0.55f

/** Đi ngược chuỗi ContextWrapper để tìm Activity đang chứa composition. */
fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
