package com.khanhminh.svgviewer.ui

import androidx.compose.ui.graphics.Color

data class SampleSvg(
    val id: String,
    val asset: String,
    val title: String,
    val subtitle: String,
    val colors: List<Color>
)

val SampleSvgs = listOf(
    SampleSvg(
        id = "aurora",
        asset = "samples/aurora.svg",
        title = "Aurora Orb",
        subtitle = "Gradient + quỹ đạo",
        colors = listOf(Color(0xFF7C6BFF), Color(0xFF3BC9E8))
    ),
    SampleSvg(
        id = "orbit",
        asset = "samples/orbit.svg",
        title = "Orbit Loader",
        subtitle = "Xoay SMIL",
        colors = listOf(Color(0xFF4FE0C4), Color(0xFF4A3BD6))
    ),
    SampleSvg(
        id = "waves",
        asset = "samples/waves.svg",
        title = "Wave Motion",
        subtitle = "Morph đường cong",
        colors = listOf(Color(0xFFFF8FB0), Color(0xFFFFC46B))
    ),
    SampleSvg(
        id = "pulse",
        asset = "samples/pulse.svg",
        title = "Pulse Grid",
        subtitle = "CSS keyframes",
        colors = listOf(Color(0xFF63EFD2), Color(0xFF2B7FFF))
    )
)
