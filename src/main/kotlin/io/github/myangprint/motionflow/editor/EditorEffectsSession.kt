package io.github.myangprint.motionflow.editor

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.EditorMouseEvent
import com.intellij.openapi.editor.event.EditorMouseEventArea
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
import io.github.myangprint.motionflow.ui.MotionPalette
import io.github.myangprint.motionflow.ui.SurfaceOverlay
import java.awt.BasicStroke
import java.awt.Graphics2D
import java.awt.Point
import java.awt.RenderingHints
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import java.awt.event.MouseEvent
import java.awt.geom.Point2D
import javax.swing.SwingUtilities
import kotlin.math.hypot
import kotlin.math.pow

class EditorEffectsSession(
    private val editor: Editor,
    private val engine: MotionEngine,
    initialPolicy: MotionPolicy,
    private val diagnostics: DiagnosticsService,
    private val onFocusGained: (EditorEffectsSession) -> Unit,
) : MotionOwner {
    private data class Press(val point: Point, val whenMillis: Long)

    override var motionDisposed: Boolean = false
        private set

    private val ownerToken = OwnerTokens.create()
    private val surfaceToken = OwnerTokens.create()
    private val trailSamples = CaretTrailSampleBuffer(MAX_TRAIL_POINTS)
    private val rippleModels = LinkedHashMap<Int, RippleVisual>()
    private val trailBreaker = breaker(EffectType.CARET_TRAIL)
    private val rippleBreaker = breaker(EffectType.CLICK_RIPPLE)
    private val pulseBreaker = breaker(EffectType.NAVIGATION_PULSE)
    private val painterBreaker = EffectCircuitBreaker {
        diagnostics.recordError("EDITOR_PAINTER_CIRCUIT_OPEN")
        engine.cancelOwner(ownerToken, CancelReason.FAILED)
        model = EditorOverlayModel(style = currentStyle())
    }
    private var policy = initialPolicy
    private var model = EditorOverlayModel(style = currentStyle())
    private var press: Press? = null
    private var lastPoint: Point2D.Float? = null
    private var lastVisualLine: Int? = null
    private var nextRippleId = 1
    private var trailGeneration = 0L

    private val overlay = SurfaceOverlay(
        target = editor.contentComponent,
        parentDisposable = this,
        onFailure = { exception -> painterBreaker.execute<Unit> { throw exception } },
        renderer = ::paintOverlay,
    )
    private val focusListener = object : FocusAdapter() {
        override fun focusGained(event: FocusEvent) {
            if (!motionDisposed) onFocusGained(this@EditorEffectsSession)
        }
    }

    init {
        editor.contentComponent.addFocusListener(focusListener)
        lastPoint = currentCaretPoint()
        lastVisualLine = editor.caretModel.primaryCaret.visualPosition.line
    }

    fun onCaretPositionChanged(event: CaretEvent) {
        if (motionDisposed || event.caret !== editor.caretModel.primaryCaret || editor.isDisposed) return
        model = model.copy(style = currentStyle())
        val caret = editor.caretModel.primaryCaret
        val visualPosition = caret.visualPosition
        val point = editor.visualPositionToPoint2D(visualPosition)
        val current = Point2D.Float(point.x.toFloat(), point.y.toFloat())
        val previous = lastPoint
        val previousLine = lastVisualLine
        lastPoint = current
        lastVisualLine = visualPosition.line

        if (!editor.contentComponent.isShowing || !editor.contentComponent.isFocusOwner || previous == null) return
        if (hypot((current.x - previous.x).toDouble(), (current.y - previous.y).toDouble()) < 0.75) return

        if (policy.effectEnabled(EffectType.CARET_TRAIL)) {
            trailBreaker.execute { startTrail(previous, current) }
        }
        if (
            previousLine != null &&
            EditorEventClassifier.isNavigationJump(previousLine, visualPosition.line, policy.navigationJumpThreshold) &&
            policy.effectEnabled(EffectType.NAVIGATION_PULSE)
        ) {
            pulseBreaker.execute { startPulse(current.y) }
        }
    }

    fun onMousePressed(event: EditorMouseEvent) {
        if (motionDisposed || event.area != EditorMouseEventArea.EDITING_AREA) return
        val mouse = event.mouseEvent
        if (mouse.button != MouseEvent.BUTTON1 || mouse.isPopupTrigger) return
        press = Press(toContentPoint(mouse), mouse.`when`)
    }

    fun onMouseReleased(event: EditorMouseEvent) {
        val acceptedPress = press
        press = null
        if (motionDisposed || acceptedPress == null || event.area != EditorMouseEventArea.EDITING_AREA) return
        if (!policy.effectEnabled(EffectType.CLICK_RIPPLE)) return
        val mouse = event.mouseEvent
        val point = toContentPoint(mouse)
        if (!EditorEventClassifier.isClick(
                pressedAt = acceptedPress.point,
                releasedAt = point,
                elapsedMillis = mouse.`when` - acceptedPress.whenMillis,
                primaryButton = mouse.button == MouseEvent.BUTTON1,
                popupTrigger = mouse.isPopupTrigger,
                dragThreshold = DRAG_THRESHOLD,
                timeoutMillis = CLICK_TIMEOUT_MILLIS,
            )
        ) return
        rippleBreaker.execute { startRipple(point) }
    }

    fun onVisibleAreaChanged() {
        if (motionDisposed || trailSamples.isEmpty) return
        trailGeneration++
        engine.cancel(trailKey(), CancelReason.EXPLICIT)
        trailSamples.clear()
        publish(model.copy(trail = emptyList(), trailRibbon = null))
    }

    internal fun caretAnchor(): EditorCaretAnchor? {
        if (motionDisposed || editor.isDisposed || !editor.contentComponent.isShowing) return null
        val point = lastPoint ?: currentCaretPoint()
        val height = CaretFlowGeometry.normalizeCaretHeight(editor.lineHeight.toFloat())
        return EditorCaretAnchor(
            component = editor.contentComponent,
            point = Point2D.Float(point.x, point.y),
            caretHeight = height,
            accent = MotionPalette.accent(editor.colorsScheme.defaultBackground),
        )
    }

    internal fun isFocusOwner(): Boolean = !motionDisposed && editor.contentComponent.isFocusOwner

    internal fun reanchorLocalTrail() {
        if (motionDisposed || editor.isDisposed) return
        trailGeneration++
        engine.cancel(trailKey(), CancelReason.REPLACED)
        trailSamples.clear()
        lastPoint = currentCaretPoint()
        lastVisualLine = editor.caretModel.primaryCaret.visualPosition.line
        publish(model.copy(trail = emptyList(), trailRibbon = null, style = currentStyle()))
    }

    fun onPolicyChanged(next: MotionPolicy) {
        if (motionDisposed) return
        policy = next
        model = model.copy(style = currentStyle())
        if (!next.enabled) {
            engine.cancelOwner(ownerToken, CancelReason.POLICY_CHANGED)
            clearModel()
            return
        }
        if (!next.effectEnabled(EffectType.CARET_TRAIL)) {
            trailGeneration++
            engine.cancel(trailKey(), CancelReason.POLICY_CHANGED)
            trailSamples.clear()
            publish(model.copy(trail = emptyList(), trailRibbon = null))
        }
        if (!next.effectEnabled(EffectType.CLICK_RIPPLE)) {
            rippleModels.keys.toList().forEach { engine.cancel(rippleKey(it), CancelReason.POLICY_CHANGED) }
            rippleModels.clear()
            publish(model.copy(ripples = emptyList()))
        }
        if (!next.effectEnabled(EffectType.NAVIGATION_PULSE)) {
            engine.cancel(pulseKey(), CancelReason.POLICY_CHANGED)
            publish(model.copy(pulse = null))
        }
    }

    override fun dispose() {
        if (motionDisposed) return
        motionDisposed = true
        trailGeneration++
        editor.contentComponent.removeFocusListener(focusListener)
        if (SwingUtilities.isEventDispatchThread()) engine.cancelOwner(ownerToken, CancelReason.OWNER_DISPOSED)
        trailSamples.clear()
        rippleModels.clear()
        model = EditorOverlayModel(style = model.style)
    }

    private fun startTrail(previous: Point2D.Float, current: Point2D.Float) {
        val now = System.nanoTime()
        val maximum = minOf(
            policy.caretTrailLength,
            if (policy.nativeSmoothCaretEra) NATIVE_AWARE_TRAIL_POINTS else MAX_TRAIL_POINTS,
        )
        val duration = policy.duration(
            if (policy.nativeSmoothCaretEra) {
                NATIVE_AWARE_TRAIL_DURATION_MILLIS
            } else {
                CaretFlowGeometry.localTrailDurationMillis(
                    hypot((current.x - previous.x).toDouble(), (current.y - previous.y).toDouble()),
                )
            },
            EffectType.CARET_TRAIL,
        )
        val generation = ++trailGeneration
        trailSamples.startAt(previous, now)
        publishTrail(now, duration, opacity = 1f)
        val accepted = engine.animate(
            key = trailKey(),
            owner = this,
            spec = MotionSpec(duration, EasingId.LINEAR),
            restart = true,
            repaintTarget = overlay,
            onFrame = { _, progress, frameNow ->
                if (generation != trailGeneration) return@animate
                val frame = CaretFlowGeometry.localTrailFrame(previous, current, progress)
                trailSamples.append(frame.point, frameNow, maximum)
                publishTrail(frameNow, duration, frame.opacity)
            },
            onEnd = { reason ->
                if (generation != trailGeneration) return@animate
                if (reason != CancelReason.REPLACED) {
                    trailSamples.clear()
                    publish(model.copy(trail = emptyList(), trailRibbon = null))
                }
            },
        )
        if (!accepted && generation == trailGeneration) {
            engine.cancel(trailKey(), CancelReason.REPLACED)
            trailSamples.clear()
            publish(model.copy(trail = emptyList(), trailRibbon = null))
        }
    }

    private fun publishTrail(nowNanos: Long, durationMillis: Int, opacity: Float) {
        val durationNanos = maxOf(1L, durationMillis.toLong() * 1_000_000L)
        trailSamples.trimExpired(nowNanos, durationNanos)
        val simplifiedCount = if (policy.highContrast || engine.degradationLevel().name != "NORMAL") 4 else policy.caretTrailLength
        val source = trailSamples.snapshot(simplifiedCount)
        if (source.isEmpty()) {
            publish(model.copy(trail = emptyList(), trailRibbon = null))
            return
        }
        val peakAlpha = (0.66 * policy.intensityScale * opacity.coerceIn(0f, 1f))
            .toFloat()
            .coerceIn(0f, 0.90f)
        val visible = source.mapIndexed { index, point ->
            val spatialProgress = (index + 1).toDouble() / source.size.toDouble()
            TrailVisual(
                point.point.x,
                point.point.y,
                if (policy.highContrast) 1f else peakAlpha * spatialProgress.pow(0.72).toFloat(),
            )
        }
        val trailRibbon = if (
            !policy.highContrast &&
            (policy.caretTrailStyle == CaretTrailStyle.SILKY_TRAIL ||
                policy.caretTrailStyle == CaretTrailStyle.SPEED_LINES)
        ) {
            CaretFlowGeometry.trailRibbon(
                visible.map { point -> Point2D.Float(point.x, point.y + model.style.lineHeight / 2f) },
                model.style.lineHeight,
            )
        } else {
            null
        }
        publish(model.copy(trail = visible, trailRibbon = trailRibbon))
    }

    private fun startRipple(point: Point) {
        if (rippleModels.size >= MAX_RIPPLES) {
            val oldest = rippleModels.keys.first()
            engine.cancel(rippleKey(oldest), CancelReason.CAPACITY)
            rippleModels.remove(oldest)
        }
        val id = nextRippleId++
        engine.animate(
            key = rippleKey(id),
            owner = this,
            spec = MotionSpec(policy.duration(RIPPLE_DURATION_MILLIS, EffectType.CLICK_RIPPLE), EasingId.EMPHASIZED_DECELERATE),
            restart = true,
            repaintTarget = overlay,
            onFrame = { value, _, _ ->
                val progress = value.toFloat()
                rippleModels[id] = RippleVisual(
                    id = id,
                    x = point.x.toFloat(),
                    y = point.y.toFloat(),
                    radius = 4f + 14f * progress,
                    alpha = ((1f - progress) * 0.30f * policy.intensityScale.toFloat()),
                )
                publish(model.copy(ripples = rippleModels.values.toList()))
            },
            onEnd = {
                rippleModels.remove(id)
                publish(model.copy(ripples = rippleModels.values.toList()))
            },
        )
    }

    private fun startPulse(y: Float) {
        val height = editor.lineHeight.toFloat().coerceIn(8f, 96f)
        engine.animate(
            key = pulseKey(),
            owner = this,
            spec = MotionSpec(
                policy.duration(PULSE_DURATION_MILLIS, EffectType.NAVIGATION_PULSE),
                EasingId.EMPHASIZED_DECELERATE,
                essential = true,
            ),
            restart = true,
            repaintTarget = overlay,
            onFrame = { value, _, _ ->
                publish(
                    model.copy(
                        pulse = PulseVisual(
                            y = y,
                            height = height,
                            alpha = if (policy.highContrast) 1f else ((1.0 - value) * 0.22 * policy.intensityScale).toFloat(),
                        ),
                    ),
                )
            },
            onEnd = { publish(model.copy(pulse = null)) },
        )
    }

    private fun publish(next: EditorOverlayModel) {
        model = next
        overlay.syncBounds()
        engine.requestRepaint(overlay)
    }

    private fun clearModel() {
        trailGeneration++
        trailSamples.clear()
        rippleModels.clear()
        publish(EditorOverlayModel(style = currentStyle()))
    }

    private fun paintOverlay(graphics: Graphics2D, size: java.awt.Dimension) {
        val snapshot = model
        if (snapshot.trail.isEmpty() && snapshot.trailRibbon == null && snapshot.ripples.isEmpty() && snapshot.pulse == null) return
        val style = snapshot.style
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        snapshot.pulse?.let { pulse ->
            graphics.color = MotionPalette.withAlpha(style.accent, pulse.alpha.toDouble())
            graphics.fillRoundRect(0, pulse.y.toInt(), size.width, pulse.height.toInt(), 8, 8)
        }
        snapshot.ripples.forEach { ripple ->
            graphics.color = MotionPalette.withAlpha(style.accent, ripple.alpha.toDouble())
            graphics.stroke = style.rippleStroke
            graphics.drawOval(
                (ripple.x - ripple.radius).toInt(),
                (ripple.y - ripple.radius).toInt(),
                (ripple.radius * 2).toInt(),
                (ripple.radius * 2).toInt(),
            )
        }
        if (snapshot.trail.size >= 2) {
            if (!style.highContrast && style.caretTrailStyle == CaretTrailStyle.SILKY_TRAIL) {
                snapshot.trailRibbon?.let { ribbon ->
                    CaretTrailPainter.paintSilkyTrail(graphics, ribbon, style.accent, snapshot.trail.last().alpha)
                }
            } else if (!style.highContrast && style.caretTrailStyle == CaretTrailStyle.SPEED_LINES) {
                snapshot.trailRibbon?.let { ribbon ->
                    CaretTrailPainter.paintSpeedLine(graphics, ribbon, style.accent, snapshot.trail.last().alpha)
                }
            } else {
                for (index in 1 until snapshot.trail.size) {
                    val from = snapshot.trail[index - 1]
                    val to = snapshot.trail[index]
                    val fromCenter = Point2D.Float(from.x, from.y + style.lineHeight / 2f)
                    val toCenter = Point2D.Float(to.x, to.y + style.lineHeight / 2f)
                    if (style.highContrast) {
                        CaretTrailPainter.paintHighContrastSegment(
                            graphics,
                            fromCenter,
                            toCenter,
                            style.lineHeight,
                            style.accent,
                        )
                    } else {
                        CaretTrailPainter.paintRoundedSegment(
                            graphics,
                            fromCenter,
                            toCenter,
                            style.lineHeight,
                            style.accent,
                            minOf(from.alpha, to.alpha),
                        )
                    }
                }
            }
        }
    }

    private fun currentStyle(): EditorOverlayStyle {
        val lineHeight = editor.lineHeight.toFloat().coerceAtLeast(1f)
        return EditorOverlayStyle(
            accent = MotionPalette.accent(editor.colorsScheme.defaultBackground),
            caretTrailStyle = policy.caretTrailStyle,
            highContrast = policy.highContrast,
            rippleStroke = BasicStroke(if (policy.highContrast) 2f else 1.5f),
            lineHeight = lineHeight,
        )
    }

    private fun currentCaretPoint(): Point2D.Float {
        val point = editor.visualPositionToPoint2D(editor.caretModel.primaryCaret.visualPosition)
        return Point2D.Float(point.x.toFloat(), point.y.toFloat())
    }

    private fun toContentPoint(mouse: MouseEvent): Point = if (mouse.component === editor.contentComponent) {
        mouse.point
    } else {
        SwingUtilities.convertPoint(mouse.component, mouse.point, editor.contentComponent)
    }

    private fun breaker(type: EffectType) = EffectCircuitBreaker {
        diagnostics.recordError("${type.name}_CIRCUIT_OPEN")
        when (type) {
            EffectType.CARET_TRAIL -> {
                trailGeneration++
                engine.cancel(trailKey(), CancelReason.FAILED)
                trailSamples.clear()
                publish(model.copy(trail = emptyList(), trailRibbon = null))
            }
            EffectType.CLICK_RIPPLE -> {
                rippleModels.keys.toList().forEach { engine.cancel(rippleKey(it), CancelReason.FAILED) }
                rippleModels.clear()
                publish(model.copy(ripples = emptyList()))
            }
            EffectType.NAVIGATION_PULSE -> {
                engine.cancel(pulseKey(), CancelReason.FAILED)
                publish(model.copy(pulse = null))
            }
            else -> Unit
        }
    }

    private fun trailKey() = EffectKey(ownerToken, surfaceToken, EffectType.CARET_TRAIL)
    private fun rippleKey(id: Int) = EffectKey(ownerToken, surfaceToken, EffectType.CLICK_RIPPLE, id.toString())
    private fun pulseKey() = EffectKey(ownerToken, surfaceToken, EffectType.NAVIGATION_PULSE)

    private companion object {
        const val MAX_TRAIL_POINTS = 12
        const val MAX_RIPPLES = 3
        const val NATIVE_AWARE_TRAIL_DURATION_MILLIS = 180
        const val NATIVE_AWARE_TRAIL_POINTS = 4
        const val RIPPLE_DURATION_MILLIS = 180
        const val PULSE_DURATION_MILLIS = 160
        const val CLICK_TIMEOUT_MILLIS = 700L
        const val DRAG_THRESHOLD = 6.0
    }
}
