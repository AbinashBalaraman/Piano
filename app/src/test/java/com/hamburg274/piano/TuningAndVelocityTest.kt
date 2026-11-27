package com.hamburg274.piano

import com.hamburg274.piano.music.PianoTuning
import com.hamburg274.piano.music.TemperamentId
import com.hamburg274.piano.music.TouchVelocityMode
import com.hamburg274.piano.music.VelocityEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TuningAndVelocityTest {

    @Test
    fun keyboardHasExact88KeysWith52WhiteAnd36Black() {
        assertEquals(88, PianoTuning.ALL_88_KEYS.size)
        assertEquals(52, PianoTuning.WHITE_KEYS.size)
        assertEquals(36, PianoTuning.BLACK_KEYS.size)
        assertEquals("A0", PianoTuning.ALL_88_KEYS.first().name)
        assertEquals("C8", PianoTuning.ALL_88_KEYS.last().name)
    }

    @Test
    fun railsbackStretchFlattensBassAndSharpensTreble() {
        val a4 = PianoTuning.midiToFrequency(69, TemperamentId.RAILSBACK, 440)
        assertEquals(440.0, a4, 0.001)

        val bassStretch = PianoTuning.getRailsbackStretchCents(21) // A0
        val trebleStretch = PianoTuning.getRailsbackStretchCents(108) // C8
        assertTrue("Expected negative bass stretch at A0", bassStretch < -18.0)
        assertTrue("Expected positive treble stretch at C8", trebleStretch > 22.0)
    }

    @Test
    fun equalTemperamentUsesExact440ReferenceAndNoStretch() {
        assertEquals(0.0, PianoTuning.getTemperamentCentsOffset(21, TemperamentId.EQUAL), 0.0001)
        assertEquals(440.0, PianoTuning.midiToFrequency(69, TemperamentId.EQUAL, 440), 0.001)
        assertEquals(880.0, PianoTuning.midiToFrequency(81, TemperamentId.EQUAL, 440), 0.001)
        assertEquals(415.0, PianoTuning.midiToFrequency(69, TemperamentId.EQUAL, 415), 0.001)
    }

    @Test
    fun velocityCurveMappingIsMonotonicAndBounded() {
        val concertCurve = VelocityEngine.PRESETS.first().points
        var prev = 0
        for (raw in 1..127) {
            val mapped = VelocityEngine.applyVelocityCurve(raw, concertCurve)
            assertTrue("Velocity must stay in 1..127", mapped in 1..127)
            assertTrue("Concert curve must be non-decreasing", mapped >= prev)
            prev = mapped
        }
    }

    @Test
    fun touchVelocityModesRespectVerticalRangeAndFixedValue() {
        val soft = VelocityEngine.computeRawTouchVelocity(0.05f, null, null, TouchVelocityMode.Y_POSITION, 80)
        val loud = VelocityEngine.computeRawTouchVelocity(0.95f, null, null, TouchVelocityMode.Y_POSITION, 80)
        assertTrue("Lower Y near ivory lip must be louder than upper felt", loud > soft + 60)

        val fixed = VelocityEngine.computeRawTouchVelocity(0.1f, null, null, TouchVelocityMode.FIXED, 92)
        assertEquals(92, fixed)
    }
}
