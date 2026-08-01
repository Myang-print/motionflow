package io.github.myangprint.motionflow.editor

import io.github.myangprint.motionflow.ui.MotionPalette
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics2D
import java.awt.LinearGradientPaint
import java.awt.geom.Path2D
import java.awt.geom.Point2D
import kotlin.math.roundToInt

internal object CaretTrailPainter {
    fun paintSpeedLine(
        graphics: Graphics2D,
        ribbon: CaretTrailRibbon,
        accent: Color,
        alpha: Float,
    ) {
        val boundedAlpha = alpha.coerceIn(0f, 0.82f)
        if (boundedAlpha <= 0f) return
        graphics.paint = LinearGradientPaint(
            ribbon.gradientStart,
            ribbon.gradientEnd,
            GRADIENT_FRACTIONS,
            arrayOf(
                MotionPalette.withAlpha(accent, 0.0),
                MotionPalette.withAlpha(accent, boundedAlpha * 0.16),
                MotionPalette.withAlpha(accent, boundedAlpha * 0.62),
                MotionPalette.withAlpha(accent, boundedAlpha.toDouble()),
            ),
        )
        graphics.fill(outlinePath(ribbon))
    }

    fun paintSilkyTrail(
        graphics: Graphics2D,
        ribbon: CaretTrailRibbon,
        accent: Color,
        alpha: Float,
    ) {
        val boundedAlpha = alpha.coerceIn(0f, 0.90f)
        if (boundedAlpha <= 0f) return
        graphics.paint = LinearGradientPaint(
            ribbon.gradientStart,
            ribbon.gradientEnd,
            SILK_GRADIENT_FRACTIONS,
            arrayOf(
                MotionPalette.withAlpha(accent, 0.0),
                MotionPalette.withAlpha(accent, boundedAlpha * 0.24),
                MotionPalette.withAlpha(accent, boundedAlpha * 0.72),
                MotionPalette.withAlpha(accent, boundedAlpha.toDouble()),
            ),
        )
        graphics.fill(outlinePath(ribbon))

        graphics.stroke = BasicStroke(
            (ribbon.caretHeight * 0.09f).coerceIn(1.4f, 2.6f),
            BasicStroke.CAP_ROUND,
            BasicStroke.JOIN_ROUND,
        )
        graphics.paint = LinearGradientPaint(
            ribbon.gradientStart,
            ribbon.gradientEnd,
            SILK_GRADIENT_FRACTIONS,
            arrayOf(
                MotionPalette.withAlpha(accent, 0.0),
                MotionPalette.withAlpha(accent, boundedAlpha * 0.12),
                MotionPalette.withAlpha(accent, boundedAlpha * 0.58),
                MotionPalette.withAlpha(accent, boundedAlpha * 0.88),
            ),
        )
        graphics.draw(smoothCenterPath(ribbon.centerLine))
    }

    fun paintRoundedSegment(
        graphics: Graphics2D,
        from: Point2D.Float,
        to: Point2D.Float,
        caretHeight: Float,
        accent: Color,
        alpha: Float,
    ) {
        graphics.stroke = BasicStroke(
            CaretFlowGeometry.trailThickness(caretHeight),
            BasicStroke.CAP_ROUND,
            BasicStroke.JOIN_ROUND,
        )
        graphics.color = MotionPalette.withAlpha(accent, alpha.coerceIn(0f, 0.76f).toDouble())
        graphics.drawLine(from.x.roundToInt(), from.y.roundToInt(), to.x.roundToInt(), to.y.roundToInt())
    }

    fun paintHighContrastSegment(
        graphics: Graphics2D,
        from: Point2D.Float,
        to: Point2D.Float,
        caretHeight: Float,
        accent: Color,
    ) {
        graphics.stroke = BasicStroke(
            CaretFlowGeometry.trailThickness(caretHeight, highContrast = true),
            BasicStroke.CAP_BUTT,
            BasicStroke.JOIN_MITER,
        )
        graphics.color = accent
        graphics.drawLine(from.x.roundToInt(), from.y.roundToInt(), to.x.roundToInt(), to.y.roundToInt())
    }

    fun paintCaretHead(
        graphics: Graphics2D,
        center: Point2D.Float,
        caretHeight: Float,
        accent: Color,
        alpha: Float,
    ) {
        val normalizedHeight = CaretFlowGeometry.normalizeCaretHeight(caretHeight)
        val headWidth = (normalizedHeight * 0.12f).coerceIn(2f, 4f)
        graphics.color = MotionPalette.withAlpha(accent, alpha.coerceIn(0f, 0.82f).toDouble())
        graphics.fillRect(
            (center.x - headWidth / 2f).roundToInt(),
            (center.y - normalizedHeight / 2f).roundToInt(),
            headWidth.roundToInt().coerceAtLeast(2),
            normalizedHeight.roundToInt().coerceAtLeast(1),
        )
    }

    private fun outlinePath(ribbon: CaretTrailRibbon): Path2D.Float =
        Path2D.Float(Path2D.WIND_NON_ZERO, ribbon.outline.size).apply {
            ribbon.outline.forEachIndexed { index, point ->
                if (index == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
            }
            closePath()
        }

    private fun smoothCenterPath(points: List<Point2D.Float>): Path2D.Float = Path2D.Float().apply {
        if (points.isEmpty()) return@apply
        moveTo(points.first().x, points.first().y)
        if (points.size == 2) {
            lineTo(points.last().x, points.last().y)
            return@apply
        }
        for (index in 1 until points.lastIndex) {
            val control = points[index]
            val next = points[index + 1]
            val middleX = (control.x + next.x) / 2f
            val middleY = (control.y + next.y) / 2f
            quadTo(control.x, control.y, middleX, middleY)
        }
        lineTo(points.last().x, points.last().y)
    }

    private val GRADIENT_FRACTIONS = floatArrayOf(0f, 0.48f, 0.82f, 1f)
    private val SILK_GRADIENT_FRACTIONS = floatArrayOf(0f, 0.38f, 0.78f, 1f)
}
