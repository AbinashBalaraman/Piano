package com.hamburg274.piano

import android.graphics.Color as AndroidColor
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Piano
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hamburg274.piano.audio.VoicingId
import com.hamburg274.piano.music.StudioTake
import com.hamburg274.piano.ui.components.AcousticsPanel
import com.hamburg274.piano.ui.components.AndroidMidiPanel
import com.hamburg274.piano.ui.components.PianoKeyboard
import com.hamburg274.piano.ui.components.PracticePanel
import com.hamburg274.piano.ui.components.SoundModuleOverlay
import com.hamburg274.piano.ui.components.StageOverview
import com.hamburg274.piano.ui.components.StudioPanel
import com.hamburg274.piano.ui.theme.BorderHairline
import com.hamburg274.piano.ui.theme.ConcertBrass
import com.hamburg274.piano.ui.theme.ConcertBrassBright
import com.hamburg274.piano.ui.theme.CormorantGaramondFamily
import com.hamburg274.piano.ui.theme.Hamburg274Theme
import com.hamburg274.piano.ui.theme.IvoryText
import com.hamburg274.piano.ui.theme.JetBrainsMonoFamily
import com.hamburg274.piano.ui.theme.MutedStoneText
import com.hamburg274.piano.ui.theme.ObsidianBg
import com.hamburg274.piano.ui.theme.RecordCrimsonLight
import com.hamburg274.piano.ui.theme.SurfaceControl
import com.hamburg274.piano.ui.theme.SurfaceHeader
import com.hamburg274.piano.ui.theme.SurfaceSelected
import com.hamburg274.piano.ui.theme.VerifiedEmerald
import com.hamburg274.piano.ui.theme.WarmSandText
import com.hamburg274.piano.viewmodel.PianoViewModel
import com.hamburg274.piano.viewmodel.WorkspaceTab

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT)
        )

        setContent {
            Hamburg274Theme {
                val vm: PianoViewModel = viewModel()
                val uiState by vm.uiState.collectAsStateWithLifecycle()

                LaunchedEffect(uiState.keepScreenOn) {
                    if (uiState.keepScreenOn) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                }

                Hamburg274App(viewModel = vm)
            }
        }
    }
}

private fun WorkspaceTab.icon(): ImageVector = when (this) {
    WorkspaceTab.STAGE -> Icons.Default.Piano
    WorkspaceTab.PRACTICE -> Icons.Default.LibraryMusic
    WorkspaceTab.ACOUSTICS -> Icons.Default.Tune
    WorkspaceTab.STUDIO -> Icons.Default.Mic
    WorkspaceTab.ANDROID_MIDI -> Icons.Default.Usb
}

@Composable
fun Hamburg274App(viewModel: PianoViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Pending SAF Export State
    var pendingMidiTake by remember { mutableStateOf<StudioTake?>(null) }
    var pendingWavExport by remember { mutableStateOf<Pair<StudioTake, VoicingId>?>(null) }

    val openScoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) viewModel.importScoreFromUri(uri)
    }

    val saveMidiLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("audio/midi")
    ) { uri ->
        val take = pendingMidiTake
        pendingMidiTake = null
        if (uri != null && take != null) {
            viewModel.exportTakeMidiToUri(take, uri)
        }
    }

    val saveWavLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("audio/wav")
    ) { uri ->
        val pair = pendingWavExport
        pendingWavExport = null
        if (uri != null && pair != null) {
            viewModel.exportTakeWavToUri(pair.first, pair.second, uri)
        }
    }

    // Back button returns from secondary workspaces to Stage
    BackHandler(enabled = !state.soundModuleMode && state.activeTab != WorkspaceTab.STAGE) {
        viewModel.selectTab(WorkspaceTab.STAGE)
    }

    if (state.soundModuleMode) {
        SoundModuleOverlay(
            settings = state.settings,
            diagnostics = state.diagnostics,
            activeNoteNames = state.activeNoteNames,
            damperCC64 = state.damperCC64,
            sostenuto = state.sostenuto,
            unaCorda = state.unaCorda,
            midiDeviceCount = state.midiDevices.size,
            onMidiPanic = { viewModel.midiPanic() },
            onExit = { viewModel.setSoundModuleMode(false) }
        )
        return
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
    ) {
        val isExpanded = maxWidth >= 840.dp
        val isWideKeys = maxWidth >= 720.dp
        val isShortHeight = maxHeight < 580.dp

        LaunchedEffect(isWideKeys) {
            viewModel.syncResponsiveKeyCount(isWideKeys)
        }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Canonical NavigationRail on Expanded tablets / foldables
            if (isExpanded) {
                NavigationRail(
                    containerColor = SurfaceHeader,
                    contentColor = IvoryText,
                    modifier = Modifier
                        .fillMaxHeight()
                        .navigationBarsPadding()
                        .border(width = 0.5.dp, color = BorderHairline)
                ) {
                    Spacer(Modifier.height(10.dp))
                    WorkspaceTab.entries.forEach { tab ->
                        val selected = state.activeTab == tab
                        NavigationRailItem(
                            selected = selected,
                            onClick = { viewModel.selectTab(tab) },
                            icon = {
                                Icon(
                                    imageVector = tab.icon(),
                                    contentDescription = tab.label,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = tab.label,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = ConcertBrassBright,
                                selectedTextColor = ConcertBrassBright,
                                unselectedIconColor = MutedStoneText,
                                unselectedTextColor = MutedStoneText,
                                indicatorColor = SurfaceSelected
                            ),
                            modifier = Modifier.testTag("nav_rail_${tab.name.lowercase()}")
                        )
                    }
                }
            }

            // Main Vertical Column: Header + Workspace + Anchored Piano Keybed
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                // Slim Concert Header
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceHeader)
                        .border(width = 0.5.dp, color = BorderHairline)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = stringResource(R.string.app_name),
                                fontFamily = CormorantGaramondFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 21.sp,
                                color = IvoryText,
                                maxLines = 1
                            )
                            Text(
                                text = if (state.settings.voicingId != VoicingId.HAMBURG_274) {
                                    "· ${state.settings.voicingId.shortName}"
                                } else {
                                    "· ${state.diagnostics.sampleRate / 1000} kHz"
                                },
                                fontFamily = JetBrainsMonoFamily,
                                fontSize = 10.5.sp,
                                color = ConcertBrass,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (state.isRecording) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(RecordCrimsonLight.copy(alpha = 0.16f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FiberManualRecord,
                                        contentDescription = "Recording active",
                                        tint = RecordCrimsonLight,
                                        modifier = Modifier.size(9.dp)
                                    )
                                    Text(
                                        text = "REC",
                                        fontFamily = JetBrainsMonoFamily,
                                        fontSize = 9.5.sp,
                                        color = RecordCrimsonLight
                                    )
                                }
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Connected MIDI badge if hardware attached
                            if (state.midiDevices.isNotEmpty()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(VerifiedEmerald.copy(alpha = 0.12f))
                                        .border(1.dp, VerifiedEmerald.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                        .clickable { viewModel.selectTab(WorkspaceTab.ANDROID_MIDI) }
                                        .padding(horizontal = 7.dp, vertical = 4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(VerifiedEmerald)
                                    )
                                    Text(
                                        text = "MIDI ${state.midiDevices.size}",
                                        fontFamily = JetBrainsMonoFamily,
                                        fontSize = 10.sp,
                                        color = VerifiedEmerald
                                    )
                                }
                            }

                            // OLED Sound-Module quick button
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .testTag("header_sound_module_button")
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SurfaceControl)
                                    .border(1.dp, BorderHairline, RoundedCornerShape(8.dp))
                                    .clickable { viewModel.setSoundModuleMode(true) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DarkMode,
                                    contentDescription = stringResource(R.string.sound_module_enter),
                                    tint = ConcertBrass,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // All 5 Tabs Fit Cleanly On-Screen Without Clipping or Ellipsis
                    if (!isExpanded) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 10.dp, end = 10.dp, bottom = 7.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            WorkspaceTab.entries.forEach { tab ->
                                val selected = state.activeTab == tab
                                Row(
                                    modifier = Modifier
                                        .testTag("nav_tab_${tab.name.lowercase()}")
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selected) SurfaceSelected else Color.Transparent)
                                        .border(
                                            1.dp,
                                            if (selected) ConcertBrass.copy(alpha = 0.55f) else BorderHairline,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { viewModel.selectTab(tab) }
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = tab.icon(),
                                        contentDescription = null,
                                        tint = if (selected) ConcertBrassBright else MutedStoneText,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = tab.label,
                                        fontSize = 10.5.sp,
                                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                        color = if (selected) ConcertBrassBright else MutedStoneText,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }

                // Dismissible Status Banner (Import / Export / Recording feedback)
                state.statusBanner?.let { msg ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfaceSelected)
                            .border(width = 0.5.dp, color = ConcertBrass.copy(alpha = 0.45f))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = msg,
                            fontSize = 11.5.sp,
                            color = ConcertBrassBright,
                            modifier = Modifier.weight(1f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        IconButton(
                            onClick = { viewModel.dismissBanner() },
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("dismiss_status_banner")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss notification",
                                tint = WarmSandText,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }

                // Active Workspace Content
                // On STAGE, the fallboard console wraps compactly so the piano keybed claims weight(1f) (~55-60% of the screen)!
                val isStageTab = state.activeTab == WorkspaceTab.STAGE
                Box(
                    modifier = if (isStageTab) {
                        Modifier.fillMaxWidth()
                    } else {
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .then(
                                if (state.activeTab == WorkspaceTab.ANDROID_MIDI) Modifier.navigationBarsPadding()
                                else Modifier
                            )
                    }
                ) {
                    when (state.activeTab) {
                        WorkspaceTab.STAGE -> {
                            StageOverview(
                                settings = state.settings,
                                onUpdateSettings = viewModel::updateSettings,
                                activeNoteNames = state.activeNoteNames,
                                lastVelocity = state.lastVelocity,
                                damperCC64 = state.damperCC64,
                                sostenuto = state.sostenuto,
                                unaCorda = state.unaCorda,
                                isRecording = state.isRecording,
                                onToggleRecord = viewModel::toggleRecording,
                                onOpenPractice = { viewModel.selectTab(WorkspaceTab.PRACTICE) },
                                onOpenAcoustics = { viewModel.selectTab(WorkspaceTab.ACOUSTICS) }
                            )
                        }

                        WorkspaceTab.PRACTICE -> {
                            PracticePanel(
                                repertoire = state.repertoire,
                                selectedPiece = state.selectedPiece,
                                onSelectPiece = viewModel::selectPiece,
                                onImportScoreClick = {
                                    openScoreLauncher.launch(
                                        arrayOf("audio/midi", "audio/x-midi", "application/xml", "text/xml", "*/*")
                                    )
                                },
                                importError = state.importError,
                                viewMode = state.practiceViewMode,
                                onViewModeChange = viewModel::setPracticeViewMode,
                                isPlaying = state.isPracticePlaying,
                                onTogglePlay = viewModel::togglePracticePlay,
                                onRestart = viewModel::restartPractice,
                                currentTimeSec = state.practiceTimeSec,
                                waitMode = state.waitMode,
                                onWaitModeToggle = { viewModel.setWaitMode(!state.waitMode) },
                                waitingForNotes = state.waitingForNotes,
                                handFilter = state.handFilter,
                                onHandFilterChange = viewModel::setHandFilter,
                                tempoPct = state.tempoPct,
                                onTempoPctChange = viewModel::setTempoPct,
                                adaptiveRamp = state.adaptiveRamp,
                                onAdaptiveRampToggle = { viewModel.setAdaptiveRamp(!state.adaptiveRamp) },
                                loopRange = state.loopRange,
                                onLoopRangeChange = viewModel::setLoopRange,
                                rubatoToleranceSec = state.rubatoToleranceSec,
                                onRubatoChange = viewModel::setRubatoTolerance,
                                showFeedback = state.showFeedback,
                                onToggleFeedback = viewModel::toggleFeedback,
                                gradeReport = state.gradeReport,
                                startWhiteIndex = state.startWhiteIndex,
                                visibleWhiteCount = state.visibleWhiteCount
                            )
                        }

                        WorkspaceTab.ACOUSTICS -> {
                            AcousticsPanel(
                                settings = state.settings,
                                onUpdateSettings = viewModel::updateSettings,
                                touchVelocityMode = state.touchVelocityMode,
                                onTouchVelocityModeChange = viewModel::setTouchVelocityMode,
                                fixedVelocity = state.fixedVelocity,
                                onFixedVelocityChange = viewModel::setFixedVelocity,
                                velocityCurve = state.velocityCurve,
                                onVelocityCurveChange = viewModel::setVelocityCurve,
                                lastRawVelocity = state.lastRawVelocity
                            )
                        }

                        WorkspaceTab.STUDIO -> {
                            StudioPanel(
                                isRecording = state.isRecording,
                                recordingElapsedSec = state.recordingElapsedSec,
                                liveEventCount = state.liveEventCount,
                                onToggleRecord = viewModel::toggleRecording,
                                takes = state.takes,
                                playingTakeId = state.playingTakeId,
                                renderingTakeId = state.renderingTakeId,
                                onPlayTake = viewModel::playTake,
                                onStopPlayback = viewModel::stopTakePlayback,
                                onExportMidi = { take ->
                                    pendingMidiTake = take
                                    val safeName = take.name.lowercase().replace(Regex("[^a-z0-9]+"), "-")
                                    saveMidiLauncher.launch("$safeName.mid")
                                },
                                onExportWav = { take, voicing ->
                                    pendingWavExport = take to voicing
                                    val safeName = take.name.lowercase().replace(Regex("[^a-z0-9]+"), "-")
                                    saveWavLauncher.launch("$safeName-${voicing.idKey}-24bit.wav")
                                },
                                onDeleteTake = viewModel::deleteTake
                            )
                        }

                        WorkspaceTab.ANDROID_MIDI -> {
                            AndroidMidiPanel(
                                midiSupported = state.midiSupported,
                                midiStatus = state.midiStatus,
                                midiDevices = state.midiDevices,
                                lastMidiMessage = state.lastMidiMessage,
                                onRescanMidi = viewModel::rescanMidi,
                                onMidiPanic = viewModel::midiPanic,
                                diagnostics = state.diagnostics,
                                keepScreenOn = state.keepScreenOn,
                                onToggleKeepScreenOn = viewModel::toggleKeepScreenOn,
                                onEnterSoundModuleMode = { viewModel.setSoundModuleMode(true) }
                            )
                        }
                    }
                }

                // Anchored 88-Key Concert Piano Keybed
                // On STAGE it expands to fill the majority of the screen (~55-60%) for true concert playability
                if (state.activeTab != WorkspaceTab.ANDROID_MIDI) {
                    PianoKeyboard(
                        activeNotes = state.activeNotes,
                        guideNotes = state.guideHighlights,
                        startWhiteIndex = state.startWhiteIndex,
                        visibleWhiteCount = state.visibleWhiteCount,
                        onViewportChange = viewModel::setViewport,
                        layoutMode = state.layoutMode,
                        onLayoutModeChange = viewModel::setLayoutMode,
                        keyLock = state.keyLock,
                        onKeyLockToggle = viewModel::toggleKeyLock,
                        showLabels = state.showLabels,
                        onShowLabelsToggle = viewModel::toggleShowLabels,
                        touchVelocityMode = state.touchVelocityMode,
                        fixedVelocity = state.fixedVelocity,
                        velocityCurve = state.velocityCurve,
                        damperCC64 = state.damperCC64,
                        onDamperChange = { cc -> viewModel.setDamperCC64(cc) },
                        sostenuto = state.sostenuto,
                        onSostenutoToggle = { viewModel.setSostenuto(!state.sostenuto) },
                        unaCorda = state.unaCorda,
                        onUnaCordaToggle = { viewModel.setUnaCorda(!state.unaCorda) },
                        lastVelocity = state.lastVelocity,
                        lastNoteName = state.lastNoteName,
                        onNoteOn = { midi, finalVel, rawVel -> viewModel.onNoteOn(midi, finalVel, rawVel) },
                        onNoteOff = { midi -> viewModel.onNoteOff(midi) },
                        compactHeight = isShortHeight || state.activeTab == WorkspaceTab.PRACTICE || state.activeTab == WorkspaceTab.ACOUSTICS,
                        fillAvailableHeight = isStageTab,
                        modifier = (if (isStageTab) Modifier.weight(1f) else Modifier).navigationBarsPadding()
                    )
                }
            }
        }
    }
}
