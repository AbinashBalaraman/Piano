package com.hamburg274.piano.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hamburg274.piano.R
import com.hamburg274.piano.audio.AcousticSettings
import com.hamburg274.piano.audio.HallIrId
import com.hamburg274.piano.audio.LidPosition
import com.hamburg274.piano.audio.StereoPerspective
import com.hamburg274.piano.audio.VoicingId
import com.hamburg274.piano.music.TemperamentId
import com.hamburg274.piano.music.TouchVelocityMode
import com.hamburg274.piano.music.VelocityCurvePoint
import com.hamburg274.piano.ui.theme.BorderHairline
import com.hamburg274.piano.ui.theme.ConcertBrass
import com.hamburg274.piano.ui.theme.ConcertBrassBright
import com.hamburg274.piano.ui.theme.CormorantGaramondFamily
import com.hamburg274.piano.ui.theme.IvoryText
import com.hamburg274.piano.ui.theme.JetBrainsMonoFamily
import com.hamburg274.piano.ui.theme.MutedStoneText
import com.hamburg274.piano.ui.theme.ObsidianBg
import com.hamburg274.piano.ui.theme.SubtleAshText
import com.hamburg274.piano.ui.theme.SurfaceCard
import com.hamburg274.piano.ui.theme.SurfaceControl
import com.hamburg274.piano.ui.theme.SurfaceSelected
import com.hamburg274.piano.ui.theme.WarmSandText
import kotlin.math.roundToInt

@Composable
fun AcousticsPanel(
    settings: AcousticSettings,
    onUpdateSettings: ((AcousticSettings) -> AcousticSettings) -> Unit,
    touchVelocityMode: TouchVelocityMode,
    onTouchVelocityModeChange: (TouchVelocityMode) -> Unit,
    fixedVelocity: Int,
    onFixedVelocityChange: (Int) -> Unit,
    velocityCurve: List<VelocityCurvePoint>,
    onVelocityCurveChange: (List<VelocityCurvePoint>) -> Unit,
    lastRawVelocity: Int?,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 12.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        val isWide = maxWidth >= 720.dp

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 1040.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isWide) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    VoicingSectionCard(
                        settings = settings,
                        onUpdateSettings = onUpdateSettings,
                        modifier = Modifier.weight(1f)
                    )
                    ResonanceAndHallCard(
                        settings = settings,
                        onUpdateSettings = onUpdateSettings,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TemperamentSectionCard(
                        settings = settings,
                        onUpdateSettings = onUpdateSettings,
                        modifier = Modifier.weight(1f)
                    )
                    TouchVelocitySectionCard(
                        touchVelocityMode = touchVelocityMode,
                        onTouchVelocityModeChange = onTouchVelocityModeChange,
                        fixedVelocity = fixedVelocity,
                        onFixedVelocityChange = onFixedVelocityChange,
                        velocityCurve = velocityCurve,
                        onVelocityCurveChange = onVelocityCurveChange,
                        lastRawVelocity = lastRawVelocity,
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                VoicingSectionCard(settings = settings, onUpdateSettings = onUpdateSettings)
                ResonanceAndHallCard(settings = settings, onUpdateSettings = onUpdateSettings)
                TemperamentSectionCard(settings = settings, onUpdateSettings = onUpdateSettings)
                TouchVelocitySectionCard(
                    touchVelocityMode = touchVelocityMode,
                    onTouchVelocityModeChange = onTouchVelocityModeChange,
                    fixedVelocity = fixedVelocity,
                    onFixedVelocityChange = onFixedVelocityChange,
                    velocityCurve = velocityCurve,
                    onVelocityCurveChange = onVelocityCurveChange,
                    lastRawVelocity = lastRawVelocity
                )
            }
        }
    }
}

@Composable
private fun VoicingSectionCard(
    settings: AcousticSettings,
    onUpdateSettings: ((AcousticSettings) -> AcousticSettings) -> Unit,
    modifier: Modifier = Modifier
) {
    AcousticsSectionBox(
        title = stringResource(R.string.acoustics_voicing_title),
        subtitle = stringResource(R.string.acoustics_voicing_subtitle),
        modifier = modifier
    ) {
        VoicingId.entries.forEach { v ->
            val selected = settings.voicingId == v
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("acoustics_voicing_${v.name.lowercase()}")
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selected) SurfaceSelected else ObsidianBg)
                    .border(1.dp, if (selected) ConcertBrass else BorderHairline, RoundedCornerShape(10.dp))
                    .clickable { onUpdateSettings { it.copy(voicingId = v) } }
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = v.displayName,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (selected) ConcertBrassBright else IvoryText
                    )
                    Text(
                        text = v.packTier.uppercase(),
                        fontFamily = JetBrainsMonoFamily,
                        fontSize = 9.5.sp,
                        color = if (selected) ConcertBrass else MutedStoneText
                    )
                }
                Text(
                    text = v.character,
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedStoneText
                )
            }
        }
    }
}

@Composable
private fun ResonanceAndHallCard(
    settings: AcousticSettings,
    onUpdateSettings: ((AcousticSettings) -> AcousticSettings) -> Unit,
    modifier: Modifier = Modifier
) {
    AcousticsSectionBox(
        title = stringResource(R.string.acoustics_resonance_title),
        subtitle = stringResource(R.string.acoustics_resonance_subtitle),
        modifier = modifier
    ) {
        LabeledPercentSlider(
            title = "Duplex & Sympathetic Resonance",
            hint = stringResource(R.string.acoustics_duplex_hint),
            value = settings.duplexMix,
            onValueChange = { v -> onUpdateSettings { it.copy(duplexMix = v) } },
            tag = "acoustics_duplex_slider"
        )

        LabeledPercentSlider(
            title = stringResource(R.string.acoustics_action_noise),
            hint = null,
            value = settings.mechanicalNoise,
            onValueChange = { v -> onUpdateSettings { it.copy(mechanicalNoise = v) } },
            tag = "acoustics_action_noise_slider"
        )

        // Lid Position & Listening Perspective
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = stringResource(R.string.acoustics_lid_position),
                    fontSize = 11.5.sp,
                    color = WarmSandText
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(ObsidianBg)
                        .border(1.dp, BorderHairline, RoundedCornerShape(8.dp))
                        .padding(2.dp)
                ) {
                    LidPosition.entries.forEach { lid ->
                        val active = settings.lidPosition == lid
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .testTag("acoustics_lid_${lid.name.lowercase()}")
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (active) SurfaceSelected else Color.Transparent)
                                .clickable { onUpdateSettings { it.copy(lidPosition = lid) } }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = lid.label,
                                fontSize = 11.sp,
                                color = if (active) ConcertBrassBright else MutedStoneText
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = stringResource(R.string.acoustics_perspective),
                    fontSize = 11.5.sp,
                    color = WarmSandText
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(ObsidianBg)
                        .border(1.dp, BorderHairline, RoundedCornerShape(8.dp))
                        .padding(2.dp)
                ) {
                    StereoPerspective.entries.forEach { persp ->
                        val active = settings.perspective == persp
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .testTag("acoustics_perspective_${persp.name.lowercase()}")
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (active) SurfaceSelected else Color.Transparent)
                                .clickable { onUpdateSettings { it.copy(perspective = persp) } }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = persp.label,
                                fontSize = 11.sp,
                                color = if (active) ConcertBrassBright else MutedStoneText
                            )
                        }
                    }
                }
            }
        }

        // Concert Hall Space
        Text(
            text = stringResource(R.string.acoustics_hall_ir),
            fontSize = 11.5.sp,
            color = WarmSandText
        )
        HallIrId.entries.forEach { hall ->
            val selected = settings.hallId == hall
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("acoustics_hall_${hall.name.lowercase()}")
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (selected) SurfaceSelected else ObsidianBg)
                    .border(1.dp, if (selected) ConcertBrass else BorderHairline, RoundedCornerShape(9.dp))
                    .clickable { onUpdateSettings { it.copy(hallId = hall) } }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = hall.displayName,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (selected) ConcertBrassBright else IvoryText
                    )
                    Text(
                        text = hall.description,
                        fontSize = 10.5.sp,
                        color = MutedStoneText
                    )
                }
                Text(
                    text = "RT60 ${hall.rt60Label}",
                    fontFamily = JetBrainsMonoFamily,
                    fontSize = 10.sp,
                    color = ConcertBrass
                )
            }
        }

        LabeledPercentSlider(
            title = "Hall Reverb Wet Mix",
            hint = null,
            value = settings.reverbMix,
            onValueChange = { v -> onUpdateSettings { it.copy(reverbMix = v) } },
            tag = "acoustics_reverb_mix_slider"
        )
    }
}

@Composable
private fun TemperamentSectionCard(
    settings: AcousticSettings,
    onUpdateSettings: ((AcousticSettings) -> AcousticSettings) -> Unit,
    modifier: Modifier = Modifier
) {
    AcousticsSectionBox(
        title = stringResource(R.string.acoustics_temperament),
        subtitle = "Stretch-tuned concert octave calibration and historical well-temperaments.",
        modifier = modifier
    ) {
        // Concert Pitch A4 Slider (415..446 Hz)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Concert Pitch Reference (A4)", fontSize = 12.sp, color = WarmSandText)
                Text(
                    text = "${settings.referenceA4Hz} Hz",
                    fontFamily = JetBrainsMonoFamily,
                    fontSize = 12.sp,
                    color = ConcertBrassBright
                )
            }
            Slider(
                value = settings.referenceA4Hz.toFloat(),
                onValueChange = { hz -> onUpdateSettings { it.copy(referenceA4Hz = hz.roundToInt()) } },
                valueRange = 415f..446f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp)
                    .testTag("acoustics_a4_pitch_slider"),
                colors = SliderDefaults.colors(
                    thumbColor = ConcertBrassBright,
                    activeTrackColor = ConcertBrass,
                    inactiveTrackColor = SurfaceControl
                )
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf(415 to "Baroque 415", 440 to "Concert 440", 442 to "Europe 442", 444 to "Bright 444").forEach { (hz, label) ->
                    Text(
                        text = label,
                        fontFamily = JetBrainsMonoFamily,
                        fontSize = 10.sp,
                        color = if (settings.referenceA4Hz == hz) ConcertBrassBright else SubtleAshText,
                        modifier = Modifier
                            .testTag("acoustics_a4_preset_$hz")
                            .clickable { onUpdateSettings { it.copy(referenceA4Hz = hz) } }
                            .padding(vertical = 2.dp)
                    )
                }
            }
        }

        TemperamentId.entries.forEach { temp ->
            val selected = settings.temperament == temp
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("acoustics_temperament_${temp.name.lowercase()}")
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (selected) SurfaceSelected else ObsidianBg)
                    .border(1.dp, if (selected) ConcertBrass else BorderHairline, RoundedCornerShape(9.dp))
                    .clickable { onUpdateSettings { it.copy(temperament = temp) } }
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = temp.displayName,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (selected) ConcertBrassBright else IvoryText
                    )
                    Text(
                        text = temp.era,
                        fontFamily = JetBrainsMonoFamily,
                        fontSize = 9.5.sp,
                        color = ConcertBrass
                    )
                }
                Text(
                    text = temp.description,
                    fontSize = 11.sp,
                    color = MutedStoneText
                )
            }
        }
    }
}

@Composable
private fun TouchVelocitySectionCard(
    touchVelocityMode: TouchVelocityMode,
    onTouchVelocityModeChange: (TouchVelocityMode) -> Unit,
    fixedVelocity: Int,
    onFixedVelocityChange: (Int) -> Unit,
    velocityCurve: List<VelocityCurvePoint>,
    onVelocityCurveChange: (List<VelocityCurvePoint>) -> Unit,
    lastRawVelocity: Int?,
    modifier: Modifier = Modifier
) {
    AcousticsSectionBox(
        title = stringResource(R.string.acoustics_touch_title),
        subtitle = stringResource(R.string.acoustics_touch_subtitle),
        modifier = modifier
    ) {
        TouchVelocityMode.entries.forEach { mode ->
            val selected = touchVelocityMode == mode
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("acoustics_touch_mode_${mode.name.lowercase()}")
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (selected) SurfaceSelected else ObsidianBg)
                    .border(1.dp, if (selected) ConcertBrass else BorderHairline, RoundedCornerShape(9.dp))
                    .clickable { onTouchVelocityModeChange(mode) }
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = mode.title,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selected) ConcertBrassBright else IvoryText
                )
                Text(
                    text = mode.description,
                    fontSize = 11.sp,
                    color = MutedStoneText
                )
            }
        }

        if (touchVelocityMode == TouchVelocityMode.FIXED) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Fixed Touch Velocity", fontSize = 11.5.sp, color = WarmSandText)
                    Text(
                        text = "vel $fixedVelocity",
                        fontFamily = JetBrainsMonoFamily,
                        fontSize = 11.5.sp,
                        color = ConcertBrass
                    )
                }
                Slider(
                    value = fixedVelocity.toFloat(),
                    onValueChange = { onFixedVelocityChange(it.roundToInt()) },
                    valueRange = 20f..127f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(30.dp)
                        .testTag("acoustics_fixed_velocity_slider"),
                    colors = SliderDefaults.colors(
                        thumbColor = ConcertBrassBright,
                        activeTrackColor = ConcertBrass,
                        inactiveTrackColor = SurfaceControl
                    )
                )
            }
        }

        VelocityCurveEditor(
            points = velocityCurve,
            onChange = onVelocityCurveChange,
            lastRawVelocity = lastRawVelocity
        )
    }
}

@Composable
private fun AcousticsSectionBox(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceCard)
            .border(1.dp, BorderHairline, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                fontFamily = CormorantGaramondFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp,
                color = IvoryText
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MutedStoneText
            )
        }
        content()
    }
}

@Composable
private fun LabeledPercentSlider(
    title: String,
    hint: String?,
    value: Float,
    onValueChange: (Float) -> Unit,
    tag: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = title, fontSize = 12.sp, color = WarmSandText)
            Text(
                text = "${(value * 100f).roundToInt()}%",
                fontFamily = JetBrainsMonoFamily,
                fontSize = 11.5.sp,
                color = ConcertBrass
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..1f,
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp)
                .testTag(tag),
            colors = SliderDefaults.colors(
                thumbColor = ConcertBrassBright,
                activeTrackColor = ConcertBrass,
                inactiveTrackColor = SurfaceControl
            )
        )
        if (hint != null) {
            Text(text = hint, fontSize = 10.5.sp, color = SubtleAshText)
        }
    }
}
