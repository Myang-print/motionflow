package io.github.myangprint.motionflow.editor

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import com.intellij.openapi.editor.event.EditorMouseEvent
import com.intellij.openapi.editor.event.EditorMouseListener
import com.intellij.openapi.editor.event.VisibleAreaEvent
import com.intellij.openapi.editor.event.VisibleAreaListener
import com.intellij.openapi.util.Disposer
import io.github.myangprint.motionflow.capability.CapabilityRegistry
import io.github.myangprint.motionflow.capability.CapabilityState
import io.github.myangprint.motionflow.core.MotionEngine
import io.github.myangprint.motionflow.core.MotionOwner
import io.github.myangprint.motionflow.diagnostics.DiagnosticsService
import io.github.myangprint.motionflow.policy.MotionPolicy
import io.github.myangprint.motionflow.policy.MotionPolicyListener
import io.github.myangprint.motionflow.policy.MotionPolicyService
import java.util.IdentityHashMap
import javax.swing.SwingUtilities

@Service(Service.Level.APP)
class EditorEffectsManager : MotionOwner {
    override var motionDisposed: Boolean = false
        private set

    private val sessions = IdentityHashMap<Editor, EditorEffectsSession>()
    private var started = false
    private var runtimeDisposable: Disposable? = null
    private var focusedSession: EditorEffectsSession? = null
    private var crossEditorFlow: CrossEditorCaretFlow? = null

    fun start() {
        check(SwingUtilities.isEventDispatchThread()) { "Editor effects must start on the EDT" }
        if (started || motionDisposed) return
        val capabilities = service<CapabilityRegistry>()
        if (!capabilities.runtimeAvailable()) return
        started = true

        com.intellij.openapi.application.ApplicationManager.getApplication().messageBus.connect(this).subscribe(
            MotionPolicyListener.TOPIC,
            MotionPolicyListener(::onPolicyChanged),
        )
        onPolicyChanged(service<MotionPolicyService>().current())
    }

    override fun dispose() {
        if (motionDisposed) return
        motionDisposed = true
        stopRuntime()
    }

    private fun startRuntime() {
        if (motionDisposed || runtimeDisposable != null) return
        val lifecycle = Disposer.newDisposable("MotionFlow editor effects")
        Disposer.register(this, lifecycle)
        runtimeDisposable = lifecycle
        crossEditorFlow = CrossEditorCaretFlow(
            parentDisposable = lifecycle,
            engine = service<MotionEngine>(),
            initialPolicy = service<MotionPolicyService>().current(),
            diagnostics = service<DiagnosticsService>(),
        )

        val factory = EditorFactory.getInstance()
        factory.addEditorFactoryListener(
            object : EditorFactoryListener {
                override fun editorCreated(event: EditorFactoryEvent) = attach(event.editor)
                override fun editorReleased(event: EditorFactoryEvent) = detach(event.editor)
            },
            lifecycle,
        )
        factory.eventMulticaster.addCaretListener(
            object : CaretListener {
                override fun caretPositionChanged(event: CaretEvent) {
                    sessions[event.editor]?.onCaretPositionChanged(event)
                }
            },
            lifecycle,
        )
        factory.eventMulticaster.addEditorMouseListener(
            object : EditorMouseListener {
                override fun mousePressed(event: EditorMouseEvent) {
                    sessions[event.editor]?.onMousePressed(event)
                }

                override fun mouseReleased(event: EditorMouseEvent) {
                    sessions[event.editor]?.onMouseReleased(event)
                }
            },
            lifecycle,
        )
        factory.eventMulticaster.addVisibleAreaListener(
            VisibleAreaListener { event: VisibleAreaEvent -> sessions[event.editor]?.onVisibleAreaChanged() },
            lifecycle,
        )
        factory.allEditors.forEach(::attach)
    }

    private fun stopRuntime() {
        val lifecycle = runtimeDisposable ?: return
        runtimeDisposable = null
        focusedSession = null
        crossEditorFlow = null
        Disposer.dispose(lifecycle)
        sessions.clear()
    }

    private fun attach(editor: Editor) {
        if (motionDisposed || sessions.containsKey(editor)) return
        if (service<CapabilityRegistry>().editorState(editor) != CapabilityState.SUPPORTED) return
        val session = EditorEffectsSession(
            editor = editor,
            engine = service<MotionEngine>(),
            initialPolicy = service<MotionPolicyService>().current(),
            diagnostics = service<DiagnosticsService>(),
            onFocusGained = ::onEditorFocusGained,
        )
        val lifecycle = runtimeDisposable ?: return
        Disposer.register(lifecycle, session)
        sessions[editor] = session
        if (session.isFocusOwner()) focusedSession = session
    }

    private fun detach(editor: Editor) {
        val session = sessions.remove(editor) ?: return
        if (focusedSession === session) focusedSession = null
        if (!session.motionDisposed) Disposer.dispose(session)
    }

    private fun onPolicyChanged(policy: MotionPolicy) {
        if (!policy.enabled) {
            stopRuntime()
            return
        }
        startRuntime()
        crossEditorFlow?.onPolicyChanged(policy)
        sessions.values.toList().forEach { it.onPolicyChanged(policy) }
    }

    private fun onEditorFocusGained(target: EditorEffectsSession) {
        check(SwingUtilities.isEventDispatchThread()) { "Editor focus changes must run on the EDT" }
        val source = focusedSession
        focusedSession = target
        if (source == null || source === target) return
        val from = source.caretAnchor() ?: return
        val lifecycle = runtimeDisposable ?: return
        SwingUtilities.invokeLater {
            if (
                motionDisposed ||
                runtimeDisposable !== lifecycle ||
                focusedSession !== target ||
                !target.isFocusOwner()
            ) return@invokeLater
            val to = target.caretAnchor() ?: return@invokeLater
            crossEditorFlow?.start(from, to)
        }
    }
}
