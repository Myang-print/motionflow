package io.github.myangprint.motionflow.editor

import java.awt.Color
import java.awt.RenderingHints
import java.awt.geom.Point2D
import java.awt.image.BufferedImage
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CaretTrailPainterTest {
    @Test
    fun `MF-ED-014 speed line color and alpha increase toward the caret`() {
        val ribbon = assertNotNull(
            CaretFlowGeometry.speedLineRibbon(
                listOf(Point2D.Float(10f, 30f), Point2D.Float(130f, 30f)),
                caretHeight = 20f,
            ),
        )
        val image = BufferedImage(150, 60, BufferedImage.TYPE_INT_ARGB)
        val graphics = image.createGraphics()
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            CaretTrailPainter.paintSpeedLine(graphics, ribbon, Color(0x4A, 0x8D, 0xFF), 0.8f)
        } finally {
            graphics.dispose()
        }

        val tailAlpha = Color(image.getRGB(34, 30), true).alpha
        val middleAlpha = Color(image.getRGB(75, 30), true).alpha
        val headAlpha = Color(image.getRGB(124, 30), true).alpha
        assertTrue(tailAlpha < middleAlpha, "Tail must fade toward transparency")
        assertTrue(middleAlpha < headAlpha, "Gradient must strengthen toward the caret")
        assertTrue(Color(image.getRGB(5, 30), true).alpha == 0, "No rounded cap may extend behind the origin")
    }
}
