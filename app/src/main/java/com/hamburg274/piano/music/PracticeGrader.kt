package com.hamburg274.piano.music

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

enum class HandPart { LH, RH }

data class ExpectedScoreNote(
    val id: String,
    val midi: Int,
    val startSec: Double,
    val durationSec: Double,
    val targetVelocity: Int,
    val measure: Int,
    val hand: HandPart,
    val fingering: Int? = null,
    val dynamicLabel: String? = null
)

data class PlayedNoteSample(
    val midi: Int,
    val timeSec: Double,
    val velocity: Int
)

data class PedalSample(
    val timeSec: Double,
    val value: Int // 0..127 (CC64)
)

data class ExpressiveGradeReport(
    val overallScore: Int,
    val pitchAccuracy: Int,
    val rhythmicPrecision: Int,
    val dynamicFidelity: Int,
    val pedalingCleanliness: Int,
    val matchedNotes: Int,
    val totalExpectedNotes: Int,
    val extraWrongNotes: Int,
    val harmonicChangesEvaluated: Int,
    val cleanPedalChanges: Int,
    val summary: String
)

object PracticeGrader {
    fun evaluateExpressivePerformance(
        expected: List<ExpectedScoreNote>,
        played: List<PlayedNoteSample>,
        pedalEvents: List<PedalSample>,
        rubatoToleranceSec: Double,
        harmonicChangeTimesSec: List<Double>
    ): ExpressiveGradeReport {
        if (expected.isEmpty()) {
            return ExpressiveGradeReport(
                overallScore = 0,
                pitchAccuracy = 0,
                rhythmicPrecision = 0,
                dynamicFidelity = 0,
                pedalingCleanliness = 100,
                matchedNotes = 0,
                totalExpectedNotes = 0,
                extraWrongNotes = played.size,
                harmonicChangesEvaluated = 0,
                cleanPedalChanges = 0,
                summary = "Select a piece and play to generate your expressive evaluation."
            )
        }

        val rubato = rubatoToleranceSec.coerceIn(0.06, 0.45)
        val matchWindow = max(0.42, rubato * 2.2)
        val usedPlayed = mutableSetOf<Int>()

        var matchedCount = 0
        var rhythmScoreSum = 0.0
        var dynamicScoreSum = 0.0

        for (exp in expected) {
            var bestIdx = -1
            var bestDelta = Double.POSITIVE_INFINITY

            for (i in played.indices) {
                if (i in usedPlayed) continue
                if (played[i].midi != exp.midi) continue
                val delta = abs(played[i].timeSec - exp.startSec)
                if (delta <= matchWindow && delta < bestDelta) {
                    bestDelta = delta
                    bestIdx = i
                }
            }

            if (bestIdx >= 0) {
                usedPlayed.add(bestIdx)
                matchedCount += 1

                val excess = max(0.0, bestDelta - rubato * 0.45)
                val rhythmNoteScore = max(0.0, 100.0 - (excess / max(0.08, matchWindow)) * 100.0)
                rhythmScoreSum += rhythmNoteScore

                val velError = abs(played[bestIdx].velocity - exp.targetVelocity)
                val excessVel = max(0, velError - 12)
                val dynamicNoteScore = max(0.0, 100.0 - (excessVel / 64.0) * 100.0)
                dynamicScoreSum += dynamicNoteScore
            }
        }

        val extraWrongNotes = max(0, played.size - matchedCount)
        val hitRatio = matchedCount.toDouble() / expected.size.toDouble()
        val wrongPenalty = min(35.0, (extraWrongNotes.toDouble() / max(1, expected.size)) * 45.0)
        val pitchAccuracy = (hitRatio * 100.0 - wrongPenalty).coerceIn(0.0, 100.0).roundToInt()

        val rhythmicPrecision = if (matchedCount > 0) (rhythmScoreSum / matchedCount).roundToInt() else 0
        val dynamicFidelity = if (matchedCount > 0) (dynamicScoreSum / matchedCount).roundToInt() else 0

        var harmonicChangesEvaluated = 0
        var cleanPedalChanges = 0

        if (pedalEvents.isNotEmpty() && harmonicChangeTimesSec.isNotEmpty()) {
            val sortedPedal = pedalEvents.sortedBy { it.timeSec }
            for (changeTime in harmonicChangeTimesSec) {
                var stateBefore = 0
                for (ev in sortedPedal) {
                    if (ev.timeSec < changeTime - 0.15) {
                        stateBefore = ev.value
                    } else {
                        break
                    }
                }
                if (stateBefore >= 50) {
                    harmonicChangesEvaluated += 1
                    val cleared = sortedPedal.any { ev ->
                        ev.timeSec >= changeTime - 0.15 &&
                            ev.timeSec <= changeTime + 0.42 &&
                            ev.value < 45
                    }
                    if (cleared) cleanPedalChanges += 1
                }
            }
        }

        val pedalingCleanliness = if (harmonicChangesEvaluated > 0) {
            ((cleanPedalChanges.toDouble() / harmonicChangesEvaluated) * 100.0).roundToInt()
        } else {
            100
        }

        val overallScore = if (played.isEmpty()) {
            0
        } else {
            (pitchAccuracy * 0.38 +
                rhythmicPrecision * 0.24 +
                dynamicFidelity * 0.22 +
                pedalingCleanliness * 0.16).roundToInt()
        }

        val summary = when {
            matchedCount == 0 ->
                "Play along with the score to measure pitch, timing, dynamics, and pedaling."
            overallScore >= 90 ->
                "Recital-ready command across pitch, expressive dynamics, and harmonic pedaling."
            pedalingCleanliness < 70 && harmonicChangesEvaluated > 0 ->
                "Clear the sustain pedal slightly after each harmonic change to avoid blurring chords."
            dynamicFidelity < 72 ->
                "Broaden your velocity contour between pianissimo passages and melodic peaks."
            rhythmicPrecision < 75 ->
                "Steadier pulse recommended—try lowering practice tempo or widening rubato tolerance."
            pitchAccuracy < 80 ->
                "Isolate hands or enable Smart Wait Mode to lock in fingerings before tempo ramping."
            else -> "Focused classical phrasing."
        }

        return ExpressiveGradeReport(
            overallScore = overallScore,
            pitchAccuracy = pitchAccuracy,
            rhythmicPrecision = rhythmicPrecision,
            dynamicFidelity = dynamicFidelity,
            pedalingCleanliness = pedalingCleanliness,
            matchedNotes = matchedCount,
            totalExpectedNotes = expected.size,
            extraWrongNotes = extraWrongNotes,
            harmonicChangesEvaluated = harmonicChangesEvaluated,
            cleanPedalChanges = cleanPedalChanges,
            summary = summary
        )
    }
}
