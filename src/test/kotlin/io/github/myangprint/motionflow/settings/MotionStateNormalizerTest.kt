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
                caretTrailStyle = "UNKNOWN",
                caretTrailCoverage = "UNKNOWN",
                caretTrailVisibility = "UNKNOWN",
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
        assertEquals(CaretTrailStyle.SILKY_TRAIL.name, normalized.state.caretTrailStyle)
        assertEquals(CaretTrailCoverage.ALL_CARETS.name, normalized.state.caretTrailCoverage)
        assertEquals(CaretTrailVisibility.CLEAR.name, normalized.state.caretTrailVisibility)
        assertEquals(1.0, normalized.state.durationScale)
        assertEquals(1.5, normalized.state.intensityScale)
        assertEquals(12, normalized.state.caretTrailLength)
        assertEquals(3, normalized.state.navigationJumpThreshold)
        assertEquals(0.25, normalized.state.inactiveFrameDimAmount)
        assertTrue(normalized.correctedFields.size >= 10)
    }

    @Test
    fun `MF-F-002 named profiles provide stable product defaults`() {
        assertEquals(0.75, MotionStateNormalizer.defaults(MotionProfileId.SUBTLE).durationScale)
        assertEquals(1.0, MotionStateNormalizer.defaults(MotionProfileId.BALANCED).durationScale)
        assertEquals(1.20, MotionStateNormalizer.defaults(MotionProfileId.EXPRESSIVE).durationScale)
        assertTrue(MotionStateNormalizer.defaults(MotionProfileId.EXPRESSIVE).inactiveFrameDimEnabled)
        assertEquals(CaretTrailStyle.SILKY_TRAIL.name, MotionStateNormalizer.defaults().caretTrailStyle)
        assertEquals(CaretTrailCoverage.ALL_CARETS.name, MotionStateNormalizer.defaults().caretTrailCoverage)
        assertEquals(CaretTrailVisibility.CLEAR.name, MotionStateNormalizer.defaults().caretTrailVisibility)
        assertEquals(CaretTrailCoverage.PRIMARY_ONLY.name, MotionStateNormalizer.defaults(MotionProfileId.SUBTLE).caretTrailCoverage)
        assertEquals(CaretTrailVisibility.SOFT.name, MotionStateNormalizer.defaults(MotionProfileId.SUBTLE).caretTrailVisibility)
        assertEquals(CaretTrailVisibility.BRIGHT.name, MotionStateNormalizer.defaults(MotionProfileId.EXPRESSIVE).caretTrailVisibility)
    }

    @Test
    fun `MF-ED-015 schema two speed-line default migrates to silky trail`() {
        val migrated = MotionStateNormalizer.normalize(
            MotionFlowState(schemaVersion = 2, caretTrailStyle = CaretTrailStyle.SPEED_LINES.name),
        )
        val explicitCurrent = MotionStateNormalizer.normalize(
            MotionFlowState(schemaVersion = CURRENT_SCHEMA_VERSION, caretTrailStyle = CaretTrailStyle.SPEED_LINES.name),
        )

        assertEquals(CaretTrailStyle.SILKY_TRAIL.name, migrated.state.caretTrailStyle)
        assertEquals(CaretTrailStyle.SPEED_LINES.name, explicitCurrent.state.caretTrailStyle)
    }

    @Test
    fun `MF-ED-016 schema three settings gain bounded all-carets and clear defaults`() {
        val migrated = MotionStateNormalizer.normalize(MotionFlowState(schemaVersion = 3))

        assertEquals(CURRENT_SCHEMA_VERSION, migrated.state.schemaVersion)
        assertEquals(CaretTrailCoverage.ALL_CARETS.name, migrated.state.caretTrailCoverage)
        assertEquals(CaretTrailVisibility.CLEAR.name, migrated.state.caretTrailVisibility)
    }
}
