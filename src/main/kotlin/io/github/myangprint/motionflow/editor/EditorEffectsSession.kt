package io.github.myangprint.motionflow.editor

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.Caret
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
import io.github.myangprint.motionflow.settings.CaretTrailCoverage
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
import java.util.IdentityHashMap
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

    private class CaretTrailState(
        val id: Long,
        val samples: CaretTrailSampleBuffer,
        var lastPoint: Point2D.Float,
        var lastVisualLine: Int,
        var generation: Long = 0L,
        var visual: CaretTrailVisual? = null,
    )

    override var motionDisposed: Boolean = false
        private set

    private val ownerToken = OwnerTokens.create()
    private val surfaceToken = OwnerTokens.create()
    private val caretTrails = IdentityHashMap<Caret, CaretTrailState>()
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
    private var nextRippleId = 1

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
        reconcileCaretStates()
    }

    fun onCaretPositionChanged(event: CaretEvent) {
        if (motionDisposed || editor.isDisposed) return
        model = model.copy(style = currentStyle())
        val caret = event.caret
        val state = caretStateFor(caret) ?: return
        val visualPosition = caret.visualPosition
        val current = currentCaretPoint(caret)
        val previous = state.lastPoint
        val previousLine = state.lastVisualLine
        state.lastPoint = current
        state.lastVisualLine = visualPosition.line

        if (!editor.contentComponent.isShowing || !editor.contentComponent.isFocusOwner) return
        if (hypot((current.x - previous.x).toDouble(), (current.y - previous.y).toDouble()) < 0.75) return

        if (policy.effectEnabled(EffectType.CARET_TRAIL)) {
            trailBreaker.execute { startTrail(state, previous, current) }
        }
        if (
            caret === editor.caretModel.primaryCaret &&
            EditorEventClassifier.isNavigationJump(previousLine, visualPosition.line, policy.navigationJumpThreshold) &&
            policy.effectEnabled(EffectType.NAVIGATION_PULSE)
        ) {
            pulseBreaker.execute { startPulse(current.y) }
        }
    }

    fun onCaretAdded(event: CaretEvent) {
        if (motionDisposed || editor.isDisposed) return
        reconcileCaretStates()
        caretStateFor(event.caret)
    }

    fun onCaretRemoved(event: CaretEvent) {
        if (motionDisposed) return
        removeCaretState(event.caret, CancelReason.OWNER_DISPOSED)
        if (editor.isDisposed) return
        reconcileCaretStates()
        publishTrailVisuals()
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
        if (motionDisposed || caretTrails.values.none { !it.samples.isEmpty || it.visual != null }) return
        clearCaretTrails(CancelReason.EXPLICIT)
    }

    internal fun caretAnchor(): EditorCaretAnchor? {
        if (motionDisposed || editor.isDisposed || !editor.contentComponent.isShowing) return null
        val primary = editor.caretModel.primaryCaret
        val point = caretTrails[primary]?.lastPoint ?: currentCaretPoint(primary)
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
        clearCaretTrails(CancelReason.REPLACED, publishAfter = false)
        reconcileCaretStates()
        caretTrails.forEach { (caret, state) ->
            state.lastPoint = currentCaretPoint(caret)
            state.lastVisualLine = caret.visualPosition.line
        }
        publish(model.copy(trails = emptyList(), style = currentStyle()))
    }

    fun onPolicyChanged(next: MotionPolicy) {
        if (motionDisposed) return
        val coverageChanged = policy.caretTrailCoverage != next.caretTrailCoverage
        policy = next
        model = model.copy(style = currentStyle())
        if (!next.enabled) {
            engine.cancelOwner(ownerToken, CancelReason.POLICY_CHANGED)
            clearModel()
            return
        }
        if (!next.effectEnabled(EffectType.CARET_TRAIL)) {
            clearCaretTrails(CancelReason.POLICY_CHANGED)
        } else if (coverageChanged) {
            clearCaretTrails(CancelReason.POLICY_CHANGED, publishAfter = false)
            reconcileCaretStates()
            publishTrailVisuals()
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
        editor.contentComponent.removeFocusListener(focusListener)
        caretTrails.values.forEach { it.generation++ }
        if (SwingUtilities.isEventDispatchThread()) engine.cancelOwner(ownerToken, CancelReason.OWNER_DISPOSED)
        caretTrails.values.forEach { it.samples.clear() }
        caretTrails.clear()
        rippleModels.clear()
        model = EditorOverlayModel(style = model.style)
    }

    private fun startTrail(state: CaretTrailState, previous: Point2D.Float, current: Point2D.Float) {
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
        val generation = ++state.generation
        state.samples.startAt(previous, now)
        publishTrail(state, now, duration, opacity = 1f)
        val accepted = engine.animate(
            key = trailKey(state),
            owner = this,
            spec = MotionSpec(duration, EasingId.LINEAR),
            restart = true,
            repaintTarget = overlay,
            onFrame = { _, progress, frameNow ->
                if (generation != state.generation) return@animate
                val frame = CaretFlowGeometry.localTrailFrame(previous, current, progress)
                state.samples.append(frame.point, frameNow, maximum)
                publishTrail(state, frameNow, duration, frame.opacity)
            },
            onEnd = { reason ->
                if (generation != state.generation) return@animate
                if (reason != CancelReason.REPLACED) {
                    state.samples.clear()
                    state.visual = null
                    publishTrailVisuals()
                }
            },
        )
        if (!accepted && generation == state.generation) {
            engine.cancel(trailKey(state), CancelReason.REPLACED)
            state.samples.clear()
            state.visual = null
            publishTrailVisuals()
        }
    }

    private fun publishTrail(state: CaretTrailState, nowNanos: Long, durationMillis: Int, opacity: Float) {
        val durationNanos = maxOf(1L, durationMillis.toLong() * 1_000_000L)
        state.samples.trimExpired(nowNanos, durationNanos)
        val simplifiedCount = if (policy.highContrast || engine.degradationLevel().name != "NORMAL") 4 else policy.caretTrailLength
        val source = state.samples.snapshot(simplifiedCount)
        if (source.isEmpty()) {
            state.visual = null
            publishTrailVisuals()
            return
        }
        val peakAlpha = (
            0.72 * policy.intensityScale * policy.caretTrailVisibility.alphaScale * opacity.coerceIn(0f, 1f)
            )
            .toFloat()
            .coerceIn(0f, 0.96f)
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
                policy.caretTrailVisibility.widthScale,
            )
        } else {
            null
        }
        state.visual = CaretTrailVisual(state.id, visible, trailRibbon)
        publishTrailVisuals()
    }

    private fun caretStateFor(caret: Caret): CaretTrailState? {
        caretTrails[caret]?.let { return it }
        if (admittedCarets().none { it === caret }) return null
        return CaretTrailState(
            id = OwnerTokens.create(),
            samples = CaretTrailSampleBuffer(MAX_TRAIL_POINTS),
            lastPoint = currentCaretPoint(caret),
            lastVisualLine = caret.visualPosition.line,
        ).also { caretTrails[caret] = it }
    }

    private fun admittedCarets(): List<Caret> = CaretTrailAdmission.select(
        primary = editor.caretModel.primaryCaret,
        allCarets = editor.caretModel.allCarets,
        includeSecondary = policy.caretTrailCoverage == CaretTrailCoverage.ALL_CARETS,
        limit = MAX_ANIMATED_CARETS,
    )

    private fun reconcileCaretStates() {
        val admitted = admittedCarets()
        caretTrails.keys.toList()
            .filter { existing -> admitted.none { it === existing } }
            .forEach { removeCaretState(it, CancelReason.POLICY_CHANGED) }
        admitted.forEach(::caretStateFor)
    }

    private fun removeCaretState(caret: Caret, reason: CancelReason) {
        val state = caretTrails.remove(caret) ?: return
        state.generation++
        engine.cancel(trailKey(state), reason)
        state.samples.clear()
        state.visual = null
    }

    private fun clearCaretTrails(reason: CancelReason, publishAfter: Boolean = true) {
        caretTrails.values.forEach { state ->
            state.generation++
            engine.cancel(trailKey(state), reason)
            state.samples.clear()
            state.visual = null
        }
        if (publishAfter) publishTrailVisuals()
    }

    private fun publishTrailVisuals() {
        val visuals = caretTrails.values.mapNotNull { it.visual }.sortedBy { it.id }
        publish(model.copy(trails = visuals))
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
        clearCaretTrails(CancelReason.POLICY_CHANGED, publishAfter = false)
        rippleModels.clear()
        publish(EditorOverlayModel(style = currentStyle()))
    }

    private fun paintOverlay(graphics: Graphics2D, size: java.awt.Dimension) {
        val snapshot = model
        if (snapshot.trails.isEmpty() && snapshot.ripples.isEmpty() && snapshot.pulse == null) return
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
        snapshot.trails.forEach { trail ->
            if (trail.points.size < 2) return@forEach
            if (!style.highContrast && style.caretTrailStyle == CaretTrailStyle.SILKY_TRAIL) {
                trail.ribbon?.let { ribbon ->
                    CaretTrailPainter.paintSilkyTrail(graphics, ribbon, style.accent, trail.points.last().alpha)
                }
            } else if (!style.highContrast && style.caretTrailStyle == CaretTrailStyle.SPEED_LINES) {
                trail.ribbon?.let { ribbon ->
                    CaretTrailPainter.paintSpeedLine(graphics, ribbon, style.accent, trail.points.last().alpha)
                }
            } else {
                for (index in 1 until trail.points.size) {
                    val from = trail.points[index - 1]
                    val to = trail.points[index]
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
                            style.trailWidthScale,
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
            trailWidthScale = policy.caretTrailVisibility.widthScale,
        )
    }

    private fun currentCaretPoint(caret: Caret): Point2D.Float {
        val point = editor.visualPositionToPoint2D(caret.visualPosition)
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
                clearCaretTrails(CancelReason.FAILED)
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

    private fun trailKey(state: CaretTrailState) =
        EffectKey(ownerToken, surfaceToken, EffectType.CARET_TRAIL, "caret-${state.id}")
    private fun rippleKey(id: Int) = EffectKey(ownerToken, surfaceToken, EffectType.CLICK_RIPPLE, id.toString())
    private fun pulseKey() = EffectKey(ownerToken, surfaceToken, EffectType.NAVIGATION_PULSE)

    private companion object {
        const val MAX_TRAIL_POINTS = 12
        const val MAX_ANIMATED_CARETS = 8
        const val MAX_RIPPLES = 3
        const val NATIVE_AWARE_TRAIL_DURATION_MILLIS = 180
        const val NATIVE_AWARE_TRAIL_POINTS = 4
        const val RIPPLE_DURATION_MILLIS = 180
        const val PULSE_DURATION_MILLIS = 160
        const val CLICK_TIMEOUT_MILLIS = 700L
        const val DRAG_THRESHOLD = 6.0
    }
}
