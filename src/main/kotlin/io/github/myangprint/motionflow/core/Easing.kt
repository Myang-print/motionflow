package io.github.myangprint.motionflow.core

import kotlin.math.abs

enum class EasingId {
    LINEAR,
    STANDARD,
    EMPHASIZED_DECELERATE,
    EMPHASIZED_ACCELERATE;

    fun transform(progress: Double): Double {
        val input = progress.coerceIn(0.0, 1.0)
        return when (this) {
            LINEAR -> input
            STANDARD -> STANDARD_CURVE.transform(input)
            EMPHASIZED_DECELERATE -> DECELERATE_CURVE.transform(input)
            EMPHASIZED_ACCELERATE -> ACCELERATE_CURVE.transform(input)
        }
    }

    private companion object {
        val STANDARD_CURVE = CubicBezier(0.20, 0.0, 0.0, 1.0)
        val DECELERATE_CURVE = CubicBezier(0.05, 0.70, 0.10, 1.0)
        val ACCELERATE_CURVE = CubicBezier(0.30, 0.0, 0.80, 0.15)
    }
}

internal class CubicBezier(
    private val x1: Double,
    private val y1: Double,
    private val x2: Double,
    private val y2: Double,
) {
    init {
        require(x1 in 0.0..1.0 && x2 in 0.0..1.0) {
            "Cubic Bezier x control points must be in [0, 1]"
        }
    }

    fun transform(progress: Double): Double {
        val x = progress.coerceIn(0.0, 1.0)
        if (x == 0.0 || x == 1.0) return x

        var t = x
        repeat(8) {
            val error = coordinate(t, x1, x2) - x
            if (abs(error) < EPSILON) return coordinate(t, y1, y2).coerceIn(0.0, 1.0)
            val derivative = derivative(t, x1, x2)
            if (abs(derivative) < EPSILON) return@repeat
            t = (t - error / derivative).coerceIn(0.0, 1.0)
        }

        var lower = 0.0
        var upper = 1.0
        repeat(16) {
            t = (lower + upper) / 2.0
            if (coordinate(t, x1, x2) < x) lower = t else upper = t
        }
        return coordinate(t, y1, y2).coerceIn(0.0, 1.0)
    }

    private fun coordinate(t: Double, first: Double, second: Double): Double {
        val inverse = 1.0 - t
        return 3.0 * inverse * inverse * t * first +
            3.0 * inverse * t * t * second +
            t * t * t
    }

    private fun derivative(t: Double, first: Double, second: Double): Double {
        val inverse = 1.0 - t
        return 3.0 * inverse * inverse * first +
            6.0 * inverse * t * (second - first) +
            3.0 * t * t * (1.0 - second)
    }

    private companion object {
        const val EPSILON = 1e-7
    }
}
