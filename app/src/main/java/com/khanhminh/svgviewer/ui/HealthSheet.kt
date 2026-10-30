package com.khanhminh.svgviewer.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.khanhminh.svgviewer.R
import com.khanhminh.svgviewer.data.SvgHealth
import com.khanhminh.svgviewer.ui.theme.AppIcons

/**
 * Phiếu chấm sức khoẻ tệp: một điểm tổng và danh sách những chỗ hay làm SVG
 * hiển thị sai khi rời khỏi app. Chỉ đọc, không tự sửa tệp.
 */
@Composable
fun HealthSheet(report: SvgHealth.Report?, onOptimize: () -> Unit) {
    val scheme = MaterialTheme.colorScheme

    Column(
        Modifier
            .padding(horizontal = 20.dp)
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = stringResource(R.string.health_title),
            style = MaterialTheme.typography.titleLarge,
            color = scheme.onSurface
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.health_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant
        )

        Spacer(Modifier.height(20.dp))

        if (report == null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = scheme.onSurface,
                    trackColor = scheme.surfaceContainerHighest,
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.health_checking),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant
                )
            }
        } else {
            ScoreCard(report)
            Spacer(Modifier.height(18.dp))

            if (report.passed) {
                CleanCard()
            } else {
                SectionLabel(
                    text = stringResource(R.string.health_checks),
                    trailing = stringResource(R.string.health_total, report.checks.size)
                )
                Spacer(Modifier.height(6.dp))
                report.checks.forEachIndexed { index, check ->
                    if (index > 0) {
                        Hairline(Modifier.padding(start = 52.dp))
                    }
                    CheckRow(check)
                }
            }

            Spacer(Modifier.height(20.dp))
            PrimaryAction(
                text = stringResource(R.string.health_optimize),
                icon = AppIcons.Optimize,
                onClick = onOptimize,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.health_note),
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ScoreCard(report: SvgHealth.Report) {
    val scheme = MaterialTheme.colorScheme
    val shown by animateFloatAsState(
        targetValue = report.score.toFloat(),
        animationSpec = tween(620, easing = Motion.Easing),
        label = "score"
    )
    val grade = when {
        report.score >= 85 -> R.string.health_grade_good
        report.score >= 60 -> R.string.health_grade_ok
        else -> R.string.health_grade_bad
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(scheme.surfaceContainer)
            .border(1.dp, scheme.outlineVariant, RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(78.dp)) {
                val stroke = 6.dp.toPx()
                val inset = stroke / 2f
                val arcSize = Size(size.width - stroke, size.height - stroke)
                drawArc(
                    color = scheme.outlineVariant,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
                drawArc(
                    color = scheme.onSurface,
                    startAngle = -90f,
                    sweepAngle = 360f * (shown / 100f),
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = shown.toInt().toString(),
                    style = MaterialTheme.typography.headlineSmall,
                    color = scheme.onSurface
                )
                Text(
                    text = stringResource(R.string.health_out_of),
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.width(18.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(grade),
                style = MaterialTheme.typography.titleMedium,
                color = scheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(
                    R.string.health_summary,
                    report.warnings,
                    report.checks.size - report.warnings
                ),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CleanCard() {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(scheme.surfaceContainer)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = AppIcons.CheckCircle,
            contentDescription = null,
            tint = scheme.onSurface,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = stringResource(R.string.health_clean),
                style = MaterialTheme.typography.titleSmall,
                color = scheme.onSurface
            )
            Text(
                text = stringResource(R.string.health_clean_desc),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CheckRow(check: SvgHealth.Check) {
    val scheme = MaterialTheme.colorScheme
    val warn = check.level == SvgHealth.Level.WARN

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(scheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (warn) AppIcons.Warning else AppIcons.Info,
                contentDescription = null,
                tint = scheme.onSurface,
                modifier = Modifier.size(17.dp)
            )
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(titleOf(check.kind)),
                    style = MaterialTheme.typography.titleSmall,
                    color = scheme.onSurface,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (check.count > 0) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.health_count, check.count),
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        color = scheme.onSurfaceVariant,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(scheme.surfaceContainerHigh)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Text(
                text = stringResource(detailOf(check.kind)),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant
            )
        }
    }
}

private fun titleOf(kind: SvgHealth.Kind): Int = when (kind) {
    SvgHealth.Kind.VIEWBOX -> R.string.health_viewbox
    SvgHealth.Kind.DIMENSIONS -> R.string.health_dimensions
    SvgHealth.Kind.ACCESSIBILITY -> R.string.health_a11y
    SvgHealth.Kind.SCRIPT -> R.string.health_script
    SvgHealth.Kind.EXTERNAL -> R.string.health_external
    SvgHealth.Kind.EMBEDDED -> R.string.health_embedded
    SvgHealth.Kind.TEXT -> R.string.health_text
    SvgHealth.Kind.FILTER -> R.string.health_filter
    SvgHealth.Kind.DUPLICATE_IDS -> R.string.health_ids
    SvgHealth.Kind.FOREIGN -> R.string.health_foreign
    SvgHealth.Kind.SIZE -> R.string.health_size
    SvgHealth.Kind.ANIMATION -> R.string.health_animation
}

private fun detailOf(kind: SvgHealth.Kind): Int = when (kind) {
    SvgHealth.Kind.VIEWBOX -> R.string.health_viewbox_desc
    SvgHealth.Kind.DIMENSIONS -> R.string.health_dimensions_desc
    SvgHealth.Kind.ACCESSIBILITY -> R.string.health_a11y_desc
    SvgHealth.Kind.SCRIPT -> R.string.health_script_desc
    SvgHealth.Kind.EXTERNAL -> R.string.health_external_desc
    SvgHealth.Kind.EMBEDDED -> R.string.health_embedded_desc
    SvgHealth.Kind.TEXT -> R.string.health_text_desc
    SvgHealth.Kind.FILTER -> R.string.health_filter_desc
    SvgHealth.Kind.DUPLICATE_IDS -> R.string.health_ids_desc
    SvgHealth.Kind.FOREIGN -> R.string.health_foreign_desc
    SvgHealth.Kind.SIZE -> R.string.health_size_desc
    SvgHealth.Kind.ANIMATION -> R.string.health_animation_desc
}
