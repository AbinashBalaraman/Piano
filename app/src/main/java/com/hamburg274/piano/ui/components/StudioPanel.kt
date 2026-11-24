package com.hamburg274.piano.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hamburg274.piano.R
import com.hamburg274.piano.audio.VoicingId
import com.hamburg274.piano.music.MidiEventType
import com.hamburg274.piano.music.StudioTake
import com.hamburg274.piano.ui.theme.BorderHairline
import com.hamburg274.piano.ui.theme.ConcertBrass
import com.hamburg274.piano.ui.theme.ConcertBrassBright
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
import java.util.Locale

@Composable
fun StudioPanel(
    isRecording: Boolean,
    recordingElapsedSec: Double,
    liveEventCount: Int,
    onToggleRecord: () -> Unit,
    takes: List<StudioTake>,
    playingTakeId: String?,
    renderingTakeId: String?,
    onPlayTake: (StudioTake, VoicingId) -> Unit,
    onStopPlayback: () -> Unit,
    onExportMidi: (StudioTake) -> Unit,
    onExportWav: (StudioTake, VoicingId) -> Unit,
    onDeleteTake: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val revoiceOverrides = remember { mutableStateMapOf<String, VoicingId>() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 12.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 920.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Recorder Console Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(SurfaceElevated)
                    .border(
                        1.dp,
                        if (isRecording) RecordCrimsonLight.copy(alpha = 0.6f) else BorderHairline,
                        RoundedCornerShape(14.dp)
                    )
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.studio_title),
                            fontFamily = CormorantGaramondFamily,
                            fontSize = 21.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = IvoryText
                        )
                        Text(
                            text = stringResource(R.string.studio_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedStoneText
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isRecording) {
                                String.format(Locale.US, "REC · %.1fs · %d MIDI events", recordingElapsedSec, liveEventCount)
                            } else {
                                "${takes.size} saved studio takes · 48 kHz / 24-bit offline bounce"
                            },
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = 11.5.sp,
                            color = if (isRecording) RecordCrimsonLight else ConcertBrass
                        )
                    }

                    Row(
                        modifier = Modifier
                            .testTag("studio_record_button")
                            .height(42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isRecording) RecordCrimson else SurfaceSelected)
                            .border(
                                1.dp,
                                if (isRecording) RecordCrimsonLight else ConcertBrass,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { onToggleRecord() }
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.FiberManualRecord,
                            contentDescription = null,
                            tint = if (isRecording) IvoryText else RecordCrimsonLight,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = stringResource(if (isRecording) R.string.studio_stop_btn else R.string.studio_record_btn),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = IvoryText
                        )
                    }
                }
            }

            // Recorded Takes List
            takes.forEach { take ->
                val selectedVoicing = revoiceOverrides[take.id] ?: VoicingId.fromKey(take.voicingId)
                val isPlaying = playingTakeId == take.id
                val isRendering = renderingTakeId == take.id
                val noteCount = remember(take.id, take.events.size) {
                    take.events.count { it.type == MidiEventType.NOTE_ON }
                }
                val ccCount = remember(take.id, take.events.size) {
                    take.events.count { it.type == MidiEventType.CC }
                }
                var dropdownOpen by remember(take.id) { mutableStateOf(false) }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("studio_take_card_${take.id}")
                        .clip(RoundedCornerShape(14.dp))
                        .background(SurfaceCard)
                        .border(
                            1.dp,
                            if (isPlaying) ConcertBrass.copy(alpha = 0.6f) else BorderHairline,
                            RoundedCornerShape(14.dp)
                        )
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = take.name,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = IvoryText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = String.format(
                                    Locale.US,
                                    "%.1fs · %d notes · %d pedal CC events",
                                    take.durationSec,
                                    noteCount,
                                    ccCount
                                ),
                                fontFamily = JetBrainsMonoFamily,
                                fontSize = 11.sp,
                                color = MutedStoneText
                            )
                        }

                        IconButton(
                            onClick = { onDeleteTake(take.id) },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("studio_delete_${take.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = stringResource(R.string.studio_delete_take),
                                tint = SubtleAshText,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Non-Destructive Re-Voice Picker
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Non-Destructive Re-Voice",
                            fontSize = 11.5.sp,
                            color = WarmSandText
                        )
                        Box {
                            Row(
                                modifier = Modifier
                                    .testTag("studio_revoice_${take.id}")
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(ObsidianBg)
                                    .border(1.dp, ConcertBrass.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                                    .clickable { dropdownOpen = true }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = selectedVoicing.displayName,
                                    fontSize = 11.5.sp,
                                    color = ConcertBrassBright
                                )
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Choose re-voice instrument",
                                    tint = ConcertBrassBright,
                                    modifier = Modifier.size(15.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = dropdownOpen,
                                onDismissRequest = { dropdownOpen = false },
                                modifier = Modifier.background(SurfaceCard)
                            ) {
                                VoicingId.entries.forEach { v ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = v.displayName,
                                                fontSize = 12.5.sp,
                                                color = if (v == selectedVoicing) ConcertBrassBright else IvoryText
                                            )
                                        },
                                        onClick = {
                                            revoiceOverrides[take.id] = v
                                            dropdownOpen = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Action Row: Audition / Export .mid / Export 24-bit WAV
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("studio_play_${take.id}")
                                .clip(RoundedCornerShape(9.dp))
                                .background(if (isPlaying) ConcertBrass else SurfaceSelected)
                                .border(1.dp, ConcertBrass, RoundedCornerShape(9.dp))
                                .clickable {
                                    if (isPlaying) onStopPlayback() else onPlayTake(take, selectedVoicing)
                                },
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = if (isPlaying) ObsidianBg else ConcertBrassBright,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.size(4.dp))
                            Text(
                                text = stringResource(if (isPlaying) R.string.studio_stop else R.string.studio_audition),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isPlaying) ObsidianBg else ConcertBrassBright
                            )
                        }

                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("studio_export_mid_${take.id}")
                                .clip(RoundedCornerShape(9.dp))
                                .background(SurfaceControl)
                                .border(1.dp, BorderHairline, RoundedCornerShape(9.dp))
                                .clickable { onExportMidi(take) },
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                tint = WarmSandText,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(Modifier.size(4.dp))
                            Text(
                                text = stringResource(R.string.studio_export_mid),
                                fontSize = 11.5.sp,
                                color = WarmSandText
                            )
                        }

                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("studio_export_wav_${take.id}")
                                .clip(RoundedCornerShape(9.dp))
                                .background(SurfaceControl)
                                .border(1.dp, BorderHairline, RoundedCornerShape(9.dp))
                                .clickable(enabled = !isRendering) { onExportWav(take, selectedVoicing) },
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AudioFile,
                                contentDescription = null,
                                tint = ConcertBrass,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(Modifier.size(4.dp))
                            Text(
                                text = stringResource(if (isRendering) R.string.studio_rendering else R.string.studio_export_wav),
                                fontSize = 11.5.sp,
                                color = ConcertBrassBright
                            )
                        }
                    }
                }
            }
        }
    }
}
