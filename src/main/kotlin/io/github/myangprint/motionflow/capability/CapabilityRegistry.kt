package io.github.myangprint.motionflow.capability

import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorKind
import io.github.myangprint.motionflow.core.EffectType
import java.awt.GraphicsEnvironment

enum class CapabilityState {
    SUPPORTED,
    DISABLED_BY_SETTING,
    DISABLED_BY_POLICY,
    UNSUPPORTED_PLATFORM,
    UNSUPPORTED_PRODUCT,
    UNSUPPORTED_SESSION,
    UNSUITABLE_SURFACE,
    DEGRADED_FOR_PERFORMANCE,
    DISABLED_AFTER_FAILURE,
}

@Service(Service.Level.APP)
class CapabilityRegistry {
    fun runtimeAvailable(): Boolean {
        val application = ApplicationManager.getApplication()
        return !GraphicsEnvironment.isHeadless() &&
            !application.isHeadlessEnvironment &&
            !application.isUnitTestMode
    }

    fun editorState(editor: Editor): CapabilityState = when {
        !runtimeAvailable() -> CapabilityState.UNSUPPORTED_SESSION
        editor.isDisposed -> CapabilityState.UNSUITABLE_SURFACE
        editor.project == null -> CapabilityState.UNSUITABLE_SURFACE
        editor.editorKind != EditorKind.MAIN_EDITOR -> CapabilityState.UNSUITABLE_SURFACE
        editor.isOneLineMode -> CapabilityState.UNSUITABLE_SURFACE
        else -> CapabilityState.SUPPORTED
    }

    fun nativeSmoothCaretEra(): Boolean = ApplicationInfo.getInstance().build.baselineVersion >= 261

    fun baseSnapshot(): Map<EffectType, CapabilityState> = EffectType.entries.associateWith {
        if (runtimeAvailable()) CapabilityState.SUPPORTED else CapabilityState.UNSUPPORTED_SESSION
    }
}
