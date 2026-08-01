package io.github.myangprint.motionflow.core

class ScalarTimeline(
    startNanos: Long,
    durationMillis: Int,
    startValue: Double,
    targetValue: Double,
    easing: EasingId,
) {
    var state: TimelineState = TimelineState.RUNNING
        private set

    private var startedAtNanos = startNanos
    private var durationNanos = durationMillis.toLong() * NANOS_PER_MILLI
    private var from = startValue
    private var target = targetValue
    private var easingId = easing

    fun sample(nowNanos: Long): TimelineSample {
        if (state == TimelineState.CANCELLED) {
            return TimelineSample(from, 0.0, finished = true)
        }
        if (state == TimelineState.FINISHED) {
            return TimelineSample(target, 1.0, finished = true)
        }

        val rawProgress = if (durationNanos == 0L) {
            1.0
        } else {
            (nowNanos - startedAtNanos).toDouble() / durationNanos.toDouble()
        }.coerceIn(0.0, 1.0)
        val eased = easingId.transform(rawProgress)
        val value = from + (target - from) * eased
        val finished = rawProgress >= 1.0
        if (finished) state = TimelineState.FINISHED
        return TimelineSample(value, rawProgress, finished)
    }

    fun retarget(
        nowNanos: Long,
        targetValue: Double,
        durationMillis: Int,
        easing: EasingId,
    ): Double {
        if (state != TimelineState.RUNNING) return sample(nowNanos).value
        val current = sample(nowNanos).value
        state = TimelineState.RUNNING
        startedAtNanos = nowNanos
        durationNanos = durationMillis.toLong() * NANOS_PER_MILLI
        from = current
        target = targetValue
        easingId = easing
        return current
    }

    fun cancel() {
        if (state == TimelineState.RUNNING) state = TimelineState.CANCELLED
    }

    private companion object {
        const val NANOS_PER_MILLI = 1_000_000L
    }
}
