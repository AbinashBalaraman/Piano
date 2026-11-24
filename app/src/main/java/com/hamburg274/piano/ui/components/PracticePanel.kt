package com.hamburg274.piano.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hamburg274.piano.R
import com.hamburg274.piano.music.ClassicalPiece
import com.hamburg274.piano.music.ExpressiveGradeReport
import com.hamburg274.piano.music.PianoTuning
import com.hamburg274.piano.ui.theme.BorderHairline
import com.hamburg274.piano.ui.theme.ConcertBrass
import com.hamburg274.piano.ui.theme.ConcertBrassBright
import com.hamburg274.piano.ui.theme.CormorantGaramondFamily
import com.hamburg274.piano.ui.theme.IvoryText
import com.hamburg274.piano.ui.theme.JetBrainsMonoFamily
import com.hamburg274.piano.ui.theme.LeftHandBlue
import com.hamburg274.piano.ui.theme.MutedStoneText
import com.hamburg274.piano.ui.theme.ObsidianBg
import com.hamburg274.piano.ui.theme.RecordCrimson
import com.hamburg274.piano.ui.theme.RecordCrimsonLight
import com.hamburg274.piano.ui.theme.SurfaceCard
import com.hamburg274.piano.ui.theme.SurfaceControl
import com.hamburg274.piano.ui.theme.SurfaceElevated
import com.hamburg274.piano.ui.theme.SurfaceSelected
import com.hamburg274.piano.ui.theme.VerifiedEmerald
import com.hamburg274.piano.ui.theme.WarmSandText
import com.hamburg274.piano.viewmodel.HandFilter
import com.hamburg274.piano.viewmodel.PracticeViewMode
import kotlin.math.roundToInt

@Composable
fun PracticePanel(
    repertoire: List<ClassicalPiece>,
    selectedPiece: ClassicalPiece,
    onSelectPiece: (String) -> Unit,
    onImportScoreClick: () -> Unit,
    importError: String?,
    viewMode: PracticeViewMode,
    onViewModeChange: (PracticeViewMode) -> Unit,
    isPlaying: Boolean,
    onTogglePlay: () -> Unit,
    onRestart: () -> Unit,
    currentTimeSec: Double,
    waitMode: Boolean,
    onWaitModeToggle: () -> Unit,
    waitingForNotes: List<Int>,
    handFilter: HandFilter,
    onHandFilterChange: (HandFilter) -> Unit,
    tempoPct: Int,
    onTempoPctChange: (Int) -> Unit,
    adaptiveRamp: Boolean,
    onAdaptiveRampToggle: () -> Unit,
    loopRange: Pair<Int, Int>?,
    onLoopRangeChange: (Pair<Int, Int>?) -> Unit,
    rubatoToleranceSec: Double,
    onRubatoChange: (Double) -> Unit,
    showFeedback: Boolean,
    onToggleFeedback: () -> Unit,
    gradeReport: ExpressiveGradeReport,
    startWhiteIndex: Int,
    visibleWhiteCount: Int,
    modifier: Modifier = Modifier
) {
    var pieceMenuExpanded by remember { mutableStateOf(false) }

    BackHandler(enabled = showFeedback) {
        onToggleFeedback()
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Primary Slim Transport Strip (scrolls cleanly on compact phones)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Piece Selector
                Box {
                    Row(
                        modifier = Modifier
                            .testTag("practice_piece_selector")
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceControl)
                            .border(1.dp, BorderHairline, RoundedCornerShape(8.dp))
                            .clickable { pieceMenuExpanded = true }
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = selectedPiece.title,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = IvoryText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Choose classical piece",
                            tint = MutedStoneText,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = pieceMenuExpanded,
                        onDismissRequest = { pieceMenuExpanded = false },
                        modifier = Modifier.background(SurfaceCard)
                    ) {
                        repertoire.forEach { piece ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(
                                            text = piece.title,
                                            color = if (piece.id == selectedPiece.id) ConcertBrassBright else IvoryText,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "${piece.composer} · ${piece.era} · ${piece.grade}",
                                            fontSize = 11.sp,
                                            color = MutedStoneText
                                        )
                                    }
                                },
                                onClick = {
                                    onSelectPiece(piece.id)
                                    pieceMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                // Import MIDI / MusicXML via Android SAF
                Row(
                    modifier = Modifier
                        .testTag("practice_import_button")
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceControl)
                        .border(1.dp, BorderHairline, RoundedCornerShape(8.dp))
                        .clickable { onImportScoreClick() }
                        .padding(horizontal = 9.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.UploadFile,
                        contentDescription = null,
                        tint = ConcertBrass,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = stringResource(R.string.practice_import_score),
                        fontSize = 11.sp,
                        color = WarmSandText
                    )
                }

                // Play / Pause
                Row(
                    modifier = Modifier
                        .testTag("practice_play_toggle")
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isPlaying) ConcertBrass else SurfaceSelected)
                        .border(1.dp, ConcertBrass, RoundedCornerShape(8.dp))
                        .clickable { onTogglePlay() }
                        .padding(horizontal = 11.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = if (isPlaying) ObsidianBg else ConcertBrassBright,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = stringResource(if (isPlaying) R.string.practice_pause else R.string.practice_play),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isPlaying) ObsidianBg else ConcertBrassBright
                    )
                }

                // Restart
                Box(
                    modifier = Modifier
                        .testTag("practice_restart_button")
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceControl)
                        .border(1.dp, BorderHairline, RoundedCornerShape(8.dp))
                        .clickable { onRestart() }
                        .padding(horizontal = 8.dp, vertical = 7.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Replay,
                        contentDescription = stringResource(R.string.practice_restart),
                        tint = WarmSandText,
                        modifier = Modifier.size(15.dp)
                    )
                }

                // Smart Wait Toggle
                Box(
                    modifier = Modifier
                        .testTag("practice_smart_wait_toggle")
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (waitMode) SurfaceSelected else SurfaceControl)
                        .border(1.dp, if (waitMode) ConcertBrass else BorderHairline, RoundedCornerShape(8.dp))
                        .clickable { onWaitModeToggle() }
                        .padding(horizontal = 9.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = stringResource(R.string.practice_smart_wait),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (waitMode) ConcertBrassBright else MutedStoneText
                    )
                }

                // Hand Isolation Segmented Switch
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(ObsidianBg)
                        .border(1.dp, BorderHairline, RoundedCornerShape(8.dp))
                        .padding(2.dp)
                ) {
                    HandFilter.entries.forEach { hf ->
                        val active = handFilter == hf
                        Box(
                            modifier = Modifier
                                .testTag("practice_hand_${hf.name.lowercase()}")
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (active) Color(0xFF272732) else Color.Transparent)
                                .clickable { onHandFilterChange(hf) }
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = hf.label,
                                fontSize = 11.sp,
                                color = if (active) IvoryText else MutedStoneText
                            )
                        }
                    }
                }

                // View Mode Switch (Urtext / Split / Waterfall)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(ObsidianBg)
                        .border(1.dp, BorderHairline, RoundedCornerShape(8.dp))
                        .padding(2.dp)
                ) {
                    PracticeViewMode.entries.forEach { vm ->
                        val active = viewMode == vm
                        Box(
                            modifier = Modifier
                                .testTag("practice_view_${vm.name.lowercase()}")
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (active) Color(0xFF272732) else Color.Transparent)
                                .clickable { onViewModeChange(vm) }
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = vm.label,
                                fontSize = 11.sp,
                                color = if (active) IvoryText else MutedStoneText
                            )
                        }
                    }
                }

                // 4D Expressive Grade Drawer Button
                Row(
                    modifier = Modifier
                        .testTag("practice_4d_grade_button")
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (showFeedback) SurfaceSelected else SurfaceControl)
                        .border(1.dp, if (showFeedback) ConcertBrass else BorderHairline, RoundedCornerShape(8.dp))
                        .clickable { onToggleFeedback() }
                        .padding(horizontal = 9.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Assessment,
                        contentDescription = null,
                        tint = ConcertBrass,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "${stringResource(R.string.practice_4d_grade)} · ${gradeReport.overallScore}%",
                        fontFamily = JetBrainsMonoFamily,
                        fontSize = 11.sp,
                        color = ConcertBrassBright
                    )
                }
            }

            // Secondary Conservatory Row: Tempo, Auto-Ramp, A-B Loop, Rubato Tolerance
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val effectiveBpm = ((selectedPiece.defaultBpm * tempoPct) / 100f).roundToInt()
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Tempo $tempoPct% ($effectiveBpm BPM)",
                        fontFamily = JetBrainsMonoFamily,
                        fontSize = 10.5.sp,
                        color = WarmSandText
                    )
                    Slider(
                        value = tempoPct.toFloat(),
                        onValueChange = { onTempoPctChange(it.roundToInt()) },
                        valueRange = 40f..125f,
                        modifier = Modifier
                            .width(96.dp)
                            .height(24.dp)
                            .testTag("practice_tempo_slider"),
                        colors = SliderDefaults.colors(
                            thumbColor = ConcertBrassBright,
                            activeTrackColor = ConcertBrass,
                            inactiveTrackColor = SurfaceControl
                        )
                    )
                }

                // Auto-Ramp +5%
                Box(
                    modifier = Modifier
                        .testTag("practice_auto_ramp_toggle")
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (adaptiveRamp) SurfaceSelected else SurfaceControl)
                        .border(1.dp, if (adaptiveRamp) ConcertBrass else BorderHairline, RoundedCornerShape(6.dp))
                        .clickable { onAdaptiveRampToggle() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.practice_auto_ramp),
                        fontSize = 10.5.sp,
                        color = if (adaptiveRamp) ConcertBrassBright else MutedStoneText
                    )
                }

                // A-B Measure Loop Toggle & Steppers
                val maxM = selectedPiece.measures.coerceAtLeast(1)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (loopRange != null) SurfaceSelected else SurfaceControl)
                        .border(1.dp, if (loopRange != null) ConcertBrass else BorderHairline, RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .testTag("practice_ab_loop_toggle")
                            .clickable {
                                if (loopRange != null) {
                                    onLoopRangeChange(null)
                                } else {
                                    onLoopRangeChange(1 to 2.coerceAtMost(maxM))
                                }
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Loop,
                            contentDescription = null,
                            tint = if (loopRange != null) ConcertBrassBright else MutedStoneText,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = if (loopRange == null) stringResource(R.string.practice_ab_loop) else "m.${loopRange.first}–m.${loopRange.second}",
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = 10.5.sp,
                            color = if (loopRange != null) ConcertBrassBright else MutedStoneText
                        )
                    }
                    if (loopRange != null && maxM > 1) {
                        Text(
                            text = "A+",
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = 10.sp,
                            color = IvoryText,
                            modifier = Modifier
                                .testTag("practice_loop_start_step")
                                .clickable {
                                    val nextA = if (loopRange.first >= loopRange.second) 1 else loopRange.first + 1
                                    onLoopRangeChange(nextA to loopRange.second)
                                }
                                .padding(horizontal = 3.dp)
                        )
                        Text(
                            text = "B+",
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = 10.sp,
                            color = IvoryText,
                            modifier = Modifier
                                .testTag("practice_loop_end_step")
                                .clickable {
                                    val nextB = if (loopRange.second >= maxM) loopRange.first else loopRange.second + 1
                                    onLoopRangeChange(loopRange.first to nextB)
                                }
                                .padding(horizontal = 3.dp)
                        )
                    }
                }

                // Rubato Tolerance Slider
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Rubato ±${(rubatoToleranceSec * 1000).roundToInt()}ms",
                        fontFamily = JetBrainsMonoFamily,
                        fontSize = 10.5.sp,
                        color = MutedStoneText
                    )
                    Slider(
                        value = rubatoToleranceSec.toFloat(),
                        onValueChange = { onRubatoChange(it.toDouble()) },
                        valueRange = 0.08f..0.34f,
                        modifier = Modifier
                            .width(80.dp)
                            .height(24.dp)
                            .testTag("practice_rubato_slider"),
                        colors = SliderDefaults.colors(
                            thumbColor = ConcertBrassBright,
                            activeTrackColor = ConcertBrass,
                            inactiveTrackColor = SurfaceControl
                        )
                    )
                }
            }

            // Optional Import Error Banner
            if (importError != null) {
                Text(
                    text = importError,
                    fontSize = 11.sp,
                    color = RecordCrimsonLight,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(RecordCrimson.copy(alpha = 0.25f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }

            // Smart Wait Prompt
            if (waitMode && waitingForNotes.isNotEmpty()) {
                val names = waitingForNotes.joinToString(" + ") { PianoTuning.midiToNoteName(it) }
                Text(
                    text = "Smart Wait — Strike $names to advance score",
                    fontFamily = JetBrainsMonoFamily,
                    fontSize = 11.sp,
                    color = ConcertBrassBright,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceSelected)
                        .border(1.dp, ConcertBrass.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }

            // Engraved Urtext Grand Staff + Falling-Note Waterfall
            UrtextScoreCanvas(
                notes = selectedPiece.notes,
                currentTimeSec = currentTimeSec,
                durationSec = selectedPiece.durationSec,
                startWhiteIndex = startWhiteIndex,
                visibleWhiteCount = visibleWhiteCount,
                viewMode = viewMode,
                loopRange = loopRange,
                handFilter = handFilter,
                pieceTitle = selectedPiece.title,
                composer = selectedPiece.composer,
                timeSignatureNum = selectedPiece.timeSignatureNum,
                timeSignatureDen = selectedPiece.timeSignatureDen,
                keySignature = selectedPiece.keySignature,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )
        }

        // Collapsible 4D Expressive Grading Overlay
        if (showFeedback) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable { onToggleFeedback() }
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceElevated)
                        .border(1.dp, ConcertBrass.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                        .clickable(enabled = false) {}
                        .padding(16.dp)
                        .testTag("practice_4d_grade_dialog"),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.practice_expressive_grading),
                                fontFamily = CormorantGaramondFamily,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = IvoryText
                            )
                            Text(
                                text = "${selectedPiece.title} · Overall ${gradeReport.overallScore}%",
                                fontFamily = JetBrainsMonoFamily,
                                fontSize = 11.sp,
                                color = ConcertBrass
                            )
                        }
                        IconButton(
                            onClick = onToggleFeedback,
                            modifier = Modifier.testTag("practice_close_grade_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.practice_close),
                                tint = MutedStoneText
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GradeMetricCard("Pitch", gradeReport.pitchAccuracy, ConcertBrass, Modifier.weight(1f))
                        GradeMetricCard("Rubato Rhythm", gradeReport.rhythmicPrecision, LeftHandBlue, Modifier.weight(1f))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GradeMetricCard("Dynamics", gradeReport.dynamicFidelity, VerifiedEmerald, Modifier.weight(1f))
                        GradeMetricCard("CC64 Pedal", gradeReport.pedalingCleanliness, ConcertBrassBright, Modifier.weight(1f))
                    }

                    Text(
                        text = gradeReport.summary,
                        fontSize = 12.sp,
                        color = WarmSandText
                    )
                    Text(
                        text = "Matched ${gradeReport.matchedNotes}/${gradeReport.totalExpectedNotes} notes · Extra strikes: ${gradeReport.extraWrongNotes} · Clean pedal changes: ${gradeReport.cleanPedalChanges}/${gradeReport.harmonicChangesEvaluated}",
                        fontFamily = JetBrainsMonoFamily,
                        fontSize = 10.5.sp,
                        color = MutedStoneText
                    )
                }
            }
        }
    }
}

@Composable
private fun GradeMetricCard(
    title: String,
    score: Int,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(ObsidianBg)
            .border(1.dp, BorderHairline, RoundedCornerShape(10.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(text = title, fontSize = 11.sp, color = MutedStoneText)
        Text(
            text = "$score%",
            fontFamily = JetBrainsMonoFamily,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = accent
        )
    }
}
