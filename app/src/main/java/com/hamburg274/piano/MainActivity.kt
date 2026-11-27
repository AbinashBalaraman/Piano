package com.hamburg274.piano

import android.graphics.Color as AndroidColor
import android.os.Build
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Piano
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hamburg274.piano.audio.VoicingId
import com.hamburg274.piano.music.StudioTake
import com.hamburg274.piano.music.VelocityEngine
import com.hamburg274.piano.ui.components.AcousticsPanel
import com.hamburg274.piano.ui.components.AndroidMidiPanel
import com.hamburg274.piano.ui.components.PianoKeyboard
import com.hamburg274.piano.ui.components.PracticePanel
import com.hamburg274.piano.ui.components.SoundModuleOverlay
import com.hamburg274.piano.ui.components.StageQuickMixerStrip
import com.hamburg274.piano.ui.components.StudioPanel
import com.hamburg274.piano.ui.components.UpdatePromptDialog
import com.hamburg274.piano.update.PlayUpdateController
import kotlinx.coroutines.delay
import com.hamburg274.piano.ui.theme.BorderHairline
import com.hamburg274.piano.ui.theme.ConcertBrass
import com.hamburg274.piano.ui.theme.ConcertBrassBright
import com.hamburg274.piano.ui.theme.CormorantGaramondFamily
import com.hamburg274.piano.ui.theme.Hamburg274Theme
import com.hamburg274.piano.ui.theme.IvoryText
import com.hamburg274.piano.ui.theme.JetBrainsMonoFamily
import com.hamburg274.piano.ui.theme.MutedStoneText
import com.hamburg274.piano.ui.theme.ObsidianBg
import com.hamburg274.piano.ui.theme.RecordCrimson
import com.hamburg274.piano.ui.theme.RecordCrimsonLight
import com.hamburg274.piano.ui.theme.SurfaceCard
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
        hideSystemBarsImmersive()

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

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBarsImmersive()
    }

    private fun hideSystemBarsImmersive() {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
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
    val context = LocalContext.current

    // Google Play In-App Update Controller (checks asynchronously after initial paint)
    val updateController = remember { PlayUpdateController(context) }
    val updateState by updateController.state.collectAsStateWithLifecycle()

    val updateFlowLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { /* InstallStateUpdatedListener handles flexible update download & completion */ }

    DisposableEffect(updateController) {
        updateController.register()
        onDispose { updateController.unregister() }
    }

    LaunchedEffect(updateController) {
        delay(1500)
        updateController.checkForUpdates(manual = false)
    }

    // Pending SAF Export State
    var pendingMidiTake by remember { mutableStateOf<StudioTake?>(null) }
    var pendingWavExport by remember { mutableStateOf<Pair<StudioTake, VoicingId>?>(null) }
    var voicingMenuOpen by remember { mutableStateOf(false) }

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

    // Back button exits Zen mode, or returns from secondary workspaces to Stage
    BackHandler(enabled = state.zenKeybedMode) {
        viewModel.toggleZenKeybedMode()
    }
    BackHandler(enabled = !state.soundModuleMode && !state.zenKeybedMode && state.activeTab != WorkspaceTab.STAGE) {
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
            .displayCutoutPadding()
    ) {
        val isLandscapeOrWide = maxWidth >= 600.dp
        val isStageTab = state.activeTab == WorkspaceTab.STAGE
        val isZen = isStageTab && state.zenKeybedMode

        LaunchedEffect(isLandscapeOrWide) {
            viewModel.syncResponsiveKeyCount(isLandscapeOrWide)
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 1. Razor-Thin Single-Row Landscape Concert Fallboard Bezel (40dp) — hidden in Zen Full-Screen mode
                if (!isZen) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .background(SurfaceHeader)
                            .border(width = 0.5.dp, color = BorderHairline)
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Left: Brand + Quick Voicing Switcher + Live Chord Badge
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.app_name),
                                fontFamily = CormorantGaramondFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 18.sp,
                                color = IvoryText,
                                maxLines = 1
                            )

                            // Compact Voicing Picker Pill right in the fallboard
                            Box {
                                Row(
                                    modifier = Modifier
                                        .testTag("stage_voicing_selector")
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(SurfaceControl)
                                        .border(1.dp, ConcertBrass.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                                        .clickable { voicingMenuOpen = true }
                                        .padding(horizontal = 7.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = state.settings.voicingId.shortName,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = ConcertBrassBright,
                                        maxLines = 1
                                    )
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Select piano voicing",
                                        tint = ConcertBrassBright,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }

                                DropdownMenu(
                                    expanded = voicingMenuOpen,
                                    onDismissRequest = { voicingMenuOpen = false },
                                    modifier = Modifier.background(SurfaceCard)
                                ) {
                                    VoicingId.entries.forEach { v ->
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(
                                                        text = v.displayName,
                                                        color = if (v == state.settings.voicingId) ConcertBrassBright else IvoryText,
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                    Text(
                                                        text = v.character,
                                                        color = MutedStoneText,
                                                        fontSize = 10.sp,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            },
                                            onClick = {
                                                viewModel.updateSettings { it.copy(voicingId = v) }
                                                voicingMenuOpen = false
                                            }
                                        )
                                    }
                                }
                            }

                            // Quiet live chord / dynamic readout on wide landscape screens
                            if (isLandscapeOrWide && state.activeNoteNames.isNotEmpty()) {
                                val dyn = state.lastVelocity?.let { VelocityEngine.velocityToDynamicMarking(it) } ?: ""
                                Text(
                                    text = "${state.activeNoteNames.take(4).joinToString(" ")} $dyn".trim(),
                                    fontFamily = JetBrainsMonoFamily,
                                    fontSize = 10.5.sp,
                                    color = ConcertBrassBright,
                                    maxLines = 1
                                )
                            }
                        }

                        // Center/Right: All 5 Compact Workspace Tabs
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            WorkspaceTab.entries.forEach { tab ->
                                val selected = state.activeTab == tab
                                Row(
                                    modifier = Modifier
                                        .testTag("nav_tab_${tab.name.lowercase()}")
                                        .clip(RoundedCornerShape(7.dp))
                                        .background(if (selected) SurfaceSelected else Color.Transparent)
                                        .border(
                                            1.dp,
                                            if (selected) ConcertBrass.copy(alpha = 0.55f) else BorderHairline,
                                            RoundedCornerShape(7.dp)
                                        )
                                        .clickable { viewModel.selectTab(tab) }
                                        .padding(horizontal = if (isLandscapeOrWide) 8.dp else 6.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = tab.icon(),
                                        contentDescription = tab.label,
                                        tint = if (selected) ConcertBrassBright else MutedStoneText,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    if (isLandscapeOrWide) {
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

                        // Far Right: Mixer toggle (Stage) · Record · MIDI Badge · OLED · 100% Fullscreen Zen
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            if (isStageTab) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .testTag("stage_mixer_toggle_button")
                                        .clip(RoundedCornerShape(7.dp))
                                        .background(if (state.stageMixerExpanded) SurfaceSelected else SurfaceControl)
                                        .border(
                                            1.dp,
                                            if (state.stageMixerExpanded) ConcertBrass else BorderHairline,
                                            RoundedCornerShape(7.dp)
                                        )
                                        .clickable { viewModel.toggleStageMixer() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Equalizer,
                                        contentDescription = "Toggle quick acoustic mixer",
                                        tint = if (state.stageMixerExpanded) ConcertBrassBright else WarmSandText,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }

                            // Quick Record button right in the fallboard
                            Row(
                                modifier = Modifier
                                    .height(28.dp)
                                    .testTag("stage_record_button")
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(if (state.isRecording) RecordCrimson else SurfaceControl)
                                    .border(
                                        1.dp,
                                        if (state.isRecording) RecordCrimsonLight else BorderHairline,
                                        RoundedCornerShape(7.dp)
                                    )
                                    .clickable { viewModel.toggleRecording() }
                                    .padding(horizontal = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = if (state.isRecording) Icons.Default.Stop else Icons.Default.FiberManualRecord,
                                    contentDescription = stringResource(if (state.isRecording) R.string.stage_stop_rec else R.string.stage_record),
                                    tint = if (state.isRecording) IvoryText else RecordCrimsonLight,
                                    modifier = Modifier.size(12.dp)
                                )
                                if (state.isRecording) {
                                    Text(
                                        text = "REC",
                                        fontFamily = JetBrainsMonoFamily,
                                        fontSize = 9.5.sp,
                                        color = IvoryText
                                    )
                                }
                            }

                            if (state.midiDevices.isNotEmpty()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    modifier = Modifier
                                        .height(28.dp)
                                        .clip(RoundedCornerShape(7.dp))
                                        .background(VerifiedEmerald.copy(alpha = 0.12f))
                                        .border(1.dp, VerifiedEmerald.copy(alpha = 0.4f), RoundedCornerShape(7.dp))
                                        .clickable { viewModel.selectTab(WorkspaceTab.ANDROID_MIDI) }
                                        .padding(horizontal = 6.dp)
                                ) {
                                    Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(VerifiedEmerald))
                                    Text(
                                        text = "${state.midiDevices.size}",
                                        fontFamily = JetBrainsMonoFamily,
                                        fontSize = 9.5.sp,
                                        color = VerifiedEmerald
                                    )
                                }
                            }

                            // OLED Sound-Module button
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .testTag("header_sound_module_button")
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(SurfaceControl)
                                    .border(1.dp, BorderHairline, RoundedCornerShape(7.dp))
                                    .clickable { viewModel.setSoundModuleMode(true) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DarkMode,
                                    contentDescription = stringResource(R.string.sound_module_enter),
                                    tint = ConcertBrass,
                                    modifier = Modifier.size(14.dp)
                                )
                            }

                            // 100% Pure Full-Screen Keybed (Zen) Toggle on Stage
                            if (isStageTab) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .testTag("stage_zen_fullscreen_button")
                                        .clip(RoundedCornerShape(7.dp))
                                        .background(SurfaceControl)
                                        .border(1.dp, BorderHairline, RoundedCornerShape(7.dp))
                                        .clickable { viewModel.toggleZenKeybedMode() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Fullscreen,
                                        contentDescription = "Enter 100% full-screen piano",
                                        tint = ConcertBrassBright,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Dismissible Status Banner (only visible briefly after import/export/save)
                if (!isZen) {
                    state.statusBanner?.let { msg ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SurfaceSelected)
                                .border(width = 0.5.dp, color = ConcertBrass.copy(alpha = 0.45f))
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = msg,
                                fontSize = 11.sp,
                                color = ConcertBrassBright,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            IconButton(
                                onClick = { viewModel.dismissBanner() },
                                modifier = Modifier
                                    .size(24.dp)
                                    .testTag("dismiss_status_banner")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss notification",
                                    tint = WarmSandText,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }

                // 3. Optional Quick Mixer Strip on Stage (collapsed by default -> piano takes ~85-93% of screen!)
                if (isStageTab && state.stageMixerExpanded && !isZen) {
                    StageQuickMixerStrip(
                        settings = state.settings,
                        onUpdateSettings = viewModel::updateSettings,
                        onOpenAcoustics = { viewModel.selectTab(WorkspaceTab.ACOUSTICS) }
                    )
                }

                // 4. Secondary Workspace Panels (Practice, Acoustics, Studio, MIDI)
                if (!isStageTab) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        when (state.activeTab) {
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
                                    onEnterSoundModuleMode = { viewModel.setSoundModuleMode(true) },
                                    isCheckingUpdate = updateState.isChecking,
                                    updateStatusMessage = updateState.statusMessage,
                                    onCheckForUpdates = { updateController.checkForUpdates(manual = true) }
                                )
                            }

                            WorkspaceTab.STAGE -> Unit
                        }
                    }
                }

                // 5. Full-Width 88-Key Concert Piano Keybed
                // On STAGE: fills 100% of remaining landscape height (~83% normal, ~100% in Zen mode!)
                // On PRACTICE: generous 156dp keybed aligned to the widescreen Urtext/Waterfall above
                // On ACOUSTICS/STUDIO: slim 126dp audition keybed for instant live tone testing
                if (state.activeTab != WorkspaceTab.ANDROID_MIDI) {
                    val secondaryHeight = when (state.activeTab) {
                        WorkspaceTab.PRACTICE -> 152.dp
                        else -> 124.dp
                    }
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
                        fillAvailableHeight = isStageTab,
                        fixedKeybedHeight = secondaryHeight,
                        hideControlRibbon = isZen,
                        modifier = if (isStageTab) Modifier.weight(1f) else Modifier
                    )
                }
            }

            // Minimal floating exit pill when in 100% Pure Full-Screen Zen Piano mode
            if (isZen) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 10.dp, end = 10.dp)
                        .testTag("stage_exit_zen_button")
                        .clip(RoundedCornerShape(8.dp))
                        .background(ObsidianBg.copy(alpha = 0.78f))
                        .border(1.dp, ConcertBrass.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .clickable { viewModel.toggleZenKeybedMode() }
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FullscreenExit,
                        contentDescription = "Show piano controls",
                        tint = ConcertBrassBright,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Controls",
                        fontSize = 10.5.sp,
                        color = ConcertBrassBright
                    )
                }
            }
            // Google Play In-App Update brand-matched modal overlay
            UpdatePromptDialog(
                state = updateState,
                onUpdateNow = { updateController.startFlexibleUpdate(updateFlowLauncher) },
                onViewInStore = { updateController.openPlayStoreListing() },
                onLater = { updateController.dismissForCooldown() }
            )
        }
    }
}
