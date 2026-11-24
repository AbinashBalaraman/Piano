package com.hamburg274.piano.music

import kotlin.math.floor
import kotlin.math.pow

enum class TemperamentId(
    val displayName: String,
    val shortName: String,
    val era: String,
    val description: String
) {
    RAILSBACK(
        displayName = "Concert Stretched (Railsback)",
        shortName = "Railsback",
        era = "Modern Concert",
        description = "Default concert grand tuning: flattens low bass and sharpens upper treble to minimize inharmonic octave beating."
    ),
    EQUAL(
        displayName = "Pure Equal Temperament",
        shortName = "Equal",
        era = "Standard Ensemble",
        description = "Mathematically uniform 100-cent semitones for layering with digital ensembles and synthesizers."
    ),
    WERCKMEISTER3(
        displayName = "Werckmeister III (1691)",
        shortName = "Werckmeister",
        era = "Baroque Well-Temperament",
        description = "Authentic key colors for Bach and late Baroque counterpoint, with pure intervals in C, G, and D major."
    ),
    KIRNBERGER3(
        displayName = "Kirnberger III (1779)",
        shortName = "Kirnberger",
        era = "Classical Transition",
        description = "Pure major thirds in home keys with four quarter-comma tempered fifths, prized by Bach’s pupils."
    ),
    VALLOTTI(
        displayName = "Vallotti (1754)",
        shortName = "Vallotti",
        era = "18th-Century Classical",
        description = "Smooth sixth-comma well-temperament across six fifths, lending luminous warmth to Mozart and Haydn."
    )
}

object PianoTuning {
    const val MIDI_MIN = 21 // A0
    const val MIDI_MAX = 108 // C8
    const val TOTAL_KEYS = 88
    const val WHITE_KEY_COUNT = 52

    private val NOTE_NAMES_SHARP = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
    private val BLACK_PITCH_CLASSES = setOf(1, 3, 6, 8, 10)

    private val HISTORICAL_CENTS_RAW: Map<TemperamentId, DoubleArray> = mapOf(
        TemperamentId.WERCKMEISTER3 to doubleArrayOf(
            0.0, -9.78, -7.82, -5.87, -9.78, -1.96, -11.73, -3.91, -7.82, -11.73, -3.91, -7.82
        ),
        TemperamentId.KIRNBERGER3 to doubleArrayOf(
            0.0, -9.78, -6.84, -5.87, -13.69, -1.96, -10.75, -3.42, -7.82, -10.26, -3.91, -11.73
        ),
        TemperamentId.VALLOTTI to doubleArrayOf(
            0.0, -5.87, -3.91, -1.96, -7.82, 1.96, -7.82, -1.96, -3.91, -5.87, 0.0, -9.78
        )
    )

    data class PianoKeyMeta(
        val midi: Int,
        val name: String,
        val isBlack: Boolean,
        val octave: Int,
        val pitchClass: Int,
        val whiteIndex: Int
    )

    val ALL_88_KEYS: List<PianoKeyMeta> = buildList {
        var whiteIdx = -1
        for (midi in MIDI_MIN..MIDI_MAX) {
            val black = isBlackKey(midi)
            if (!black) whiteIdx += 1
            add(
                PianoKeyMeta(
                    midi = midi,
                    name = midiToNoteName(midi),
                    isBlack = black,
                    octave = floor(midi / 12.0).toInt() - 1,
                    pitchClass = midi % 12,
                    whiteIndex = whiteIdx
                )
            )
        }
    }

    val WHITE_KEYS: List<PianoKeyMeta> = ALL_88_KEYS.filter { !it.isBlack }
    val BLACK_KEYS: List<PianoKeyMeta> = ALL_88_KEYS.filter { it.isBlack }

    fun isBlackKey(midi: Int): Boolean = ((midi % 12) + 12) % 12 in BLACK_PITCH_CLASSES

    fun midiToNoteName(midi: Int): String {
        val pc = ((midi % 12) + 12) % 12
        val octave = floor(midi / 12.0).toInt() - 1
        return "${NOTE_NAMES_SHARP[pc]}$octave"
    }

    fun noteNameToMidi(name: String): Int {
        val match = Regex("^([A-Ga-g])([#b]?)(-?\\d)$").matchEntire(name.trim()) ?: return 60
        val letterMap = mapOf('C' to 0, 'D' to 2, 'E' to 4, 'F' to 5, 'G' to 7, 'A' to 9, 'B' to 11)
        var pc = letterMap[match.groupValues[1].uppercase()[0]] ?: 0
        when (match.groupValues[2]) {
            "#" -> pc += 1
            "b" -> pc -= 1
        }
        val octave = match.groupValues[3].toInt()
        return (octave + 1) * 12 + pc
    }

    /**
     * Railsback concert grand stretch curve in cents relative to 12-TET.
     * Flattens low bass (~-24 cents at A0) and sharpens upper treble (~+30 cents at C8).
     */
    fun getRailsbackStretchCents(midi: Int): Double {
        val d = midi - 69
        if (d == 0) return 0.0
        return if (d < 0) {
            val x = -d / 48.0
            -(2.2 * x + 21.8 * x.pow(3.1))
        } else {
            val x = d / 39.0
            3.0 * x + 27.0 * x.pow(2.75)
        }
    }

    fun getTemperamentCentsOffset(midi: Int, temperament: TemperamentId): Double {
        if (temperament == TemperamentId.EQUAL) return 0.0
        if (temperament == TemperamentId.RAILSBACK) return getRailsbackStretchCents(midi)

        val pc = ((midi % 12) + 12) % 12
        val table = HISTORICAL_CENTS_RAW[temperament] ?: return 0.0
        val rawOffset = table[pc] - table[9]
        val mildStretch = getRailsbackStretchCents(midi) * 0.25
        return rawOffset + mildStretch
    }

    fun midiToFrequency(
        midi: Int,
        temperament: TemperamentId = TemperamentId.RAILSBACK,
        referenceA4Hz: Int = 440
    ): Double {
        val clampedRef = referenceA4Hz.coerceIn(410, 466).toDouble()
        val centsOffset = getTemperamentCentsOffset(midi, temperament)
        val semitoneOffset = (midi - 69) + centsOffset / 100.0
        return clampedRef * 2.0.pow(semitoneOffset / 12.0)
    }

    /**
     * Physical steel-string inharmonicity coefficient B for a 274 cm concert grand.
     */
    fun getStringInharmonicity(midi: Int): Double {
        val normalized = (midi - MIDI_MIN).toDouble() / (MIDI_MAX - MIDI_MIN).toDouble()
        if (midi < 36) {
            return 0.00018 + (36 - midi) * 0.000012
        }
        return 0.00008 + normalized.coerceIn(0.0, 1.0).pow(2.8) * 0.0024
    }
}
