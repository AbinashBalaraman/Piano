package com.hamburg274.piano

import com.hamburg274.piano.music.ExpectedScoreNote
import com.hamburg274.piano.music.HandPart
import com.hamburg274.piano.music.MidiCodec
import com.hamburg274.piano.music.PedalSample
import com.hamburg274.piano.music.PlayedNoteSample
import com.hamburg274.piano.music.PracticeGrader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MidiAndGraderTest {

    @Test
    fun standardMidiEncodeAndDecodeRoundTripPreservesNotes() {
        val smf = MidiCodec.encodeStandardMidiFile(MidiCodec.DEMO_STUDIO_TAKE.events, bpm = 120)
        assertTrue("SMF header MThd expected", smf.size > 24 && smf[0].toInt() == 0x4D && smf[1].toInt() == 0x54)

        val parsed = MidiCodec.parseStandardMidiFile(smf, "RoundTrip")
        assertEquals("RoundTrip", parsed.title)
        assertTrue("Expected playable notes after SMF decode", parsed.notes.size >= 10)
    }

    @Test
    fun expressiveGraderRewardsAccuratePitchRhythmDynamicsAndPedal() {
        val expected = listOf(
            ExpectedScoreNote("n1", 60, 0.0, 0.5, 72, 1, HandPart.RH),
            ExpectedScoreNote("n2", 64, 0.5, 0.5, 76, 1, HandPart.RH),
            ExpectedScoreNote("n3", 67, 1.0, 0.5, 80, 2, HandPart.RH)
        )
        val played = listOf(
            PlayedNoteSample(60, 0.02, 74),
            PlayedNoteSample(64, 0.51, 75),
            PlayedNoteSample(67, 1.02, 79)
        )
        val pedals = listOf(
            PedalSample(0.0, 100),
            PedalSample(0.98, 12),
            PedalSample(1.06, 105)
        )
        val report = PracticeGrader.evaluateExpressivePerformance(
            expected = expected,
            played = played,
            pedalEvents = pedals,
            rubatoToleranceSec = 0.16,
            harmonicChangeTimesSec = listOf(1.0)
        )
        assertEquals(100, report.pitchAccuracy)
        assertTrue("Overall score should be >= 90", report.overallScore >= 90)
        assertEquals(3, report.matchedNotes)
    }
}
