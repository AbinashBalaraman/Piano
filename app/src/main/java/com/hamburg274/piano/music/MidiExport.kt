package com.hamburg274.piano.music

import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

enum class MidiEventType { NOTE_ON, NOTE_OFF, CC }

data class RecordedMidiEvent(
    val timeSec: Double,
    val type: MidiEventType,
    val midi: Int? = null,
    val velocity: Int? = null,
    val cc: Int? = null,
    val value: Int? = null
)

data class StudioTake(
    val id: String,
    val name: String,
    val createdAt: Long,
    val durationSec: Double,
    val voicingId: String,
    val events: List<RecordedMidiEvent>
)

data class ParsedScoreResult(
    val title: String,
    val notes: List<ExpectedScoreNote>,
    val bpm: Int,
    val durationSec: Double
)

object MidiCodec {

    val DEMO_STUDIO_TAKE = StudioTake(
        id = "demo-chopin-sketch",
        name = "Recital Sketch — Nocturne Cadence (Demo)",
        createdAt = System.currentTimeMillis() - 120_000L,
        durationSec = 4.8,
        voicingId = "hamburg-274",
        events = listOf(
            RecordedMidiEvent(0.0, MidiEventType.CC, cc = 64, value = 108),
            RecordedMidiEvent(0.05, MidiEventType.NOTE_ON, midi = 48, velocity = 56),
            RecordedMidiEvent(0.08, MidiEventType.NOTE_ON, midi = 60, velocity = 64),
            RecordedMidiEvent(0.12, MidiEventType.NOTE_ON, midi = 64, velocity = 72),
            RecordedMidiEvent(0.65, MidiEventType.NOTE_OFF, midi = 48, velocity = 64),
            RecordedMidiEvent(0.65, MidiEventType.NOTE_OFF, midi = 60, velocity = 64),
            RecordedMidiEvent(0.90, MidiEventType.NOTE_ON, midi = 67, velocity = 78),
            RecordedMidiEvent(1.50, MidiEventType.NOTE_OFF, midi = 64, velocity = 64),
            RecordedMidiEvent(1.55, MidiEventType.NOTE_OFF, midi = 67, velocity = 64),
            RecordedMidiEvent(1.75, MidiEventType.CC, cc = 64, value = 18),
            RecordedMidiEvent(1.85, MidiEventType.CC, cc = 64, value = 112),
            RecordedMidiEvent(1.90, MidiEventType.NOTE_ON, midi = 53, velocity = 58),
            RecordedMidiEvent(1.95, MidiEventType.NOTE_ON, midi = 65, velocity = 70),
            RecordedMidiEvent(2.00, MidiEventType.NOTE_ON, midi = 69, velocity = 82),
            RecordedMidiEvent(2.80, MidiEventType.NOTE_OFF, midi = 53, velocity = 64),
            RecordedMidiEvent(2.80, MidiEventType.NOTE_OFF, midi = 65, velocity = 64),
            RecordedMidiEvent(2.95, MidiEventType.NOTE_ON, midi = 72, velocity = 86),
            RecordedMidiEvent(3.45, MidiEventType.NOTE_OFF, midi = 69, velocity = 64),
            RecordedMidiEvent(3.65, MidiEventType.NOTE_OFF, midi = 72, velocity = 64),
            RecordedMidiEvent(3.75, MidiEventType.CC, cc = 64, value = 0),
            RecordedMidiEvent(3.85, MidiEventType.CC, cc = 64, value = 96),
            RecordedMidiEvent(3.90, MidiEventType.NOTE_ON, midi = 48, velocity = 52),
            RecordedMidiEvent(3.92, MidiEventType.NOTE_ON, midi = 64, velocity = 64),
            RecordedMidiEvent(3.95, MidiEventType.NOTE_ON, midi = 67, velocity = 66),
            RecordedMidiEvent(4.50, MidiEventType.NOTE_OFF, midi = 48, velocity = 64),
            RecordedMidiEvent(4.50, MidiEventType.NOTE_OFF, midi = 64, velocity = 64),
            RecordedMidiEvent(4.50, MidiEventType.NOTE_OFF, midi = 67, velocity = 64),
            RecordedMidiEvent(4.75, MidiEventType.CC, cc = 64, value = 0)
        )
    )

    private fun writeVarLen(out: ByteArrayOutputStream, value: Int) {
        var v = max(0, value)
        val stack = IntArray(5)
        var count = 0
        stack[count++] = v and 0x7F
        v = v ushr 7
        while (v > 0) {
            stack[count++] = (v and 0x7F) or 0x80
            v = v ushr 7
        }
        for (i in count - 1 downTo 0) {
            out.write(stack[i])
        }
    }

    private fun writeUint32BE(out: ByteArrayOutputStream, v: Int) {
        out.write((v ushr 24) and 0xFF)
        out.write((v ushr 16) and 0xFF)
        out.write((v ushr 8) and 0xFF)
        out.write(v and 0xFF)
    }

    private fun writeUint16BE(out: ByteArrayOutputStream, v: Int) {
        out.write((v ushr 8) and 0xFF)
        out.write(v and 0xFF)
    }

    /**
     * Encodes a studio take into Standard MIDI File (SMF Format 0) preserving
     * true finger Note-On/Off separately from continuous CC64/CC66/CC67 pedal lanes.
     */
    fun encodeStandardMidiFile(
        events: List<RecordedMidiEvent>,
        bpm: Int = 120,
        ticksPerQuarter: Int = 480
    ): ByteArray {
        val sorted = events.sortedBy { it.timeSec }
        val track = ByteArrayOutputStream()

        val mpq = (60_000_000.0 / bpm.coerceIn(20, 300)).roundToInt()
        track.write(0x00); track.write(0xFF); track.write(0x51); track.write(0x03)
        track.write((mpq ushr 16) and 0xFF)
        track.write((mpq ushr 8) and 0xFF)
        track.write(mpq and 0xFF)

        // Program Change: Acoustic Grand Piano
        track.write(0x00); track.write(0xC0); track.write(0x00)

        val ticksPerSec = (bpm / 60.0) * ticksPerQuarter
        var lastTick = 0

        for (ev in sorted) {
            val tick = max(lastTick, (ev.timeSec * ticksPerSec).roundToInt())
            val delta = tick - lastTick
            lastTick = tick

            when (ev.type) {
                MidiEventType.NOTE_ON -> {
                    val m = ev.midi ?: continue
                    val vel = (ev.velocity ?: 80).coerceIn(1, 127)
                    writeVarLen(track, delta)
                    track.write(0x90); track.write(m and 0x7F); track.write(vel)
                }
                MidiEventType.NOTE_OFF -> {
                    val m = ev.midi ?: continue
                    val vel = (ev.velocity ?: 64).coerceIn(0, 127)
                    writeVarLen(track, delta)
                    track.write(0x80); track.write(m and 0x7F); track.write(vel)
                }
                MidiEventType.CC -> {
                    val cc = ev.cc ?: continue
                    val v = (ev.value ?: 0).coerceIn(0, 127)
                    writeVarLen(track, delta)
                    track.write(0xB0); track.write(cc and 0x7F); track.write(v)
                }
            }
        }

        // End of Track
        track.write(0x00); track.write(0xFF); track.write(0x2F); track.write(0x00)
        val trackBytes = track.toByteArray()

        val out = ByteArrayOutputStream()
        out.write(byteArrayOf(0x4D, 0x54, 0x68, 0x64)) // MThd
        writeUint32BE(out, 6)
        writeUint16BE(out, 0)
        writeUint16BE(out, 1)
        writeUint16BE(out, ticksPerQuarter)
        out.write(byteArrayOf(0x4D, 0x54, 0x72, 0x6B)) // MTrk
        writeUint32BE(out, trackBytes.size)
        out.write(trackBytes)
        return out.toByteArray()
    }

    fun parseStandardMidiFile(data: ByteArray, defaultTitle: String = "Imported MIDI"): ParsedScoreResult {
        if (data.size < 14 ||
            data[0].toInt() != 0x4D || data[1].toInt() != 0x54 ||
            data[2].toInt() != 0x68 || data[3].toInt() != 0x64
        ) {
            throw IllegalArgumentException("Invalid Standard MIDI File header.")
        }

        val numTracks = ((data[10].toInt() and 0xFF) shl 8) or (data[11].toInt() and 0xFF)
        val division = ((data[12].toInt() and 0xFF) shl 8) or (data[13].toInt() and 0xFF)
        val ticksPerQuarter = if (division > 0) division else 480

        var offset = 14
        var bpm = 120
        data class RawEv(val tick: Int, val isOn: Boolean, val midi: Int, val vel: Int)
        val rawEvents = mutableListOf<RawEv>()

        for (t in 0 until numTracks) {
            if (offset + 8 > data.size) break
            if (data[offset].toInt() != 0x4D || data[offset + 1].toInt() != 0x54 ||
                data[offset + 2].toInt() != 0x72 || data[offset + 3].toInt() != 0x6B
            ) break

            val trackLen = ((data[offset + 4].toInt() and 0xFF) shl 24) or
                ((data[offset + 5].toInt() and 0xFF) shl 16) or
                ((data[offset + 6].toInt() and 0xFF) shl 8) or
                (data[offset + 7].toInt() and 0xFF)
            offset += 8
            val trackEnd = min(data.size, offset + trackLen)

            var tick = 0
            var runningStatus = 0

            while (offset < trackEnd) {
                var delta = 0
                while (offset < trackEnd) {
                    val b = data[offset++].toInt() and 0xFF
                    delta = (delta shl 7) or (b and 0x7F)
                    if ((b and 0x80) == 0) break
                }
                tick += delta
                if (offset >= trackEnd) break

                var status = data[offset].toInt() and 0xFF
                if ((status and 0x80) != 0) {
                    offset++
                    if (status < 0xF0) runningStatus = status
                } else {
                    status = runningStatus
                }

                when (status) {
                    0xFF -> {
                        if (offset >= trackEnd) break
                        val metaType = data[offset++].toInt() and 0xFF
                        var len = 0
                        while (offset < trackEnd) {
                            val b = data[offset++].toInt() and 0xFF
                            len = (len shl 7) or (b and 0x7F)
                            if ((b and 0x80) == 0) break
                        }
                        if (metaType == 0x51 && len == 3 && offset + 3 <= trackEnd) {
                            val mpq = ((data[offset].toInt() and 0xFF) shl 16) or
                                ((data[offset + 1].toInt() and 0xFF) shl 8) or
                                (data[offset + 2].toInt() and 0xFF)
                            if (mpq > 0) bpm = (60_000_000.0 / mpq).roundToInt()
                        }
                        offset += len
                    }
                    0xF0, 0xF7 -> {
                        var len = 0
                        while (offset < trackEnd) {
                            val b = data[offset++].toInt() and 0xFF
                            len = (len shl 7) or (b and 0x7F)
                            if ((b and 0x80) == 0) break
                        }
                        offset += len
                    }
                    else -> {
                        val high = status and 0xF0
                        if (high == 0x90 && offset + 2 <= trackEnd) {
                            val m = data[offset++].toInt() and 0x7F
                            val v = data[offset++].toInt() and 0x7F
                            rawEvents.add(RawEv(tick, v > 0, m, v))
                        } else if (high == 0x80 && offset + 2 <= trackEnd) {
                            val m = data[offset++].toInt() and 0x7F
                            val v = data[offset++].toInt() and 0x7F
                            rawEvents.add(RawEv(tick, false, m, v))
                        } else if (high == 0xC0 || high == 0xD0) {
                            offset += 1
                        } else {
                            offset += 2
                        }
                    }
                }
            }
            offset = trackEnd
        }

        rawEvents.sortBy { it.tick }
        val secPerTick = 60.0 / (bpm * ticksPerQuarter)
        val active = mutableMapOf<Int, Pair<Int, Int>>()
        val notes = mutableListOf<ExpectedScoreNote>()
        var seq = 0

        for (ev in rawEvents) {
            if (ev.isOn) {
                active[ev.midi] = ev.tick to ev.vel
            } else {
                val start = active.remove(ev.midi)
                if (start != null) {
                    val startSec = start.first * secPerTick
                    val durSec = max(0.08, (ev.tick - start.first) * secPerTick)
                    val measure = (start.first / (ticksPerQuarter * 4)) + 1
                    notes.add(
                        ExpectedScoreNote(
                            id = "imp-${seq++}",
                            midi = ev.midi,
                            startSec = startSec,
                            durationSec = durSec,
                            targetVelocity = start.second,
                            measure = measure,
                            hand = if (ev.midi >= 60) HandPart.RH else HandPart.LH
                        )
                    )
                }
            }
        }

        notes.sortWith(compareBy<ExpectedScoreNote> { it.startSec }.thenBy { it.midi })
        val durationSec = notes.fold(0.0) { acc, n -> max(acc, n.startSec + n.durationSec) }
        return ParsedScoreResult(defaultTitle, notes, bpm, durationSec)
    }

    fun parseMusicXml(xmlText: String): ParsedScoreResult {
        val title = Regex("<work-title>([^<]+)</work-title>", RegexOption.IGNORE_CASE).find(xmlText)?.groupValues?.get(1)?.trim()
            ?: Regex("<movement-title>([^<]+)</movement-title>", RegexOption.IGNORE_CASE).find(xmlText)?.groupValues?.get(1)?.trim()
            ?: "Imported Urtext Score"

        val bpm = Regex("tempo=\"(\\d+(?:\\.\\d+)?)\"", RegexOption.IGNORE_CASE).find(xmlText)?.groupValues?.get(1)?.toDoubleOrNull()?.roundToInt()
            ?: Regex("<per-minute>(\\d+(?:\\.\\d+)?)</per-minute>", RegexOption.IGNORE_CASE).find(xmlText)?.groupValues?.get(1)?.toDoubleOrNull()?.roundToInt()
            ?: 96

        val divisions = Regex("<divisions>(\\d+)</divisions>", RegexOption.IGNORE_CASE).find(xmlText)?.groupValues?.get(1)?.toIntOrNull()?.coerceAtLeast(1) ?: 4
        val secPerDiv = 60.0 / (bpm * divisions)

        val notes = mutableListOf<ExpectedScoreNote>()
        val measureRegex = Regex("<measure\\b[^>]*>([\\s\\S]*?)</measure>", RegexOption.IGNORE_CASE)
        val tokenRegex = Regex("<(note|backup|forward)\\b[^>]*>([\\s\\S]*?)</\\1>", RegexOption.IGNORE_CASE)

        var measureNum = 0
        var cursorDiv = 0
        var lastNoteStartDiv = 0
        var seq = 0

        for (mMatch in measureRegex.findAll(xmlText)) {
            measureNum += 1
            val measureBody = mMatch.groupValues[1]
            for (tMatch in tokenRegex.findAll(measureBody)) {
                val tag = tMatch.groupValues[1].lowercase()
                val body = tMatch.groupValues[2]
                val durDiv = Regex("<duration>(\\d+)</duration>", RegexOption.IGNORE_CASE).find(body)?.groupValues?.get(1)?.toIntOrNull() ?: divisions

                when (tag) {
                    "backup" -> { cursorDiv = max(0, cursorDiv - durDiv); continue }
                    "forward" -> { cursorDiv += durDiv; continue }
                }

                val isChord = Regex("<chord\\s*/>", RegexOption.IGNORE_CASE).containsMatchIn(body)
                val isRest = Regex("<rest\\b", RegexOption.IGNORE_CASE).containsMatchIn(body)
                val startDiv = if (isChord) lastNoteStartDiv else cursorDiv

                if (!isRest) {
                    val step = Regex("<step>([A-G])</step>", RegexOption.IGNORE_CASE).find(body)?.groupValues?.get(1)
                    val oct = Regex("<octave>(\\d)</octave>", RegexOption.IGNORE_CASE).find(body)?.groupValues?.get(1)
                    val alter = Regex("<alter>(-?\\d)</alter>", RegexOption.IGNORE_CASE).find(body)?.groupValues?.get(1)?.toIntOrNull() ?: 0
                    val staff = Regex("<staff>(\\d)</staff>", RegexOption.IGNORE_CASE).find(body)?.groupValues?.get(1)?.toIntOrNull()
                    if (step != null && oct != null) {
                        val acc = when {
                            alter > 0 -> "#"
                            alter < 0 -> "b"
                            else -> ""
                        }
                        val midi = PianoTuning.noteNameToMidi("$step$acc$oct")
                        val resolvedStaff = staff ?: if (midi >= 60) 1 else 2
                        notes.add(
                            ExpectedScoreNote(
                                id = "mxml-${seq++}",
                                midi = midi,
                                startSec = startDiv * secPerDiv,
                                durationSec = max(0.1, durDiv * secPerDiv),
                                targetVelocity = if (resolvedStaff == 1) 76 else 62,
                                measure = measureNum,
                                hand = if (resolvedStaff == 1) HandPart.RH else HandPart.LH
                            )
                        )
                    }
                }

                if (!isChord) {
                    lastNoteStartDiv = cursorDiv
                    cursorDiv += durDiv
                }
            }
        }

        notes.sortWith(compareBy<ExpectedScoreNote> { it.startSec }.thenBy { it.midi })
        val durationSec = notes.fold(0.0) { acc, n -> max(acc, n.startSec + n.durationSec) }
        return ParsedScoreResult(title, notes, bpm, durationSec)
    }

    /**
     * Encodes stereo FloatArray channels into lossless 24-bit PCM WAV bytes.
     */
    fun encodeWav24Bit(left: FloatArray, right: FloatArray, sampleRate: Int): ByteArray {
        val numFrames = min(left.size, right.size)
        val numChannels = 2
        val bytesPerSample = 3
        val blockAlign = numChannels * bytesPerSample
        val dataSize = numFrames * blockAlign
        val out = ByteArray(44 + dataSize)

        fun putStr(off: Int, s: String) {
            for (i in s.indices) out[off + i] = s[i].code.toByte()
        }
        fun putU32LE(off: Int, v: Int) {
            out[off] = (v and 0xFF).toByte()
            out[off + 1] = ((v ushr 8) and 0xFF).toByte()
            out[off + 2] = ((v ushr 16) and 0xFF).toByte()
            out[off + 3] = ((v ushr 24) and 0xFF).toByte()
        }
        fun putU16LE(off: Int, v: Int) {
            out[off] = (v and 0xFF).toByte()
            out[off + 1] = ((v ushr 8) and 0xFF).toByte()
        }

        putStr(0, "RIFF")
        putU32LE(4, 36 + dataSize)
        putStr(8, "WAVE")
        putStr(12, "fmt ")
        putU32LE(16, 16)
        putU16LE(20, 1)
        putU16LE(22, numChannels)
        putU32LE(24, sampleRate)
        putU32LE(28, sampleRate * blockAlign)
        putU16LE(32, blockAlign)
        putU16LE(34, 24)
        putStr(36, "data")
        putU32LE(40, dataSize)

        var off = 44
        for (i in 0 until numFrames) {
            for (ch in 0..1) {
                val s = (if (ch == 0) left[i] else right[i]).coerceIn(-1f, 1f)
                val int24 = if (s < 0f) (s * 8388608f).roundToInt() else (s * 8388607f).roundToInt()
                out[off++] = (int24 and 0xFF).toByte()
                out[off++] = ((int24 shr 8) and 0xFF).toByte()
                out[off++] = ((int24 shr 16) and 0xFF).toByte()
            }
        }
        return out
    }
}
