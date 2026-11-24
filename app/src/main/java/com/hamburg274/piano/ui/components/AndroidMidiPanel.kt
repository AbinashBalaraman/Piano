package com.hamburg274.piano.ui.components

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BluetoothAudio
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ScreenLockPortrait
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hamburg274.piano.R
import com.hamburg274.piano.audio.AudioDiagnosticsSnapshot
import com.hamburg274.piano.midi.ConnectedMidiDevice
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
import com.hamburg274.piano.ui.theme.SurfaceSelected
import com.hamburg274.piano.ui.theme.VerifiedEmerald
import com.hamburg274.piano.ui.theme.WarmSandText
import com.hamburg274.piano.ui.theme.WarningAmber
import java.util.Locale

private enum class ThreeFingerStatus { IDLE, PASSED, INTERCEPTED }

@Composable
fun AndroidMidiPanel(
    midiSupported: Boolean,
    midiStatus: String,
    midiDevices: List<ConnectedMidiDevice>,
    lastMidiMessage: String,
    onRescanMidi: () -> Unit,
    onMidiPanic: () -> Unit,
    diagnostics: AudioDiagnosticsSnapshot,
    keepScreenOn: Boolean,
    onToggleKeepScreenOn: () -> Unit,
    onEnterSoundModuleMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var activeTouchCount by remember { mutableIntStateOf(0) }
    var maxTouchCount by remember { mutableIntStateOf(0) }
    var gestureStatus by remember { mutableStateOf(ThreeFingerStatus.IDLE) }
    var useInrCurrency by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 12.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 960.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Native USB OTG & Bluetooth LE MIDI Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(SurfaceCard)
                    .border(1.dp, BorderHairline, RoundedCornerShape(14.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Usb,
                            contentDescription = null,
                            tint = ConcertBrass,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = stringResource(R.string.midi_title),
                                fontFamily = CormorantGaramondFamily,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = IvoryText
                            )
                            Text(
                                text = stringResource(R.string.midi_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedStoneText
                            )
                        }
                    }
                }

                // MIDI Status & Last Traffic
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(ObsidianBg)
                        .border(1.dp, BorderHairline, RoundedCornerShape(10.dp))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (midiSupported) midiStatus else "Android MIDI subsystem unavailable on this device",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (midiDevices.isNotEmpty()) VerifiedEmerald else WarmSandText
                        )
                        Text(
                            text = lastMidiMessage,
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = 10.5.sp,
                            color = ConcertBrass
                        )
                    }
                }

                if (midiDevices.isNotEmpty()) {
                    midiDevices.forEach { dev ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(SurfaceSelected)
                                .border(1.dp, ConcertBrass.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(dev.name, fontSize = 12.5.sp, fontWeight = FontWeight.Medium, color = IvoryText)
                                Text("${dev.manufacturer} · ${dev.state}", fontSize = 10.5.sp, color = MutedStoneText)
                            }
                            Text("CONNECTED", fontFamily = JetBrainsMonoFamily, fontSize = 10.sp, color = VerifiedEmerald)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .testTag("midi_rescan_button")
                            .clip(RoundedCornerShape(9.dp))
                            .background(SurfaceControl)
                            .border(1.dp, BorderHairline, RoundedCornerShape(9.dp))
                            .clickable { onRescanMidi() },
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = ConcertBrass, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(6.dp))
                        Text(stringResource(R.string.midi_rescan), fontSize = 12.sp, color = IvoryText)
                    }

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .testTag("midi_panic_button")
                            .clip(RoundedCornerShape(9.dp))
                            .background(RecordCrimson.copy(alpha = 0.25f))
                            .border(1.dp, RecordCrimsonLight.copy(alpha = 0.5f), RoundedCornerShape(9.dp))
                            .clickable { onMidiPanic() },
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.midi_panic),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = RecordCrimsonLight
                        )
                    }
                }
            }

            // 2. Android OEM 3-Finger Chord Diagnostic
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(SurfaceCard)
                    .border(1.dp, BorderHairline, RoundedCornerShape(14.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.TouchApp, contentDescription = null, tint = ConcertBrass, modifier = Modifier.size(20.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.diag_3finger_title),
                            fontFamily = CormorantGaramondFamily,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = IvoryText
                        )
                        Text(
                            text = stringResource(R.string.diag_3finger_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedStoneText
                        )
                    }
                }

                // Multi-touch diagnostic pad
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(104.dp)
                        .testTag("three_finger_diagnostic_pad")
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            when (gestureStatus) {
                                ThreeFingerStatus.PASSED -> VerifiedEmerald.copy(alpha = 0.12f)
                                ThreeFingerStatus.INTERCEPTED -> WarningAmber.copy(alpha = 0.14f)
                                ThreeFingerStatus.IDLE -> ObsidianBg
                            }
                        )
                        .border(
                            1.dp,
                            when (gestureStatus) {
                                ThreeFingerStatus.PASSED -> VerifiedEmerald.copy(alpha = 0.6f)
                                ThreeFingerStatus.INTERCEPTED -> WarningAmber.copy(alpha = 0.7f)
                                ThreeFingerStatus.IDLE -> ConcertBrass.copy(alpha = 0.35f)
                            },
                            RoundedCornerShape(12.dp)
                        )
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val pressedCount = event.changes.count { it.pressed }
                                    activeTouchCount = pressedCount
                                    if (pressedCount > maxTouchCount) maxTouchCount = pressedCount

                                    // Detect sudden simultaneous cancellation indicative of OEM 3-finger screenshot interception
                                    val allConsumedOrCancelled = event.changes.isNotEmpty() &&
                                        event.changes.all { !it.pressed && it.previousPressed } &&
                                        maxTouchCount == 2

                                    if (pressedCount >= 3) {
                                        gestureStatus = ThreeFingerStatus.PASSED
                                    } else if (allConsumedOrCancelled && gestureStatus == ThreeFingerStatus.IDLE) {
                                        gestureStatus = ThreeFingerStatus.INTERCEPTED
                                    }
                                    event.changes.forEach { it.consume() }
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(horizontal = 12.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            repeat(3) { idx ->
                                val lit = activeTouchCount > idx || gestureStatus == ThreeFingerStatus.PASSED
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(if (lit) ConcertBrassBright else SurfaceControl)
                                )
                            }
                        }
                        Text(
                            text = when (gestureStatus) {
                                ThreeFingerStatus.PASSED -> stringResource(R.string.diag_3finger_passed)
                                ThreeFingerStatus.INTERCEPTED -> stringResource(R.string.diag_3finger_warning)
                                ThreeFingerStatus.IDLE -> "Press & hold 3 fingers here simultaneously (Detected: $activeTouchCount)"
                            },
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = when (gestureStatus) {
                                ThreeFingerStatus.PASSED -> VerifiedEmerald
                                ThreeFingerStatus.INTERCEPTED -> WarningAmber
                                ThreeFingerStatus.IDLE -> IvoryText
                            }
                        )
                        Text(
                            text = when (gestureStatus) {
                                ThreeFingerStatus.PASSED -> stringResource(R.string.diag_3finger_passed_desc)
                                ThreeFingerStatus.INTERCEPTED -> stringResource(R.string.diag_3finger_warning_desc)
                                ThreeFingerStatus.IDLE -> "Peak simultaneous contacts: $maxTouchCount · Verifies C-E-G triad multi-touch"
                            },
                            fontSize = 10.5.sp,
                            color = MutedStoneText
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .testTag("open_android_settings_button")
                            .clip(RoundedCornerShape(9.dp))
                            .background(SurfaceControl)
                            .border(1.dp, BorderHairline, RoundedCornerShape(9.dp))
                            .clickable {
                                runCatching {
                                    context.startActivity(
                                        Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    )
                                }
                            },
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null, tint = ConcertBrass, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.size(5.dp))
                        Text(stringResource(R.string.diag_open_settings), fontSize = 11.5.sp, color = IvoryText)
                    }

                    Box(
                        modifier = Modifier
                            .height(40.dp)
                            .testTag("reset_3finger_test_button")
                            .clip(RoundedCornerShape(9.dp))
                            .background(ObsidianBg)
                            .border(1.dp, BorderHairline, RoundedCornerShape(9.dp))
                            .clickable {
                                activeTouchCount = 0
                                maxTouchCount = 0
                                gestureStatus = ThreeFingerStatus.IDLE
                            }
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(stringResource(R.string.diag_reset), fontSize = 11.5.sp, color = MutedStoneText)
                    }
                }
            }

            // 3. Pro Audio Diagnostics & Sound-Module Mode
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(SurfaceCard)
                    .border(1.dp, BorderHairline, RoundedCornerShape(14.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = stringResource(R.string.pro_audio_title),
                    fontFamily = CormorantGaramondFamily,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = IvoryText
                )
                Text(
                    text = stringResource(R.string.pro_audio_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedStoneText
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DiagMetricTile("Native Clock", "${diagnostics.sampleRate} Hz", Modifier.weight(1f))
                    DiagMetricTile("Frames / Burst", "${diagnostics.framesPerBurst}", Modifier.weight(1f))
                    DiagMetricTile(
                        "Est. Latency",
                        String.format(Locale.US, "%.1f ms", diagnostics.estimatedLatencyMs),
                        Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DiagMetricTile("Active Voices", "${diagnostics.activeVoices}", Modifier.weight(1f))
                    DiagMetricTile("Underruns", "${diagnostics.underrunCount}", Modifier.weight(1f))
                    DiagMetricTile("Output Route", diagnostics.outputRouteLabel, Modifier.weight(1f))
                }

                // Legacy Bluetooth A2DP warning or general latency tip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (diagnostics.isLegacyBluetoothA2dp) WarningAmber.copy(alpha = 0.14f)
                            else ObsidianBg
                        )
                        .border(
                            1.dp,
                            if (diagnostics.isLegacyBluetoothA2dp) WarningAmber.copy(alpha = 0.65f) else BorderHairline,
                            RoundedCornerShape(10.dp)
                        )
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = if (diagnostics.isLegacyBluetoothA2dp) Icons.Default.Warning else Icons.Default.BluetoothAudio,
                        contentDescription = null,
                        tint = if (diagnostics.isLegacyBluetoothA2dp) WarningAmber else ConcertBrass,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = stringResource(R.string.pro_audio_latency_tip),
                        fontSize = 11.sp,
                        color = if (diagnostics.isLegacyBluetoothA2dp) WarningAmber else MutedStoneText
                    )
                }

                // OLED Sound-Module Mode & Keep Screen On
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1.4f)
                            .height(44.dp)
                            .testTag("enter_sound_module_button")
                            .clip(RoundedCornerShape(10.dp))
                            .background(SurfaceSelected)
                            .border(1.dp, ConcertBrass, RoundedCornerShape(10.dp))
                            .clickable { onEnterSoundModuleMode() }
                            .padding(horizontal = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.DarkMode, contentDescription = null, tint = ConcertBrassBright, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(6.dp))
                        Text(
                            text = stringResource(R.string.sound_module_enter),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ConcertBrassBright,
                            maxLines = 1
                        )
                    }

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("keep_screen_on_toggle")
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (keepScreenOn) SurfaceControl else ObsidianBg)
                            .border(
                                1.dp,
                                if (keepScreenOn) VerifiedEmerald.copy(alpha = 0.5f) else BorderHairline,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { onToggleKeepScreenOn() }
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ScreenLockPortrait,
                            contentDescription = null,
                            tint = if (keepScreenOn) VerifiedEmerald else MutedStoneText,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.size(5.dp))
                        Text(
                            text = stringResource(if (keepScreenOn) R.string.screen_awake_locked else R.string.keep_screen_on),
                            fontSize = 11.sp,
                            color = if (keepScreenOn) VerifiedEmerald else MutedStoneText,
                            maxLines = 1
                        )
                    }
                }
            }

            // 4. Transparent Editions & Offline Sound Packs (USD / INR)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(SurfaceCard)
                    .border(1.dp, BorderHairline, RoundedCornerShape(14.dp))
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
                            text = stringResource(R.string.editions_title),
                            fontFamily = CormorantGaramondFamily,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = IvoryText
                        )
                        Text(
                            text = stringResource(R.string.editions_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedStoneText
                        )
                    }

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(ObsidianBg)
                            .border(1.dp, BorderHairline, RoundedCornerShape(8.dp))
                            .padding(2.dp)
                    ) {
                        listOf(false to "USD", true to "INR").forEach { (isInr, label) ->
                            Box(
                                modifier = Modifier
                                    .testTag("currency_${label.lowercase()}")
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (useInrCurrency == isInr) SurfaceSelected else Color.Transparent)
                                    .clickable { useInrCurrency = isInr }
                                    .padding(horizontal = 9.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = label,
                                    fontFamily = JetBrainsMonoFamily,
                                    fontSize = 10.5.sp,
                                    color = if (useInrCurrency == isInr) ConcertBrassBright else MutedStoneText
                                )
                            }
                        }
                    }
                }

                EditionTierRow(
                    title = "Concert Stage Lite",
                    price = if (useInrCurrency) "₹0 · Included" else "$0 · Included",
                    details = "Full 88-key Hamburg 274 engine, Railsback tuning, USB/BLE MIDI, continuous CC64 half-pedaling, and offline DSP.",
                    active = true
                )
                EditionTierRow(
                    title = "Concert Virtuoso (One-Time Buyout)",
                    price = if (useInrCurrency) "₹1,499 once" else "$19.99 once",
                    details = "Permanent unlock for NY Studio 274, Felt Upright, Baroque Harpsichord, 1820s Fortepiano, and 24-bit WAV studio stems.",
                    active = false
                )
                EditionTierRow(
                    title = "Conservatory Academy",
                    price = if (useInrCurrency) "₹299 / mo" else "$4.99 / mo",
                    details = "Expanded Urtext classical anthology, unlimited MusicXML/MIDI imports, and multi-device practice progress.",
                    active = false
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(9.dp))
                        .background(VerifiedEmerald.copy(alpha = 0.08f))
                        .border(1.dp, VerifiedEmerald.copy(alpha = 0.35f), RoundedCornerShape(9.dp))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Shield, contentDescription = null, tint = VerifiedEmerald, modifier = Modifier.size(16.dp))
                    Text(
                        text = stringResource(R.string.privacy_badge),
                        fontSize = 11.sp,
                        color = VerifiedEmerald
                    )
                }
            }
        }
    }
}

@Composable
private fun DiagMetricTile(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(9.dp))
            .background(ObsidianBg)
            .border(1.dp, BorderHairline, RoundedCornerShape(9.dp))
            .padding(horizontal = 9.dp, vertical = 7.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(text = label, fontSize = 10.sp, color = MutedStoneText, maxLines = 1)
        Text(
            text = value,
            fontFamily = JetBrainsMonoFamily,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Medium,
            color = IvoryText,
            maxLines = 1
        )
    }
}

@Composable
private fun EditionTierRow(
    title: String,
    price: String,
    details: String,
    active: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (active) SurfaceSelected else ObsidianBg)
            .border(1.dp, if (active) ConcertBrass.copy(alpha = 0.5f) else BorderHairline, RoundedCornerShape(10.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (active) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ConcertBrassBright, modifier = Modifier.size(14.dp))
                }
                Text(title, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = IvoryText)
            }
            Text(price, fontFamily = JetBrainsMonoFamily, fontSize = 11.sp, color = ConcertBrassBright)
        }
        Text(details, fontSize = 11.sp, color = SubtleAshText)
    }
}
