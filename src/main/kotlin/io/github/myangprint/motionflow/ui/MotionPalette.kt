package io.github.myangprint.motionflow.ui

import com.intellij.ui.JBColor
import java.awt.Color
import javax.swing.UIManager
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

object MotionPalette {
    fun accent(background: Color): Color {
        val candidate = listOf(
            "Component.focusColor",
            "Focus.color",
            "Link.activeForeground",
            "Link.foreground",
        ).firstNotNullOfOrNull(UIManager::getColor) ?: JBColor(Color(0x5B8DEF), Color(0x80AFFF))
        if (contrastRatio(candidate, background) >= 3.0) return candidate
        val destination = if (relativeLuminance(background) > 0.45) Color.BLACK else Color.WHITE
        for (step in 1..10) {
            val adjusted = mix(candidate, destination, step / 10.0)
            if (contrastRatio(adjusted, background) >= 3.0) return adjusted
        }
        return destination
    }

    fun withAlpha(color: Color, alpha: Double): Color = Color(
        color.red,
        color.green,
        color.blue,
        (alpha.coerceIn(0.0, 1.0) * 255.0).toInt(),
    )

    fun contrastRatio(first: Color, second: Color): Double {
        val bright = max(relativeLuminance(first), relativeLuminance(second))
        val dark = min(relativeLuminance(first), relativeLuminance(second))
        return (bright + 0.05) / (dark + 0.05)
    }

    private fun mix(from: Color, to: Color, amount: Double): Color {
        val inverse = 1.0 - amount
        return Color(
            (from.red * inverse + to.red * amount).toInt(),
            (from.green * inverse + to.green * amount).toInt(),
            (from.blue * inverse + to.blue * amount).toInt(),
        )
    }

    private fun relativeLuminance(color: Color): Double {
        fun channel(value: Int): Double {
            val normalized = value / 255.0
            return if (normalized <= 0.04045) normalized / 12.92 else ((normalized + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(color.red) + 0.7152 * channel(color.green) + 0.0722 * channel(color.blue)
    }
}
