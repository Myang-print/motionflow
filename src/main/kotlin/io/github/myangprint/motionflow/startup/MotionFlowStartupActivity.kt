package io.github.myangprint.motionflow.startup

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import io.github.myangprint.motionflow.editor.EditorEffectsManager
import io.github.myangprint.motionflow.workbench.ProjectEffectsService

class MotionFlowStartupActivity : StartupActivity.DumbAware {
    override fun runActivity(project: Project) {
        service<EditorEffectsManager>().start()
        project.service<ProjectEffectsService>().start()
    }
}
