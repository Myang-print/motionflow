package io.github.myangprint.motionflow.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ScalarTimelineTest {
    @Test
    fun `MF-E-001 and MF-E-009 progress depends on elapsed monotonic time not frame count`() {
        val timeline = ScalarTimeline(1_000L, 100, 0.0, 10.0, EasingId.LINEAR)
        assertEquals(0.0, timeline.sample(0L).value, 1e-9)
        assertEquals(5.0, timeline.sample(50_001_000L).value, 1e-9)
        val late = timeline.sample(200_001_000L)
        assertEquals(10.0, late.value, 1e-9)
        assertTrue(late.finished)
    }

    @Test
    fun `MF-E-006 retarget starts from current interpolated value`() {
        val timeline = ScalarTimeline(0L, 100, 0.0, 10.0, EasingId.LINEAR)
        val current = timeline.retarget(50_000_000L, 20.0, 100, EasingId.LINEAR)
        assertEquals(5.0, current, 1e-9)
        assertEquals(5.0, timeline.sample(50_000_000L).value, 1e-9)
        assertEquals(12.5, timeline.sample(100_000_000L).value, 1e-9)
        assertEquals(20.0, timeline.sample(150_000_000L).value, 1e-9)
    }

    @Test
    fun `MF-E-005 cancellation is idempotent`() {
        val timeline = ScalarTimeline(0L, 100, 0.0, 1.0, EasingId.LINEAR)
        timeline.cancel()
        timeline.cancel()
        assertEquals(TimelineState.CANCELLED, timeline.state)
        assertTrue(timeline.sample(100_000_000L).finished)
    }
}
