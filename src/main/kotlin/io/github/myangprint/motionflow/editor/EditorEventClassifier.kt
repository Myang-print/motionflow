package io.github.myangprint.motionflow.editor

import java.awt.Point
import kotlin.math.abs

object EditorEventClassifier {
    fun isClick(
        pressedAt: Point,
        releasedAt: Point,
        elapsedMillis: Long,
        primaryButton: Boolean,
        popupTrigger: Boolean,
        dragThreshold: Double = 6.0,
        timeoutMillis: Long = 700L,
    ): Boolean = primaryButton &&
        !popupTrigger &&
        elapsedMillis in 0..timeoutMillis &&
        pressedAt.distance(releasedAt) <= dragThreshold

    fun isNavigationJump(previousVisualLine: Int, nextVisualLine: Int, threshold: Int): Boolean =
        abs(nextVisualLine - previousVisualLine) >= threshold
}
