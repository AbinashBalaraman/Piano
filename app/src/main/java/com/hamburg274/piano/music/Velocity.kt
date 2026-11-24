package com.hamburg274.piano.music

import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

enum class TouchVelocityMode(val title: String, val description: String) {
    Y_POSITION(
        title = "Vertical Y-Position Velocity",
        description = "Hinge (top) yields ppp; front ivory edge yields fff."
    ),
    PRESSURE_SIZE(
        title = "Touch-Size / Pressure Fusion",
        description = "Calibrated via Android MotionEvent pressure & contact area."
    ),
    FIXED(
        title = "Fixed Velocity",
        description = "Consistent volume for absolute beginners."
    )
}

data class VelocityCurvePoint(
    val x: Int, // 0..127 input
    val y: Int  // 1..127 output
)

data class VelocityCurvePreset(
    val id: String,
    val name: String,
    val description: String,
    val points: List<VelocityCurvePoint>
)

object VelocityEngine {
    val PRESETS: List<VelocityCurvePreset> = listOf(
        VelocityCurvePreset(
            id = "concert",
            name = "Concert Grand",
            description = "Balanced classical S-curve with nuanced pp control and commanding fff headroom.",
            points = listOf(
                VelocityCurvePoint(0, 8),
                VelocityCurvePoint(32, 26),
                VelocityCurvePoint(64, 62),
                VelocityCurvePoint(96, 100),
                VelocityCurvePoint(127, 127)
            )
        ),
        VelocityCurvePreset(
            id = "linear",
            name = "Linear 1:1",
            description = "Direct unmodified velocity mapping.",
            points = listOf(
                VelocityCurvePoint(0, 1),
                VelocityCurvePoint(32, 32),
                VelocityCurvePoint(64, 64),
                VelocityCurvePoint(96, 96),
                VelocityCurvePoint(127, 127)
            )
        ),
        VelocityCurvePreset(
            id = "light",
            name = "Light Touch",
            description = "Reaches forte effortlessly; ideal for soft touchscreen taps.",
            points = listOf(
                VelocityCurvePoint(0, 14),
                VelocityCurvePoint(32, 48),
                VelocityCurvePoint(64, 82),
                VelocityCurvePoint(96, 112),
                VelocityCurvePoint(127, 127)
            )
        ),
        VelocityCurvePreset(
            id = "heavy",
            name = "Heavy Weighted",
            description = "Expanded pianissimo zone requiring deliberate strikes for fortissimo.",
            points = listOf(
                VelocityCurvePoint(0, 4),
                VelocityCurvePoint(32, 18),
                VelocityCurvePoint(64, 46),
                VelocityCurvePoint(96, 84),
                VelocityCurvePoint(127, 127)
            )
        ),
        VelocityCurvePreset(
            id = "pianissimo",
            name = "Nocturne",
            description = "Maximizes subtle dynamic shading for Chopin, Debussy, and Satie.",
            points = listOf(
                VelocityCurvePoint(0, 4),
                VelocityCurvePoint(32, 20),
                VelocityCurvePoint(64, 48),
                VelocityCurvePoint(96, 78),
                VelocityCurvePoint(127, 112)
            )
        )
    )

    fun computeRawTouchVelocity(
        yRatio: Float,
        pressure: Float?,
        contactSizePx: Float?,
        mode: TouchVelocityMode,
        fixedVelocity: Int
    ): Int {
        val clampedY = yRatio.coerceIn(0f, 1f).toDouble()
        return when (mode) {
            TouchVelocityMode.FIXED -> fixedVelocity.coerceIn(1, 127)
            TouchVelocityMode.PRESSURE_SIZE -> {
                val hasPressure = pressure != null && pressure > 0.01f && pressure < 0.999f
                val pressureNorm = if (hasPressure) pressure!!.coerceIn(0f, 1f).toDouble() else clampedY
                val hasSize = contactSizePx != null && contactSizePx > 1f
                val sizeNorm = if (hasSize) {
                    ((sqrt(contactSizePx!!.toDouble()) - 4.0) / 26.0).coerceIn(0.0, 1.0)
                } else {
                    clampedY
                }
                val fused = when {
                    hasPressure && hasSize -> pressureNorm * 0.55 + sizeNorm * 0.30 + clampedY * 0.15
                    hasSize -> sizeNorm * 0.65 + clampedY * 0.35
                    else -> pressureNorm * 0.70 + clampedY * 0.30
                }
                (14.0 + fused * 112.0).roundToInt().coerceIn(1, 127)
            }
            TouchVelocityMode.Y_POSITION -> {
                val shaped = clampedY.pow(0.92)
                (14.0 + shaped * 112.0).roundToInt().coerceIn(1, 127)
            }
        }
    }

    fun applyVelocityCurve(rawVelocity: Int, points: List<VelocityCurvePoint>): Int {
        if (rawVelocity <= 0) return 0
        val clamped = rawVelocity.coerceIn(1, 127)
        if (points.size < 2) return clamped

        val sorted = points.sortedBy { it.x }
        if (clamped <= sorted.first().x) return sorted.first().y.coerceIn(1, 127)
        if (clamped >= sorted.last().x) return sorted.last().y.coerceIn(1, 127)

        for (i in 0 until sorted.size - 1) {
            val p0 = sorted[i]
            val p1 = sorted[i + 1]
            if (clamped in p0.x..p1.x) {
                val span = p1.x - p0.x
                if (span <= 0) return p1.y.coerceIn(1, 127)
                val t = (clamped - p0.x).toDouble() / span.toDouble()
                val y = p0.y + t * (p1.y - p0.y)
                return y.roundToInt().coerceIn(1, 127)
            }
        }
        return clamped
    }

    fun velocityToDynamicMarking(velocity: Int): String = when {
        velocity <= 8 -> "silent"
        velocity <= 22 -> "ppp"
        velocity <= 38 -> "pp"
        velocity <= 54 -> "p"
        velocity <= 70 -> "mp"
        velocity <= 86 -> "mf"
        velocity <= 102 -> "f"
        velocity <= 116 -> "ff"
        else -> "fff"
    }
}
