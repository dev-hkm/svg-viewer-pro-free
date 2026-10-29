package com.khanhminh.svgviewer.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.adamglin.phosphoricons.LightGroup
import com.adamglin.phosphoricons.light.*

/**
 * Bộ icon dùng chung: chỉ Phosphor Icons (Light), không emoji, không icon ký tự.
 */
object AppIcons {
    val Back: ImageVector get() = LightGroup.ArrowLeft
    val Close: ImageVector get() = LightGroup.X
    val Check: ImageVector get() = LightGroup.Check
    val CaretDown: ImageVector get() = LightGroup.CaretDown

    val FolderOpen: ImageVector get() = LightGroup.FolderOpen
    val Clipboard: ImageVector get() = LightGroup.ClipboardText
    val FileSvg: ImageVector get() = LightGroup.FileSvg
    val History: ImageVector get() = LightGroup.ClockCounterClockwise
    val Image: ImageVector get() = LightGroup.Image
    val Trash: ImageVector get() = LightGroup.Trash
    val Share: ImageVector get() = LightGroup.ShareNetwork
    val Export: ImageVector get() = LightGroup.Export

    val Gear: ImageVector get() = LightGroup.Gear
    val Sun: ImageVector get() = LightGroup.Sun
    val Moon: ImageVector get() = LightGroup.Moon
    val Info: ImageVector get() = LightGroup.Info

    val Fit: ImageVector get() = LightGroup.ArrowsOut
    val ZoomIn: ImageVector get() = LightGroup.MagnifyingGlassPlus
    val ZoomOut: ImageVector get() = LightGroup.MagnifyingGlassMinus
    val Play: ImageVector get() = LightGroup.Play
    val Pause: ImageVector get() = LightGroup.Pause
    val Replay: ImageVector get() = LightGroup.ArrowCounterClockwise
    val Checker: ImageVector get() = LightGroup.Checkerboard
    val Grid: ImageVector get() = LightGroup.GridFour
    val Refresh: ImageVector get() = LightGroup.ArrowClockwise
    val Lock: ImageVector get() = LightGroup.LockSimple
    val LockOpen: ImageVector get() = LightGroup.LockSimpleOpen
    val Ruler: ImageVector get() = LightGroup.Ruler
    val Speed: ImageVector get() = LightGroup.Gauge
    val Pin: ImageVector get() = LightGroup.PushPin
    val Copy: ImageVector get() = LightGroup.Copy
    val More: ImageVector get() = LightGroup.DotsThree
    val Code: ImageVector get() = LightGroup.FileCode
    val Palette: ImageVector get() = LightGroup.Palette
    val Swatches: ImageVector get() = LightGroup.Swatches
    val Printer: ImageVector get() = LightGroup.Printer
    val IconSet: ImageVector get() = LightGroup.Images
    val Optimize: ImageVector get() = LightGroup.MagicWand
    val Invert: ImageVector get() = LightGroup.CircleHalf
    val Download: ImageVector get() = LightGroup.DownloadSimple

    val Sparkle: ImageVector get() = LightGroup.Sparkle
    val Wave: ImageVector get() = LightGroup.Waveform
    val Orbit: ImageVector get() = LightGroup.CirclesFour
    val Pulse: ImageVector get() = LightGroup.Pulse
    val Aurora: ImageVector get() = LightGroup.SunHorizon

    /** Mũi tên phải tự lật theo RTL. */
    val ChevronRight: ImageVector by lazy {
        ImageVector.Builder(
            name = "ChevronRight",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 256f,
            viewportHeight = 256f,
            autoMirror = true
        ).apply {
            path(
                fill = SolidColor(Color.Black),
                stroke = null,
                strokeLineWidth = 0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 4f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(180.24f, 132.24f)
                lineToRelative(-80f, 80f)
                arcToRelative(6f, 6f, 0f, false, true, -8.48f, -8.48f)
                lineTo(167.51f, 128f)
                lineTo(91.76f, 52.24f)
                arcToRelative(6f, 6f, 0f, false, true, 8.48f, -8.48f)
                lineToRelative(80f, 80f)
                arcTo(6f, 6f, 0f, false, true, 180.24f, 132.24f)
                close()
            }
        }.build()
    }
}
