package io.github.myangprint.motionflow.ui

import com.intellij.openapi.components.service
import io.github.myangprint.motionflow.MotionFlowBundle
import io.github.myangprint.motionflow.core.CancelReason
import io.github.myangprint.motionflow.core.EasingId
import io.github.myangprint.motionflow.core.EffectKey
import io.github.myangprint.motionflow.core.EffectType
import io.github.myangprint.motionflow.core.MotionEngine
import io.github.myangprint.motionflow.core.MotionOwner
import io.github.myangprint.motionflow.core.MotionSpec
import io.github.myangprint.motionflow.core.OwnerTokens
import io.github.myangprint.motionflow.policy.MotionPolicy
import java.awt.AlphaComposite
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.Ellipse2D
import java.awt.geom.Path2D
import javax.swing.JComponent
import javax.swing.SwingUtilities
import javax.swing.UIManager
import javax.accessibility.AccessibleContext

class MotionPreview : JComponent(), MotionOwner {
    override var motionDisposed: Boolean = false
        private set

    private val ownerToken = OwnerTokens.create()
    private val surfaceToken = OwnerTokens.create()
    private var progress = 0.0
    private var alpha = 1.0
    private var policy = MotionPolicy.DISABLED

    init {
        preferredSize = Dimension(520, 120)
        minimumSize = Dimension(260, 100)
        toolTipText = MotionFlowBundle.message("settings.preview.accessibleDescription")
    }

    override fun getAccessibleContext(): AccessibleContext {
        if (accessibleContext == null) accessibleContext = AccessibleMotionPreview()
        return accessibleContext
    }

    fun play(nextPolicy: MotionPolicy) {
        if (motionDisposed) return
        policy = nextPolicy
        pause()
        if (!nextPolicy.enabled) {
            repaint()
            return
        }
        val duration = if (nextPolicy.reducedMotion) 80 else nextPolicy.duration(650, EffectType.PREVIEW)
        service<MotionEngine>().animate(
            key = key(),
            owner = this,
            spec = MotionSpec(duration, EasingId.EMPHASIZED_DECELERATE),
            restart = true,
            repaintTarget = this,
            onFrame = { value, _, _ ->
                progress = if (nextPolicy.reducedMotion) 0.5 else value
                alpha = if (nextPolicy.reducedMotion) 1.0 - value * 0.45 else 1.0
            },
            onEnd = {
                progress = if (nextPolicy.reducedMotion) 0.5 else 1.0
                alpha = 1.0
                repaint()
            },
        )
    }

    fun pause() {
        if (motionDisposed || !SwingUtilities.isEventDispatchThread()) return
        service<MotionEngine>().cancel(key(), CancelReason.EXPLICIT)
    }

    override fun dispose() {
        if (motionDisposed) return
        motionDisposed = true
        if (SwingUtilities.isEventDispatchThread()) service<MotionEngine>().cancelOwner(ownerToken, CancelReason.OWNER_DISPOSED)
    }

    override fun paintComponent(graphics: Graphics) {
        super.paintComponent(graphics)
        val g = graphics.create() as Graphics2D
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            val background = UIManager.getColor("Panel.background") ?: Color(0x2B2D30)
            val accent = MotionPalette.accent(background)
            g.color = background
            g.fillRoundRect(0, 0, width, height, 14, 14)
            g.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha.toFloat())

            val margin = 34.0
            val centerY = height * 0.55
            val travel = (width - margin * 2).coerceAtLeast(1.0)
            val headX = margin + travel * progress
            val path = Path2D.Double()
            path.moveTo(margin, centerY)
            path.curveTo(
                margin + travel * 0.28,
                centerY - 38.0 * policy.intensityScale,
                margin + travel * 0.58,
                centerY + 28.0 * policy.intensityScale,
                width - margin,
                centerY - 8.0,
            )
            g.stroke = BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
            g.color = MotionPalette.withAlpha(accent, 0.28)
            g.draw(path)
            g.color = accent
            g.fill(Ellipse2D.Double(headX - 6.0, centerY - 6.0, 12.0, 12.0))
            g.color = MotionPalette.withAlpha(accent, 0.22)
            g.fill(Ellipse2D.Double(headX - 18.0, centerY - 18.0, 36.0, 36.0))
        } finally {
            g.dispose()
        }
    }

    private fun key() = EffectKey(ownerToken, surfaceToken, EffectType.PREVIEW)

    protected inner class AccessibleMotionPreview : AccessibleJComponent() {
        override fun getAccessibleName(): String = MotionFlowBundle.message("settings.preview.accessibleName")

        override fun getAccessibleDescription(): String =
            MotionFlowBundle.message("settings.preview.accessibleDescription")
    }
}
