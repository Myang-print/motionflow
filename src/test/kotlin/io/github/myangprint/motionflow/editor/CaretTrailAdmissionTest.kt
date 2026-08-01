package io.github.myangprint.motionflow.editor

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class CaretTrailAdmissionTest {
    @Test
    fun `MF-ED-002 all-carets policy keeps the primary first and caps work at eight`() {
        val primary = Any()
        val secondary = List(12) { Any() }

        val selected = CaretTrailAdmission.select(
            primary = primary,
            allCarets = secondary + primary,
            includeSecondary = true,
            limit = 8,
        )

        assertEquals(8, selected.size)
        assertSame(primary, selected.first())
        assertEquals(8, selected.distinctBy(System::identityHashCode).size)
    }

    @Test
    fun `MF-ED-016 primary-only coverage remains selectable`() {
        val primary = Any()
        val selected = CaretTrailAdmission.select(primary, List(4) { Any() }, false, 8)

        assertEquals(1, selected.size)
        assertSame(primary, selected.single())
    }

    @Test
    fun `MF-ED-002 admitted carets retain independent exact-origin buffers`() {
        val buffers = List(3) { CaretTrailSampleBuffer(4) }
        buffers.forEachIndexed { index, buffer ->
            buffer.startAt(java.awt.geom.Point2D.Float(index.toFloat(), 10f), index.toLong())
        }

        assertTrue(buffers.map { it.snapshot().single().point.x }.toSet().size == 3)
    }
}
