package io.github.myangprint.motionflow.core

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class MotionMetricsTest {
    @Test
    fun `MF-P-006 ring buffer remains bounded and ordered`() {
        val buffer = LongRingBuffer(3)
        (1L..5L).forEach(buffer::add)
        assertEquals(3, buffer.size)
        assertContentEquals(longArrayOf(3L, 4L, 5L), buffer.snapshot())
    }

    @Test
    fun `MF-E-010 percentile uses nearest-rank calculation`() {
        val buffer = LongRingBuffer(10)
        (1L..10L).forEach(buffer::add)
        assertEquals(5L, buffer.percentile(0.50))
        assertEquals(10L, buffer.percentile(0.95))
    }
}
