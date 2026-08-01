package io.github.myangprint.motionflow.policy

import io.github.myangprint.motionflow.core.EffectType
import io.github.myangprint.motionflow.settings.MotionFlowState
import io.github.myangprint.motionflow.settings.MotionProfileId
import io.github.myangprint.motionflow.settings.MotionStateNormalizer
import io.github.myangprint.motionflow.settings.PowerSaveBehavior
import io.github.myangprint.motionflow.settings.CaretTrailStyle
import kotlin.math.roundToInt

data class MotionEnvironment(
    val headless: Boolean = false,
    val unitTest: Boolean = false,
    val powerSave: Boolean = false,
    val highContrast: Boolean = false,
    val nativeSmoothCaretEra: Boolean = false,
)

data class MotionPolicy(
    val enabled: Boolean,
    val profile: MotionProfileId,
    val reducedMotion: Boolean,
    val powerSave: Boolean,
    val powerSaveLimitsMotion: Boolean,
    val highContrast: Boolean,
    val nativeSmoothCaretEra: Boolean,
    val durationScale: Double,
    val intensityScale: Double,
    val caretTrailLength: Int,
    val caretTrailStyle: CaretTrailStyle,
    val navigationJumpThreshold: Int,
    val inactiveFrameDimAmount: Double,
    val effects: Map<EffectType, Boolean>,
) {
    fun effectEnabled(type: EffectType): Boolean = enabled && effects[type] == true

    fun duration(baseMillis: Int, type: EffectType): Int {
        if (!effectEnabled(type)) return 0
        if ((reducedMotion || powerSaveLimitsMotion) && type.essential) return minOf(80, baseMillis)
        return (baseMillis * durationScale).roundToInt().coerceIn(1, 2_000)
    }

    companion object {
        val DISABLED = MotionPolicy(
            enabled = false,
            profile = MotionProfileId.BALANCED,
            reducedMotion = false,
            powerSave = false,
            powerSaveLimitsMotion = false,
            highContrast = false,
            nativeSmoothCaretEra = false,
            durationScale = 1.0,
            intensityScale = 1.0,
            caretTrailLength = 7,
            caretTrailStyle = CaretTrailStyle.SILKY_TRAIL,
            navigationJumpThreshold = 8,
            inactiveFrameDimAmount = 0.10,
            effects = EffectType.entries.associateWith { false },
        )
    }
}

object MotionPolicyResolver {
    fun resolve(rawState: MotionFlowState, environment: MotionEnvironment): MotionPolicy {
        val state = MotionStateNormalizer.normalize(rawState).state
        val profile = MotionProfileId.valueOf(state.profile)
        val powerBehavior = PowerSaveBehavior.valueOf(state.powerSaveBehavior)
        val globallyEnabled = state.enabled && !environment.headless && !environment.unitTest

        var trail = state.caretTrailEnabled
        var ripple = state.clickRippleEnabled
        var pulse = state.navigationPulseEnabled
        var context = state.editorContextCueEnabled
        var toolWindow = state.toolWindowCueEnabled
        var dim = state.inactiveFrameDimEnabled
        var preview = globallyEnabled

        if (state.reducedMotion) {
            trail = false
            ripple = false
            dim = false
        }
        if (environment.powerSave) {
            when (powerBehavior) {
                PowerSaveBehavior.DISABLE_ALL -> {
                    trail = false
                    ripple = false
                    pulse = false
                    context = false
                    toolWindow = false
                    dim = false
                    preview = false
                }
                PowerSaveBehavior.ESSENTIAL_ONLY -> {
                    trail = false
                    ripple = false
                    dim = false
                }
                PowerSaveBehavior.IGNORE -> Unit
            }
        }
        if (environment.highContrast) {
            ripple = false
            dim = false
        }
        if (!globallyEnabled) {
            trail = false
            ripple = false
            pulse = false
            context = false
            toolWindow = false
            dim = false
            preview = false
        }

        return MotionPolicy(
            enabled = globallyEnabled,
            profile = profile,
            reducedMotion = state.reducedMotion,
            powerSave = environment.powerSave,
            powerSaveLimitsMotion = environment.powerSave && powerBehavior != PowerSaveBehavior.IGNORE,
            highContrast = environment.highContrast,
            nativeSmoothCaretEra = environment.nativeSmoothCaretEra,
            durationScale = state.durationScale,
            intensityScale = state.intensityScale,
            caretTrailLength = state.caretTrailLength,
            caretTrailStyle = CaretTrailStyle.valueOf(state.caretTrailStyle),
            navigationJumpThreshold = state.navigationJumpThreshold,
            inactiveFrameDimAmount = state.inactiveFrameDimAmount,
            effects = mapOf(
                EffectType.CARET_TRAIL to trail,
                EffectType.CLICK_RIPPLE to ripple,
                EffectType.NAVIGATION_PULSE to pulse,
                EffectType.EDITOR_CONTEXT to context,
                EffectType.TOOL_WINDOW_FOCUS to toolWindow,
                EffectType.INACTIVE_FRAME_DIM to dim,
                EffectType.PREVIEW to preview,
            ),
        )
    }
}
