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
        ribbon: SpeedLineRibbon,
        accent: Color,
        alpha: Float,
    ) {
        val boundedAlpha = alpha.coerceIn(0f, 0.82f)
        if (boundedAlpha <= 0f) return
        val path = Path2D.Float(Path2D.WIND_NON_ZERO, ribbon.outline.size)
        ribbon.outline.forEachIndexed { index, point ->
            if (index == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
        }
        path.closePath()

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
        graphics.fill(path)
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

    private val GRADIENT_FRACTIONS = floatArrayOf(0f, 0.48f, 0.82f, 1f)
}
