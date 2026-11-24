package com.hamburg274.piano.music

import kotlin.math.floor
import kotlin.math.max
import kotlin.math.round

data class ClassicalPiece(
    val id: String,
    val title: String,
    val composer: String,
    val opus: String,
    val era: String,
    val grade: String,
    val keySignature: String,
    val timeSignatureNum: Int,
    val timeSignatureDen: Int,
    val defaultBpm: Int,
    val recommendedRubatoSec: Double,
    val recommendedTemperament: TemperamentId,
    val measures: Int,
    val harmonicChangeTimesSec: List<Double>,
    val notes: List<ExpectedScoreNote>,
    val durationSec: Double,
    val urtextNote: String
)

private data class RawBeatNote(
    val note: String,
    val beat: Double,
    val dur: Double,
    val vel: Int,
    val hand: HandPart,
    val fingering: Int? = null,
    val dynamicLabel: String? = null
)

object ClassicalRepertoire {

    private fun round4(v: Double): Double = round(v * 10000.0) / 10000.0

    private fun buildPiece(
        id: String,
        title: String,
        composer: String,
        opus: String,
        era: String,
        grade: String,
        keySignature: String,
        timeNum: Int,
        timeDen: Int,
        defaultBpm: Int,
        recommendedRubatoSec: Double,
        recommendedTemperament: TemperamentId,
        measures: Int,
        urtextNote: String,
        rawNotes: List<RawBeatNote>,
        harmonicChangeBeats: List<Double>
    ): ClassicalPiece {
        val secPerBeat = 60.0 / defaultBpm
        val beatsPerMeasure = timeNum * (4.0 / timeDen)
        val notes = rawNotes.mapIndexed { i, r ->
            ExpectedScoreNote(
                id = "$id-n$i",
                midi = PianoTuning.noteNameToMidi(r.note),
                startSec = round4(r.beat * secPerBeat),
                durationSec = round4(r.dur * secPerBeat),
                targetVelocity = r.vel,
                measure = floor(r.beat / beatsPerMeasure).toInt() + 1,
                hand = r.hand,
                fingering = r.fingering,
                dynamicLabel = r.dynamicLabel
            )
        }.sortedWith(compareBy<ExpectedScoreNote> { it.startSec }.thenBy { it.midi })

        val durationSec = notes.fold(0.0) { acc, n -> max(acc, n.startSec + n.durationSec) }
        return ClassicalPiece(
            id = id,
            title = title,
            composer = composer,
            opus = opus,
            era = era,
            grade = grade,
            keySignature = keySignature,
            timeSignatureNum = timeNum,
            timeSignatureDen = timeDen,
            defaultBpm = defaultBpm,
            recommendedRubatoSec = recommendedRubatoSec,
            recommendedTemperament = recommendedTemperament,
            measures = measures,
            harmonicChangeTimesSec = harmonicChangeBeats.map { round4(it * secPerBeat) },
            notes = notes,
            durationSec = round((durationSec + 0.8) * 100.0) / 100.0,
            urtextNote = urtextNote
        )
    }

    private fun createBachPreludeC(): ClassicalPiece {
        data class BachBar(val b1: String, val b2: String, val r1: String, val r2: String, val r3: String, val vel: Int, val dyn: String? = null)
        val bars = listOf(
            BachBar("C4", "E4", "G4", "C5", "E5", 62, "p"),
            BachBar("C4", "D4", "A4", "D5", "F5", 66),
            BachBar("B3", "D4", "G4", "D5", "F5", 68),
            BachBar("C4", "E4", "G4", "C5", "E5", 64),
            BachBar("C4", "E4", "A4", "E5", "A5", 74, "mp"),
            BachBar("C4", "D4", "F#4", "A4", "D5", 76),
            BachBar("B3", "D4", "G4", "D5", "G5", 78, "mf"),
            BachBar("B3", "C4", "E4", "G4", "C5", 66, "p")
        )
        val raw = mutableListOf<RawBeatNote>()
        val harmonicBeats = mutableListOf<Double>()

        bars.forEachIndexed { mIdx, c ->
            val baseBeat = mIdx * 4.0
            if (mIdx > 0) harmonicBeats.add(baseBeat)
            for (half in 0..1) {
                val h = baseBeat + half * 2.0
                raw.add(RawBeatNote(c.b1, h, 2.0, max(42, c.vel - 4), HandPart.LH, 5, if (half == 0) c.dyn else null))
                raw.add(RawBeatNote(c.b2, h + 0.25, 1.75, c.vel - 8, HandPart.LH, 2))
                raw.add(RawBeatNote(c.r1, h + 0.50, 0.45, c.vel - 4, HandPart.RH, 1))
                raw.add(RawBeatNote(c.r2, h + 0.75, 0.45, c.vel, HandPart.RH, 3))
                raw.add(RawBeatNote(c.r3, h + 1.00, 0.45, c.vel + 6, HandPart.RH, 5))
                raw.add(RawBeatNote(c.r1, h + 1.25, 0.45, c.vel - 6, HandPart.RH, 1))
                raw.add(RawBeatNote(c.r2, h + 1.50, 0.45, c.vel - 2, HandPart.RH, 3))
                raw.add(RawBeatNote(c.r3, h + 1.75, 0.45, c.vel + 4, HandPart.RH, 5))
            }
        }

        return buildPiece(
            id = "bach-bwv846",
            title = "Prelude in C Major",
            composer = "Johann Sebastian Bach",
            opus = "BWV 846 · WTC Book I",
            era = "Baroque",
            grade = "Grade 4",
            keySignature = "C Major",
            timeNum = 4,
            timeDen = 4,
            defaultBpm = 76,
            recommendedRubatoSec = 0.10,
            recommendedTemperament = TemperamentId.WERCKMEISTER3,
            measures = 8,
            urtextNote = "Urtext edition · Maintain resonant bass finger-legato with minimal dry pedal.",
            rawNotes = raw,
            harmonicChangeBeats = harmonicBeats
        )
    }

    private fun createBeethovenMoonlight(): ClassicalPiece {
        data class MelNote(val beat: Double, val note: String, val dur: Double, val vel: Int, val fing: Int)
        data class MoonBar(val m: Int, val bass: List<String>, val trip: Triple<String, String, String>, val mel: List<MelNote> = emptyList())

        val bars = listOf(
            MoonBar(0, listOf("C#2", "C#3"), Triple("G#3", "C#4", "E4")),
            MoonBar(1, listOf("B1", "B2"), Triple("G#3", "C#4", "E4")),
            MoonBar(2, listOf("A1", "A2"), Triple("A3", "C#4", "E4")),
            MoonBar(3, listOf("G#1", "G#2"), Triple("G#3", "C4", "F#4")),
            MoonBar(4, listOf("C#2", "C#3"), Triple("G#3", "C#4", "E4"), listOf(
                MelNote(3.0, "G#4", 0.75, 68, 5),
                MelNote(3.75, "G#4", 0.25, 64, 5)
            )),
            MoonBar(5, listOf("G#1", "G#2"), Triple("G#3", "D#4", "F#4"), listOf(
                MelNote(0.0, "G#4", 3.0, 72, 5),
                MelNote(3.0, "G#4", 0.75, 66, 5),
                MelNote(3.75, "G#4", 0.25, 62, 5)
            )),
            MoonBar(6, listOf("C#2", "C#3"), Triple("G#3", "C#4", "E4"), listOf(
                MelNote(0.0, "A4", 2.0, 78, 5),
                MelNote(2.0, "A4", 2.0, 70, 5)
            )),
            MoonBar(7, listOf("A1", "A2"), Triple("A3", "C#4", "F#4"), listOf(
                MelNote(0.0, "G#4", 2.0, 64, 4),
                MelNote(2.0, "F#4", 2.0, 56, 3)
            ))
        )

        val raw = mutableListOf<RawBeatNote>()
        val harmonicBeats = mutableListOf<Double>()

        for (bar in bars) {
            val base = bar.m * 4.0
            if (bar.m > 0) harmonicBeats.add(base)
            bar.bass.forEachIndexed { idx, n ->
                raw.add(RawBeatNote(n, base, 4.0, 48, HandPart.LH, if (idx == 0) 5 else 1, if (bar.m == 0 && idx == 0) "pp" else null))
            }
            val tripList = listOf(bar.trip.first, bar.trip.second, bar.trip.third)
            for (beat in 0..3) {
                for (t in 0..2) {
                    raw.add(RawBeatNote(tripList[t], base + beat + t / 3.0, 0.34, 42 + t * 2, HandPart.RH, t + 1))
                }
            }
            for (mNote in bar.mel) {
                raw.add(RawBeatNote(mNote.note, base + mNote.beat, mNote.dur, mNote.vel, HandPart.RH, mNote.fing, if (bar.m == 4 && mNote.beat == 3.0) "p" else null))
            }
        }

        return buildPiece(
            id = "beethoven-op27-2",
            title = "Moonlight Sonata (I. Adagio)",
            composer = "Ludwig van Beethoven",
            opus = "Op. 27, No. 2",
            era = "Classical",
            grade = "Grade 6",
            keySignature = "C# Minor",
            timeNum = 4,
            timeDen = 4,
            defaultBpm = 54,
            recommendedRubatoSec = 0.20,
            recommendedTemperament = TemperamentId.RAILSBACK,
            measures = 8,
            urtextNote = "Senza sordini · Voiced cantabile top pinky melody over whisper-soft triplets.",
            rawNotes = raw,
            harmonicChangeBeats = harmonicBeats
        )
    }

    private fun createChopinPreludeEm(): ClassicalPiece {
        val raw = mutableListOf<RawBeatNote>()
        val harmonicBeats = listOf(0.5, 4.5, 8.5, 12.5, 16.5, 20.5)
        raw.add(RawBeatNote("B4", 0.0, 0.5, 58, HandPart.RH, 3, "p"))

        data class MelItem(val beat: Double, val note: String, val dur: Double, val vel: Int, val fing: Int, val dyn: String? = null)
        val melody = listOf(
            MelItem(0.5, "B4", 3.0, 64, 3),
            MelItem(3.5, "C5", 1.0, 68, 4),
            MelItem(4.5, "B4", 3.0, 62, 3),
            MelItem(7.5, "C5", 1.0, 66, 4),
            MelItem(8.5, "B4", 3.0, 60, 3),
            MelItem(11.5, "C5", 1.0, 72, 4, "mp"),
            MelItem(12.5, "B4", 2.5, 68, 3),
            MelItem(15.0, "A4", 1.5, 64, 2),
            MelItem(16.5, "A4", 3.0, 60, 2),
            MelItem(19.5, "B4", 1.0, 66, 3),
            MelItem(20.5, "A4", 3.5, 56, 2, "pp")
        )
        melody.forEach { raw.add(RawBeatNote(it.note, it.beat, it.dur, it.vel, HandPart.RH, it.fing, it.dyn)) }

        val lhBars = listOf(
            listOf("G3", "B3", "E4"),
            listOf("F#3", "A3", "E4"),
            listOf("F3", "A3", "D#4"),
            listOf("F3", "A3", "D4"),
            listOf("E3", "G#3", "D4"),
            listOf("E3", "G3", "C4")
        )
        lhBars.forEachIndexed { barIdx, chord ->
            val startBeat = 0.5 + barIdx * 4.0
            for (eighth in 0..7) {
                val b = startBeat + eighth * 0.5
                chord.forEachIndexed { idx, n ->
                    raw.add(RawBeatNote(n, b, 0.45, 44 + if (eighth % 4 == 0) 4 else 0, HandPart.LH, if (idx == 0) 5 else if (idx == 1) 3 else 1))
                }
            }
        }

        return buildPiece(
            id = "chopin-op28-4",
            title = "Prelude in E Minor",
            composer = "Frédéric Chopin",
            opus = "Op. 28, No. 4",
            era = "Romantic",
            grade = "Grade 5",
            keySignature = "E Minor",
            timeNum = 2,
            timeDen = 2,
            defaultBpm = 62,
            recommendedRubatoSec = 0.28,
            recommendedTemperament = TemperamentId.RAILSBACK,
            measures = 6,
            urtextNote = "Largo · Expressive rubato with syncopated pedal changes on every harmonic shift.",
            rawNotes = raw,
            harmonicChangeBeats = harmonicBeats
        )
    }

    private fun createSatieGymnopedie(): ClassicalPiece {
        val raw = mutableListOf<RawBeatNote>()
        val harmonicBeats = mutableListOf<Double>()
        for (m in 0..7) {
            val base = m * 3.0
            if (m > 0) harmonicBeats.add(base)
            val isG = m % 2 == 0
            raw.add(RawBeatNote(if (isG) "G2" else "D2", base, 3.0, 52, HandPart.LH, 5, if (m == 0) "pp" else null))
            val chord = if (isG) listOf("B3", "D4", "F#4") else listOf("A3", "C#4", "F#4")
            chord.forEachIndexed { idx, n ->
                raw.add(RawBeatNote(n, base + 1.0, 2.0, 46, HandPart.LH, if (idx == 0) 4 else if (idx == 1) 2 else 1))
            }
        }
        val mel = listOf(
            RawBeatNote("F#5", 13.0, 1.0, 66, HandPart.RH, 3, "p"),
            RawBeatNote("A5", 14.0, 1.0, 70, HandPart.RH, 5),
            RawBeatNote("G5", 15.0, 1.0, 68, HandPart.RH, 4),
            RawBeatNote("F#5", 16.0, 1.0, 64, HandPart.RH, 3),
            RawBeatNote("C#5", 17.0, 1.0, 62, HandPart.RH, 2),
            RawBeatNote("B4", 18.0, 1.0, 60, HandPart.RH, 1),
            RawBeatNote("C#5", 19.0, 1.0, 64, HandPart.RH, 2),
            RawBeatNote("D5", 20.0, 1.0, 66, HandPart.RH, 3),
            RawBeatNote("A4", 21.0, 3.0, 56, HandPart.RH, 1, "pp")
        )
        raw.addAll(mel)

        return buildPiece(
            id = "satie-gymnopedie1",
            title = "Gymnopédie No. 1",
            composer = "Erik Satie",
            opus = "Trois Gymnopédies (1888)",
            era = "Impressionist",
            grade = "Grade 4",
            keySignature = "D Major",
            timeNum = 3,
            timeDen = 4,
            defaultBpm = 72,
            recommendedRubatoSec = 0.22,
            recommendedTemperament = TemperamentId.RAILSBACK,
            measures = 8,
            urtextNote = "Lent et douloureux · Ideal for Una Corda (soft pedal) & half-pedaling.",
            rawNotes = raw,
            harmonicChangeBeats = harmonicBeats
        )
    }

    private fun createMozartK545(): ClassicalPiece {
        val raw = mutableListOf<RawBeatNote>()
        val harmonicBeats = listOf(4.0, 8.0, 12.0)
        val alberti = listOf(
            0.0 to listOf("C4", "G4", "E4", "G4"),
            2.0 to listOf("C4", "G4", "E4", "G4"),
            4.0 to listOf("D4", "G4", "F4", "G4"),
            6.0 to listOf("C4", "G4", "E4", "G4"),
            8.0 to listOf("C4", "A4", "F4", "A4"),
            10.0 to listOf("C4", "G4", "E4", "G4"),
            12.0 to listOf("B3", "G4", "D4", "G4"),
            14.0 to listOf("C4", "G4", "E4", "G4")
        )
        for ((start, pat) in alberti) {
            for (rep in 0..1) {
                pat.forEachIndexed { idx, n ->
                    raw.add(RawBeatNote(n, start + rep + idx * 0.25, 0.24, 54 + if (idx == 0) 6 else 0, HandPart.LH, if (idx == 0) 5 else if (idx == 2) 3 else 1))
                }
            }
        }
        val rh = listOf(
            RawBeatNote("C5", 0.0, 2.0, 78, HandPart.RH, 1, "mf"),
            RawBeatNote("E5", 2.0, 1.0, 82, HandPart.RH, 3),
            RawBeatNote("G5", 3.0, 1.0, 84, HandPart.RH, 5),
            RawBeatNote("B4", 4.0, 1.5, 76, HandPart.RH, 2),
            RawBeatNote("C5", 5.5, 0.25, 74, HandPart.RH, 3),
            RawBeatNote("D5", 5.75, 0.25, 74, HandPart.RH, 4),
            RawBeatNote("C5", 6.0, 1.0, 78, HandPart.RH, 3),
            RawBeatNote("A5", 8.0, 2.0, 86, HandPart.RH, 5, "f"),
            RawBeatNote("G5", 10.0, 1.0, 80, HandPart.RH, 4),
            RawBeatNote("C6", 11.0, 1.0, 88, HandPart.RH, 5),
            RawBeatNote("G5", 12.0, 1.0, 78, HandPart.RH, 3),
            RawBeatNote("F5", 13.0, 0.5, 74, HandPart.RH, 2),
            RawBeatNote("E5", 13.5, 0.5, 72, HandPart.RH, 1),
            RawBeatNote("E5", 14.0, 2.0, 74, HandPart.RH, 2, "p")
        )
        raw.addAll(rh)

        return buildPiece(
            id = "mozart-k545",
            title = "Sonata in C Major (I. Allegro)",
            composer = "Wolfgang Amadeus Mozart",
            opus = "K. 545 · Sonata facile",
            era = "Classical",
            grade = "Grade 5",
            keySignature = "C Major",
            timeNum = 4,
            timeDen = 4,
            defaultBpm = 112,
            recommendedRubatoSec = 0.09,
            recommendedTemperament = TemperamentId.VALLOTTI,
            measures = 4,
            urtextNote = "Crisp Classical articulation · Try Vallotti temperament & dialed-back Duplex mix.",
            rawNotes = raw,
            harmonicChangeBeats = harmonicBeats
        )
    }

    private fun createDebussyClairDeLune(): ClassicalPiece {
        data class ClairBar(val beat: Double, val lh: List<String>, val rh: List<String>, val dur: Double, val vel: Int, val dyn: String? = null)
        val chords = listOf(
            ClairBar(0.5, listOf("F4", "A4"), listOf("F5", "A5"), 2.5, 48, "pp"),
            ClairBar(3.0, listOf("D4", "F4"), listOf("D5", "F5"), 3.0, 54),
            ClairBar(6.0, listOf("C4", "E4"), listOf("E5", "C5"), 1.5, 58),
            ClairBar(7.5, listOf("B3", "D4"), listOf("D5", "B4"), 1.5, 52),
            ClairBar(9.0, listOf("A3", "C4"), listOf("C5", "A4"), 3.0, 60, "p"),
            ClairBar(12.0, listOf("G3", "B3"), listOf("B4", "G4"), 3.0, 46, "pp")
        )
        val raw = mutableListOf<RawBeatNote>()
        for (c in chords) {
            c.lh.forEachIndexed { idx, n -> raw.add(RawBeatNote(n, c.beat, c.dur, c.vel - 6, HandPart.LH, if (idx == 0) 4 else 2)) }
            c.rh.forEachIndexed { idx, n -> raw.add(RawBeatNote(n, c.beat, c.dur, c.vel, HandPart.RH, if (idx == 0) 1 else 3, if (idx == 1) c.dyn else null)) }
        }

        return buildPiece(
            id = "debussy-clair",
            title = "Clair de Lune (Opening)",
            composer = "Claude Debussy",
            opus = "Suite bergamasque, L. 75",
            era = "Impressionist",
            grade = "Grade 7",
            keySignature = "C / F Modal",
            timeNum = 9,
            timeDen = 8,
            defaultBpm = 58,
            recommendedRubatoSec = 0.30,
            recommendedTemperament = TemperamentId.RAILSBACK,
            measures = 5,
            urtextNote = "Andante très expressif · Open Duplex Resonance & Hall Reverb for luminous bloom.",
            rawNotes = raw,
            harmonicChangeBeats = listOf(3.0, 6.0, 9.0, 12.0)
        )
    }

    val PIECES: List<ClassicalPiece> = listOf(
        createBachPreludeC(),
        createBeethovenMoonlight(),
        createChopinPreludeEm(),
        createSatieGymnopedie(),
        createMozartK545(),
        createDebussyClairDeLune()
    )
}
