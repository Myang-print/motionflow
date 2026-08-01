package io.github.myangprint.motionflow.settings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MotionStateNormalizerTest {
    @Test
    fun `MF-F-008 invalid persisted values are clamped to documented ranges`() {
        val normalized = MotionStateNormalizer.normalize(
            MotionFlowState(
                schemaVersion = -1,
                profile = "UNKNOWN",
                powerSaveBehavior = "UNKNOWN",
                durationScale = Double.NaN,
                intensityScale = 9.0,
                caretTrailLength = 100,
                navigationJumpThreshold = -5,
                inactiveFrameDimAmount = 1.0,
            ),
        )
        assertEquals(CURRENT_SCHEMA_VERSION, normalized.state.schemaVersion)
        assertEquals(MotionProfileId.BALANCED.name, normalized.state.profile)
        assertEquals(PowerSaveBehavior.DISABLE_ALL.name, normalized.state.powerSaveBehavior)
        assertEquals(1.0, normalized.state.durationScale)
        assertEquals(1.5, normalized.state.intensityScale)
        assertEquals(12, normalized.state.caretTrailLength)
        assertEquals(3, normalized.state.navigationJumpThreshold)
        assertEquals(0.25, normalized.state.inactiveFrameDimAmount)
        assertTrue(normalized.correctedFields.size >= 8)
    }

    @Test
    fun `MF-F-002 named profiles provide stable product defaults`() {
        assertEquals(0.75, MotionStateNormalizer.defaults(MotionProfileId.SUBTLE).durationScale)
        assertEquals(1.0, MotionStateNormalizer.defaults(MotionProfileId.BALANCED).durationScale)
        assertEquals(1.20, MotionStateNormalizer.defaults(MotionProfileId.EXPRESSIVE).durationScale)
        assertTrue(MotionStateNormalizer.defaults(MotionProfileId.EXPRESSIVE).inactiveFrameDimEnabled)
    }
}
