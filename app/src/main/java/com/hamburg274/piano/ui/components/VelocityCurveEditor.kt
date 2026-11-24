package com.hamburg274.piano.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hamburg274.piano.R
import com.hamburg274.piano.music.VelocityCurvePoint
import com.hamburg274.piano.music.VelocityEngine
import com.hamburg274.piano.ui.theme.BorderHairline
import com.hamburg274.piano.ui.theme.ConcertBrass
import com.hamburg274.piano.ui.theme.IvoryText
import com.hamburg274.piano.ui.theme.JetBrainsMonoFamily
import com.hamburg274.piano.ui.theme.MutedStoneText
import com.hamburg274.piano.ui.theme.ObsidianBg
import com.hamburg274.piano.ui.theme.WarmSandText
import kotlin.math.hypot
import kotlin.math.roundToInt

@Composable
fun VelocityCurveEditor(
    points: List<VelocityCurvePoint>,
    onChange: (List<VelocityCurvePoint>) -> Unit,
    lastRawVelocity: Int?,
    modifier: Modifier = Modifier
) {
    val latestPoints by rememberUpdatedState(points)
    val latestOnChange by rememberUpdatedState(onChange)
    var draggingIdx by remember { mutableIntStateOf(-1) }

    val mappedLast = remember(lastRawVelocity, points) {
        if (lastRawVelocity != null && lastRawVelocity > 0) {
            VelocityEngine.applyVelocityCurve(lastRawVelocity, points)
        } else null
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.acoustics_velocity_curve_title),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = WarmSandText
        )

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            VelocityEngine.PRESETS.forEach { preset ->
                Box(
                    modifier = Modifier
                        .testTag("vel_preset_${preset.id}")
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF222229))
                        .clickable { onChange(preset.points) }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = preset.name,
                        fontSize = 11.sp,
                        color = WarmSandText
                    )
                }
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(168.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF121216))
                .border(1.dp, BorderHairline, RoundedCornerShape(10.dp))
                .testTag("velocity_curve_canvas")
                .pointerInput(Unit) {
                    val pad = 18.dp.toPx()
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: continue
                            val plotW = (size.width - pad * 2f).coerceAtLeast(1f)
                            val plotH = (size.height - pad * 2f).coerceAtLeast(1f)

                            if (change.pressed && !change.previousPressed) {
                                var best = -1
                                var bestDist = 36.dp.toPx()
                                latestPoints.forEachIndexed { idx, pt ->
                                    val px = pad + (pt.x / 127f) * plotW
                                    val py = pad + plotH - (pt.y / 127f) * plotH
                                    val d = hypot(change.position.x - px, change.position.y - py)
                                    if (d < bestDist) {
                                        bestDist = d
                                        best = idx
                                    }
                                }
                                draggingIdx = best
                                if (best >= 0) change.consume()
                            } else if (change.pressed && draggingIdx >= 0) {
                                val next = latestPoints.toMutableList()
                                val rawY = (((pad + plotH - change.position.y) / plotH) * 127f).roundToInt().coerceIn(1, 127)
                                val idx = draggingIdx
                                if (idx == 0 || idx == next.lastIndex) {
                                    next[idx] = next[idx].copy(y = rawY)
                                } else {
                                    val minX = next[idx - 1].x + 8
                                    val maxX = next[idx + 1].x - 8
                                    val rawX = (((change.position.x - pad) / plotW) * 127f).roundToInt().coerceIn(minX, maxX)
                                    next[idx] = VelocityCurvePoint(rawX, rawY)
                                }
                                latestOnChange(next)
                                change.consume()
                            } else if (!change.pressed) {
                                draggingIdx = -1
                            }
                        }
                    }
                }
        ) {
            val pad = 18.dp.toPx()
            val plotW = size.width - pad * 2f
            val plotH = size.height - pad * 2f

            fun toX(v: Int) = pad + (v / 127f) * plotW
            fun toY(v: Int) = pad + plotH - (v / 127f) * plotH

            // Grid
            listOf(0.25f, 0.5f, 0.75f).forEach { f ->
                drawLine(Color.White.copy(alpha = 0.06f), Offset(pad + plotW * f, pad), Offset(pad + plotW * f, pad + plotH), 1f)
                drawLine(Color.White.copy(alpha = 0.06f), Offset(pad, pad + plotH * f), Offset(pad + plotW, pad + plotH * f), 1f)
            }
            drawRect(
                color = Color.White.copy(alpha = 0.12f),
                topLeft = Offset(pad, pad),
                size = Size(plotW, plotH),
                style = Stroke(width = 1f)
            )
            // Linear 1:1 diagonal reference
            drawLine(
                color = Color.White.copy(alpha = 0.16f),
                start = Offset(toX(0), toY(0)),
                end = Offset(toX(127), toY(127)),
                strokeWidth = 1f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
            )

            // Curve polyline
            if (points.size >= 2) {
                val path = Path().apply {
                    moveTo(toX(points.first().x), toY(points.first().y))
                    points.drop(1).forEach { lineTo(toX(it.x), toY(it.y)) }
                }
                drawPath(
                    path = path,
                    color = ConcertBrass,
                    style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }

            // Live strike crosshair
            if (lastRawVelocity != null && mappedLast != null) {
                val sx = toX(lastRawVelocity)
                val sy = toY(mappedLast)
                drawLine(
                    color = ConcertBrass.copy(alpha = 0.45f),
                    start = Offset(sx, pad + plotH),
                    end = Offset(sx, sy),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f))
                )
                drawCircle(color = IvoryText, radius = 4.dp.toPx(), center = Offset(sx, sy))
            }

            // 5 Control Nodes
            points.forEachIndexed { idx, pt ->
                val cx = toX(pt.x)
                val cy = toY(pt.y)
                val active = draggingIdx == idx
                drawCircle(
                    color = ObsidianBg,
                    radius = if (active) 7.dp.toPx() else 5.5.dp.toPx(),
                    center = Offset(cx, cy)
                )
                drawCircle(
                    color = if (active) IvoryText else ConcertBrass,
                    radius = if (active) 5.5.dp.toPx() else 4.2.dp.toPx(),
                    center = Offset(cx, cy)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Drag 5 nodes to sculpt response",
                fontFamily = JetBrainsMonoFamily,
                fontSize = 10.sp,
                color = MutedStoneText
            )
            Text(
                text = if (lastRawVelocity != null && mappedLast != null) "In $lastRawVelocity → Out $mappedLast" else "Strike key to inspect",
                fontFamily = JetBrainsMonoFamily,
                fontSize = 10.sp,
                color = ConcertBrass
            )
        }
    }
}
