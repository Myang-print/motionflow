package io.github.myangprint.motionflow.core

import com.intellij.openapi.Disposable
import java.util.concurrent.atomic.AtomicLong

interface MotionOwner : Disposable {
    val motionDisposed: Boolean
}

enum class EffectType(val spatial: Boolean, val essential: Boolean) {
    CARET_TRAIL(spatial = true, essential = false),
    CLICK_RIPPLE(spatial = true, essential = false),
    NAVIGATION_PULSE(spatial = false, essential = true),
    EDITOR_CONTEXT(spatial = false, essential = true),
    TOOL_WINDOW_FOCUS(spatial = false, essential = true),
    INACTIVE_FRAME_DIM(spatial = false, essential = false),
    PREVIEW(spatial = true, essential = false),
}

data class EffectKey(
    val ownerToken: Long,
    val surfaceToken: Long,
    val type: EffectType,
    val channel: String = "default",
)

data class MotionSpec(
    val durationMillis: Int,
    val easing: EasingId = EasingId.STANDARD,
    val essential: Boolean = false,
) {
    init {
        require(durationMillis >= 0) { "durationMillis must be non-negative" }
    }
}

enum class CancelReason {
    REPLACED,
    POLICY_CHANGED,
    OWNER_DISPOSED,
    CAPACITY,
    FAILED,
    PLUGIN_DISPOSED,
    EXPLICIT,
}

enum class TimelineState {
    RUNNING,
    FINISHED,
    CANCELLED,
}

data class TimelineSample(
    val value: Double,
    val progress: Double,
    val finished: Boolean,
)

object OwnerTokens {
    private val next = AtomicLong(1L)

    fun create(): Long = next.getAndIncrement()
}
