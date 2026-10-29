package com.khanhminh.svgviewer.ui

import androidx.compose.ui.graphics.vector.ImageVector
import com.khanhminh.svgviewer.ui.theme.AppIcons

data class SampleSvg(
    val id: String,
    val asset: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector
)

val SampleSvgs = listOf(
    SampleSvg(
        id = "aurora",
        asset = "samples/aurora.svg",
        title = "Aurora Orb",
        subtitle = "Gradient quỹ đạo",
        icon = AppIcons.Aurora
    ),
    SampleSvg(
        id = "orbit",
        asset = "samples/orbit.svg",
        title = "Orbit Loader",
        subtitle = "Xoay SMIL",
        icon = AppIcons.Orbit
    ),
    SampleSvg(
        id = "waves",
        asset = "samples/waves.svg",
        title = "Wave Motion",
        subtitle = "Morph đường cong",
        icon = AppIcons.Wave
    ),
    SampleSvg(
        id = "pulse",
        asset = "samples/pulse.svg",
        title = "Pulse Grid",
        subtitle = "CSS keyframes",
        icon = AppIcons.Pulse
    )
)
