package com.hamburg274.piano.viewmodel

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hamburg274.piano.audio.AcousticSettings
import com.hamburg274.piano.audio.AudioDiagnosticsSnapshot
import com.hamburg274.piano.audio.HamburgPianoEngine
import com.hamburg274.piano.audio.VoicingId
import com.hamburg274.piano.midi.AndroidMidiController
import com.hamburg274.piano.midi.ConnectedMidiDevice
import com.hamburg274.piano.music.ClassicalPiece
import com.hamburg274.piano.music.ClassicalRepertoire
import com.hamburg274.piano.music.ExpressiveGradeReport
import com.hamburg274.piano.music.HandPart
import com.hamburg274.piano.music.MidiCodec
import com.hamburg274.piano.music.MidiEventType
import com.hamburg274.piano.music.PedalSample
import com.hamburg274.piano.music.PianoTuning
import com.hamburg274.piano.music.PlayedNoteSample
import com.hamburg274.piano.music.PracticeGrader
import com.hamburg274.piano.music.RecordedMidiEvent
import com.hamburg274.piano.music.StudioTake
import com.hamburg274.piano.music.TemperamentId
import com.hamburg274.piano.music.TouchVelocityMode
import com.hamburg274.piano.music.VelocityCurvePoint
import com.hamburg274.piano.music.VelocityEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

enum class WorkspaceTab(val label: String) {
    STAGE("Stage"),
    PRACTICE("Practice"),
    ACOUSTICS("Acoustics"),
    STUDIO("Studio"),
    ANDROID_MIDI("MIDI")
}

enum class KeyboardLayoutMode(val label: String) {
    SINGLE("Single"),
    DUAL("Dual"),
    DUET("Duet")
}

enum class PracticeViewMode(val label: String) {
    URTEXT("Urtext"),
    SPLIT("Split"),
    WATERFALL("Waterfall")
}

enum class HandFilter(val label: String) {
    BOTH("Both"),
    RH("RH"),
    LH("LH")
}

data class GuideNoteHighlight(
    val midi: Int,
    val hand: HandPart
)

data class PianoUiState(
    val activeTab: WorkspaceTab = WorkspaceTab.STAGE,
    val soundModuleMode: Boolean = false,
    val keepScreenOn: Boolean = true,

    // Acoustics & Velocity
    val settings: AcousticSettings = AcousticSettings(),
    val touchVelocityMode: TouchVelocityMode = TouchVelocityMode.Y_POSITION,
    val fixedVelocity: Int = 82,
    val velocityCurve: List<VelocityCurvePoint> = VelocityEngine.PRESETS.first().points,

    // Keybed Ergonomics (Default C4..E5 = 10 generous playable white keys on phones)
    val startWhiteIndex: Int = 23,
    val visibleWhiteCount: Int = 10,
    val layoutMode: KeyboardLayoutMode = KeyboardLayoutMode.SINGLE,
    val keyLock: Boolean = true,
    val showLabels: Boolean = true,

    // Live Notes & Pedals
    val activeNotes: Set<Int> = emptySet(),
    val lastVelocity: Int? = null,
    val lastRawVelocity: Int? = null,
    val lastNoteName: String? = null,
    val damperCC64: Int = 0,
    val sostenuto: Boolean = false,
    val unaCorda: Boolean = false,

    // Practice Engine
    val repertoire: List<ClassicalPiece> = ClassicalRepertoire.PIECES,
    val selectedPieceId: String = ClassicalRepertoire.PIECES.first().id,
    val practiceViewMode: PracticeViewMode = PracticeViewMode.SPLIT,
    val isPracticePlaying: Boolean = false,
    val practiceTimeSec: Double = 0.0,
    val waitMode: Boolean = false,
    val handFilter: HandFilter = HandFilter.BOTH,
    val tempoPct: Int = 100,
    val adaptiveRamp: Boolean = false,
    val loopRange: Pair<Int, Int>? = null,
    val rubatoToleranceSec: Double = 0.16,
    val showFeedback: Boolean = false,
    val importError: String? = null,
    val playedSamples: List<PlayedNoteSample> = emptyList(),
    val pedalSamples: List<PedalSample> = emptyList(),

    // Studio Recorder
    val isRecording: Boolean = false,
    val recordingElapsedSec: Double = 0.0,
    val liveEventCount: Int = 0,
    val takes: List<StudioTake> = listOf(MidiCodec.DEMO_STUDIO_TAKE),
    val playingTakeId: String? = null,
    val renderingTakeId: String? = null,
    val statusBanner: String? = null,

    // MIDI & Pro Audio Diagnostics
    val midiSupported: Boolean = false,
    val midiStatus: String = "Ready",
    val midiDevices: List<ConnectedMidiDevice> = emptyList(),
    val lastMidiMessage: String = "No MIDI traffic yet",
    val diagnostics: AudioDiagnosticsSnapshot = AudioDiagnosticsSnapshot()
) {
    val selectedPiece: ClassicalPiece
        get() = repertoire.firstOrNull { it.id == selectedPieceId } ?: repertoire.first()

    val activeNoteNames: List<String>
        get() = activeNotes.sorted().map { PianoTuning.midiToNoteName(it) }

    val guideHighlights: List<GuideNoteHighlight>
        get() {
            if (activeTab != WorkspaceTab.PRACTICE) return emptyList()
            return selectedPiece.notes.filter { n ->
                matchesHand(n.hand) &&
                    n.startSec >= practiceTimeSec - 0.08 &&
                    n.startSec <= practiceTimeSec + 0.35
            }.map { GuideNoteHighlight(it.midi, it.hand) }
        }

    val waitingForNotes: List<Int>
        get() {
            if (!waitMode) return emptyList()
            return selectedPiece.notes
                .filter { matchesHand(it.hand) && abs(it.startSec - practiceTimeSec) <= 0.14 }
                .filter { it.midi !in activeNotes }
                .map { it.midi }
                .distinct()
        }

    val gradeReport: ExpressiveGradeReport
        get() {
            val expected = selectedPiece.notes.filter { matchesHand(it.hand) }
            return PracticeGrader.evaluateExpressivePerformance(
                expected = expected,
                played = playedSamples,
                pedalEvents = pedalSamples,
                rubatoToleranceSec = rubatoToleranceSec,
                harmonicChangeTimesSec = selectedPiece.harmonicChangeTimesSec
            )
        }

    fun matchesHand(hand: HandPart): Boolean = when (handFilter) {
        HandFilter.BOTH -> true
        HandFilter.RH -> hand == HandPart.RH
        HandFilter.LH -> hand == HandPart.LH
    }
}

class PianoViewModel(application: Application) : AndroidViewModel(application) {

    private val engine = HamburgPianoEngine(application)
    private val _uiState = MutableStateFlow(PianoUiState())
    val uiState: StateFlow<PianoUiState> = _uiState.asStateFlow()

    private val midiController: AndroidMidiController

    private var recordingStartNanos = 0L
    private val recordedEvents = mutableListOf<RecordedMidiEvent>()
    private var recordingTimerJob: Job? = null
    private var practiceClockJob: Job? = null
    private var takePlaybackJob: Job? = null
    private var savedVoicingBeforeAudition: VoicingId? = null
    private val practiceTriggeredIds = mutableSetOf<String>()

    init {
        engine.start()
        midiController = AndroidMidiController(
            context = application,
            onNoteOn = { note, rawVel ->
                val mapped = VelocityEngine.applyVelocityCurve(rawVel, _uiState.value.velocityCurve)
                onNoteOn(note, mapped, rawVel)
            },
            onNoteOff = { note -> onNoteOff(note) },
            onCC = { cc, value ->
                when (cc) {
                    64 -> setDamperCC64(value)
                    66 -> setSostenuto(value >= 64)
                    67 -> setUnaCorda(value >= 64)
                }
            },
            onPanic = { midiPanic() },
            onStateUpdated = { devices, status, lastMsg ->
                _uiState.update {
                    it.copy(
                        midiDevices = devices,
                        midiStatus = status,
                        lastMidiMessage = lastMsg
                    )
                }
            }
        )
        _uiState.update {
            it.copy(
                midiSupported = midiController.isSupported,
                diagnostics = engine.getDiagnostics()
            )
        }
        midiController.start()

        // Periodic diagnostics refresh
        viewModelScope.launch {
            while (isActive) {
                delay(500)
                val st = _uiState.value
                if (st.activeTab == WorkspaceTab.ANDROID_MIDI || st.soundModuleMode) {
                    _uiState.update { it.copy(diagnostics = engine.getDiagnostics()) }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        practiceClockJob?.cancel()
        takePlaybackJob?.cancel()
        recordingTimerJob?.cancel()
        midiController.stop()
        engine.stop()
    }

    fun selectTab(tab: WorkspaceTab) {
        _uiState.update { it.copy(activeTab = tab, statusBanner = null) }
    }

    fun setSoundModuleMode(enabled: Boolean) {
        _uiState.update {
            it.copy(
                soundModuleMode = enabled,
                diagnostics = engine.getDiagnostics()
            )
        }
    }

    fun toggleKeepScreenOn() {
        _uiState.update { it.copy(keepScreenOn = !it.keepScreenOn) }
    }

    fun dismissBanner() {
        _uiState.update { it.copy(statusBanner = null, importError = null) }
    }

    fun updateSettings(transform: (AcousticSettings) -> AcousticSettings) {
        _uiState.update { st ->
            val next = transform(st.settings)
            engine.updateSettings(next)
            st.copy(settings = next)
        }
    }

    fun setTouchVelocityMode(mode: TouchVelocityMode) {
        _uiState.update { it.copy(touchVelocityMode = mode) }
    }

    fun setFixedVelocity(vel: Int) {
        _uiState.update { it.copy(fixedVelocity = vel.coerceIn(20, 127)) }
    }

    fun setVelocityCurve(points: List<VelocityCurvePoint>) {
        _uiState.update { it.copy(velocityCurve = points) }
    }

    fun setViewport(startWhite: Int, visibleCount: Int) {
        val count = visibleCount.coerceIn(8, 28)
        val start = startWhite.coerceIn(0, PianoTuning.WHITE_KEY_COUNT - count)
        _uiState.update { it.copy(startWhiteIndex = start, visibleWhiteCount = count) }
    }

    fun syncResponsiveKeyCount(isExpandedWidth: Boolean) {
        _uiState.update { st ->
            if (isExpandedWidth && st.visibleWhiteCount < 14) {
                st.copy(visibleWhiteCount = 15, startWhiteIndex = 16) // C3..C5 on tablets
            } else if (!isExpandedWidth && st.visibleWhiteCount > 13) {
                st.copy(visibleWhiteCount = 10, startWhiteIndex = 23) // C4..E5 on phones
            } else {
                st
            }
        }
    }

    fun setLayoutMode(mode: KeyboardLayoutMode) {
        _uiState.update { it.copy(layoutMode = mode) }
    }

    fun toggleKeyLock() {
        _uiState.update { it.copy(keyLock = !it.keyLock) }
    }

    fun toggleShowLabels() {
        _uiState.update { it.copy(showLabels = !it.showLabels) }
    }

    fun onNoteOn(midi: Int, finalVelocity: Int, rawVelocity: Int? = null, fromPlayback: Boolean = false) {
        engine.noteOn(midi, finalVelocity)
        _uiState.update { st ->
            val nextNotes = st.activeNotes + midi
            val nextPlayed = if (!fromPlayback && st.activeTab == WorkspaceTab.PRACTICE) {
                st.playedSamples + PlayedNoteSample(midi, st.practiceTimeSec, finalVelocity)
            } else {
                st.playedSamples
            }
            st.copy(
                activeNotes = nextNotes,
                lastVelocity = finalVelocity,
                lastRawVelocity = rawVelocity ?: st.lastRawVelocity,
                lastNoteName = PianoTuning.midiToNoteName(midi),
                playedSamples = nextPlayed
            )
        }

        if (!fromPlayback && _uiState.value.isRecording) {
            val tSec = (System.nanoTime() - recordingStartNanos) / 1_000_000_000.0
            synchronized(recordedEvents) {
                recordedEvents.add(RecordedMidiEvent(tSec, MidiEventType.NOTE_ON, midi = midi, velocity = finalVelocity))
            }
            _uiState.update { it.copy(liveEventCount = recordedEvents.size) }
        }
    }

    fun onNoteOff(midi: Int, fromPlayback: Boolean = false) {
        engine.noteOff(midi)
        _uiState.update { st -> st.copy(activeNotes = st.activeNotes - midi) }
        if (!fromPlayback && _uiState.value.isRecording) {
            val tSec = (System.nanoTime() - recordingStartNanos) / 1_000_000_000.0
            synchronized(recordedEvents) {
                recordedEvents.add(RecordedMidiEvent(tSec, MidiEventType.NOTE_OFF, midi = midi, velocity = 64))
            }
            _uiState.update { it.copy(liveEventCount = recordedEvents.size) }
        }
    }

    fun setDamperCC64(cc64: Int, fromPlayback: Boolean = false) {
        val clamped = cc64.coerceIn(0, 127)
        engine.setDamperPedal(clamped)
        _uiState.update { st ->
            val nextPedals = if (!fromPlayback && st.activeTab == WorkspaceTab.PRACTICE) {
                st.pedalSamples + PedalSample(st.practiceTimeSec, clamped)
            } else {
                st.pedalSamples
            }
            st.copy(damperCC64 = clamped, pedalSamples = nextPedals)
        }
        if (!fromPlayback && _uiState.value.isRecording) {
            val tSec = (System.nanoTime() - recordingStartNanos) / 1_000_000_000.0
            synchronized(recordedEvents) {
                recordedEvents.add(RecordedMidiEvent(tSec, MidiEventType.CC, cc = 64, value = clamped))
            }
            _uiState.update { it.copy(liveEventCount = recordedEvents.size) }
        }
    }

    fun setSostenuto(engaged: Boolean, fromPlayback: Boolean = false) {
        engine.setSostenutoPedal(engaged)
        _uiState.update { it.copy(sostenuto = engaged) }
        if (!fromPlayback && _uiState.value.isRecording) {
            val tSec = (System.nanoTime() - recordingStartNanos) / 1_000_000_000.0
            synchronized(recordedEvents) {
                recordedEvents.add(RecordedMidiEvent(tSec, MidiEventType.CC, cc = 66, value = if (engaged) 127 else 0))
            }
            _uiState.update { it.copy(liveEventCount = recordedEvents.size) }
        }
    }

    fun setUnaCorda(engaged: Boolean, fromPlayback: Boolean = false) {
        engine.setUnaCordaPedal(engaged)
        _uiState.update { it.copy(unaCorda = engaged) }
        if (!fromPlayback && _uiState.value.isRecording) {
            val tSec = (System.nanoTime() - recordingStartNanos) / 1_000_000_000.0
            synchronized(recordedEvents) {
                recordedEvents.add(RecordedMidiEvent(tSec, MidiEventType.CC, cc = 67, value = if (engaged) 127 else 0))
            }
            _uiState.update { it.copy(liveEventCount = recordedEvents.size) }
        }
    }

    fun midiPanic() {
        engine.panic()
        _uiState.update {
            it.copy(
                activeNotes = emptySet(),
                damperCC64 = 0,
                sostenuto = false,
                unaCorda = false
            )
        }
    }

    fun rescanMidi() {
        midiController.rescan()
    }

    // --- Studio Recording & Non-Destructive Re-Voice ---

    fun toggleRecording() {
        if (_uiState.value.isRecording) stopRecording() else startRecording()
    }

    fun startRecording() {
        synchronized(recordedEvents) {
            recordedEvents.clear()
            val st = _uiState.value
            if (st.damperCC64 > 0) recordedEvents.add(RecordedMidiEvent(0.0, MidiEventType.CC, cc = 64, value = st.damperCC64))
            if (st.unaCorda) recordedEvents.add(RecordedMidiEvent(0.0, MidiEventType.CC, cc = 67, value = 127))
        }
        recordingStartNanos = System.nanoTime()
        _uiState.update { it.copy(isRecording = true, recordingElapsedSec = 0.0, liveEventCount = recordedEvents.size) }
        recordingTimerJob?.cancel()
        recordingTimerJob = viewModelScope.launch {
            while (isActive && _uiState.value.isRecording) {
                val sec = (System.nanoTime() - recordingStartNanos) / 1_000_000_000.0
                _uiState.update { it.copy(recordingElapsedSec = sec) }
                delay(100)
            }
        }
    }

    fun stopRecording() {
        recordingTimerJob?.cancel()
        val durationSec = max(0.5, (System.nanoTime() - recordingStartNanos) / 1_000_000_000.0)
        val snapshot = synchronized(recordedEvents) { recordedEvents.toList() }
        _uiState.update { st ->
            if (snapshot.isEmpty()) {
                st.copy(isRecording = false)
            } else {
                val take = StudioTake(
                    id = "take-${System.currentTimeMillis()}",
                    name = "Studio Take #${st.takes.size + 1} (${st.settings.voicingId.shortName})",
                    createdAt = System.currentTimeMillis(),
                    durationSec = durationSec,
                    voicingId = st.settings.voicingId.idKey,
                    events = snapshot
                )
                st.copy(
                    isRecording = false,
                    takes = listOf(take) + st.takes,
                    statusBanner = "Saved ${take.name} (${snapshot.size} MIDI events)"
                )
            }
        }
    }

    fun playTake(take: StudioTake, revoiceId: VoicingId) {
        stopTakePlayback()
        savedVoicingBeforeAudition = _uiState.value.settings.voicingId
        engine.updateSettings(_uiState.value.settings.copy(voicingId = revoiceId))
        _uiState.update { it.copy(playingTakeId = take.id) }

        takePlaybackJob = viewModelScope.launch {
            val sorted = take.events.sortedBy { it.timeSec }
            val startNanos = System.nanoTime()
            for (ev in sorted) {
                if (!isActive) break
                val targetMs = (ev.timeSec * 1000.0).toLong()
                val elapsedMs = (System.nanoTime() - startNanos) / 1_000_000L
                val waitMs = targetMs - elapsedMs
                if (waitMs > 0) delay(waitMs)
                when (ev.type) {
                    MidiEventType.NOTE_ON -> ev.midi?.let { onNoteOn(it, ev.velocity ?: 80, fromPlayback = true) }
                    MidiEventType.NOTE_OFF -> ev.midi?.let { onNoteOff(it, fromPlayback = true) }
                    MidiEventType.CC -> when (ev.cc) {
                        64 -> setDamperCC64(ev.value ?: 0, fromPlayback = true)
                        66 -> setSostenuto((ev.value ?: 0) >= 64, fromPlayback = true)
                        67 -> setUnaCorda((ev.value ?: 0) >= 64, fromPlayback = true)
                    }
                }
            }
            delay(500)
            stopTakePlayback()
        }
    }

    fun stopTakePlayback() {
        takePlaybackJob?.cancel()
        takePlaybackJob = null
        midiPanic()
        savedVoicingBeforeAudition?.let { original ->
            engine.updateSettings(_uiState.value.settings.copy(voicingId = original))
            savedVoicingBeforeAudition = null
        }
        _uiState.update { it.copy(playingTakeId = null) }
    }

    fun deleteTake(id: String) {
        if (_uiState.value.playingTakeId == id) stopTakePlayback()
        _uiState.update { it.copy(takes = it.takes.filterNot { t -> t.id == id }) }
    }

    fun exportTakeMidiToUri(take: StudioTake, uri: Uri) {
        viewModelScope.launch {
            try {
                val bytes = withContext(Dispatchers.Default) {
                    MidiCodec.encodeStandardMidiFile(take.events, bpm = 120)
                }
                withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
                }
                _uiState.update { it.copy(statusBanner = "Exported lossless Standard MIDI (.mid) with true CC64 lane.") }
            } catch (e: Exception) {
                _uiState.update { it.copy(statusBanner = "MIDI export failed: ${e.message}") }
            }
        }
    }

    fun exportTakeWavToUri(take: StudioTake, revoiceId: VoicingId, uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(renderingTakeId = take.id) }
            try {
                val bytes = withContext(Dispatchers.Default) {
                    engine.renderTakeToWav24(take.events, revoiceId)
                }
                withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
                }
                _uiState.update {
                    it.copy(
                        renderingTakeId = null,
                        statusBanner = "Exported 48 kHz / 24-bit WAV (${revoiceId.shortName})."
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(renderingTakeId = null, statusBanner = "WAV render failed: ${e.message}")
                }
            }
        }
    }

    // --- Practice Engine ---

    fun selectPiece(id: String) {
        stopPracticePlayback()
        practiceTriggeredIds.clear()
        val piece = _uiState.value.repertoire.firstOrNull { it.id == id } ?: return
        _uiState.update {
            it.copy(
                selectedPieceId = id,
                practiceTimeSec = 0.0,
                loopRange = null,
                playedSamples = emptyList(),
                pedalSamples = emptyList(),
                rubatoToleranceSec = piece.recommendedRubatoSec,
                importError = null
            )
        }
    }

    fun setPracticeViewMode(mode: PracticeViewMode) {
        _uiState.update { it.copy(practiceViewMode = mode) }
    }

    fun setWaitMode(enabled: Boolean) {
        _uiState.update { it.copy(waitMode = enabled) }
    }

    fun setHandFilter(filter: HandFilter) {
        _uiState.update { it.copy(handFilter = filter) }
    }

    fun setTempoPct(pct: Int) {
        _uiState.update { it.copy(tempoPct = pct.coerceIn(40, 125)) }
    }

    fun setAdaptiveRamp(enabled: Boolean) {
        _uiState.update { it.copy(adaptiveRamp = enabled) }
    }

    fun setLoopRange(range: Pair<Int, Int>?) {
        _uiState.update { it.copy(loopRange = range) }
    }

    fun setRubatoTolerance(sec: Double) {
        _uiState.update { it.copy(rubatoToleranceSec = sec.coerceIn(0.08, 0.34)) }
    }

    fun toggleFeedback() {
        _uiState.update { it.copy(showFeedback = !it.showFeedback) }
    }

    fun restartPractice() {
        val st = _uiState.value
        val startSec = st.loopRange?.let { measureStartSec(st.selectedPiece, it.first) } ?: 0.0
        practiceTriggeredIds.clear()
        _uiState.update {
            it.copy(
                practiceTimeSec = startSec,
                playedSamples = emptyList(),
                pedalSamples = emptyList()
            )
        }
    }

    fun togglePracticePlay() {
        if (_uiState.value.isPracticePlaying) {
            stopPracticePlayback()
        } else {
            val st = _uiState.value
            if (st.practiceTimeSec >= st.selectedPiece.durationSec - 0.1) {
                restartPractice()
            }
            startPracticePlayback()
        }
    }

    private fun stopPracticePlayback() {
        practiceClockJob?.cancel()
        practiceClockJob = null
        _uiState.update { it.copy(isPracticePlaying = false) }
    }

    private fun startPracticePlayback() {
        _uiState.update { it.copy(isPracticePlaying = true) }
        practiceClockJob?.cancel()
        practiceClockJob = viewModelScope.launch {
            var lastNanos = System.nanoTime()
            while (isActive && _uiState.value.isPracticePlaying) {
                delay(16)
                val nowNanos = System.nanoTime()
                val dt = min(0.06, (nowNanos - lastNanos) / 1_000_000_000.0)
                lastNanos = nowNanos

                val st = _uiState.value
                val piece = st.selectedPiece
                val cur = st.practiceTimeSec

                val activeHandNotes = piece.notes.filter { st.matchesHand(it.hand) }
                val pendingWait = if (st.waitMode) {
                    activeHandNotes.any { n ->
                        abs(n.startSec - cur) <= 0.09 &&
                            "wait-${n.id}" !in practiceTriggeredIds &&
                            n.midi !in st.activeNotes
                    }
                } else {
                    false
                }

                if (st.waitMode) {
                    activeHandNotes.forEach { n ->
                        if (abs(n.startSec - cur) <= 0.14 && n.midi in st.activeNotes) {
                            practiceTriggeredIds.add("wait-${n.id}")
                        }
                    }
                }

                if (!pendingWait) {
                    val nextTime = cur + dt * (st.tempoPct / 100.0)

                    // Audition guide notes (demo playback or opposite hand in Hand Isolation)
                    for (n in piece.notes) {
                        val shouldAutoPlay = (!st.waitMode && st.handFilter == HandFilter.BOTH) ||
                            (st.handFilter != HandFilter.BOTH && !st.matchesHand(n.hand))
                        if (shouldAutoPlay && n.startSec >= cur && n.startSec < nextTime && n.id !in practiceTriggeredIds) {
                            practiceTriggeredIds.add(n.id)
                            engine.noteOn(n.midi, n.targetVelocity)
                            val holdMs = max(120L, ((n.durationSec / (st.tempoPct / 100.0)) * 920.0).toLong())
                            viewModelScope.launch {
                                delay(holdMs)
                                engine.noteOff(n.midi)
                            }
                        }
                    }

                    val loopEndSec = st.loopRange?.let {
                        measureStartSec(piece, it.second + 1) ?: piece.durationSec
                    } ?: piece.durationSec
                    val loopStartSec = st.loopRange?.let { measureStartSec(piece, it.first) ?: 0.0 } ?: 0.0

                    if (nextTime >= loopEndSec) {
                        if (st.loopRange != null) {
                            practiceTriggeredIds.clear()
                            val nextTempo = if (st.adaptiveRamp && st.tempoPct < 120) {
                                min(120, st.tempoPct + 5)
                            } else {
                                st.tempoPct
                            }
                            _uiState.update { it.copy(practiceTimeSec = loopStartSec, tempoPct = nextTempo) }
                        } else {
                            _uiState.update { it.copy(practiceTimeSec = piece.durationSec, isPracticePlaying = false) }
                            break
                        }
                    } else {
                        _uiState.update { it.copy(practiceTimeSec = nextTime) }
                    }
                }
            }
        }
    }

    private fun measureStartSec(piece: ClassicalPiece, measure: Int): Double? =
        piece.notes.filter { it.measure == measure }.minOfOrNull { it.startSec }

    fun importScoreFromUri(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(importError = null) }
            try {
                val app = getApplication<Application>()
                val displayName = resolveFileName(app, uri) ?: "Imported Score"
                val bytes = withContext(Dispatchers.IO) {
                    app.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        ?: throw IllegalStateException("Cannot read selected file.")
                }

                val isMidi = displayName.lowercase().endsWith(".mid") ||
                    displayName.lowercase().endsWith(".midi") ||
                    (bytes.size >= 4 && bytes[0].toInt() == 0x4D && bytes[1].toInt() == 0x54)

                val parsed = withContext(Dispatchers.Default) {
                    if (isMidi) {
                        MidiCodec.parseStandardMidiFile(bytes, displayName.replace(Regex("\\.(mid|midi)$", RegexOption.IGNORE_CASE), ""))
                    } else {
                        MidiCodec.parseMusicXml(bytes.toString(Charsets.UTF_8))
                    }
                }

                if (parsed.notes.isEmpty()) {
                    throw IllegalArgumentException("No playable notes found in selected score.")
                }

                val maxMeasure = parsed.notes.maxOfOrNull { it.measure } ?: 1
                val imported = ClassicalPiece(
                    id = "imp-${System.currentTimeMillis()}",
                    title = parsed.title,
                    composer = if (isMidi) "Imported Standard MIDI" else "Imported MusicXML",
                    opus = "${parsed.notes.size} notes",
                    era = "Classical",
                    grade = "Custom",
                    keySignature = "Urtext",
                    timeSignatureNum = 4,
                    timeSignatureDen = 4,
                    defaultBpm = parsed.bpm,
                    recommendedRubatoSec = 0.16,
                    recommendedTemperament = TemperamentId.RAILSBACK,
                    measures = maxMeasure,
                    harmonicChangeTimesSec = emptyList(),
                    notes = parsed.notes,
                    durationSec = max(2.0, parsed.durationSec + 0.8),
                    urtextNote = "Imported score engraved onto Grand Staff & Falling-Note Waterfall."
                )

                stopPracticePlayback()
                practiceTriggeredIds.clear()
                _uiState.update { st ->
                    st.copy(
                        repertoire = listOf(imported) + st.repertoire,
                        selectedPieceId = imported.id,
                        practiceTimeSec = 0.0,
                        loopRange = null,
                        playedSamples = emptyList(),
                        pedalSamples = emptyList(),
                        statusBanner = "Imported \"${imported.title}\" (${parsed.notes.size} notes)"
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(importError = e.message ?: "Unable to parse imported score.") }
            }
        }
    }

    private fun resolveFileName(app: Application, uri: Uri): String? {
        return try {
            app.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        } catch (_: Exception) {
            uri.lastPathSegment
        }
    }
}
