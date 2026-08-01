package io.github.myangprint.motionflow.core

enum class DegradationLevel {
    NORMAL,
    SIMPLIFIED,
    NONESSENTIAL_PAUSED,
}

class AdaptiveDegradation {
    var level: DegradationLevel = DegradationLevel.NORMAL
        private set

    private val activeWindow = LongRingBuffer(WINDOW_SIZE)
    private var underBudgetSamples = 0

    fun record(frameCostNanos: Long): DegradationLevel {
        activeWindow.add(frameCostNanos)
        if (activeWindow.size < WINDOW_SIZE) return level

        val p95 = activeWindow.percentile(0.95)
        if (p95 > ENTER_THRESHOLD_NANOS) {
            underBudgetSamples = 0
            level = when (level) {
                DegradationLevel.NORMAL -> DegradationLevel.SIMPLIFIED
                DegradationLevel.SIMPLIFIED -> DegradationLevel.NONESSENTIAL_PAUSED
                DegradationLevel.NONESSENTIAL_PAUSED -> level
            }
            activeWindow.clear()
            return level
        }

        if (p95 < RECOVERY_THRESHOLD_NANOS) {
            underBudgetSamples += WINDOW_SIZE
            if (underBudgetSamples >= RECOVERY_SAMPLES) {
                level = when (level) {
                    DegradationLevel.NONESSENTIAL_PAUSED -> DegradationLevel.SIMPLIFIED
                    DegradationLevel.SIMPLIFIED -> DegradationLevel.NORMAL
                    DegradationLevel.NORMAL -> level
                }
                underBudgetSamples = 0
            }
        } else {
            underBudgetSamples = 0
        }
        activeWindow.clear()
        return level
    }

    fun reset() {
        level = DegradationLevel.NORMAL
        underBudgetSamples = 0
        activeWindow.clear()
    }

    private companion object {
        const val WINDOW_SIZE = 120
        const val RECOVERY_SAMPLES = 600
        const val ENTER_THRESHOLD_NANOS = 2_000_000L
        const val RECOVERY_THRESHOLD_NANOS = 1_000_000L
    }
}
