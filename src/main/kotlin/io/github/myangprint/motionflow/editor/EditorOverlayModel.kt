package io.github.myangprint.motionflow.editor

import java.awt.Color
import java.awt.BasicStroke

data class TrailVisual(
    val x: Float,
    val y: Float,
    val alpha: Float,
)

data class RippleVisual(
    val id: Int,
    val x: Float,
    val y: Float,
    val radius: Float,
    val alpha: Float,
)

data class PulseVisual(
    val y: Float,
    val height: Float,
    val alpha: Float,
)

data class EditorOverlayModel(
    val trail: List<TrailVisual> = emptyList(),
    val ripples: List<RippleVisual> = emptyList(),
    val pulse: PulseVisual? = null,
    val style: EditorOverlayStyle = EditorOverlayStyle(),
)

data class EditorOverlayStyle(
    val accent: Color = Color.WHITE,
    val trailStroke: BasicStroke = BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND),
    val rippleStroke: BasicStroke = BasicStroke(1.5f),
    val lineHeight: Float = 16f,
)
