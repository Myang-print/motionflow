package io.github.myangprint.motionflow.policy

import io.github.myangprint.motionflow.core.EffectType
import io.github.myangprint.motionflow.settings.MotionFlowState
import io.github.myangprint.motionflow.settings.PowerSaveBehavior
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MotionPolicyTest {
    @Test
    fun `MF-A-002 reduced motion disables spatial effects and caps orientation cues`() {
        val policy = MotionPolicyResolver.resolve(
            MotionFlowState(reducedMotion = true, inactiveFrameDimEnabled = true),
            MotionEnvironment(),
        )
        assertFalse(policy.effectEnabled(EffectType.CARET_TRAIL))
        assertFalse(policy.effectEnabled(EffectType.CLICK_RIPPLE))
        assertFalse(policy.effectEnabled(EffectType.INACTIVE_FRAME_DIM))
        assertTrue(policy.effectEnabled(EffectType.NAVIGATION_PULSE))
        assertEquals(80, policy.duration(180, EffectType.NAVIGATION_PULSE))
    }

    @Test
    fun `MF-P-009 Power Save disable-all overrides user settings`() {
        val policy = MotionPolicyResolver.resolve(
            MotionFlowState(powerSaveBehavior = PowerSaveBehavior.DISABLE_ALL.name),
            MotionEnvironment(powerSave = true),
        )
        EffectType.entries.forEach { assertFalse(policy.effectEnabled(it)) }
    }

    @Test
    fun `MF-P-009 Power Save essential-only preserves orientation cues`() {
        val policy = MotionPolicyResolver.resolve(
            MotionFlowState(powerSaveBehavior = PowerSaveBehavior.ESSENTIAL_ONLY.name),
            MotionEnvironment(powerSave = true),
        )
        assertFalse(policy.effectEnabled(EffectType.CARET_TRAIL))
        assertTrue(policy.effectEnabled(EffectType.EDITOR_CONTEXT))
        assertEquals(80, policy.duration(200, EffectType.EDITOR_CONTEXT))
    }

    @Test
    fun `MF-P-009 Power Save ignore preserves configured timing`() {
        val policy = MotionPolicyResolver.resolve(
            MotionFlowState(powerSaveBehavior = PowerSaveBehavior.IGNORE.name),
            MotionEnvironment(powerSave = true),
        )
        assertTrue(policy.effectEnabled(EffectType.CARET_TRAIL))
        assertTrue(policy.effectEnabled(EffectType.EDITOR_CONTEXT))
        assertEquals(200, policy.duration(200, EffectType.EDITOR_CONTEXT))
    }

    @Test
    fun `MF-C-005 headless and unit test environments disable all runtime effects`() {
        val headless = MotionPolicyResolver.resolve(MotionFlowState(), MotionEnvironment(headless = true))
        val test = MotionPolicyResolver.resolve(MotionFlowState(), MotionEnvironment(unitTest = true))
        assertFalse(headless.enabled)
        assertFalse(test.enabled)
    }
}
