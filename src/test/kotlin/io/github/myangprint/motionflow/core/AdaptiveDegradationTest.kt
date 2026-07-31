package io.github.myangprint.motionflow.core

import kotlin.test.Test
import kotlin.test.assertEquals

class AdaptiveDegradationTest {
    @Test
    fun `MF-P-008 degradation requires sustained over-budget samples and recovers with hysteresis`() {
        val degradation = AdaptiveDegradation()
        repeat(119) { degradation.record(3_000_000L) }
        assertEquals(DegradationLevel.NORMAL, degradation.level)
        degradation.record(3_000_000L)
        assertEquals(DegradationLevel.SIMPLIFIED, degradation.level)
        repeat(120) { degradation.record(3_000_000L) }
        assertEquals(DegradationLevel.NONESSENTIAL_PAUSED, degradation.level)
        repeat(600) { degradation.record(500_000L) }
        assertEquals(DegradationLevel.SIMPLIFIED, degradation.level)
        repeat(600) { degradation.record(500_000L) }
        assertEquals(DegradationLevel.NORMAL, degradation.level)
    }
}
