package com.khanhminh.svgviewer.ui

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlin.math.roundToInt

/** Nhịp chuyển động dùng chung: nhanh, dứt khoát, không nảy. */
object Motion {
    const val Screen = 300
    val Easing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)
}

/** Chuyển màn hình trượt ngang thuần, có parallax nhẹ cho màn phía sau. */
fun screenTransition(direction: Int): ContentTransform {
    val dir = if (direction >= 0) 1 else -1
    return slideInHorizontally(
        animationSpec = tween(Motion.Screen, easing = Motion.Easing)
    ) { full -> dir * full } togetherWith
        slideOutHorizontally(
            animationSpec = tween(Motion.Screen, easing = Motion.Easing)
        ) { full -> (dir * -0.26f * full).roundToInt() }
}

/** Predictive back có progress tương tác, khôi phục vị trí khi huỷ gesture. */
@Composable
fun PredictiveBack(
    enabled: Boolean,
    onBack: () -> Unit,
    onProgress: (Float) -> Unit = {}
) {
    val back = rememberUpdatedState(onBack)
    val progress = rememberUpdatedState(onProgress)

    PredictiveBackHandler(enabled = enabled) { events: Flow<androidx.activity.BackEventCompat> ->
        try {
            events.collect { event ->
                val raw = event.progress.coerceIn(0f, 1f)
                progress.value(1f - (1f - raw) * (1f - raw))
            }
            progress.value(1f)
            back.value()
        } finally {
            progress.value(0f)
        }
    }
}

/**
 * Chạm có phản hồi vật lý: nén nhẹ khi nhấn, nảy lại bằng spring.
 * Không ripple để giữ bề mặt phẳng đúng ngôn ngữ tối giản.
 */
fun Modifier.tap(
    role: Role? = Role.Button,
    enabled: Boolean = true,
    minScale: Float = 0.97f,
    onClick: () -> Unit
): Modifier = composed {
    val scale = remember { Animatable(1f) }
    val source = remember { MutableInteractionSource() }

    LaunchedEffect(source) {
        source.interactions.filter { it is PressInteraction }.collectLatest { interaction ->
            when (interaction) {
                is PressInteraction.Press -> scale.animateTo(
                    minScale,
                    tween(90, easing = Motion.Easing)
                )
                is PressInteraction.Release -> scale.animateTo(
                    1f,
                    spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMedium)
                )
                is PressInteraction.Cancel -> scale.animateTo(
                    1f,
                    spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
                )
            }
        }
    }

    this
        .graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
        }
        .clickable(
            interactionSource = source,
            indication = null,
            enabled = enabled,
            role = role,
            onClick = onClick
        )
}

/** Biến thiên theo gesture back: dịch sang phải, thu nhỏ, bo góc dần. */
fun Modifier.predictiveBackVisual(progress: Float, maxTranslationDp: Float = 56f): Modifier =
    graphicsLayer {
        val p = progress.coerceIn(0f, 1f)
        if (p > 0f) {
            translationX = maxTranslationDp.dp.toPx() * p
            val s = 1f - 0.05f * p
            scaleX = s
            scaleY = s
            shape = RoundedCornerShape((22 * p).dp)
            clip = true
        }
    }
