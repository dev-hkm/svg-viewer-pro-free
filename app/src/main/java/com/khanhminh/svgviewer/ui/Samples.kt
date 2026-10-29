package com.khanhminh.svgviewer.ui

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import com.khanhminh.svgviewer.R
import com.khanhminh.svgviewer.ui.theme.AppIcons

data class SampleSvg(
    val id: String,
    val asset: String,
    @StringRes val titleRes: Int,
    @StringRes val subtitleRes: Int,
    val icon: ImageVector
)

val SampleSvgs = listOf(
    SampleSvg(
        id = "aurora",
        asset = "samples/aurora.svg",
        titleRes = R.string.sample_aurora,
        subtitleRes = R.string.sample_aurora_desc,
        icon = AppIcons.Aurora
    ),
    SampleSvg(
        id = "orbit",
        asset = "samples/orbit.svg",
        titleRes = R.string.sample_orbit,
        subtitleRes = R.string.sample_orbit_desc,
        icon = AppIcons.Orbit
    ),
    SampleSvg(
        id = "waves",
        asset = "samples/waves.svg",
        titleRes = R.string.sample_waves,
        subtitleRes = R.string.sample_waves_desc,
        icon = AppIcons.Wave
    ),
    SampleSvg(
        id = "pulse",
        asset = "samples/pulse.svg",
        titleRes = R.string.sample_pulse,
        subtitleRes = R.string.sample_pulse_desc,
        icon = AppIcons.Pulse
    )
)
