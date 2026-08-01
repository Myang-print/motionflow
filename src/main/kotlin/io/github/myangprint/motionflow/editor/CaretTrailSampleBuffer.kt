package io.github.myangprint.motionflow.editor

import java.awt.geom.Point2D
import kotlin.math.hypot

internal data class TimedTrailPoint(
    val point: Point2D.Float,
    val createdAtNanos: Long,
)

/** EDT-confined bounded history for one visual caret movement. */
internal class CaretTrailSampleBuffer(
    private val capacity: Int,
) {
    private val samples = ArrayDeque<TimedTrailPoint>(capacity)

    init {
        require(capacity >= 2) { "Caret trail capacity must be at least two samples" }
    }

    val isEmpty: Boolean
        get() = samples.isEmpty()

    fun startAt(origin: Point2D.Float, nowNanos: Long) {
        samples.clear()
        samples.addLast(TimedTrailPoint(origin.copy(), nowNanos))
    }

    fun append(point: Point2D.Float, nowNanos: Long, maximum: Int = capacity): Boolean {
        val previous = samples.lastOrNull()?.point
        if (
            previous != null &&
            hypot((point.x - previous.x).toDouble(), (point.y - previous.y).toDouble()) < MIN_SAMPLE_DISTANCE
        ) return false

        samples.addLast(TimedTrailPoint(point.copy(), nowNanos))
        trimTo(maximum)
        return true
    }

    fun trimExpired(nowNanos: Long, maximumAgeNanos: Long) {
        while (samples.isNotEmpty() && nowNanos - samples.first().createdAtNanos >= maximumAgeNanos) {
            samples.removeFirst()
        }
    }

    fun snapshot(limit: Int = capacity): List<TimedTrailPoint> = samples.takeLast(limit.coerceIn(2, capacity))

    fun clear() {
        samples.clear()
    }

    private fun trimTo(requestedMaximum: Int) {
        val maximum = requestedMaximum.coerceIn(2, capacity)
        while (samples.size > maximum) samples.removeFirst()
    }

    private fun Point2D.Float.copy(): Point2D.Float = Point2D.Float(x, y)

    private companion object {
        const val MIN_SAMPLE_DISTANCE = 0.55
    }
}
