package io.github.myangprint.motionflow.editor

import java.awt.geom.Point2D
import kotlin.test.Test
import kotlin.test.assertEquals

class CaretTrailSampleBufferTest {
    @Test
    fun `MF-ED-015 every movement replaces stale history with the exact previous caret`() {
        val buffer = CaretTrailSampleBuffer(capacity = 12)
        buffer.startAt(Point2D.Float(10f, 10f), 1L)
        buffer.append(Point2D.Float(50f, 10f), 2L)

        val exactPreviousCaret = Point2D.Float(90f, 35f)
        buffer.startAt(exactPreviousCaret, 3L)

        val snapshot = buffer.snapshot()
        assertEquals(1, snapshot.size)
        assertEquals(exactPreviousCaret, snapshot.single().point)
    }

    @Test
    fun `MF-ED-015 frame samples remain bounded and preserve the newest head`() {
        val buffer = CaretTrailSampleBuffer(capacity = 5)
        buffer.startAt(Point2D.Float(0f, 0f), 0L)
        (1..10).forEach { index ->
            buffer.append(Point2D.Float(index.toFloat(), 0f), index.toLong(), maximum = 5)
        }

        val snapshot = buffer.snapshot()
        assertEquals(5, snapshot.size)
        assertEquals(Point2D.Float(10f, 0f), snapshot.last().point)
    }
}
