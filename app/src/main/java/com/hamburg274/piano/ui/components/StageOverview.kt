package com.hamburg274.piano.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hamburg274.piano.R
import com.hamburg274.piano.audio.AcousticSettings
import com.hamburg274.piano.ui.theme.BorderHairline
import com.hamburg274.piano.ui.theme.ConcertBrass
import com.hamburg274.piano.ui.theme.ConcertBrassBright
import com.hamburg274.piano.ui.theme.ConcertBrassDeep
import com.hamburg274.piano.ui.theme.JetBrainsMonoFamily
import com.hamburg274.piano.ui.theme.MutedStoneText
import com.hamburg274.piano.ui.theme.ObsidianBg
import com.hamburg274.piano.ui.theme.SurfaceControl
import com.hamburg274.piano.ui.theme.SurfaceElevated
import com.hamburg274.piano.ui.theme.WarmSandText
import kotlin.math.roundToInt

/**
 * Collapsible single-row acoustic mixer drawer for Stage mode.
 * Kept hidden by default so the concert piano commands ~85-93% of the landscape display,
 * expanding on demand for instant Gain, Duplex Resonance, and Hall Reverb adjustments.
 */
@Composable
fun StageQuickMixerStrip(
    settings: AcousticSettings,
    onUpdateSettings: ((AcousticSettings) -> AcousticSettings) -> Unit,
    onOpenAcoustics: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceElevated)
            .border(width = 0.5.dp, color = BorderHairline)
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag("stage_quick_mixer_strip"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        InlineConsoleFader(
            label = stringResource(R.string.stage_master_gain),
            value = settings.masterVolume,
            onValueChange = { v -> onUpdateSettings { it.copy(masterVolume = v) } },
            tag = "stage_master_gain_slider",
            modifier = Modifier.weight(1f)
        )
        InlineConsoleFader(
            label = stringResource(R.string.stage_duplex_resonance),
            value = settings.duplexMix,
            onValueChange = { v -> onUpdateSettings { it.copy(duplexMix = v) } },
            tag = "stage_duplex_slider",
            modifier = Modifier.weight(1f)
        )
        InlineConsoleFader(
            label = settings.hallId.displayName.substringBefore(" "),
            value = settings.reverbMix,
            onValueChange = { v -> onUpdateSettings { it.copy(reverbMix = v) } },
            tag = "stage_reverb_slider",
            modifier = Modifier.weight(1f)
        )

        // Tuning pill + shortcut to full Acoustics DSP studio
        Row(
            modifier = Modifier
                .testTag("stage_open_acoustics_button")
                .clip(RoundedCornerShape(7.dp))
                .background(SurfaceControl)
                .border(1.dp, BorderHairline, RoundedCornerShape(7.dp))
                .clickable { onOpenAcoustics() }
                .padding(horizontal = 9.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Tune,
                contentDescription = null,
                tint = ConcertBrass,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = "${settings.temperament.shortName} · ${settings.referenceA4Hz}Hz",
                fontFamily = JetBrainsMonoFamily,
                fontSize = 10.sp,
                color = WarmSandText,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun InlineConsoleFader(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    tag: String,
    modifier: Modifier = Modifier
) {
    val latestCallback by rememberUpdatedState(onValueChange)
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Medium,
            color = MutedStoneText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Canvas(
            modifier = Modifier
                .weight(1f)
                .height(22.dp)
                .testTag(tag)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: continue
                            if (change.pressed) {
                                val pad = 6.dp.toPx()
                                val usable = (size.width - pad * 2f).coerceAtLeast(1f)
                                val ratio = ((change.position.x - pad) / usable).coerceIn(0f, 1f)
                                latestCallback(ratio)
                                change.consume()
                            }
                        }
                    }
                }
        ) {
            val pad = 6.dp.toPx()
            val trackH = 3.5.dp.toPx()
            val cy = size.height * 0.5f
            val usableW = (size.width - pad * 2f).coerceAtLeast(1f)
            val thumbX = pad + value.coerceIn(0f, 1f) * usableW

            drawRoundRect(
                color = SurfaceControl,
                topLeft = Offset(pad, cy - trackH * 0.5f),
                size = Size(usableW, trackH),
                cornerRadius = CornerRadius(trackH * 0.5f)
            )
            drawRoundRect(
                brush = Brush.horizontalGradient(listOf(ConcertBrassDeep, ConcertBrass)),
                topLeft = Offset(pad, cy - trackH * 0.5f),
                size = Size((thumbX - pad).coerceAtLeast(0f), trackH),
                cornerRadius = CornerRadius(trackH * 0.5f)
            )
            drawCircle(
                color = ObsidianBg,
                radius = 6.5.dp.toPx(),
                center = Offset(thumbX, cy)
            )
            drawCircle(
                color = ConcertBrassBright,
                radius = 5.dp.toPx(),
                center = Offset(thumbX, cy)
            )
        }

        Box(modifier = Modifier.padding(end = 2.dp)) {
            Text(
                text = "${(value * 100f).roundToInt()}%",
                fontFamily = JetBrainsMonoFamily,
                fontSize = 10.sp,
                color = ConcertBrass
            )
        }
    }
}
