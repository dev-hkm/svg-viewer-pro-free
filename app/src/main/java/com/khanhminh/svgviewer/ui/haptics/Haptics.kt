package com.khanhminh.svgviewer.ui.haptics

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Haptic engine: mỗi tương tác chỉ một nhịp, ngắn và mảnh.
 * Không pattern nhiều nhịp, không rung kéo dài.
 */
interface Haptics {
    fun click()
    fun tick()
    fun selection()
    fun impact()
    fun success()
    fun warning()
}

class AndroidHaptics(
    context: Context,
    private val view: View?,
    private val enabled: () -> Boolean = { true }
) : Haptics {

    private var lastAt = 0L

    private fun allow(minIntervalMs: Long): Boolean {
        if (!enabled()) return false
        val now = android.os.SystemClock.uptimeMillis()
        if (now - lastAt < minIntervalMs) return false
        lastAt = now
        return true
    }

    private val vibrator: Vibrator? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator ?: (context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    } catch (_: Exception) {
        null
    }

    private val hasVibrator: Boolean = try {
        vibrator?.hasVibrator() == true
    } catch (_: Exception) {
        false
    }

    private fun pulse(ms: Long, amplitude: Int, fallback: Int) {
        if (hasVibrator && vibrator != null) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(ms, amplitude))
                    return
                }
            } catch (_: Exception) {
                // rơi về phản hồi của View
            }
        }
        try {
            view?.performHapticFeedback(fallback, HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING)
        } catch (_: Exception) {
            // thiết bị không hỗ trợ
        }
    }

    private fun primitive(strength: Float, ms: Long, amplitude: Int, fallback: Int) {
        if (hasVibrator && vibrator != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                if (vibrator.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_CLICK)) {
                    vibrator.vibrate(
                        VibrationEffect.startComposition()
                            .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, strength)
                            .compose()
                    )
                    return
                }
            } catch (_: Exception) {
                // rơi về một-shot
            }
        }
        pulse(ms, amplitude, fallback)
    }

    override fun click() {
        if (!allow(45L)) return
        primitive(0.55f, 9L, 90, HapticFeedbackConstants.KEYBOARD_TAP)
    }

    override fun tick() {
        if (!allow(38L)) return
        pulse(5L, 45, HapticFeedbackConstants.CLOCK_TICK)
    }

    override fun selection() {
        if (!allow(45L)) return
        primitive(0.45f, 7L, 75, HapticFeedbackConstants.KEYBOARD_TAP)
    }

    override fun impact() {
        if (!allow(50L)) return
        primitive(0.85f, 13L, 140, HapticFeedbackConstants.CONTEXT_CLICK)
    }

    override fun success() {
        if (!allow(60L)) return
        primitive(0.75f, 11L, 115, HapticFeedbackConstants.KEYBOARD_TAP)
    }

    override fun warning() {
        if (!allow(60L)) return
        primitive(0.90f, 15L, 145, HapticFeedbackConstants.CONTEXT_CLICK)
    }
}

private object NoHaptics : Haptics {
    override fun click() = Unit
    override fun tick() = Unit
    override fun selection() = Unit
    override fun impact() = Unit
    override fun success() = Unit
    override fun warning() = Unit
}

val LocalHaptics = staticCompositionLocalOf<Haptics> { NoHaptics }
