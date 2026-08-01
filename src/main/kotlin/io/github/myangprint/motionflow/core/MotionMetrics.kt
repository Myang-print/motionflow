package io.github.myangprint.motionflow.core

import kotlin.math.ceil

class LongRingBuffer(private val capacity: Int) {
    init {
        require(capacity > 0)
    }

    private val values = LongArray(capacity)
    private var nextIndex = 0
    var size: Int = 0
        private set

    fun add(value: Long) {
        values[nextIndex] = value
        nextIndex = (nextIndex + 1) % capacity
        if (size < capacity) size++
    }

    fun clear() {
        nextIndex = 0
        size = 0
    }

    fun snapshot(): LongArray {
        if (size == 0) return LongArray(0)
        val result = LongArray(size)
        val start = if (size == capacity) nextIndex else 0
        repeat(size) { index ->
            result[index] = values[(start + index) % capacity]
        }
        return result
    }

    fun percentile(percentile: Double): Long {
        require(percentile in 0.0..1.0)
        val sorted = snapshot().apply { sort() }
        if (sorted.isEmpty()) return 0L
        val index = (ceil(percentile * sorted.size).toInt() - 1).coerceIn(0, sorted.lastIndex)
        return sorted[index]
    }
}

data class MotionMetricsSnapshot(
    val activeTimelines: Int,
    val peakTimelines: Int,
    val submitted: Long,
    val retargeted: Long,
    val completed: Long,
    val cancelled: Long,
    val rejected: Long,
    val framesDropped: Long,
    val repaintRequests: Long,
    val repaintCalls: Long,
    val frameSamples: Int,
    val frameP50Nanos: Long,
    val frameP95Nanos: Long,
    val frameP99Nanos: Long,
)

class MotionMetrics {
    private val frameCosts = LongRingBuffer(512)
    private var active = 0
    private var peak = 0
    private var submitted = 0L
    private var retargeted = 0L
    private var completed = 0L
    private var cancelled = 0L
    private var rejected = 0L
    private var framesDropped = 0L
    private var repaintRequests = 0L
    private var repaintCalls = 0L

    fun timelineSubmitted(activeCount: Int) {
        submitted++
        active = activeCount
        peak = maxOf(peak, activeCount)
    }

    fun timelineRetargeted() {
        retargeted++
    }

    fun timelineCompleted(activeCount: Int) {
        completed++
        active = activeCount
    }

    fun timelineCancelled(activeCount: Int) {
        cancelled++
        active = activeCount
    }

    fun timelineRejected() {
        rejected++
    }

    fun frameDropped() {
        framesDropped++
    }

    fun repaintRequested() {
        repaintRequests++
    }

    fun repaintIssued() {
        repaintCalls++
    }

    fun recordFrameCost(nanos: Long) {
        frameCosts.add(nanos.coerceAtLeast(0L))
    }

    fun snapshot(activeCount: Int = active): MotionMetricsSnapshot = MotionMetricsSnapshot(
        activeTimelines = activeCount,
        peakTimelines = peak,
        submitted = submitted,
        retargeted = retargeted,
        completed = completed,
        cancelled = cancelled,
        rejected = rejected,
        framesDropped = framesDropped,
        repaintRequests = repaintRequests,
        repaintCalls = repaintCalls,
        frameSamples = frameCosts.size,
        frameP50Nanos = frameCosts.percentile(0.50),
        frameP95Nanos = frameCosts.percentile(0.95),
        frameP99Nanos = frameCosts.percentile(0.99),
    )
}
