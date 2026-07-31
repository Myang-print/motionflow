package io.github.myangprint.motionflow.workbench

import com.intellij.ide.ui.UISettings
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.wm.WindowManager
import com.intellij.openapi.wm.ex.ToolWindowManagerListener
import io.github.myangprint.motionflow.capability.CapabilityRegistry
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
import io.github.myangprint.motionflow.policy.MotionPolicyListener
import io.github.myangprint.motionflow.policy.MotionPolicyService
import io.github.myangprint.motionflow.ui.MotionPalette
import io.github.myangprint.motionflow.ui.SurfaceOverlay
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Dialog
import java.awt.Dimension
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.Window
import java.awt.event.WindowEvent
import java.awt.event.WindowFocusListener
import javax.swing.JComponent
import javax.swing.JFrame
import javax.swing.SwingUtilities
import javax.swing.UIManager

@Service(Service.Level.PROJECT)
class ProjectEffectsService(private val project: Project) : MotionOwner {
    private data class CueVisual(
        val alpha: Float = 0f,
        val accent: Color = Color.WHITE,
        val highContrast: Boolean = false,
    )

    private data class FrameDimVisual(val alpha: Float = 0f)

    private inner class CueOwner(
        val type: EffectType,
        target: JComponent,
        private val edgeOnly: Boolean,
    ) : MotionOwner {
        override var motionDisposed: Boolean = false
            private set
        val ownerToken = OwnerTokens.create()
        var visual = cueVisual(0f)
        val overlay = SurfaceOverlay(
            target = target,
            parentDisposable = this,
            onFailure = { exception ->
                val breaker = if (type == EffectType.EDITOR_CONTEXT) contextBreaker else toolWindowBreaker
                breaker.execute<Unit> { throw exception }
            },
            renderer = ::paint,
        )

        override fun dispose() {
            motionDisposed = true
        }

        private fun paint(graphics: Graphics2D, size: Dimension) {
            val snapshot = visual
            if (snapshot.alpha <= 0f || size.width <= 0 || size.height <= 0) return
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            graphics.color = MotionPalette.withAlpha(
                snapshot.accent,
                if (snapshot.highContrast) 1.0 else snapshot.alpha.toDouble(),
            )
            if (edgeOnly) {
                val edgeWidth = if (snapshot.highContrast) 3 else 4
                graphics.fillRoundRect(0, 0, edgeWidth, size.height, edgeWidth, edgeWidth)
            } else {
                graphics.stroke = BasicStroke(if (snapshot.highContrast) 2f else 2.5f)
                graphics.drawRoundRect(2, 2, (size.width - 5).coerceAtLeast(0), (size.height - 5).coerceAtLeast(0), 10, 10)
            }
        }
    }

    override var motionDisposed: Boolean = false
        private set

    private val surfaceToken = OwnerTokens.create()
    private val engine by lazy { service<MotionEngine>() }
    private val diagnostics by lazy { service<DiagnosticsService>() }
    private val contextBreaker by lazy { breaker(EffectType.EDITOR_CONTEXT) }
    private val toolWindowBreaker by lazy { breaker(EffectType.TOOL_WINDOW_FOCUS) }
    private val dimBreaker by lazy { breaker(EffectType.INACTIVE_FRAME_DIM) }
    private var policy = service<MotionPolicyService>().current()
    private var started = false
    private var runtimeDisposable: com.intellij.openapi.Disposable? = null
    private var activeToolWindowId: String? = null
    private var contextCue: CueOwner? = null
    private var toolWindowCue: CueOwner? = null
    private var frame: JFrame? = null
    private var frameDimOverlay: SurfaceOverlay? = null
    private var frameDim = FrameDimVisual()

    private val frameFocusListener = object : WindowFocusListener {
        override fun windowGainedFocus(event: WindowEvent) = clearFrameDim()
        override fun windowLostFocus(event: WindowEvent) {
            if (event.oppositeWindow != null && isOwnedBy(event.oppositeWindow, event.window)) return
            requestFrameDim()
        }
    }

    fun start() {
        check(SwingUtilities.isEventDispatchThread()) { "Workbench effects must start on the EDT" }
        if (started || motionDisposed || !service<CapabilityRegistry>().runtimeAvailable()) return
        started = true
        ApplicationManager.getApplication().messageBus.connect(this).subscribe(
            MotionPolicyListener.TOPIC,
            MotionPolicyListener(::onPolicyChanged),
        )
        onPolicyChanged(policy)
    }

    override fun dispose() {
        if (motionDisposed) return
        stopRuntime()
        motionDisposed = true
    }

    private fun startRuntime() {
        if (motionDisposed || runtimeDisposable != null) return
        val lifecycle = Disposer.newDisposable("MotionFlow workbench effects")
        Disposer.register(this, lifecycle)
        runtimeDisposable = lifecycle
        val connection = project.messageBus.connect(lifecycle)
        connection.subscribe(
            FileEditorManagerListener.FILE_EDITOR_MANAGER,
            object : FileEditorManagerListener {
                override fun selectionChanged(event: FileEditorManagerEvent) {
                    val component = event.newEditor?.component ?: return
                    if (component.isShowing) contextBreaker.execute { showCue(component, EffectType.EDITOR_CONTEXT) }
                }
            },
        )
        connection.subscribe(
            ToolWindowManagerListener.TOPIC,
            object : ToolWindowManagerListener {
                override fun stateChanged(toolWindowManager: ToolWindowManager) {
                    val next = toolWindowManager.activeToolWindowId
                    if (next == null || next == activeToolWindowId) return
                    activeToolWindowId = next
                    val toolWindow = toolWindowManager.getToolWindow(next) ?: return
                    val component = toolWindow.component
                    if (toolWindow.isVisible && component.isShowing) {
                        toolWindowBreaker.execute { showCue(component, EffectType.TOOL_WINDOW_FOCUS) }
                    }
                }
            },
        )
        attachFrame()
    }

    private fun stopRuntime() {
        val lifecycle = runtimeDisposable ?: return
        runtimeDisposable = null
        contextCue?.let(::disposeCue)
        toolWindowCue?.let(::disposeCue)
        frame?.removeWindowFocusListener(frameFocusListener)
        frame = null
        frameDim = FrameDimVisual()
        frameDimOverlay = null
        if (SwingUtilities.isEventDispatchThread()) engine.cancelOwner(surfaceToken, CancelReason.POLICY_CHANGED)
        Disposer.dispose(lifecycle)
    }

    private fun attachFrame() {
        val resolved = WindowManager.getInstance().getFrame(project) ?: return
        if (frame === resolved) return
        frame?.removeWindowFocusListener(frameFocusListener)
        frame = resolved
        resolved.addWindowFocusListener(frameFocusListener)
        val target = resolved.rootPane.contentPane as? JComponent ?: return
        frameDimOverlay = SurfaceOverlay(
            target = target,
            parentDisposable = runtimeDisposable ?: return,
            onFailure = { exception -> dimBreaker.execute<Unit> { throw exception } },
        ) { graphics, size ->
                val snapshot = frameDim
                if (snapshot.alpha <= 0f) return@SurfaceOverlay
                graphics.color = Color(0, 0, 0, (snapshot.alpha.coerceIn(0f, 0.35f) * 255).toInt())
                graphics.fillRect(0, 0, size.width, size.height)
            }
    }

    private fun showCue(target: JComponent, type: EffectType) {
        if (!policy.effectEnabled(type)) return
        val previous = if (type == EffectType.EDITOR_CONTEXT) contextCue else toolWindowCue
        previous?.let(::disposeCue)
        val cue = CueOwner(type, target, edgeOnly = type == EffectType.EDITOR_CONTEXT)
        Disposer.register(runtimeDisposable ?: return, cue)
        if (type == EffectType.EDITOR_CONTEXT) contextCue = cue else toolWindowCue = cue
        val intensity = policy.intensityScale
        engine.animate(
            key = EffectKey(cue.ownerToken, surfaceToken, type),
            owner = cue,
            spec = MotionSpec(
                policy.duration(if (type == EffectType.EDITOR_CONTEXT) 200 else 160, type),
                EasingId.EMPHASIZED_DECELERATE,
                essential = true,
            ),
            restart = true,
            repaintTarget = cue.overlay,
            onFrame = { value, _, _ ->
                cue.visual = cue.visual.copy(alpha = ((1.0 - value) * 0.85 * intensity).toFloat())
            },
            onEnd = {
                cue.visual = cue.visual.copy(alpha = 0f)
                if (type == EffectType.EDITOR_CONTEXT && contextCue === cue) contextCue = null
                if (type == EffectType.TOOL_WINDOW_FOCUS && toolWindowCue === cue) toolWindowCue = null
                if (!cue.motionDisposed) Disposer.dispose(cue)
            },
        )
    }

    private fun requestFrameDim() {
        if (!policy.effectEnabled(EffectType.INACTIVE_FRAME_DIM)) return
        if (policy.highContrast || UISettings.getInstance().presentationMode) return
        val overlay = frameDimOverlay ?: return
        dimBreaker.execute {
            engine.animate(
                key = EffectKey(surfaceToken, surfaceToken, EffectType.INACTIVE_FRAME_DIM),
                owner = this,
                spec = MotionSpec(policy.duration(120, EffectType.INACTIVE_FRAME_DIM), EasingId.STANDARD),
                from = frameDim.alpha.toDouble(),
                target = policy.inactiveFrameDimAmount,
                repaintTarget = overlay,
                onFrame = { value, _, _ -> frameDim = FrameDimVisual(value.toFloat()) },
            )
        }
    }

    private fun clearFrameDim() {
        if (motionDisposed || !SwingUtilities.isEventDispatchThread()) return
        engine.cancel(
            EffectKey(surfaceToken, surfaceToken, EffectType.INACTIVE_FRAME_DIM),
            CancelReason.EXPLICIT,
        )
        frameDim = FrameDimVisual()
        frameDimOverlay?.repaint()
    }

    private fun onPolicyChanged(next: MotionPolicy) {
        policy = next
        if (!next.enabled) {
            stopRuntime()
            return
        }
        startRuntime()
        contextCue?.let { cue ->
            cue.visual = cueVisual(cue.visual.alpha)
            cue.overlay.repaint()
        }
        toolWindowCue?.let { cue ->
            cue.visual = cueVisual(cue.visual.alpha)
            cue.overlay.repaint()
        }
        if (!next.effectEnabled(EffectType.EDITOR_CONTEXT)) contextCue?.let(::disposeCue)
        if (!next.effectEnabled(EffectType.TOOL_WINDOW_FOCUS)) toolWindowCue?.let(::disposeCue)
        if (!next.effectEnabled(EffectType.INACTIVE_FRAME_DIM)) clearFrameDim()
    }

    private fun disposeCue(cue: CueOwner) {
        engine.cancel(EffectKey(cue.ownerToken, surfaceToken, cue.type), CancelReason.REPLACED)
        if (!cue.motionDisposed) Disposer.dispose(cue)
        if (contextCue === cue) contextCue = null
        if (toolWindowCue === cue) toolWindowCue = null
    }

    private fun breaker(type: EffectType) = EffectCircuitBreaker {
        diagnostics.recordError("${type.name}_CIRCUIT_OPEN")
        when (type) {
            EffectType.EDITOR_CONTEXT -> contextCue?.let(::disposeCue)
            EffectType.TOOL_WINDOW_FOCUS -> toolWindowCue?.let(::disposeCue)
            EffectType.INACTIVE_FRAME_DIM -> clearFrameDim()
            else -> Unit
        }
    }

    private fun cueVisual(alpha: Float): CueVisual {
        val background = UIManager.getColor("Panel.background") ?: Color(0x2B2D30)
        return CueVisual(
            alpha = alpha,
            accent = MotionPalette.accent(background),
            highContrast = policy.highContrast,
        )
    }

    private fun isOwnedBy(candidate: Window, owner: Window): Boolean {
        var current: Window? = candidate
        while (current != null) {
            if (current === owner) return true
            if (current is Dialog && current.isModal) return true
            current = current.owner
        }
        return false
    }
}
