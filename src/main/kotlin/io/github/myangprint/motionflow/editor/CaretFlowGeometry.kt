package io.github.myangprint.motionflow.editor

import java.awt.geom.Point2D
import kotlin.math.hypot
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt

internal data class CaretFlowFrame(
    val headX: Float,
    val headY: Float,
    val tailX: Float,
    val tailY: Float,
    val caretHeight: Float,
    val alpha: Float,
)

internal data class CaretTrailRibbon(
    val outline: List<Point2D.Float>,
    val centerLine: List<Point2D.Float>,
    val gradientStart: Point2D.Float,
    val gradientEnd: Point2D.Float,
    val headLeft: Point2D.Float,
    val headRight: Point2D.Float,
    val caretHeight: Float,
    val headThickness: Float,
    val coreWidth: Float,
)

internal data class LocalCaretTrailFrame(
    val point: Point2D.Float,
    val travelProgress: Float,
    val opacity: Float,
)

internal object CaretFlowGeometry {
    private const val MIN_CARET_HEIGHT = 8f
    private const val MAX_CARET_HEIGHT = 96f
    private const val HIGH_CONTRAST_THICKNESS = 3f
    private const val TAIL_LAG = 0.18f

    fun normalizeCaretHeight(value: Float): Float = value.coerceIn(MIN_CARET_HEIGHT, MAX_CARET_HEIGHT)

    fun trailThickness(caretHeight: Float, highContrast: Boolean = false): Float {
        val normalized = normalizeCaretHeight(caretHeight)
        return if (highContrast) minOf(normalized, HIGH_CONTRAST_THICKNESS) else normalized
    }

    /**
     * Projects a vertical caret silhouette onto the normal of the travel vector.
     * Horizontal travel keeps the full caret height, vertical travel keeps only
     * the narrow caret stem, and diagonal travel blends continuously between them.
     */
    fun projectedTrailThickness(
        deltaX: Float,
        deltaY: Float,
        caretHeight: Float,
        widthScale: Float = 1f,
    ): Float {
        val normalizedHeight = normalizeCaretHeight(caretHeight)
        val stemWidth = caretStemWidth(normalizedHeight)
        val length = hypot(deltaX.toDouble(), deltaY.toDouble()).toFloat()
        if (length < 0.5f) return stemWidth
        val normalX = -deltaY / length
        val normalY = deltaX / length
        val projected = abs(normalY) * normalizedHeight + abs(normalX) * stemWidth
        return (projected * widthScale.coerceIn(0.5f, 1.5f)).coerceAtLeast(1f)
    }

    fun transitionDurationMillis(distance: Double): Int =
        (180.0 + distance.coerceAtLeast(0.0) * 0.24).roundToInt().coerceIn(180, 420)

    fun localTrailDurationMillis(distance: Double): Int =
        (220.0 + distance.coerceAtLeast(0.0) * 0.20).roundToInt().coerceIn(220, 360)

    fun localTrailFrame(
        from: Point2D.Float,
        to: Point2D.Float,
        progress: Double,
    ): LocalCaretTrailFrame {
        val bounded = progress.coerceIn(0.0, 1.0).toFloat()
        val rawTravel = (bounded / LOCAL_TRAVEL_FRACTION).coerceIn(0f, 1f)
        val easedTravel = rawTravel * rawTravel * (3f - 2f * rawTravel)
        val fadeProgress = ((bounded - LOCAL_TRAVEL_FRACTION) / (1f - LOCAL_TRAVEL_FRACTION)).coerceIn(0f, 1f)
        val opacity = (1f - fadeProgress) * (1f - fadeProgress)
        return LocalCaretTrailFrame(
            point = Point2D.Float(
                lerp(from.x, to.x, easedTravel),
                lerp(from.y, to.y, easedTravel),
            ),
            travelProgress = easedTravel,
            opacity = opacity,
        )
    }

    /**
     * Builds a tapered ribbon around a motion path. The tail converges to the
     * origin. Its cross-section is the motion-normal projection of the native
     * vertical caret footprint, preventing a full-height horizontal bar during
     * vertical movement while retaining caret-height emphasis horizontally.
     */
    fun trailRibbon(
        points: List<Point2D.Float>,
        caretHeight: Float,
        widthScale: Float = 1f,
    ): CaretTrailRibbon? {
        val accepted = ArrayList<Point2D.Float>(points.size)
        points.forEach { point ->
            val previous = accepted.lastOrNull()
            if (previous == null || hypot((point.x - previous.x).toDouble(), (point.y - previous.y).toDouble()) >= 0.5) {
                accepted += Point2D.Float(point.x, point.y)
            }
        }
        if (accepted.size < 2) return null

        val normalizedHeight = normalizeCaretHeight(caretHeight)
        val left = ArrayList<Point2D.Float>(accepted.size)
        val right = ArrayList<Point2D.Float>(accepted.size)
        accepted.indices.forEach { index ->
            val previous = accepted[if (index == 0) 0 else index - 1]
            val next = accepted[if (index == accepted.lastIndex) accepted.lastIndex else index + 1]
            val deltaX = next.x - previous.x
            val deltaY = next.y - previous.y
            val length = hypot(deltaX.toDouble(), deltaY.toDouble()).toFloat()
            if (length < 0.5f) return null

            val normalX = -deltaY / length
            val normalY = deltaX / length
            val spatialProgress = index.toDouble() / accepted.lastIndex.toDouble()
            val taperScale = spatialProgress.pow(RIBBON_TAPER_EXPONENT).toFloat()
            val projectedThickness = projectedTrailThickness(deltaX, deltaY, normalizedHeight, widthScale)
            val offset = projectedThickness / 2f * taperScale
            val center = accepted[index]
            left += Point2D.Float(center.x + normalX * offset, center.y + normalY * offset)
            right += Point2D.Float(center.x - normalX * offset, center.y - normalY * offset)
        }

        val outline = ArrayList<Point2D.Float>(accepted.size * 2)
        outline.addAll(left)
        right.asReversed().forEach { outline += it }
        return CaretTrailRibbon(
            outline = outline.toList(),
            centerLine = accepted.toList(),
            gradientStart = accepted.first(),
            gradientEnd = accepted.last(),
            headLeft = left.last(),
            headRight = right.last(),
            caretHeight = normalizedHeight,
            headThickness = projectedTrailThickness(
                accepted.last().x - accepted[accepted.lastIndex - 1].x,
                accepted.last().y - accepted[accepted.lastIndex - 1].y,
                normalizedHeight,
                widthScale,
            ),
            coreWidth = (caretStemWidth(normalizedHeight) * 0.72f * widthScale.coerceIn(0.5f, 1.5f))
                .coerceIn(1.4f, 3.6f),
        )
    }

    fun transitionFrame(
        fromX: Float,
        fromY: Float,
        fromHeight: Float,
        toX: Float,
        toY: Float,
        toHeight: Float,
        progress: Double,
        intensityScale: Double,
    ): CaretFlowFrame {
        val headProgress = progress.coerceIn(0.0, 1.0).toFloat()
        val tailProgress = (headProgress - TAIL_LAG).coerceAtLeast(0f)
        val entryFade = (headProgress / 0.08f).coerceIn(0f, 1f)
        val exitFade = ((1f - headProgress) / 0.24f).coerceIn(0f, 1f)
        return CaretFlowFrame(
            headX = lerp(fromX, toX, headProgress),
            headY = lerp(fromY, toY, headProgress),
            tailX = lerp(fromX, toX, tailProgress),
            tailY = lerp(fromY, toY, tailProgress),
            caretHeight = lerp(normalizeCaretHeight(fromHeight), normalizeCaretHeight(toHeight), headProgress),
            alpha = (0.62 * intensityScale.coerceIn(0.0, 2.25) * entryFade * exitFade)
                .toFloat()
                .coerceIn(0f, 0.94f),
        )
    }

    private fun lerp(from: Float, to: Float, progress: Float): Float = from + (to - from) * progress

    private fun caretStemWidth(caretHeight: Float): Float = (caretHeight * 0.14f).coerceIn(2f, 4f)

    private const val RIBBON_TAPER_EXPONENT = 0.72
    private const val LOCAL_TRAVEL_FRACTION = 0.62f
}
