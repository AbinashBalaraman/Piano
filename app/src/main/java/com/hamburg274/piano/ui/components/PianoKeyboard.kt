package com.hamburg274.piano.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hamburg274.piano.R
import com.hamburg274.piano.music.HandPart
import com.hamburg274.piano.music.PianoTuning
import com.hamburg274.piano.music.TouchVelocityMode
import com.hamburg274.piano.music.VelocityCurvePoint
import com.hamburg274.piano.music.VelocityEngine
import com.hamburg274.piano.ui.theme.BorderHairline
import com.hamburg274.piano.ui.theme.ConcertBrass
import com.hamburg274.piano.ui.theme.ConcertBrassBright
import com.hamburg274.piano.ui.theme.ConcertBrassDeep
import com.hamburg274.piano.ui.theme.IvoryText
import com.hamburg274.piano.ui.theme.JetBrainsMonoFamily
import com.hamburg274.piano.ui.theme.LeftHandBlue
import com.hamburg274.piano.ui.theme.MutedStoneText
import com.hamburg274.piano.ui.theme.ObsidianBg
import com.hamburg274.piano.ui.theme.RosewoodFeltBottom
import com.hamburg274.piano.ui.theme.RosewoodFeltTop
import com.hamburg274.piano.ui.theme.SurfaceCard
import com.hamburg274.piano.ui.theme.SurfaceControl
import com.hamburg274.piano.ui.theme.SurfaceSelected
import com.hamburg274.piano.viewmodel.GuideNoteHighlight
import com.hamburg274.piano.viewmodel.KeyboardLayoutMode
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt

@Composable
fun PianoKeyboard(
    activeNotes: Set<Int>,
    guideNotes: List<GuideNoteHighlight>,
    startWhiteIndex: Int,
    visibleWhiteCount: Int,
    onViewportChange: (Int, Int) -> Unit,
    layoutMode: KeyboardLayoutMode,
    onLayoutModeChange: (KeyboardLayoutMode) -> Unit,
    keyLock: Boolean,
    onKeyLockToggle: () -> Unit,
    showLabels: Boolean,
    onShowLabelsToggle: () -> Unit,
    touchVelocityMode: TouchVelocityMode,
    fixedVelocity: Int,
    velocityCurve: List<VelocityCurvePoint>,
    damperCC64: Int,
    onDamperChange: (Int) -> Unit,
    sostenuto: Boolean,
    onSostenutoToggle: () -> Unit,
    unaCorda: Boolean,
    onUnaCordaToggle: () -> Unit,
    lastVelocity: Int?,
    lastNoteName: String?,
    onNoteOn: (midi: Int, finalVel: Int, rawVel: Int) -> Unit,
    onNoteOff: (midi: Int) -> Unit,
    compactHeight: Boolean,
    fillAvailableHeight: Boolean = false,
    modifier: Modifier = Modifier
) {
    val guideMap = remember(guideNotes) {
        guideNotes.associate { it.midi to it.hand }
    }
    val damperPct = ((damperCC64 / 127f) * 100f).roundToInt()

    val clampedStart = startWhiteIndex.coerceIn(0, PianoTuning.WHITE_KEY_COUNT - visibleWhiteCount)
    val rangeStartName = PianoTuning.WHITE_KEYS[clampedStart].name
    val rangeEndName = PianoTuning.WHITE_KEYS[(clampedStart + visibleWhiteCount - 1).coerceAtMost(PianoTuning.WHITE_KEY_COUNT - 1)].name

    // Musical dual-manual pairing: Treble stays on main anchor (e.g. C4..E5), Bass sits an octave lower (C3..E4)
    val trebleStart = clampedStart
    val bassStart = (clampedStart - 7).coerceIn(0, PianoTuning.WHITE_KEY_COUNT - visibleWhiteCount)

    val fixedKeybedHeight = when {
        compactHeight && layoutMode == KeyboardLayoutMode.SINGLE -> 186.dp
        compactHeight -> 252.dp
        layoutMode == KeyboardLayoutMode.SINGLE -> 244.dp
        else -> 310.dp
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF131318))
            .border(width = 0.5.dp, color = BorderHairline)
    ) {
        // Two-Row Zero-Clipping Keybed Control Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceCard)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Row 1: Full-Width 88-Key Interactive Compass + Zoom -/+ + Range Readout
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                IconButton(
                    onClick = { onViewportChange(clampedStart, (visibleWhiteCount + 2).coerceAtMost(24)) },
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(SurfaceControl)
                        .testTag("keybed_zoom_out")
                ) {
                    Icon(
                        imageVector = Icons.Default.ZoomOut,
                        contentDescription = stringResource(R.string.cd_zoom_out),
                        tint = MutedStoneText,
                        modifier = Modifier.size(15.dp)
                    )
                }

                OctaveCompassBar(
                    activeNotes = activeNotes,
                    startWhiteIndex = clampedStart,
                    visibleWhiteCount = visibleWhiteCount,
                    onViewportChange = onViewportChange,
                    modifier = Modifier
                        .weight(1f)
                        .height(26.dp)
                        .testTag("octave_compass_bar")
                )

                IconButton(
                    onClick = { onViewportChange(clampedStart, (visibleWhiteCount - 2).coerceAtLeast(7)) },
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(SurfaceControl)
                        .testTag("keybed_zoom_in")
                ) {
                    Icon(
                        imageVector = Icons.Default.ZoomIn,
                        contentDescription = stringResource(R.string.cd_zoom_in),
                        tint = MutedStoneText,
                        modifier = Modifier.size(15.dp)
                    )
                }

                Text(
                    text = "$rangeStartName–$rangeEndName",
                    fontFamily = JetBrainsMonoFamily,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = ConcertBrass
                )
            }

            // Row 2: Layout Mode + Key-Lock + Labels + Una Corda & Sostenuto Pedals (All Visible!)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    // Single / Dual / Duet Switcher
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(7.dp))
                            .background(ObsidianBg)
                            .border(1.dp, BorderHairline, RoundedCornerShape(7.dp))
                            .padding(2.dp)
                    ) {
                        KeyboardLayoutMode.entries.forEach { mode ->
                            val selected = layoutMode == mode
                            Box(
                                modifier = Modifier
                                    .testTag("layout_mode_${mode.name.lowercase()}")
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(if (selected) Color(0xFF282833) else Color.Transparent)
                                    .clickable { onLayoutModeChange(mode) }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = mode.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (selected) IvoryText else MutedStoneText,
                                    fontSize = 10.5.sp
                                )
                            }
                        }
                    }

                    // Key-Lock vs Glissando
                    Row(
                        modifier = Modifier
                            .testTag("key_lock_toggle")
                            .clip(RoundedCornerShape(7.dp))
                            .background(if (keyLock) SurfaceSelected else SurfaceControl)
                            .border(
                                1.dp,
                                if (keyLock) ConcertBrass.copy(alpha = 0.45f) else BorderHairline,
                                RoundedCornerShape(7.dp)
                            )
                            .clickable { onKeyLockToggle() }
                            .padding(horizontal = 7.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = if (keyLock) Icons.Default.Lock else Icons.Default.OpenWith,
                            contentDescription = stringResource(if (keyLock) R.string.keybed_key_lock else R.string.keybed_glissando),
                            tint = if (keyLock) ConcertBrassBright else MutedStoneText,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = if (keyLock) "Lock" else "Gliss",
                            fontSize = 10.5.sp,
                            color = if (keyLock) ConcertBrassBright else MutedStoneText,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Note Labels toggle
                    Box(
                        modifier = Modifier
                            .testTag("note_labels_toggle")
                            .clip(RoundedCornerShape(7.dp))
                            .background(if (showLabels) Color(0xFF282833) else SurfaceControl)
                            .border(1.dp, BorderHairline, RoundedCornerShape(7.dp))
                            .clickable { onShowLabelsToggle() }
                            .padding(horizontal = 7.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "C4",
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = 10.5.sp,
                            color = if (showLabels) IvoryText else MutedStoneText
                        )
                    }
                }

                // Right-aligned Una Corda & Sostenuto Pedals (always immediately reachable)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    PedalToggleChip(
                        label = "UC",
                        engaged = unaCorda,
                        onClick = onUnaCordaToggle,
                        tag = "pedal_una_corda"
                    )
                    PedalToggleChip(
                        label = "Sos",
                        engaged = sostenuto,
                        onClick = onSostenutoToggle,
                        tag = "pedal_sostenuto"
                    )
                }
            }
        }

        // Main Keybed + Thumb Vertical-Slide Continuous Sustain Pedal (CC64)
        Row(
            modifier = if (fillAvailableHeight) {
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
            } else {
                Modifier
                    .fillMaxWidth()
                    .height(fixedKeybedHeight)
            }
        ) {
            // Left Vertical-Slide Half-Pedal Column (CC64 0..127)
            VerticalSustainPedal(
                damperCC64 = damperCC64,
                damperPct = damperPct,
                onDamperChange = onDamperChange,
                modifier = Modifier
                    .width(46.dp)
                    .fillMaxHeight()
                    .testTag("sustain_cc64_slider")
            )

            // Key Rows
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                when (layoutMode) {
                    KeyboardLayoutMode.SINGLE -> {
                        KeyRowCanvas(
                            startWhite = clampedStart,
                            whiteCount = visibleWhiteCount,
                            flipped = false,
                            activeNotes = activeNotes,
                            guideMap = guideMap,
                            showLabels = showLabels,
                            keyLock = keyLock,
                            touchVelocityMode = touchVelocityMode,
                            fixedVelocity = fixedVelocity,
                            velocityCurve = velocityCurve,
                            onNoteOn = onNoteOn,
                            onNoteOff = onNoteOff,
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("piano_keybed_single")
                        )
                    }
                    KeyboardLayoutMode.DUAL -> {
                        KeyRowCanvas(
                            startWhite = trebleStart,
                            whiteCount = visibleWhiteCount,
                            flipped = false,
                            activeNotes = activeNotes,
                            guideMap = guideMap,
                            showLabels = showLabels,
                            keyLock = keyLock,
                            touchVelocityMode = touchVelocityMode,
                            fixedVelocity = fixedVelocity,
                            velocityCurve = velocityCurve,
                            onNoteOn = onNoteOn,
                            onNoteOff = onNoteOff,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .testTag("piano_keybed_treble")
                        )
                        KeyRowCanvas(
                            startWhite = bassStart,
                            whiteCount = visibleWhiteCount,
                            flipped = false,
                            activeNotes = activeNotes,
                            guideMap = guideMap,
                            showLabels = showLabels,
                            keyLock = keyLock,
                            touchVelocityMode = touchVelocityMode,
                            fixedVelocity = fixedVelocity,
                            velocityCurve = velocityCurve,
                            onNoteOn = onNoteOn,
                            onNoteOff = onNoteOff,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .testTag("piano_keybed_bass")
                        )
                    }
                    KeyboardLayoutMode.DUET -> {
                        KeyRowCanvas(
                            startWhite = trebleStart,
                            whiteCount = visibleWhiteCount,
                            flipped = true,
                            activeNotes = activeNotes,
                            guideMap = guideMap,
                            showLabels = showLabels,
                            keyLock = keyLock,
                            touchVelocityMode = touchVelocityMode,
                            fixedVelocity = fixedVelocity,
                            velocityCurve = velocityCurve,
                            onNoteOn = onNoteOn,
                            onNoteOff = onNoteOff,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .testTag("piano_keybed_duet_top")
                        )
                        KeyRowCanvas(
                            startWhite = bassStart,
                            whiteCount = visibleWhiteCount,
                            flipped = false,
                            activeNotes = activeNotes,
                            guideMap = guideMap,
                            showLabels = showLabels,
                            keyLock = keyLock,
                            touchVelocityMode = touchVelocityMode,
                            fixedVelocity = fixedVelocity,
                            velocityCurve = velocityCurve,
                            onNoteOn = onNoteOn,
                            onNoteOff = onNoteOff,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .testTag("piano_keybed_duet_bottom")
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PedalToggleChip(
    label: String,
    engaged: Boolean,
    onClick: () -> Unit,
    tag: String
) {
    Box(
        modifier = Modifier
            .testTag(tag)
            .clip(RoundedCornerShape(7.dp))
            .background(if (engaged) Color(0xFF2C2416) else SurfaceControl)
            .border(
                1.dp,
                if (engaged) ConcertBrass else BorderHairline,
                RoundedCornerShape(7.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 5.dp)
    ) {
        Text(
            text = label,
            fontFamily = JetBrainsMonoFamily,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Medium,
            color = if (engaged) ConcertBrassBright else MutedStoneText
        )
    }
}

@Composable
private fun OctaveCompassBar(
    activeNotes: Set<Int>,
    startWhiteIndex: Int,
    visibleWhiteCount: Int,
    onViewportChange: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val latestVisible by rememberUpdatedState(visibleWhiteCount)
    val latestCallback by rememberUpdatedState(onViewportChange)

    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(5.dp))
            .background(Color(0xFF0C0C0F))
            .border(1.dp, BorderHairline, RoundedCornerShape(5.dp))
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: continue
                        if (change.pressed) {
                            val ratio = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                            val centered = (ratio * PianoTuning.WHITE_KEY_COUNT - latestVisible / 2f).roundToInt()
                            val clamped = centered.coerceIn(0, PianoTuning.WHITE_KEY_COUNT - latestVisible)
                            latestCallback(clamped, latestVisible)
                            change.consume()
                        }
                    }
                }
            }
    ) {
        val wkW = size.width / PianoTuning.WHITE_KEY_COUNT
        PianoTuning.WHITE_KEYS.forEachIndexed { idx, wk ->
            val isActive = wk.midi in activeNotes
            drawRect(
                color = if (isActive) ConcertBrass else Color(0x8CD9D4C9),
                topLeft = Offset(idx * wkW, 0f),
                size = Size((wkW - 0.5f).coerceAtLeast(1f), size.height)
            )
        }
        // Subtle black key silhouettes on compass for instant octave orientation
        PianoTuning.BLACK_KEYS.forEach { bk ->
            val bx = (bk.whiteIndex + 1) * wkW - wkW * 0.34f
            drawRect(
                color = Color(0xCC121216),
                topLeft = Offset(bx, 0f),
                size = Size((wkW * 0.68f).coerceAtLeast(1.2f), size.height * 0.56f)
            )
        }

        val left = (startWhiteIndex.toFloat() / PianoTuning.WHITE_KEY_COUNT) * size.width
        val width = (visibleWhiteCount.toFloat() / PianoTuning.WHITE_KEY_COUNT) * size.width
        drawRect(
            color = ConcertBrass.copy(alpha = 0.24f),
            topLeft = Offset(left, 0f),
            size = Size(width, size.height)
        )
        drawRect(
            color = ConcertBrassBright,
            topLeft = Offset(left + 1f, 1f),
            size = Size((width - 2f).coerceAtLeast(2f), size.height - 2f),
            style = Stroke(width = 1.8.dp.toPx())
        )
    }
}

@Composable
private fun VerticalSustainPedal(
    damperCC64: Int,
    damperPct: Int,
    onDamperChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val latestCallback by rememberUpdatedState(onDamperChange)
    Column(
        modifier = modifier
            .background(Color(0xFF141419))
            .border(width = 0.5.dp, color = BorderHairline)
            .padding(vertical = 8.dp, horizontal = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = stringResource(R.string.keybed_cc64),
            fontFamily = JetBrainsMonoFamily,
            fontSize = 9.sp,
            color = MutedStoneText
        )

        Canvas(
            modifier = Modifier
                .weight(1f)
                .width(26.dp)
                .padding(vertical = 6.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF0A0A0D))
                .border(1.dp, BorderHairline, RoundedCornerShape(6.dp))
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: continue
                            if (change.pressed) {
                                val ratio = (change.position.y / size.height.toFloat()).coerceIn(0f, 1f)
                                latestCallback((ratio * 127f).roundToInt())
                                change.consume()
                            }
                        }
                    }
                }
        ) {
            val fillH = (damperCC64 / 127f) * size.height
            // Half-pedal 50% mark
            drawLine(
                color = ConcertBrass.copy(alpha = 0.4f),
                start = Offset(0f, size.height * 0.5f),
                end = Offset(size.width, size.height * 0.5f),
                strokeWidth = 1.dp.toPx()
            )
            // Brass fill bar
            if (fillH > 0f) {
                drawRect(
                    brush = Brush.verticalGradient(listOf(ConcertBrassDeep, ConcertBrass)),
                    topLeft = Offset.Zero,
                    size = Size(size.width, fillH)
                )
            }
            // Ivory thumb handle
            val handleY = (fillH - 4.5.dp.toPx()).coerceIn(0f, size.height - 9.dp.toPx())
            drawRoundRect(
                color = IvoryText,
                topLeft = Offset(2.dp.toPx(), handleY),
                size = Size(size.width - 4.dp.toPx(), 9.dp.toPx()),
                cornerRadius = CornerRadius(3.dp.toPx())
            )
        }

        Box(
            modifier = Modifier
                .testTag("sustain_toggle_tap")
                .clip(RoundedCornerShape(4.dp))
                .clickable { onDamperChange(if (damperCC64 >= 64) 0 else 127) }
                .padding(horizontal = 2.dp, vertical = 2.dp)
        ) {
            Text(
                text = "$damperPct%",
                fontFamily = JetBrainsMonoFamily,
                fontSize = 10.sp,
                color = ConcertBrass
            )
        }
    }
}

@Composable
private fun KeyRowCanvas(
    startWhite: Int,
    whiteCount: Int,
    flipped: Boolean,
    activeNotes: Set<Int>,
    guideMap: Map<Int, HandPart>,
    showLabels: Boolean,
    keyLock: Boolean,
    touchVelocityMode: TouchVelocityMode,
    fixedVelocity: Int,
    velocityCurve: List<VelocityCurvePoint>,
    onNoteOn: (midi: Int, finalVel: Int, rawVel: Int) -> Unit,
    onNoteOff: (midi: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val clampedStart = startWhite.coerceIn(0, PianoTuning.WHITE_KEY_COUNT - whiteCount)
    val visibleWhites = remember(clampedStart, whiteCount) {
        PianoTuning.WHITE_KEYS.subList(clampedStart, clampedStart + whiteCount)
    }
    val visibleBlacks = remember(clampedStart, whiteCount) {
        PianoTuning.BLACK_KEYS.filter { it.whiteIndex >= clampedStart && it.whiteIndex < clampedStart + whiteCount - 1 }
    }

    val latestStart by rememberUpdatedState(clampedStart)
    val latestCount by rememberUpdatedState(whiteCount)
    val latestWhites by rememberUpdatedState(visibleWhites)
    val latestBlacks by rememberUpdatedState(visibleBlacks)
    val latestKeyLock by rememberUpdatedState(keyLock)
    val latestVelMode by rememberUpdatedState(touchVelocityMode)
    val latestFixedVel by rememberUpdatedState(fixedVelocity)
    val latestCurve by rememberUpdatedState(velocityCurve)
    val latestOnNoteOn by rememberUpdatedState(onNoteOn)
    val latestOnNoteOff by rememberUpdatedState(onNoteOff)

    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val pointerNoteMap = remember { mutableMapOf<PointerId, Int>() }

    fun hitTest(pos: Offset, size: IntSize): Pair<Int, Float>? {
        if (size.width <= 0 || size.height <= 0) return null
        val relX = (pos.x / size.width.toFloat()).coerceIn(0f, 1f)
        val relY = (pos.y / size.height.toFloat()).coerceIn(0f, 1f)
        if (relY <= 0.61f) {
            val blackWFrac = 0.60f / latestCount
            for (bk in latestBlacks) {
                val localIdx = bk.whiteIndex - latestStart
                val centerFrac = (localIdx + 1f) / latestCount
                if (abs(relX - centerFrac) <= blackWFrac * 0.5f) {
                    return bk.midi to (relY / 0.61f).coerceIn(0f, 1f)
                }
            }
        }
        val wIdx = floor(relX * latestCount).toInt().coerceIn(0, latestCount - 1)
        val wk = latestWhites.getOrNull(wIdx) ?: return null
        return wk.midi to relY
    }

    Column(
        modifier = modifier.graphicsLayer {
            if (flipped) rotationZ = 180f
        }
    ) {
        // Authentic Hamburg rosewood keybed felt rail
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .background(Brush.verticalGradient(listOf(RosewoodFeltTop, RosewoodFeltBottom)))
        )

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0A0A0D))
                .onSizeChanged { canvasSize = it }
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            for (change in event.changes) {
                                val id = change.id
                                if (change.pressed && !change.previousPressed) {
                                    val hit = hitTest(change.position, canvasSize)
                                    if (hit != null) {
                                        pointerNoteMap[id] = hit.first
                                        val raw = VelocityEngine.computeRawTouchVelocity(
                                            yRatio = hit.second,
                                            pressure = change.pressure,
                                            contactSizePx = null,
                                            mode = latestVelMode,
                                            fixedVelocity = latestFixedVel
                                        )
                                        val mapped = VelocityEngine.applyVelocityCurve(raw, latestCurve)
                                        latestOnNoteOn(hit.first, mapped, raw)
                                    }
                                    change.consume()
                                } else if (change.pressed && change.previousPressed) {
                                    if (!latestKeyLock) {
                                        val hit = hitTest(change.position, canvasSize)
                                        val prev = pointerNoteMap[id]
                                        if (hit != null && hit.first != prev) {
                                            if (prev != null) latestOnNoteOff(prev)
                                            pointerNoteMap[id] = hit.first
                                            val raw = VelocityEngine.computeRawTouchVelocity(
                                                yRatio = hit.second,
                                                pressure = change.pressure,
                                                contactSizePx = null,
                                                mode = latestVelMode,
                                                fixedVelocity = latestFixedVel
                                            )
                                            val mapped = VelocityEngine.applyVelocityCurve(raw, latestCurve)
                                            latestOnNoteOn(hit.first, mapped, raw)
                                        }
                                    }
                                    change.consume()
                                } else if (!change.pressed && change.previousPressed) {
                                    val prev = pointerNoteMap.remove(id)
                                    if (prev != null) latestOnNoteOff(prev)
                                    change.consume()
                                }
                            }
                        }
                    }
                }
        ) {
            val whiteW = size.width / whiteCount.toFloat()
            val blackW = whiteW * 0.60f
            val blackH = size.height * 0.60f
            val corner = CornerRadius(5.dp.toPx(), 5.dp.toPx())
            val lipH = 5.dp.toPx()

            val textPaint = android.graphics.Paint().apply {
                isAntiAlias = true
                textAlign = android.graphics.Paint.Align.CENTER
                textSize = (whiteW * 0.27f).coerceIn(19f, 32f)
                typeface = android.graphics.Typeface.MONOSPACE
            }

            // Draw Ivory White Keys with subtle top shadow & front ivory lip bevel
            visibleWhites.forEachIndexed { idx, wk ->
                val x = idx * whiteW
                val isPressed = wk.midi in activeNotes
                val guide = guideMap[wk.midi]
                val isC = wk.pitchClass == 0

                val brush = when {
                    isPressed -> Brush.verticalGradient(listOf(Color(0xFFE5D5AE), Color(0xFFDFC893), ConcertBrass))
                    guide == HandPart.RH -> Brush.verticalGradient(listOf(IvoryText, Color(0xFFF2E6C8), Color(0xFFDFC07A)))
                    guide == HandPart.LH -> Brush.verticalGradient(listOf(IvoryText, Color(0xFFDCE5F0), Color(0xFF9DB7D4)))
                    else -> Brush.verticalGradient(listOf(Color(0xFFEEE8DC), Color(0xFFFAF7F0), IvoryText, Color(0xFFE7E1D4)))
                }

                val topOff = if (isPressed) 2.5.dp.toPx() else 0f
                drawRoundRect(
                    brush = brush,
                    topLeft = Offset(x + 0.8f, topOff),
                    size = Size(whiteW - 1.6f, size.height - topOff),
                    cornerRadius = corner
                )

                // Subtle front key lip bevel when unpressed
                if (!isPressed) {
                    drawRoundRect(
                        color = Color(0xFFCEC6B7),
                        topLeft = Offset(x + 1.2f, size.height - lipH),
                        size = Size(whiteW - 2.4f, lipH),
                        cornerRadius = CornerRadius(3.dp.toPx())
                    )
                }

                // Guide dot
                if (guide != null && !isPressed) {
                    drawCircle(
                        color = if (guide == HandPart.RH) Color(0xFFB38632) else LeftHandBlue,
                        radius = 4.5.dp.toPx(),
                        center = Offset(x + whiteW * 0.5f, size.height - 30.dp.toPx())
                    )
                }

                if (showLabels || isC) {
                    textPaint.color = when {
                        isPressed -> android.graphics.Color.rgb(24, 21, 16)
                        isC -> android.graphics.Color.rgb(80, 72, 60)
                        else -> android.graphics.Color.rgb(142, 134, 120)
                    }
                    textPaint.isFakeBoldText = isC || isPressed
                    drawContext.canvas.nativeCanvas.drawText(
                        wk.name,
                        x + whiteW * 0.5f,
                        size.height - 10.dp.toPx(),
                        textPaint
                    )
                }
            }

            // Draw Ebony Black Keys with realistic bevel & specular face
            visibleBlacks.forEach { bk ->
                val localIdx = bk.whiteIndex - clampedStart
                val left = (localIdx + 1) * whiteW - blackW * 0.5f
                val isPressed = bk.midi in activeNotes
                val guide = guideMap[bk.midi]

                val brush = when {
                    isPressed -> Brush.verticalGradient(listOf(Color(0xFF3E3320), Color(0xFF9A7738)))
                    guide == HandPart.RH -> Brush.verticalGradient(listOf(Color(0xFF352C1D), ConcertBrassDeep))
                    guide == HandPart.LH -> Brush.verticalGradient(listOf(Color(0xFF1E2C3E), LeftHandBlue))
                    else -> Brush.verticalGradient(listOf(Color(0xFF2D2D36), Color(0xFF17171C), Color(0xFF0A0A0D)))
                }

                // Cast shadow onto ivory keys
                drawRoundRect(
                    color = Color.Black.copy(alpha = 0.42f),
                    topLeft = Offset(left + 2.5f, 3f),
                    size = Size(blackW, blackH + 3.dp.toPx()),
                    cornerRadius = corner
                )
                val topOff = if (isPressed) 2.5.dp.toPx() else 0f
                drawRoundRect(
                    brush = brush,
                    topLeft = Offset(left, topOff),
                    size = Size(blackW, blackH - topOff),
                    cornerRadius = corner
                )
                // Inner ebony top-face highlight
                drawRoundRect(
                    color = Color.White.copy(alpha = if (isPressed) 0.16f else 0.09f),
                    topLeft = Offset(left + 2.dp.toPx(), topOff + 2.dp.toPx()),
                    size = Size(blackW - 4.dp.toPx(), (blackH - 12.dp.toPx()).coerceAtLeast(10f)),
                    cornerRadius = CornerRadius(3.dp.toPx()),
                    style = Stroke(width = 1.dp.toPx())
                )
            }
        }
    }
}
