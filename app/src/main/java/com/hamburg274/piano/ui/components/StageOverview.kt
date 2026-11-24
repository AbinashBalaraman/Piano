package com.hamburg274.piano.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hamburg274.piano.R
import com.hamburg274.piano.audio.AcousticSettings
import com.hamburg274.piano.audio.VoicingId
import com.hamburg274.piano.music.VelocityEngine
import com.hamburg274.piano.ui.theme.BorderHairline
import com.hamburg274.piano.ui.theme.ConcertBrass
import com.hamburg274.piano.ui.theme.ConcertBrassBright
import com.hamburg274.piano.ui.theme.ConcertBrassDeep
import com.hamburg274.piano.ui.theme.CormorantGaramondFamily
import com.hamburg274.piano.ui.theme.IvoryText
import com.hamburg274.piano.ui.theme.JetBrainsMonoFamily
import com.hamburg274.piano.ui.theme.MutedStoneText
import com.hamburg274.piano.ui.theme.ObsidianBg
import com.hamburg274.piano.ui.theme.RecordCrimson
import com.hamburg274.piano.ui.theme.RecordCrimsonLight
import com.hamburg274.piano.ui.theme.SubtleAshText
import com.hamburg274.piano.ui.theme.SurfaceCard
import com.hamburg274.piano.ui.theme.SurfaceControl
import com.hamburg274.piano.ui.theme.SurfaceElevated
import com.hamburg274.piano.ui.theme.SurfaceSelected
import com.hamburg274.piano.ui.theme.WarmSandText
import kotlin.math.roundToInt

/**
 * Compact Concert Fallboard Console positioned above the dominant piano keybed on the Stage tab.
 * Designed so the 88-key piano commands the majority of the viewport while keeping voicing,
 * live chord/dynamics telemetry, acoustic faders, and primary actions at fingertip reach.
 */
@Composable
fun StageOverview(
    settings: AcousticSettings,
    onUpdateSettings: ((AcousticSettings) -> AcousticSettings) -> Unit,
    activeNoteNames: List<String>,
    lastVelocity: Int?,
    damperCC64: Int,
    sostenuto: Boolean,
    unaCorda: Boolean,
    isRecording: Boolean,
    onToggleRecord: () -> Unit,
    onOpenPractice: () -> Unit,
    onOpenAcoustics: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeVoicing = settings.voicingId
    var voicingExpanded by remember { mutableStateOf(false) }

    val dynamicMarking = lastVelocity?.let { VelocityEngine.velocityToDynamicMarking(it) }
    val damperPct = ((damperCC64 / 127f) * 100f).roundToInt()

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 920.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(SurfaceElevated, Color(0xFF101014))
                    )
                )
                .border(1.dp, BorderHairline, RoundedCornerShape(14.dp))
                .padding(horizontal = 13.dp, vertical = 11.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Row 1: Voicing Display Title + Compact Voicing Switcher
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    Text(
                        text = activeVoicing.displayName,
                        fontFamily = CormorantGaramondFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 22.sp,
                        lineHeight = 24.sp,
                        color = IvoryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${settings.temperament.shortName} · A4=${settings.referenceA4Hz}Hz · ${settings.hallId.displayName}",
                        fontFamily = JetBrainsMonoFamily,
                        fontSize = 10.sp,
                        color = MutedStoneText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Box(modifier = Modifier.padding(start = 8.dp)) {
                    Row(
                        modifier = Modifier
                            .testTag("stage_voicing_selector")
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceControl)
                            .border(1.dp, ConcertBrass.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .clickable { voicingExpanded = true }
                            .padding(horizontal = 9.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = activeVoicing.shortName,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = ConcertBrassBright,
                            maxLines = 1
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Select piano voicing",
                            tint = ConcertBrassBright,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = voicingExpanded,
                        onDismissRequest = { voicingExpanded = false },
                        modifier = Modifier.background(SurfaceCard)
                    ) {
                        VoicingId.entries.forEach { v ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(
                                            text = v.displayName,
                                            color = if (v == activeVoicing) ConcertBrassBright else IvoryText,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = v.packTier,
                                            fontFamily = JetBrainsMonoFamily,
                                            color = MutedStoneText,
                                            fontSize = 10.sp
                                        )
                                    }
                                },
                                onClick = {
                                    onUpdateSettings { it.copy(voicingId = v) }
                                    voicingExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Row 2: Compact Live Telemetry Strip (Chord · Dynamic · Pedal)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(9.dp))
                    .background(ObsidianBg.copy(alpha = 0.9f))
                    .border(
                        1.dp,
                        if (activeNoteNames.isNotEmpty()) ConcertBrass.copy(alpha = 0.35f) else BorderHairline,
                        RoundedCornerShape(9.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (activeNoteNames.isNotEmpty()) ConcertBrassBright else SubtleAshText.copy(alpha = 0.5f))
                    )
                    Text(
                        text = if (activeNoteNames.isNotEmpty()) {
                            activeNoteNames.take(5).joinToString(" ")
                        } else {
                            "Touch upper felt for ppp · front lip for fff"
                        },
                        fontFamily = if (activeNoteNames.isNotEmpty()) JetBrainsMonoFamily else null,
                        fontSize = if (activeNoteNames.isNotEmpty()) 11.5.sp else 10.5.sp,
                        color = if (activeNoteNames.isNotEmpty()) ConcertBrassBright else SubtleAshText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (dynamicMarking != null && lastVelocity != null) {
                        Text(
                            text = "$dynamicMarking $lastVelocity",
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = ConcertBrass
                        )
                    }
                    Text(
                        text = buildString {
                            append("CC64 $damperPct%")
                            if (sostenuto) append(" · Sos")
                            if (unaCorda) append(" · UC")
                        },
                        fontFamily = JetBrainsMonoFamily,
                        fontSize = 10.5.sp,
                        color = if (damperCC64 > 0 || sostenuto || unaCorda) ConcertBrassBright else MutedStoneText
                    )
                }
            }

            // Row 3: Slim Console Hardware Faders (Gain · Duplex · Hall)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ConsoleFader(
                    label = stringResource(R.string.stage_master_gain),
                    value = settings.masterVolume,
                    onValueChange = { v -> onUpdateSettings { it.copy(masterVolume = v) } },
                    tag = "stage_master_gain_slider",
                    modifier = Modifier.weight(1f)
                )
                ConsoleFader(
                    label = stringResource(R.string.stage_duplex_resonance),
                    value = settings.duplexMix,
                    onValueChange = { v -> onUpdateSettings { it.copy(duplexMix = v) } },
                    tag = "stage_duplex_slider",
                    modifier = Modifier.weight(1f)
                )
                ConsoleFader(
                    label = "Hall",
                    value = settings.reverbMix,
                    onValueChange = { v -> onUpdateSettings { it.copy(reverbMix = v) } },
                    tag = "stage_reverb_slider",
                    modifier = Modifier.weight(1f)
                )
            }

            // Row 4: Clean Unclipped Action Row (Record · Score · Voicing)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StageActionPill(
                    label = stringResource(if (isRecording) R.string.stage_stop_rec else R.string.stage_record),
                    icon = if (isRecording) Icons.Default.Stop else Icons.Default.FiberManualRecord,
                    iconTint = if (isRecording) IvoryText else RecordCrimsonLight,
                    textColor = IvoryText,
                    bgColor = if (isRecording) RecordCrimson else SurfaceControl,
                    borderColor = if (isRecording) RecordCrimsonLight else BorderHairline,
                    onClick = onToggleRecord,
                    tag = "stage_record_button",
                    modifier = Modifier.weight(1f)
                )
                StageActionPill(
                    label = stringResource(R.string.stage_urtext_score),
                    icon = Icons.Default.Book,
                    iconTint = ConcertBrassBright,
                    textColor = ConcertBrassBright,
                    bgColor = SurfaceSelected,
                    borderColor = ConcertBrass.copy(alpha = 0.45f),
                    onClick = onOpenPractice,
                    tag = "stage_open_practice_button",
                    modifier = Modifier.weight(1f)
                )
                StageActionPill(
                    label = stringResource(R.string.stage_voicing_dsp),
                    icon = Icons.Default.Tune,
                    iconTint = WarmSandText,
                    textColor = WarmSandText,
                    bgColor = SurfaceControl,
                    borderColor = BorderHairline,
                    onClick = onOpenAcoustics,
                    tag = "stage_open_acoustics_button",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun StageActionPill(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    textColor: Color,
    bgColor: Color,
    borderColor: Color,
    onClick: () -> Unit,
    tag: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(38.dp)
            .testTag(tag)
            .clip(RoundedCornerShape(9.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(9.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(14.dp)
        )
        Spacer(Modifier.size(5.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Slim boutique studio fader replacing chunky default sliders on the Stage fallboard.
 */
@Composable
private fun ConsoleFader(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    tag: String,
    modifier: Modifier = Modifier
) {
    val latestCallback by rememberUpdatedState(onValueChange)
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 10.5.sp,
                color = WarmSandText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${(value * 100f).roundToInt()}%",
                fontFamily = JetBrainsMonoFamily,
                fontSize = 10.sp,
                color = ConcertBrass
            )
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(22.dp)
                .testTag(tag)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: continue
                            if (change.pressed) {
                                val pad = 7.dp.toPx()
                                val usable = (size.width - pad * 2f).coerceAtLeast(1f)
                                val ratio = ((change.position.x - pad) / usable).coerceIn(0f, 1f)
                                latestCallback(ratio)
                                change.consume()
                            }
                        }
                    }
                }
        ) {
            val pad = 7.dp.toPx()
            val trackH = 4.dp.toPx()
            val cy = size.height * 0.5f
            val usableW = (size.width - pad * 2f).coerceAtLeast(1f)
            val thumbX = pad + value.coerceIn(0f, 1f) * usableW

            // Recessed track
            drawRoundRect(
                color = SurfaceControl,
                topLeft = Offset(pad, cy - trackH * 0.5f),
                size = Size(usableW, trackH),
                cornerRadius = CornerRadius(trackH * 0.5f)
            )
            // Warm brass active fill
            drawRoundRect(
                brush = Brush.horizontalGradient(listOf(ConcertBrassDeep, ConcertBrass)),
                topLeft = Offset(pad, cy - trackH * 0.5f),
                size = Size((thumbX - pad).coerceAtLeast(0f), trackH),
                cornerRadius = CornerRadius(trackH * 0.5f)
            )
            // Tactile circular brass knob
            drawCircle(
                color = ObsidianBg,
                radius = 7.dp.toPx(),
                center = Offset(thumbX, cy)
            )
            drawCircle(
                color = ConcertBrassBright,
                radius = 5.5.dp.toPx(),
                center = Offset(thumbX, cy)
            )
        }
    }
}
