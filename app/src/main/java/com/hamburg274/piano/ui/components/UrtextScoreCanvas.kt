package com.hamburg274.piano.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hamburg274.piano.music.ExpectedScoreNote
import com.hamburg274.piano.music.HandPart
import com.hamburg274.piano.music.PianoTuning
import com.hamburg274.piano.ui.theme.BorderHairline
import com.hamburg274.piano.ui.theme.ConcertBrass
import com.hamburg274.piano.ui.theme.ConcertBrassBright
import com.hamburg274.piano.ui.theme.CormorantGaramondFamily
import com.hamburg274.piano.ui.theme.InkDark
import com.hamburg274.piano.ui.theme.InkMuted
import com.hamburg274.piano.ui.theme.JetBrainsMonoFamily
import com.hamburg274.piano.ui.theme.LeftHandBlue
import com.hamburg274.piano.ui.theme.LeftHandBlueGlow
import com.hamburg274.piano.ui.theme.MutedStoneText
import com.hamburg274.piano.ui.theme.ParchmentBg
import com.hamburg274.piano.ui.theme.ParchmentHeader
import com.hamburg274.piano.viewmodel.HandFilter
import com.hamburg274.piano.viewmodel.PracticeViewMode
import java.util.Locale
import kotlin.math.floor
import kotlin.math.max

private val PC_TO_DIATONIC = intArrayOf(0, 0, 1, 1, 2, 3, 3, 4, 4, 5, 5, 6)

private fun midiToDiatonicStep(midi: Int): Int {
    val octave = floor(midi / 12.0).toInt() - 1
    val pc = ((midi % 12) + 12) % 12
    return octave * 7 + PC_TO_DIATONIC[pc]
}

@Composable
fun UrtextScoreCanvas(
    notes: List<ExpectedScoreNote>,
    currentTimeSec: Double,
    durationSec: Double,
    startWhiteIndex: Int,
    visibleWhiteCount: Int,
    viewMode: PracticeViewMode,
    loopRange: Pair<Int, Int>?,
    handFilter: HandFilter,
    pieceTitle: String,
    composer: String,
    timeSignatureNum: Int,
    timeSignatureDen: Int,
    keySignature: String,
    modifier: Modifier = Modifier
) {
    val filteredNotes = remember(notes, handFilter) {
        notes.filter { n ->
            when (handFilter) {
                HandFilter.BOTH -> true
                HandFilter.RH -> n.hand == HandPart.RH
                HandFilter.LH -> n.hand == HandPart.LH
            }
        }
    }

    val measureBounds = remember(notes) {
        val map = linkedMapOf<Int, Double>()
        for (n in notes) {
            val prev = map[n.measure]
            if (prev == null || n.startSec < prev) map[n.measure] = n.startSec
        }
        map.entries.sortedBy { it.key }
    }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // 1. Engraved Urtext Parchment Grand Staff
        if (viewMode == PracticeViewMode.URTEXT || viewMode == PracticeViewMode.SPLIT) {
            val staffMod = if (viewMode == PracticeViewMode.URTEXT) {
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
            } else {
                Modifier
                    .weight(0.56f)
                    .fillMaxWidth()
            }

            Column(
                modifier = staffMod
                    .padding(horizontal = 12.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(ParchmentBg)
                    .border(1.dp, ConcertBrass.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                    .testTag("urtext_score_sheet")
            ) {
                // Score Header Strip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ParchmentHeader)
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = pieceTitle,
                            fontFamily = CormorantGaramondFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = InkDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = composer,
                            fontSize = 11.sp,
                            color = InkMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = String.format(
                            Locale.US,
                            "%s · %d/%d · %.1fs / %.1fs",
                            keySignature,
                            timeSignatureNum,
                            timeSignatureDen,
                            currentTimeSec,
                            durationSec
                        ),
                        fontFamily = JetBrainsMonoFamily,
                        fontSize = 10.sp,
                        color = InkMuted
                    )
                }

                // Grand Staff Engraving Canvas
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 4.dp)
                ) {
                    val windowDur = 6.8
                    val windowStart = max(0.0, currentTimeSec - 1.25)
                    val windowEnd = windowStart + windowDur

                    val leftMargin = 58.dp.toPx()
                    val rightPad = 14.dp.toPx()
                    val playableW = (size.width - leftMargin - rightPad).coerceAtLeast(10f)

                    val stepPx = (size.height / 42f).coerceIn(2.6f, 5.2f)
                    val trebleBottomY = size.height * 0.42f
                    val bassTopY = size.height * 0.62f

                    fun timeToX(sec: Double): Float =
                        leftMargin + (((sec - windowStart) / windowDur).toFloat() * playableW)

                    fun noteY(midi: Int, hand: HandPart): Float {
                        val step = midiToDiatonicStep(midi)
                        val raw = if (hand == HandPart.RH) {
                            trebleBottomY - (step - 30) * stepPx
                        } else {
                            bassTopY - (step - 26) * stepPx
                        }
                        return raw.coerceIn(10f, size.height - 10f)
                    }

                    // A-B Loop Highlight Tint
                    if (loopRange != null) {
                        val startEntry = measureBounds.firstOrNull { it.key == loopRange.first }
                        val endEntry = measureBounds.firstOrNull { it.key == loopRange.second + 1 }
                        if (startEntry != null) {
                            val x1 = max(leftMargin, timeToX(startEntry.value))
                            val x2 = timeToX(endEntry?.value ?: durationSec).coerceAtMost(size.width - rightPad)
                            if (x2 > x1) {
                                drawRect(
                                    color = ConcertBrass.copy(alpha = 0.16f),
                                    topLeft = Offset(x1, 6f),
                                    size = Size(x2 - x1, size.height - 12f)
                                )
                            }
                        }
                    }

                    // System brace line
                    val staffTop = trebleBottomY - 8 * stepPx
                    val staffBottom = bassTopY + 8 * stepPx
                    drawLine(
                        color = InkDark,
                        start = Offset(12.dp.toPx(), staffTop),
                        end = Offset(12.dp.toPx(), staffBottom),
                        strokeWidth = 2.dp.toPx()
                    )

                    // Treble & Bass 5-line staves
                    for (line in 0..4) {
                        val ty = trebleBottomY - line * (stepPx * 2f)
                        drawLine(InkDark.copy(alpha = 0.42f), Offset(12.dp.toPx(), ty), Offset(size.width - rightPad, ty), 1f)
                        val by = bassTopY + line * (stepPx * 2f)
                        drawLine(InkDark.copy(alpha = 0.42f), Offset(12.dp.toPx(), by), Offset(size.width - rightPad, by), 1f)
                    }

                    val clefPaint = android.graphics.Paint().apply {
                        isAntiAlias = true
                        color = android.graphics.Color.rgb(26, 25, 22)
                        textSize = stepPx * 7.8f
                        typeface = android.graphics.Typeface.SERIF
                    }
                    drawContext.canvas.nativeCanvas.drawText("𝄞", 16.dp.toPx(), trebleBottomY + stepPx * 0.5f, clefPaint)
                    clefPaint.textSize = stepPx * 6.0f
                    drawContext.canvas.nativeCanvas.drawText("𝄢", 17.dp.toPx(), bassTopY + stepPx * 5.6f, clefPaint)

                    val sigPaint = android.graphics.Paint().apply {
                        isAntiAlias = true
                        color = android.graphics.Color.rgb(26, 25, 22)
                        textSize = stepPx * 3.4f
                        isFakeBoldText = true
                        typeface = android.graphics.Typeface.SERIF
                    }
                    val sigX = leftMargin - 16.dp.toPx()
                    drawContext.canvas.nativeCanvas.drawText("$timeSignatureNum", sigX, trebleBottomY - stepPx * 4.2f, sigPaint)
                    drawContext.canvas.nativeCanvas.drawText("$timeSignatureDen", sigX, trebleBottomY - stepPx * 0.4f, sigPaint)
                    drawContext.canvas.nativeCanvas.drawText("$timeSignatureNum", sigX, bassTopY + stepPx * 3.4f, sigPaint)
                    drawContext.canvas.nativeCanvas.drawText("$timeSignatureDen", sigX, bassTopY + stepPx * 7.2f, sigPaint)

                    // Measure Barlines
                    val barPaint = android.graphics.Paint().apply {
                        isAntiAlias = true
                        color = android.graphics.Color.rgb(109, 102, 89)
                        textSize = 9.sp.toPx()
                        typeface = android.graphics.Typeface.MONOSPACE
                    }
                    for ((mNum, sec) in measureBounds) {
                        val x = timeToX(sec)
                        if (x < leftMargin - 2f || x > size.width - rightPad) continue
                        drawLine(InkDark.copy(alpha = 0.28f), Offset(x, staffTop), Offset(x, staffBottom), 1f)
                        drawContext.canvas.nativeCanvas.drawText("m.$mNum", x + 3.dp.toPx(), staffTop - 3.dp.toPx(), barPaint)
                    }

                    // Engraved Notes, Ledger Lines, Accidentals, Fingerings, Dynamics
                    val fingPaint = android.graphics.Paint().apply {
                        isAntiAlias = true
                        color = android.graphics.Color.rgb(102, 95, 82)
                        textSize = 8.5.sp.toPx()
                        textAlign = android.graphics.Paint.Align.CENTER
                        typeface = android.graphics.Typeface.MONOSPACE
                    }
                    val dynPaint = android.graphics.Paint().apply {
                        isAntiAlias = true
                        color = android.graphics.Color.rgb(122, 36, 45)
                        textSize = 11.5.sp.toPx()
                        textAlign = android.graphics.Paint.Align.CENTER
                        typeface = android.graphics.Typeface.create(android.graphics.Typeface.SERIF, android.graphics.Typeface.BOLD_ITALIC)
                    }
                    val sharpPaint = android.graphics.Paint().apply {
                        isAntiAlias = true
                        textSize = 10.sp.toPx()
                        isFakeBoldText = true
                    }

                    for (n in filteredNotes) {
                        if (n.startSec + n.durationSec < windowStart - 0.3 || n.startSec > windowEnd + 0.3) continue
                        val x = timeToX(n.startSec)
                        if (x < leftMargin - 6f || x > size.width - rightPad) continue
                        val y = noteY(n.midi, n.hand)

                        val isActive = currentTimeSec >= n.startSec - 0.08 && currentTimeSec <= n.startSec + max(0.2, n.durationSec)
                        val isPast = currentTimeSec > n.startSec + n.durationSec
                        val sharp = PianoTuning.isBlackKey(n.midi)
                        val isOpenHead = n.durationSec >= 1.1

                        val noteColor = when {
                            isActive && n.hand == HandPart.RH -> Color(0xFFA87924)
                            isActive -> Color(0xFF2F5F8F)
                            isPast -> Color(0xFF8C8577)
                            else -> InkDark
                        }

                        // Middle C ledger line
                        if (n.midi == 60) {
                            drawLine(InkDark, Offset(x - 7.dp.toPx(), y), Offset(x + 7.dp.toPx(), y), 1.1f)
                        }

                        if (sharp) {
                            sharpPaint.color = android.graphics.Color.argb(
                                (noteColor.alpha * 255).toInt(),
                                (noteColor.red * 255).toInt(),
                                (noteColor.green * 255).toInt(),
                                (noteColor.blue * 255).toInt()
                            )
                            drawContext.canvas.nativeCanvas.drawText("♯", x - 9.dp.toPx(), y + 3.dp.toPx(), sharpPaint)
                        }

                        // Stem
                        val stemUp = if (n.hand == HandPart.RH) y >= trebleBottomY - 4 * stepPx else y >= bassTopY + 4 * stepPx
                        val rx = 4.2.dp.toPx()
                        val ry = 3.0.dp.toPx()
                        val stemX = if (stemUp) x + rx * 0.85f else x - rx * 0.85f
                        val stemY2 = if (stemUp) y - 18.dp.toPx() else y + 18.dp.toPx()
                        drawLine(noteColor, Offset(stemX, y), Offset(stemX, stemY2), 1.2.dp.toPx())

                        // Tilted Notehead
                        withTransform({ rotate(-18f, Offset(x, y)) }) {
                            drawOval(
                                color = if (isOpenHead) ParchmentBg else noteColor,
                                topLeft = Offset(x - rx, y - ry),
                                size = Size(rx * 2f, ry * 2f)
                            )
                            if (isOpenHead) {
                                drawOval(
                                    color = noteColor,
                                    topLeft = Offset(x - rx, y - ry),
                                    size = Size(rx * 2f, ry * 2f),
                                    style = Stroke(width = 1.4.dp.toPx())
                                )
                            }
                        }

                        n.fingering?.let { f ->
                            val fy = if (n.hand == HandPart.RH) (y - 9.dp.toPx()).coerceAtLeast(10f) else (y + 12.dp.toPx()).coerceAtMost(size.height - 4f)
                            drawContext.canvas.nativeCanvas.drawText("$f", x, fy, fingPaint)
                        }

                        n.dynamicLabel?.let { dyn ->
                            drawContext.canvas.nativeCanvas.drawText(dyn, x, (trebleBottomY + bassTopY) * 0.52f, dynPaint)
                        }
                    }

                    // Playhead cursor
                    val playX = timeToX(currentTimeSec)
                    drawLine(
                        color = Color(0xFFB38632),
                        start = Offset(playX, 6f),
                        end = Offset(playX, size.height - 6f),
                        strokeWidth = 2.dp.toPx()
                    )
                    val tri = Path().apply {
                        moveTo(playX - 4.dp.toPx(), 6f)
                        lineTo(playX + 4.dp.toPx(), 6f)
                        lineTo(playX, 13.dp.toPx())
                        close()
                    }
                    drawPath(tri, Color(0xFFB38632))
                }
            }
        }

        // 2. Synchronized Keybed-Aligned Falling-Note Waterfall
        if (viewMode == PracticeViewMode.WATERFALL || viewMode == PracticeViewMode.SPLIT) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color(0xFF0B0B0F))
                    .border(width = 0.5.dp, color = BorderHairline)
                    .testTag("falling_note_waterfall")
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    // Left 48.dp gutter aligned with PianoKeyboard's CC64 sustain column
                    Box(
                        modifier = Modifier
                            .width(48.dp)
                            .fillMaxHeight()
                            .background(Color(0xFF111116))
                            .border(width = 0.5.dp, color = BorderHairline),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Text(
                            text = "PED",
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = 9.sp,
                            color = MutedStoneText,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }

                    Canvas(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        val lookahead = 3.2
                        val clampedStart = startWhiteIndex.coerceIn(0, PianoTuning.WHITE_KEY_COUNT - visibleWhiteCount)
                        val whiteW = size.width / visibleWhiteCount.toFloat()

                        // Octave vertical grid aligned with white keys
                        for (idx in 0 until visibleWhiteCount) {
                            val wk = PianoTuning.WHITE_KEYS[clampedStart + idx]
                            val x = idx * whiteW
                            drawLine(
                                color = if (wk.pitchClass == 0) ConcertBrass.copy(alpha = 0.24f) else Color.White.copy(alpha = 0.04f),
                                start = Offset(x, 0f),
                                end = Offset(x, size.height),
                                strokeWidth = 1f
                            )
                        }

                        val firstMidi = PianoTuning.WHITE_KEYS[clampedStart].midi
                        val lastMidi = PianoTuning.WHITE_KEYS[clampedStart + visibleWhiteCount - 1].midi

                        for (n in filteredNotes) {
                            if (n.startSec + n.durationSec < currentTimeSec - 0.1 || n.startSec > currentTimeSec + lookahead) continue
                            if (n.midi < firstMidi || n.midi > lastMidi + 1) continue
                            val keyMeta = PianoTuning.ALL_88_KEYS.getOrNull(n.midi - 21) ?: continue

                            val leftX: Float
                            val barW: Float
                            if (!keyMeta.isBlack) {
                                val localIdx = keyMeta.whiteIndex - clampedStart
                                leftX = localIdx * whiteW + whiteW * 0.10f
                                barW = whiteW * 0.80f
                            } else {
                                val localIdx = keyMeta.whiteIndex - clampedStart
                                barW = whiteW * 0.56f
                                leftX = (localIdx + 1) * whiteW - barW * 0.5f
                            }

                            val timeUntilHit = n.startSec - currentTimeSec
                            val bottomY = size.height * (1f - (timeUntilHit / lookahead).toFloat())
                            val barH = max(10.dp.toPx(), ((n.durationSec / lookahead).toFloat() * size.height))
                            val topY = bottomY - barH
                            val isHit = timeUntilHit <= 0.08 && timeUntilHit + n.durationSec >= -0.05

                            val fillColor = when {
                                n.hand == HandPart.RH && isHit -> ConcertBrassBright
                                n.hand == HandPart.RH -> ConcertBrass.copy(alpha = 0.86f)
                                isHit -> LeftHandBlueGlow
                                else -> LeftHandBlue.copy(alpha = 0.86f)
                            }

                            drawRoundRect(
                                color = fillColor,
                                topLeft = Offset(leftX, topY),
                                size = Size(barW, barH),
                                cornerRadius = CornerRadius(3.dp.toPx())
                            )
                        }

                        // Strike line at bottom of waterfall
                        drawRect(
                            brush = Brush.horizontalGradient(
                                listOf(
                                    ConcertBrass.copy(alpha = 0.25f),
                                    ConcertBrass.copy(alpha = 0.85f),
                                    ConcertBrass.copy(alpha = 0.25f)
                                )
                            ),
                            topLeft = Offset(0f, size.height - 2.dp.toPx()),
                            size = Size(size.width, 2.dp.toPx())
                        )
                    }
                }

                // Hand legend
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 56.dp, top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(9.dp, 7.dp).clip(RoundedCornerShape(2.dp)).background(ConcertBrass))
                        Text("Right Hand", fontSize = 10.sp, color = MutedStoneText)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(9.dp, 7.dp).clip(RoundedCornerShape(2.dp)).background(LeftHandBlue))
                        Text("Left Hand", fontSize = 10.sp, color = MutedStoneText)
                    }
                }
            }
        }
    }
}
