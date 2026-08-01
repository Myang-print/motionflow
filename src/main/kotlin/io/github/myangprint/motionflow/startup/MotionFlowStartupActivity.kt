package io.github.myangprint.motionflow.startup

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import io.github.myangprint.motionflow.editor.EditorEffectsManager
import io.github.myangprint.motionflow.workbench.ProjectEffectsService

class MotionFlowStartupActivity : StartupActivity.DumbAware {
    override fun runActivity(project: Project) {
        scheduleStartupOnEdt(
            dispatch = { task ->
                ApplicationManager.getApplication().invokeLater(task, ModalityState.nonModal())
            },
            isExpired = { project.isDisposed },
        ) {
            service<EditorEffectsManager>().start()
            project.service<ProjectEffectsService>().start()
        }
    }
}

internal fun scheduleStartupOnEdt(
    dispatch: (Runnable) -> Unit,
    isExpired: () -> Boolean,
    startServices: () -> Unit,
) {
    dispatch(Runnable {
        if (!isExpired()) startServices()
    })
}
