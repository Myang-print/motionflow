package io.github.myangprint.motionflow.editor

import java.awt.geom.Point2D
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CaretFlowGeometryTest {
    @Test
    fun `MF-ED-001 standard trail thickness equals caret height`() {
        assertEquals(8f, CaretFlowGeometry.trailThickness(4f))
        assertEquals(20f, CaretFlowGeometry.trailThickness(20f))
        assertEquals(96f, CaretFlowGeometry.trailThickness(120f))
    }

    @Test
    fun `MF-A-006 high contrast keeps a short solid indicator`() {
        assertEquals(3f, CaretFlowGeometry.trailThickness(20f, highContrast = true))
    }

    @Test
    fun `MF-ED-013 split flow keeps its tail behind the animated caret`() {
        val frame = CaretFlowGeometry.transitionFrame(
            fromX = 0f,
            fromY = 10f,
            fromHeight = 16f,
            toX = 100f,
            toY = 50f,
            toHeight = 24f,
            progress = 0.5,
            intensityScale = 1.0,
        )

        assertEquals(50f, frame.headX)
        assertTrue(frame.tailX < frame.headX)
        assertEquals(20f, frame.caretHeight)
        assertTrue(frame.alpha > 0f)
    }

    @Test
    fun `MF-ED-013 split flow duration is distance aware and bounded`() {
        assertEquals(180, CaretFlowGeometry.transitionDurationMillis(0.0))
        assertTrue(CaretFlowGeometry.transitionDurationMillis(500.0) > 180)
        assertEquals(420, CaretFlowGeometry.transitionDurationMillis(10_000.0))
    }

    @Test
    fun `MF-ED-014 horizontal trail tapers to its origin and meets the caret at full height`() {
        val ribbon = assertNotNull(
            CaretFlowGeometry.trailRibbon(
                listOf(Point2D.Float(10f, 20f), Point2D.Float(110f, 20f)),
                caretHeight = 20f,
            ),
        )

        assertEquals(ribbon.gradientStart, ribbon.outline.first())
        assertEquals(ribbon.gradientStart, ribbon.outline.last())
        assertEquals(20.0, ribbon.headLeft.distance(ribbon.headRight), 0.001)
        assertEquals(20f, ribbon.headThickness)
        assertTrue(abs(signedArea(ribbon.outline)) > 100.0)
    }

    @Test
    fun `MF-ED-014 vertical trail projects to a narrow caret stem instead of an orthogonal bar`() {
        val horizontal = assertNotNull(
            CaretFlowGeometry.trailRibbon(
                listOf(Point2D.Float(0f, 0f), Point2D.Float(100f, 0f)),
                caretHeight = 20f,
            ),
        )
        val vertical = assertNotNull(
            CaretFlowGeometry.trailRibbon(
                listOf(Point2D.Float(0f, 0f), Point2D.Float(0f, 100f)),
                caretHeight = 20f,
            ),
        )
        val diagonal = assertNotNull(
            CaretFlowGeometry.trailRibbon(
                listOf(Point2D.Float(0f, 0f), Point2D.Float(100f, 100f)),
                caretHeight = 20f,
            ),
        )

        assertEquals(20f, horizontal.headThickness)
        assertTrue(vertical.headThickness in 2f..4f)
        assertTrue(diagonal.headThickness in vertical.headThickness..horizontal.headThickness)
        assertEquals(vertical.headThickness.toDouble(), vertical.headLeft.distance(vertical.headRight), 0.001)
    }

    @Test
    fun `MF-ED-013 projected trail stays nondegenerate from every side and diagonal`() {
        val destinations = listOf(
            Point2D.Float(100f, 0f),
            Point2D.Float(-100f, 0f),
            Point2D.Float(0f, 100f),
            Point2D.Float(0f, -100f),
            Point2D.Float(80f, 60f),
            Point2D.Float(-80f, 60f),
            Point2D.Float(80f, -60f),
            Point2D.Float(-80f, -60f),
        )

        destinations.forEach { destination ->
            val ribbon = assertNotNull(
                CaretFlowGeometry.trailRibbon(
                    listOf(Point2D.Float(0f, 0f), destination),
                    caretHeight = 18f,
                ),
            )
            assertEquals(ribbon.headThickness.toDouble(), hypot(
                (ribbon.headRight.x - ribbon.headLeft.x).toDouble(),
                (ribbon.headRight.y - ribbon.headLeft.y).toDouble(),
            ), 0.001)
            assertTrue(ribbon.headThickness in 2f..18f)
            assertTrue(abs(signedArea(ribbon.outline)) > 10.0, "Degenerate ribbon toward $destination")
        }
    }

    @Test
    fun `MF-ED-016 visibility width choices scale projected geometry monotonically`() {
        val soft = CaretFlowGeometry.projectedTrailThickness(100f, 0f, 20f, 0.86f)
        val clear = CaretFlowGeometry.projectedTrailThickness(100f, 0f, 20f, 1.10f)
        val bright = CaretFlowGeometry.projectedTrailThickness(100f, 0f, 20f, 1.20f)

        assertTrue(soft < clear)
        assertTrue(clear < bright)
    }

    @Test
    fun `MF-ED-013 lag follows the same arrival vector from every side`() {
        listOf(
            100f to 0f,
            -100f to 0f,
            0f to 100f,
            0f to -100f,
            80f to 60f,
            -80f to -60f,
        ).forEach { (toX, toY) ->
            val frame = CaretFlowGeometry.transitionFrame(
                fromX = 0f,
                fromY = 0f,
                fromHeight = 16f,
                toX = toX,
                toY = toY,
                toHeight = 16f,
                progress = 0.65,
                intensityScale = 1.0,
            )
            val directionDot = frame.headX * (frame.headX - frame.tailX) +
                frame.headY * (frame.headY - frame.tailY)
            assertTrue(directionDot > 0f, "Tail moved ahead of the caret toward ($toX, $toY)")
        }
    }

    @Test
    fun `MF-ED-015 local trail begins at the exact previous caret and settles at the destination`() {
        val from = Point2D.Float(18f, 24f)
        val to = Point2D.Float(118f, 64f)

        val start = CaretFlowGeometry.localTrailFrame(from, to, 0.0)
        val middle = CaretFlowGeometry.localTrailFrame(from, to, 0.31)
        val settled = CaretFlowGeometry.localTrailFrame(from, to, 0.62)
        val faded = CaretFlowGeometry.localTrailFrame(from, to, 1.0)

        assertEquals(from, start.point)
        assertEquals(1f, start.opacity)
        assertTrue(middle.point.x in from.x..to.x)
        assertTrue(middle.point.y in from.y..to.y)
        assertEquals(to, settled.point)
        assertEquals(1f, settled.opacity)
        assertEquals(to, faded.point)
        assertEquals(0f, faded.opacity)
    }

    @Test
    fun `MF-ED-015 local trail timing is distance aware and bounded`() {
        assertEquals(220, CaretFlowGeometry.localTrailDurationMillis(0.0))
        assertTrue(CaretFlowGeometry.localTrailDurationMillis(300.0) > 220)
        assertEquals(360, CaretFlowGeometry.localTrailDurationMillis(10_000.0))
    }

    private fun signedArea(points: List<Point2D.Float>): Double = points.indices.sumOf { index ->
        val current = points[index]
        val next = points[(index + 1) % points.size]
        current.x.toDouble() * next.y - next.x.toDouble() * current.y
    } / 2.0
}
