package io.github.myangprint.motionflow.editor

import io.github.myangprint.motionflow.settings.CaretTrailStyle
import java.awt.Color
import java.awt.BasicStroke

internal data class TrailVisual(
    val x: Float,
    val y: Float,
    val alpha: Float,
)

internal data class RippleVisual(
    val id: Int,
    val x: Float,
    val y: Float,
    val radius: Float,
    val alpha: Float,
)

internal data class PulseVisual(
    val y: Float,
    val height: Float,
    val alpha: Float,
)

internal data class EditorOverlayModel(
    val trail: List<TrailVisual> = emptyList(),
    val trailRibbon: CaretTrailRibbon? = null,
    val ripples: List<RippleVisual> = emptyList(),
    val pulse: PulseVisual? = null,
    val style: EditorOverlayStyle = EditorOverlayStyle(),
)

internal data class EditorOverlayStyle(
    val accent: Color = Color.WHITE,
    val caretTrailStyle: CaretTrailStyle = CaretTrailStyle.SILKY_TRAIL,
    val highContrast: Boolean = false,
    val rippleStroke: BasicStroke = BasicStroke(1.5f),
    val lineHeight: Float = 16f,
)
