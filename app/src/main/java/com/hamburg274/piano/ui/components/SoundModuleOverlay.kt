package com.hamburg274.piano.ui.components

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material3.Icon
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
import com.hamburg274.piano.audio.AudioDiagnosticsSnapshot
import com.hamburg274.piano.ui.theme.BorderHairline
import com.hamburg274.piano.ui.theme.ConcertBrass
import com.hamburg274.piano.ui.theme.ConcertBrassBright
import com.hamburg274.piano.ui.theme.CormorantGaramondFamily
import com.hamburg274.piano.ui.theme.IvoryText
import com.hamburg274.piano.ui.theme.JetBrainsMonoFamily
import com.hamburg274.piano.ui.theme.MutedStoneText
import com.hamburg274.piano.ui.theme.RecordCrimson
import com.hamburg274.piano.ui.theme.RecordCrimsonLight
import com.hamburg274.piano.ui.theme.SubtleAshText
import kotlin.math.roundToInt

@Composable
fun SoundModuleOverlay(
    settings: AcousticSettings,
    diagnostics: AudioDiagnosticsSnapshot,
    activeNoteNames: List<String>,
    damperCC64: Int,
    sostenuto: Boolean,
    unaCorda: Boolean,
    midiDeviceCount: Int,
    onMidiPanic: () -> Unit,
    onExit: () -> Unit
) {
    BackHandler { onExit() }

    val damperPct = ((damperCC64 / 127f) * 100f).roundToInt()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF040405)) // True OLED pixel-off black
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp)
            .testTag("sound_module_overlay"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 460.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Icon(
                imageVector = Icons.Default.DarkMode,
                contentDescription = null,
                tint = ConcertBrass.copy(alpha = 0.7f),
                modifier = Modifier.size(30.dp)
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "OLED HEADLESS MIDI SOUND-MODULE",
                    fontFamily = JetBrainsMonoFamily,
                    fontSize = 10.sp,
                    letterSpacing = 1.4.sp,
                    color = SubtleAshText
                )
                Text(
                    text = settings.voicingId.displayName,
                    fontFamily = CormorantGaramondFamily,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = IvoryText.copy(alpha = 0.9f)
                )
                Text(
                    text = "${diagnostics.sampleRate} Hz · ${diagnostics.activeVoices} active voices · $midiDeviceCount MIDI inputs",
                    fontFamily = JetBrainsMonoFamily,
                    fontSize = 11.sp,
                    color = MutedStoneText
                )
            }

            // Live Chord & Pedal Monitor
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF0B0B0E))
                    .border(1.dp, BorderHairline, RoundedCornerShape(14.dp))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (activeNoteNames.isNotEmpty()) activeNoteNames.joinToString("  ") else "Awaiting USB / BLE MIDI…",
                    fontFamily = JetBrainsMonoFamily,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (activeNoteNames.isNotEmpty()) ConcertBrassBright else SubtleAshText
                )
                Text(
                    text = "Damper CC64: $damperPct%   ·   Sostenuto: ${if (sostenuto) "ON" else "OFF"}   ·   Una Corda: ${if (unaCorda) "ON" else "OFF"}",
                    fontFamily = JetBrainsMonoFamily,
                    fontSize = 10.5.sp,
                    color = MutedStoneText
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("sound_module_panic_button")
                        .clip(RoundedCornerShape(10.dp))
                        .background(RecordCrimson.copy(alpha = 0.25f))
                        .border(1.dp, RecordCrimsonLight.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .clickable { onMidiPanic() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.midi_panic),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = RecordCrimsonLight
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("sound_module_exit_button")
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF181510))
                        .border(1.dp, ConcertBrass, RoundedCornerShape(10.dp))
                        .clickable { onExit() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.sound_module_exit),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ConcertBrassBright
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}
