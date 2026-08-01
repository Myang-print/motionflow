package io.github.myangprint.motionflow.editor

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import io.github.myangprint.motionflow.core.CancelReason
import io.github.myangprint.motionflow.core.EasingId
import io.github.myangprint.motionflow.core.EffectCircuitBreaker
import io.github.myangprint.motionflow.core.EffectKey
import io.github.myangprint.motionflow.core.EffectType
import io.github.myangprint.motionflow.core.MotionEngine
import io.github.myangprint.motionflow.core.MotionOwner
import io.github.myangprint.motionflow.core.MotionSpec
import io.github.myangprint.motionflow.core.OwnerTokens
import io.github.myangprint.motionflow.diagnostics.DiagnosticsService
import io.github.myangprint.motionflow.policy.MotionPolicy
import io.github.myangprint.motionflow.settings.CaretTrailStyle
import io.github.myangprint.motionflow.ui.SurfaceOverlay
import java.awt.Color
import java.awt.Graphics2D
import java.awt.Point
import java.awt.RenderingHints
import java.awt.geom.Point2D
import java.util.IdentityHashMap
import javax.swing.JComponent
import javax.swing.JRootPane
import javax.swing.SwingUtilities
import kotlin.math.hypot
import kotlin.math.roundToInt

internal data class EditorCaretAnchor(
    val component: JComponent,
    val point: Point2D.Float,
    val caretHeight: Float,
    val accent: Color,
)

internal class CrossEditorCaretFlow(
    parentDisposable: Disposable,
    private val engine: MotionEngine,
    initialPolicy: MotionPolicy,
    private val diagnostics: DiagnosticsService,
) : MotionOwner {
    private data class FlowVisual(
        val frame: CaretFlowFrame,
        val accent: Color,
        val highContrast: Boolean,
        val style: CaretTrailStyle,
        val ribbon: CaretTrailRibbon?,
    )

    private class FlowState(
        var visual: FlowVisual,
        val samples: CaretTrailSampleBuffer,
    )

    private data class Transition(
        val id: Long,
        val key: EffectKey,
        val disposable: Disposable,
        val overlay: SurfaceOverlay,
        val state: FlowState,
    )

    override var motionDisposed: Boolean = false
        private set

    private val ownerToken = OwnerTokens.create()
    private val transitions = IdentityHashMap<JRootPane, Transition>()
    private val breaker = EffectCircuitBreaker {
        diagnostics.recordError("CROSS_EDITOR_CARET_FLOW_CIRCUIT_OPEN")
        cancelAll(CancelReason.FAILED)
    }
    private var policy = initialPolicy

    init {
        Disposer.register(parentDisposable, this)
    }

    fun start(from: EditorCaretAnchor, to: EditorCaretAnchor) {
        check(SwingUtilities.isEventDispatchThread()) { "Cross-editor caret flow must start on the EDT" }
        if (motionDisposed || !policy.effectEnabled(EffectType.CARET_TRAIL)) return
        if (!from.component.isShowing || !to.component.isShowing || from.component === to.component) return

        val rootPane = SwingUtilities.getRootPane(from.component) ?: return
        if (rootPane !== SwingUtilities.getRootPane(to.component)) return
        val rootContent = rootPane.contentPane as? JComponent ?: return
        if (!rootContent.isShowing) return

        val fromCenter = centerIn(from, rootContent)
        val toCenter = centerIn(to, rootContent)
        val distance = hypot(
            (toCenter.x - fromCenter.x).toDouble(),
            (toCenter.y - fromCenter.y).toDouble(),
        )
        if (distance < MIN_FLOW_DISTANCE) return

        cancel(rootPane, CancelReason.REPLACED)
        val id = OwnerTokens.create()
        val key = EffectKey(ownerToken, OwnerTokens.create(), EffectType.CARET_TRAIL, CROSS_EDITOR_CHANNEL)
        val lifecycle = Disposer.newDisposable("MotionFlow split-editor caret flow")
        Disposer.register(this, lifecycle)
        val samples = CaretTrailSampleBuffer(MAX_CROSS_TRAIL_SAMPLES).apply {
            startAt(fromCenter, System.nanoTime())
        }
        val initialFrame = CaretFlowGeometry.transitionFrame(
            fromX = fromCenter.x,
            fromY = fromCenter.y,
            fromHeight = from.caretHeight,
            toX = toCenter.x,
            toY = toCenter.y,
            toHeight = to.caretHeight,
            progress = 0.0,
            intensityScale = policy.intensityScale,
        )
        val state = FlowState(
            visual = flowVisual(initialFrame, to.accent, samples.snapshot()),
            samples = samples,
        )
        val overlay = SurfaceOverlay(
            target = rootContent,
            parentDisposable = lifecycle,
            onFailure = { exception -> breaker.execute<Unit> { throw exception } },
            renderer = { graphics, _ -> paintFlow(graphics, state.visual) },
        )
        transitions[rootPane] = Transition(id, key, lifecycle, overlay, state)

        val accepted = engine.animate(
            key = key,
            owner = this,
            spec = MotionSpec(
                durationMillis = policy.duration(
                    CaretFlowGeometry.transitionDurationMillis(distance),
                    EffectType.CARET_TRAIL,
                ),
                easing = EasingId.EMPHASIZED_DECELERATE,
            ),
            restart = true,
            repaintTarget = overlay,
            onFrame = { value, _, frameNow ->
                val current = transitions[rootPane]
                if (current?.id != id) return@animate
                val frame = CaretFlowGeometry.transitionFrame(
                    fromX = fromCenter.x,
                    fromY = fromCenter.y,
                    fromHeight = from.caretHeight,
                    toX = toCenter.x,
                    toY = toCenter.y,
                    toHeight = to.caretHeight,
                    progress = value,
                    intensityScale = policy.intensityScale,
                )
                current.state.samples.append(
                    Point2D.Float(frame.headX, frame.headY),
                    frameNow,
                    policy.caretTrailLength,
                )
                current.state.visual = flowVisual(
                    frame = frame,
                    accent = to.accent,
                    samples = current.state.samples.snapshot(policy.caretTrailLength),
                )
                current.overlay.syncBounds()
            },
            onEnd = { finish(rootPane, id) },
        )
        if (!accepted) finish(rootPane, id)
    }

    fun onPolicyChanged(next: MotionPolicy) {
        check(SwingUtilities.isEventDispatchThread()) { "Cross-editor caret policy must update on the EDT" }
        policy = next
        if (!next.effectEnabled(EffectType.CARET_TRAIL)) cancelAll(CancelReason.POLICY_CHANGED)
    }

    override fun dispose() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(::dispose)
            return
        }
        if (motionDisposed) return
        motionDisposed = true
        engine.cancelOwner(ownerToken, CancelReason.OWNER_DISPOSED)
        transitions.entries.toList().forEach { (rootPane, transition) -> finish(rootPane, transition.id) }
    }

    private fun centerIn(anchor: EditorCaretAnchor, target: JComponent): Point2D.Float {
        val topLeft = SwingUtilities.convertPoint(
            anchor.component,
            Point(anchor.point.x.roundToInt(), anchor.point.y.roundToInt()),
            target,
        )
        return Point2D.Float(
            topLeft.x.toFloat(),
            topLeft.y + CaretFlowGeometry.normalizeCaretHeight(anchor.caretHeight) / 2f,
        )
    }

    private fun cancel(rootPane: JRootPane, reason: CancelReason) {
        val transition = transitions[rootPane] ?: return
        engine.cancel(transition.key, reason)
        if (transitions[rootPane]?.id == transition.id) finish(rootPane, transition.id)
    }

    private fun cancelAll(reason: CancelReason) {
        transitions.entries.toList().forEach { (rootPane, transition) ->
            engine.cancel(transition.key, reason)
            if (transitions[rootPane]?.id == transition.id) finish(rootPane, transition.id)
        }
    }

    private fun finish(rootPane: JRootPane, id: Long) {
        val transition = transitions[rootPane] ?: return
        if (transition.id != id) return
        transitions.remove(rootPane)
        Disposer.dispose(transition.disposable)
    }

    private fun paintFlow(graphics: Graphics2D, visual: FlowVisual) {
        val frame = visual.frame
        if (frame.alpha <= 0f) return
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        val tail = Point2D.Float(frame.tailX, frame.tailY)
        val head = Point2D.Float(frame.headX, frame.headY)
        when {
            visual.highContrast -> CaretTrailPainter.paintHighContrastSegment(
                graphics,
                tail,
                head,
                frame.caretHeight,
                visual.accent,
            )
            visual.style == CaretTrailStyle.SPEED_LINES -> visual.ribbon?.let { ribbon ->
                CaretTrailPainter.paintSpeedLine(graphics, ribbon, visual.accent, frame.alpha)
            }
            visual.style == CaretTrailStyle.SILKY_TRAIL -> visual.ribbon?.let { ribbon ->
                CaretTrailPainter.paintSilkyTrail(graphics, ribbon, visual.accent, frame.alpha)
            }
            else -> CaretTrailPainter.paintRoundedSegment(
                graphics,
                tail,
                head,
                frame.caretHeight,
                visual.accent,
                frame.alpha * 0.72f,
            )
        }
        CaretTrailPainter.paintCaretHead(graphics, head, frame.caretHeight, visual.accent, frame.alpha)
    }

    private fun flowVisual(
        frame: CaretFlowFrame,
        accent: Color,
        samples: List<TimedTrailPoint>,
    ): FlowVisual = FlowVisual(
        frame = frame,
        accent = accent,
        highContrast = policy.highContrast,
        style = policy.caretTrailStyle,
        ribbon = when {
            policy.highContrast -> null
            policy.caretTrailStyle == CaretTrailStyle.SILKY_TRAIL -> CaretFlowGeometry.trailRibbon(
                samples.map { it.point },
                frame.caretHeight,
            )
            policy.caretTrailStyle == CaretTrailStyle.SPEED_LINES -> CaretFlowGeometry.trailRibbon(
                listOf(Point2D.Float(frame.tailX, frame.tailY), Point2D.Float(frame.headX, frame.headY)),
                frame.caretHeight,
            )
            else -> null
        },
    )

    private companion object {
        const val CROSS_EDITOR_CHANNEL = "cross-editor"
        const val MIN_FLOW_DISTANCE = 24.0
        const val MAX_CROSS_TRAIL_SAMPLES = 12
    }
}
