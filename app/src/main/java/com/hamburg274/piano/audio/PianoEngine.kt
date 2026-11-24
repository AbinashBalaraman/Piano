package com.hamburg274.piano.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Process
import com.hamburg274.piano.music.MidiCodec
import com.hamburg274.piano.music.MidiEventType
import com.hamburg274.piano.music.PianoTuning
import com.hamburg274.piano.music.RecordedMidiEvent
import com.hamburg274.piano.music.TemperamentId
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tanh

enum class VoicingId(
    val idKey: String,
    val displayName: String,
    val shortName: String,
    val packTier: String,
    val character: String,
    val brightnessBias: Float,
    val hammerHardness: Float,
    val decayMultiplier: Float,
    val unisonDetuneCents: Float,
    val warmthBoost: Float,
    val isPlucked: Boolean = false
) {
    HAMBURG_274(
        idKey = "hamburg-274",
        displayName = "Hamburg 274 Concert Grand",
        shortName = "Hamburg 274",
        packTier = "Core",
        character = "Rounded cheeks, warm Sitka spruce low end, singing classical treble.",
        brightnessBias = 1.0f,
        hammerHardness = 0.62f,
        decayMultiplier = 1.15f,
        unisonDetuneCents = 1.15f,
        warmthBoost = 0.36f
    ),
    NEW_YORK_274(
        idKey = "new-york-274",
        displayName = "New York Studio 274",
        shortName = "NY Studio 274",
        packTier = "Expansion",
        character = "Crisp, assertive concert attack with sparkling projection for jazz & modern scores.",
        brightnessBias = 1.42f,
        hammerHardness = 0.85f,
        decayMultiplier = 1.02f,
        unisonDetuneCents = 0.90f,
        warmthBoost = 0.14f
    ),
    FELT_UPRIGHT(
        idKey = "felt-upright",
        displayName = "Intimate Felt Upright",
        shortName = "Felt Upright",
        packTier = "Expansion",
        character = "Velvet wool felt rail between hammers and strings for late-night chamber intimacy.",
        brightnessBias = 0.52f,
        hammerHardness = 0.24f,
        decayMultiplier = 0.78f,
        unisonDetuneCents = 1.85f,
        warmthBoost = 0.48f
    ),
    BAROQUE_HARPSICHORD(
        idKey = "baroque-harpsichord",
        displayName = "Baroque Harpsichord",
        shortName = "Harpsichord",
        packTier = "Expansion",
        character = "Double-manual plucked quill response tailored for Bach, Scarlatti, and Rameau.",
        brightnessBias = 1.75f,
        hammerHardness = 0.95f,
        decayMultiplier = 0.65f,
        unisonDetuneCents = 0.40f,
        warmthBoost = -0.15f,
        isPlucked = true
    ),
    VIENNESE_FORTEPIANO(
        idKey = "viennese-fortepiano",
        displayName = "1820s Viennese Fortepiano",
        shortName = "Fortepiano",
        packTier = "Expansion",
        character = "Transparent leather-hammered clarity with rapid Classical register separation.",
        brightnessBias = 1.18f,
        hammerHardness = 0.58f,
        decayMultiplier = 0.72f,
        unisonDetuneCents = 1.35f,
        warmthBoost = 0.08f
    );

    companion object {
        fun fromKey(key: String): VoicingId = entries.firstOrNull { it.idKey == key } ?: HAMBURG_274
    }
}

enum class HallIrId(
    val displayName: String,
    val rt60Label: String,
    val description: String,
    val feedback: Float
) {
    PHILHARMONIC(
        displayName = "Philharmonic Hall",
        rt60Label = "2.4 s",
        description = "Expansive shoebox concert hall with warm early wood reflections.",
        feedback = 0.84f
    ),
    RECITAL(
        displayName = "Chamber Recital Room",
        rt60Label = "1.4 s",
        description = "Focused conservatory recital acoustics ideal for Beethoven & Chopin.",
        feedback = 0.74f
    ),
    SALON(
        displayName = "Intimate Salon",
        rt60Label = "0.8 s",
        description = "Close mahogany drawing-room ambience for Mozart and Baroque clarity.",
        feedback = 0.62f
    ),
    DRY(
        displayName = "Dry Close Studio",
        rt60Label = "0.0 s",
        description = "Pure close-miked soundboard with hall reverb bypassed.",
        feedback = 0.0f
    )
}

enum class LidPosition(val label: String, val cutoffHz: Float) {
    FULL("Full", 16500f),
    HALF("Half", 7600f),
    CLOSED("Closed", 3300f)
}

enum class StereoPerspective(val label: String) {
    PIANIST("Pianist"),
    AUDIENCE("Audience")
}

data class AcousticSettings(
    val voicingId: VoicingId = VoicingId.HAMBURG_274,
    val temperament: TemperamentId = TemperamentId.RAILSBACK,
    val referenceA4Hz: Int = 440,
    val duplexMix: Float = 0.42f,
    val reverbMix: Float = 0.34f,
    val hallId: HallIrId = HallIrId.RECITAL,
    val lidPosition: LidPosition = LidPosition.FULL,
    val perspective: StereoPerspective = StereoPerspective.PIANIST,
    val mechanicalNoise: Float = 0.35f,
    val masterVolume: Float = 0.82f
)

data class AudioDiagnosticsSnapshot(
    val state: String = "Standby",
    val sampleRate: Int = 48000,
    val framesPerBurst: Int = 192,
    val estimatedLatencyMs: Float = 8.0f,
    val activeVoices: Int = 0,
    val underrunCount: Int = 0,
    val outputRouteLabel: String = "Device Speaker / Wired",
    val isLegacyBluetoothA2dp: Boolean = false,
    val damperCC64: Int = 0,
    val sostenutoCC66: Boolean = false,
    val unaCordaCC67: Boolean = false
)

private sealed interface EngineCommand {
    data class NoteOn(val midi: Int, val velocity: Int) : EngineCommand
    data class NoteOff(val midi: Int) : EngineCommand
    data class Damper(val cc64: Int) : EngineCommand
    data class Sostenuto(val engaged: Boolean) : EngineCommand
    data class UnaCorda(val engaged: Boolean) : EngineCommand
    data class UpdateSettings(val settings: AcousticSettings) : EngineCommand
    data object Panic : EngineCommand
}

private class SynthVoice(
    val midi: Int,
    val velocity: Int,
    val sampleRate: Int,
    val isSilentLift: Boolean,
    val isPlucked: Boolean,
    val panLeft: Float,
    val panRight: Float,
    val partialIncs: DoubleArray,
    val partialGains: FloatArray,
    val targetAmp: Float,
    val decayCoeff: Float,
    val filterAlphaStart: Float,
    val filterAlphaEnd: Float,
    val hammerLevel: Float
) {
    val phases = DoubleArray(partialIncs.size)
    var env = if (isSilentLift) 0f else 0.0001f
    var attackPhase = !isSilentLift
    var ageSamples = 0
    var isKeyHeld = true
    var isSostenutoHeld = false
    var isReleasing = false
    var releaseCoeff = 0.9992f
    var lpState = 0f
    var thumpEnv = 0f
    var thumpLp = 0f
    var active = true
    var sympatheticEnv = 0f
    var sympatheticPhase = 0.0

    fun renderSample(noiseSample: Float): Float {
        if (!active) return 0f
        ageSamples++

        if (isSilentLift) {
            if (sympatheticEnv > 0.0001f) {
                sympatheticPhase += partialIncs[0]
                if (sympatheticPhase > TWO_PI) sympatheticPhase -= TWO_PI
                val out = sin(sympatheticPhase).toFloat() * sympatheticEnv
                sympatheticEnv *= 0.99993f
                return out
            }
            return 0f
        }

        if (attackPhase) {
            val attackStep = if (isPlucked) targetAmp / (sampleRate * 0.002f) else targetAmp / (sampleRate * 0.0045f)
            env += attackStep
            if (env >= targetAmp) {
                env = targetAmp
                attackPhase = false
            }
        } else {
            env *= if (isReleasing) releaseCoeff else decayCoeff
            if (env < 0.00018f && thumpEnv < 0.0001f) {
                active = false
                return 0f
            }
        }

        // Additive inharmonic partial synthesis
        var sum = 0f
        for (i in partialIncs.indices) {
            var p = phases[i] + partialIncs[i]
            if (p >= TWO_PI) p -= TWO_PI
            phases[i] = p
            val wave = if (i == 0 && isPlucked) {
                // Plucked harpsichord sawtooth fundamental
                ((p / PI) - 1.0).toFloat()
            } else if (i == 0) {
                // Warm piano triangle-sine hybrid fundamental
                val s = sin(p).toFloat()
                s * 0.72f + (s * s * s) * 0.14f
            } else {
                sin(p).toFloat()
            }
            sum += wave * partialGains[i]
        }

        // Dynamic spectral darkening over note decay
        val tNorm = min(1f, ageSamples.toFloat() / (sampleRate * 2.0f))
        val alpha = filterAlphaStart + (filterAlphaEnd - filterAlphaStart) * tNorm
        lpState += (sum - lpState) * alpha

        var sample = lpState * env

        // Initial hammer felt / quill transient
        if (ageSamples < sampleRate * 0.032f && hammerLevel > 0.001f) {
            val hEnv = exp(-ageSamples / (sampleRate * 0.009f))
            sample += noiseSample * hammerLevel * hEnv
        }

        // Register-aware bass/treble mechanical damper release thump
        if (thumpEnv > 0.0002f) {
            thumpLp += (noiseSample - thumpLp) * (if (midi < 52) 0.05f else 0.16f)
            sample += thumpLp * thumpEnv
            thumpEnv *= 0.9965f
        }

        return sample
    }

    companion object {
        private const val TWO_PI = 2.0 * PI
    }
}

private class CombDelay(size: Int) {
    private val buf = FloatArray(max(16, size))
    private var idx = 0
    private var lp = 0f

    fun process(input: Float, feedback: Float, damp: Float): Float {
        val out = buf[idx]
        lp = out * (1f - damp) + lp * damp
        buf[idx] = input + lp * feedback
        idx++
        if (idx >= buf.size) idx = 0
        return out
    }

    fun clear() {
        buf.fill(0f)
        lp = 0f
    }
}

class HamburgPianoEngine(context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val hardwareSampleRate: Int
    private val framesPerBurst: Int

    private val commands = ConcurrentLinkedQueue<EngineCommand>()
    private val isRunning = AtomicBoolean(false)
    private val activeVoiceCount = AtomicInteger(0)
    private val underrunCounter = AtomicInteger(0)

    @Volatile
    private var currentSettings = AcousticSettings()
    @Volatile
    private var currentDamperCC64 = 0
    @Volatile
    private var currentSostenuto = false
    @Volatile
    private var currentUnaCorda = false
    @Volatile
    private var audioStateLabel = "Standby"

    private var audioThread: Thread? = null
    private var audioTrack: AudioTrack? = null

    init {
        val nativeSr = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)?.toIntOrNull()
        val nativeFrames = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER)?.toIntOrNull()
        hardwareSampleRate = if (nativeSr != null && nativeSr in 22050..96000) nativeSr else 48000
        framesPerBurst = if (nativeFrames != null && nativeFrames in 64..1024) nativeFrames else 192
    }

    fun start() {
        if (isRunning.getAndSet(true)) return

        val thread = Thread({
            Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
            runAudioLoop()
        }, "Hamburg274-AudioDSP")
        thread.isDaemon = true
        audioThread = thread
        thread.start()
    }

    fun stop() {
        isRunning.set(false)
        audioThread?.join(400)
        audioThread = null
        try {
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.release()
        } catch (_: Exception) {
        }
        audioTrack = null
        audioStateLabel = "Stopped"
    }

    fun noteOn(midi: Int, velocity: Int) {
        if (!isRunning.get()) start()
        if (velocity <= 0) {
            commands.add(EngineCommand.NoteOff(midi))
        } else {
            commands.add(EngineCommand.NoteOn(midi.coerceIn(21, 108), velocity.coerceIn(1, 127)))
        }
    }

    fun noteOff(midi: Int) {
        commands.add(EngineCommand.NoteOff(midi.coerceIn(21, 108)))
    }

    fun setDamperPedal(cc64: Int) {
        val clamped = cc64.coerceIn(0, 127)
        currentDamperCC64 = clamped
        commands.add(EngineCommand.Damper(clamped))
    }

    fun setSostenutoPedal(engaged: Boolean) {
        currentSostenuto = engaged
        commands.add(EngineCommand.Sostenuto(engaged))
    }

    fun setUnaCordaPedal(engaged: Boolean) {
        currentUnaCorda = engaged
        commands.add(EngineCommand.UnaCorda(engaged))
    }

    fun updateSettings(settings: AcousticSettings) {
        currentSettings = settings
        commands.add(EngineCommand.UpdateSettings(settings))
    }

    fun panic() {
        currentDamperCC64 = 0
        currentSostenuto = false
        currentUnaCorda = false
        commands.add(EngineCommand.Panic)
    }

    fun getDiagnostics(): AudioDiagnosticsSnapshot {
        val outputs = try {
            audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        } catch (_: Exception) {
            emptyArray()
        }

        var isA2dp = false
        var routeLabel = "Built-in Speaker"
        for (dev in outputs) {
            when (dev.type) {
                AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
                AudioDeviceInfo.TYPE_WIRED_HEADSET -> {
                    routeLabel = "Wired Headphones (Low Latency)"
                    isA2dp = false
                    break
                }
                AudioDeviceInfo.TYPE_USB_DEVICE,
                AudioDeviceInfo.TYPE_USB_HEADSET -> {
                    routeLabel = "USB-C DAC / Audio"
                    isA2dp = false
                    break
                }
                AudioDeviceInfo.TYPE_BLE_HEADSET,
                AudioDeviceInfo.TYPE_BLE_SPEAKER -> {
                    routeLabel = "Bluetooth LE Audio (LC3)"
                    isA2dp = false
                    break
                }
                AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> {
                    routeLabel = "Bluetooth A2DP (High Lag)"
                    isA2dp = true
                }
            }
        }

        val estLatency = (framesPerBurst.toFloat() * 2f / hardwareSampleRate.toFloat()) * 1000f
        return AudioDiagnosticsSnapshot(
            state = audioStateLabel,
            sampleRate = hardwareSampleRate,
            framesPerBurst = framesPerBurst,
            estimatedLatencyMs = (estLatency * 10f).toInt() / 10f,
            activeVoices = activeVoiceCount.get(),
            underrunCount = underrunCounter.get(),
            outputRouteLabel = routeLabel,
            isLegacyBluetoothA2dp = isA2dp,
            damperCC64 = currentDamperCC64,
            sostenutoCC66 = currentSostenuto,
            unaCordaCC67 = currentUnaCorda
        )
    }

    private fun runAudioLoop() {
        val sr = hardwareSampleRate
        val burstFrames = framesPerBurst.coerceIn(128, 512)
        val minBufBytes = AudioTrack.getMinBufferSize(
            sr,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_FLOAT
        ).coerceAtLeast(burstFrames * 2 * 4 * 2)

        val track = try {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                        .setSampleRate(sr)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                        .build()
                )
                .setTransferMode(AudioTrack.MODE_STREAM)
                .setBufferSizeInBytes(minBufBytes)
                .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
                .build()
        } catch (_: Exception) {
            audioStateLabel = "Audio Init Error"
            isRunning.set(false)
            return
        }

        audioTrack = track
        track.play()
        audioStateLabel = "Running (Low-Latency)"

        val outBuffer = FloatArray(burstFrames * 2)
        val voices = HashMap<Int, SynthVoice>(48)
        var settings = currentSettings
        var damperCC64 = currentDamperCC64
        var sostenuto = currentSostenuto
        var unaCorda = currentUnaCorda

        // Duplex shimmer comb & stereo Schroeder hall reverb banks
        val duplexComb = CombDelay((sr * 0.019).toInt())
        val revL1 = CombDelay((sr * 0.0297).toInt())
        val revL2 = CombDelay((sr * 0.0371).toInt())
        val revL3 = CombDelay((sr * 0.0411).toInt())
        val revR1 = CombDelay((sr * 0.0313).toInt())
        val revR2 = CombDelay((sr * 0.0389).toInt())
        val revR3 = CombDelay((sr * 0.0437).toInt())

        var lidLpL = 0f
        var lidLpR = 0f
        var warmthLpL = 0f
        var warmthLpR = 0f
        var prng = 48151623

        while (isRunning.get()) {
            // Drain lock-free command queue
            while (true) {
                val cmd = commands.poll() ?: break
                when (cmd) {
                    is EngineCommand.UpdateSettings -> settings = cmd.settings
                    is EngineCommand.UnaCorda -> unaCorda = cmd.engaged
                    is EngineCommand.Sostenuto -> {
                        val was = sostenuto
                        sostenuto = cmd.engaged
                        if (sostenuto && !was) {
                            voices.values.forEach { if (it.isKeyHeld) it.isSostenutoHeld = true }
                        } else if (!sostenuto && was) {
                            voices.values.forEach { v ->
                                if (v.isSostenutoHeld) {
                                    v.isSostenutoHeld = false
                                    if (!v.isKeyHeld && damperCC64 < 96 && v.midi < 90) {
                                        applyDamperRelease(v, damperCC64 / 127f, sr, settings)
                                    }
                                }
                            }
                        }
                    }
                    is EngineCommand.Damper -> {
                        val prev = damperCC64
                        damperCC64 = cmd.cc64
                        val frac = damperCC64 / 127f
                        // Re-pedaling catch
                        if (damperCC64 > prev && damperCC64 >= 72) {
                            voices.values.forEach { v ->
                                if (v.isReleasing && !v.isKeyHeld) {
                                    v.isReleasing = false
                                }
                            }
                        }
                        if (damperCC64 < 96 && damperCC64 < prev) {
                            voices.values.forEach { v ->
                                if (!v.isKeyHeld && !v.isSostenutoHeld && v.midi < 90 && !v.isSilentLift) {
                                    applyDamperRelease(v, frac, sr, settings)
                                }
                            }
                        }
                    }
                    is EngineCommand.NoteOn -> {
                        val v = createVoice(cmd.midi, cmd.velocity, sr, settings, unaCorda)
                        // Sympathetic excitation of silently held dampers
                        if (!v.isSilentLift) {
                            val velNorm = cmd.velocity / 127f
                            voices.values.forEach { existing ->
                                if (existing.isSilentLift && existing.midi != cmd.midi) {
                                    val interval = kotlin.math.abs(existing.midi - cmd.midi) % 12
                                    if (interval == 0 || interval == 7) {
                                        existing.sympatheticEnv = max(existing.sympatheticEnv, velNorm * 0.045f)
                                    }
                                }
                            }
                        }
                        if (voices.size >= 48) {
                            val oldest = voices.values.maxByOrNull { it.ageSamples }
                            if (oldest != null) voices.remove(oldest.midi)
                        }
                        voices[cmd.midi] = v
                    }
                    is EngineCommand.NoteOff -> {
                        val v = voices[cmd.midi]
                        if (v != null) {
                            v.isKeyHeld = false
                            if (v.isSilentLift) {
                                voices.remove(cmd.midi)
                            } else if (!v.isSostenutoHeld && v.midi < 90 && damperCC64 < 96) {
                                applyDamperRelease(v, damperCC64 / 127f, sr, settings)
                            }
                        }
                    }
                    EngineCommand.Panic -> {
                        voices.clear()
                        damperCC64 = 0
                        sostenuto = false
                        unaCorda = false
                        duplexComb.clear()
                        revL1.clear(); revL2.clear(); revL3.clear()
                        revR1.clear(); revR2.clear(); revR3.clear()
                    }
                }
            }

            val profile = settings.voicingId
            val lidAlpha = (2.0 * PI * settings.lidPosition.cutoffHz / sr).toFloat().coerceIn(0.05f, 0.99f)
            val warmthAlpha = (2.0 * PI * 220.0 / sr).toFloat().coerceIn(0.005f, 0.25f)
            val isDry = settings.hallId == HallIrId.DRY
            val hallFb = settings.hallId.feedback
            val wetAmt = if (isDry) 0f else (settings.reverbMix * if (settings.perspective == StereoPerspective.AUDIENCE) 1.12f else 0.85f).coerceIn(0f, 0.95f)
            val dryAmt = 1f - wetAmt * 0.25f
            val duplexGain = settings.duplexMix * 0.20f * (0.65f + (damperCC64 / 127f) * 0.75f)
            val master = settings.masterVolume

            var i = 0
            while (i < burstFrames) {
                prng = prng * 1664525 + 1013904223
                val noise = ((prng ushr 8).toFloat() / 8388608f) - 1f

                var sumL = 0f
                var sumR = 0f

                val iter = voices.values.iterator()
                while (iter.hasNext()) {
                    val v = iter.next()
                    val s = v.renderSample(noise)
                    if (!v.active) {
                        iter.remove()
                        continue
                    }
                    sumL += s * v.panLeft
                    sumR += s * v.panRight
                }

                // Duplex shimmer resonator bus
                val mono = (sumL + sumR) * 0.5f
                val dup = duplexComb.process(mono * duplexGain, 0.36f, 0.42f)
                sumL += dup
                sumR += dup

                // Sitka spruce soundboard warmth low-shelf
                warmthLpL += (sumL - warmthLpL) * warmthAlpha
                warmthLpR += (sumR - warmthLpR) * warmthAlpha
                sumL += warmthLpL * profile.warmthBoost
                sumR += warmthLpR * profile.warmthBoost

                // Concert lid acoustic ceiling
                lidLpL += (sumL - lidLpL) * lidAlpha
                lidLpR += (sumR - lidLpR) * lidAlpha

                // Stereo Hall Reverb
                var outL = lidLpL * dryAmt
                var outR = lidLpR * dryAmt
                if (wetAmt > 0.005f) {
                    val wL = (revL1.process(lidLpL, hallFb, 0.34f) +
                        revL2.process(lidLpL, hallFb * 0.96f, 0.38f) +
                        revL3.process(lidLpL, hallFb * 0.92f, 0.42f)) * 0.32f
                    val wR = (revR1.process(lidLpR, hallFb, 0.34f) +
                        revR2.process(lidLpR, hallFb * 0.96f, 0.38f) +
                        revR3.process(lidLpR, hallFb * 0.92f, 0.42f)) * 0.32f
                    outL += wL * wetAmt
                    outR += wR * wetAmt
                }

                // Smooth concert limiter preventing fortissimo clipping
                outBuffer[i * 2] = tanh((outL * master * 1.15f).toDouble()).toFloat()
                outBuffer[i * 2 + 1] = tanh((outR * master * 1.15f).toDouble()).toFloat()
                i++
            }

            activeVoiceCount.set(voices.size)
            track.write(outBuffer, 0, outBuffer.size, AudioTrack.WRITE_BLOCKING)
            try {
                underrunCounter.set(track.underrunCount)
            } catch (_: Exception) {
            }
        }
    }

    private fun createVoice(
        midi: Int,
        velocity: Int,
        sr: Int,
        settings: AcousticSettings,
        unaCorda: Boolean
    ): SynthVoice {
        val profile = settings.voicingId
        val freq = PianoTuning.midiToFrequency(midi, settings.temperament, settings.referenceA4Hz)
        val inharmB = PianoTuning.getStringInharmonicity(midi)
        val velNorm = velocity / 127f
        val keyPos = (midi - 21f) / 87f

        val panVal = if (settings.perspective == StereoPerspective.PIANIST) {
            (keyPos * 2f - 1f) * 0.62f
        } else {
            (1f - keyPos * 2f) * 0.32f
        }
        val panAngle = (panVal + 1f) * 0.25f * PI.toFloat()
        val panL = cos(panAngle)
        val panR = sin(panAngle)

        val detuneRatio = 2.0.pow((if (unaCorda) profile.unisonDetuneCents * 0.4 else profile.unisonDetuneCents.toDouble()) / 1200.0)
        val twoPiOverSr = (2.0 * PI) / sr

        val freqs = if (profile.isPlucked) {
            doubleArrayOf(
                freq * sqrt(1.0 + inharmB),
                2.0 * freq * detuneRatio * sqrt(1.0 + inharmB * 4.0),
                3.0 * freq * sqrt(1.0 + inharmB * 9.0)
            )
        } else {
            doubleArrayOf(
                freq * sqrt(1.0 + inharmB),
                freq * detuneRatio * sqrt(1.0 + inharmB),
                2.0 * freq * sqrt(1.0 + inharmB * 4.0),
                3.0 * freq * sqrt(1.0 + inharmB * 9.0),
                4.0 * freq * sqrt(1.0 + inharmB * 16.0)
            )
        }

        val gains = if (profile.isPlucked) {
            floatArrayOf(0.50f, 0.32f, 0.16f)
        } else {
            floatArrayOf(
                0.56f,
                if (unaCorda) 0.22f else 0.42f,
                0.24f * (0.6f + velNorm * 0.55f),
                0.12f * (0.35f + velNorm * 0.85f),
                0.06f * velNorm.pow(1.3f)
            )
        }

        val incs = DoubleArray(freqs.size) { idx ->
            val f = freqs[idx]
            if (f > sr * 0.45) {
                gains[idx] = 0f
                0.0
            } else {
                f * twoPiOverSr
            }
        }

        val amp = (0.055f + velNorm.pow(1.45f) * 0.30f) *
            (if (unaCorda) 0.84f else 1f) *
            (when {
                midi < 40 -> 1.15f
                midi > 84 -> 0.82f
                else -> 1f
            })

        val naturalDecaySec = max(0.95f, (8.8f - keyPos * 7.0f) * profile.decayMultiplier)
        val decayCoeff = exp(-6.2 / (naturalDecaySec * sr)).toFloat()

        val unaCut = if (unaCorda) 0.65f else 1f
        val peakHz = ((420f + velNorm.pow(1.55f) * 8600f) * profile.brightnessBias * unaCut).coerceIn(240f, sr * 0.42f)
        val endHz = (peakHz * if (profile.isPlucked) 0.55f else 0.24f).coerceAtLeast(220f)
        val alphaStart = (2.0 * PI * peakHz / sr).toFloat().coerceIn(0.03f, 0.98f)
        val alphaEnd = (2.0 * PI * endHz / sr).toFloat().coerceIn(0.02f, 0.90f)

        val hammerLevel = velNorm * profile.hammerHardness * (0.06f + settings.mechanicalNoise * 0.14f) * (if (unaCorda) 0.5f else 1f)

        return SynthVoice(
            midi = midi,
            velocity = velocity,
            sampleRate = sr,
            isSilentLift = velocity <= 8,
            isPlucked = profile.isPlucked,
            panLeft = panL,
            panRight = panR,
            partialIncs = incs,
            partialGains = gains,
            targetAmp = amp,
            decayCoeff = decayCoeff,
            filterAlphaStart = alphaStart,
            filterAlphaEnd = alphaEnd,
            hammerLevel = hammerLevel
        )
    }

    private fun applyDamperRelease(v: SynthVoice, pedalFraction: Float, sr: Int, settings: AcousticSettings) {
        v.isReleasing = true
        val regFactor = when {
            v.midi < 48 -> 1.35f
            v.midi > 72 -> 0.75f
            else -> 1f
        }
        val releaseSec = (0.09f + pedalFraction.pow(2.2f) * 2.4f) * regFactor
        v.releaseCoeff = exp(-6.5 / (releaseSec * sr)).toFloat()
        if (pedalFraction < 0.25f && settings.mechanicalNoise > 0.05f && v.ageSamples > sr * 0.03f) {
            v.thumpEnv = settings.mechanicalNoise * (if (v.midi < 52) 0.065f else 0.022f) * min(1f, v.velocity / 100f)
        }
    }

    /**
     * Offline 48 kHz / 24-bit WAV renderer for non-destructive Re-Voice export.
     */
    fun renderTakeToWav24(events: List<RecordedMidiEvent>, voicingOverride: VoicingId? = null): ByteArray {
        val sr = 48000
        val renderSettings = currentSettings.copy(voicingId = voicingOverride ?: currentSettings.voicingId)
        val maxTime = events.maxOfOrNull { it.timeSec } ?: 1.0
        val totalSec = min(120.0, max(2.0, maxTime + 2.8))
        val totalFrames = (totalSec * sr).toInt()

        val left = FloatArray(totalFrames)
        val right = FloatArray(totalFrames)
        val sorted = events.sortedBy { it.timeSec }

        val voices = HashMap<Int, SynthVoice>(32)
        val revL = CombDelay((sr * 0.031).toInt())
        val revR = CombDelay((sr * 0.037).toInt())

        var evIdx = 0
        var damper = 0
        var sostenuto = false
        var unaCorda = false
        var prng = 48151623
        val wet = if (renderSettings.hallId == HallIrId.DRY) 0f else renderSettings.reverbMix
        val fb = renderSettings.hallId.feedback

        for (frame in 0 until totalFrames) {
            val tSec = frame.toDouble() / sr
            while (evIdx < sorted.size && sorted[evIdx].timeSec <= tSec) {
                val ev = sorted[evIdx++]
                when (ev.type) {
                    MidiEventType.NOTE_ON -> {
                        val m = ev.midi ?: continue
                        val vel = ev.velocity ?: 80
                        if (vel > 0) voices[m] = createVoice(m, vel, sr, renderSettings, unaCorda)
                    }
                    MidiEventType.NOTE_OFF -> {
                        val m = ev.midi ?: continue
                        voices[m]?.let { v ->
                            v.isKeyHeld = false
                            if (!v.isSostenutoHeld && v.midi < 90 && damper < 96) {
                                applyDamperRelease(v, damper / 127f, sr, renderSettings)
                            }
                        }
                    }
                    MidiEventType.CC -> {
                        when (ev.cc) {
                            64 -> {
                                val prev = damper
                                damper = (ev.value ?: 0).coerceIn(0, 127)
                                if (damper < 96 && damper < prev) {
                                    voices.values.forEach { v ->
                                        if (!v.isKeyHeld && !v.isSostenutoHeld && v.midi < 90) {
                                            applyDamperRelease(v, damper / 127f, sr, renderSettings)
                                        }
                                    }
                                }
                            }
                            66 -> {
                                val eng = (ev.value ?: 0) >= 64
                                if (eng && !sostenuto) voices.values.forEach { if (it.isKeyHeld) it.isSostenutoHeld = true }
                                sostenuto = eng
                            }
                            67 -> unaCorda = (ev.value ?: 0) >= 64
                        }
                    }
                }
            }

            prng = prng * 1664525 + 1013904223
            val noise = ((prng ushr 8).toFloat() / 8388608f) - 1f
            var sL = 0f
            var sR = 0f
            val iter = voices.values.iterator()
            while (iter.hasNext()) {
                val v = iter.next()
                val s = v.renderSample(noise)
                if (!v.active) {
                    iter.remove()
                    continue
                }
                sL += s * v.panLeft
                sR += s * v.panRight
            }

            if (wet > 0.01f) {
                sL += revL.process(sL, fb, 0.36f) * wet * 0.55f
                sR += revR.process(sR, fb, 0.36f) * wet * 0.55f
            }

            left[frame] = tanh((sL * renderSettings.masterVolume * 1.15f).toDouble()).toFloat()
            right[frame] = tanh((sR * renderSettings.masterVolume * 1.15f).toDouble()).toFloat()
        }

        return MidiCodec.encodeWav24Bit(left, right, sr)
    }
}
