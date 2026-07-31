package io.github.myangprint.motionflow.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.components.service
import io.github.myangprint.motionflow.settings.MotionSettingsService

class ToggleMotionAction : ToggleAction() {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun isSelected(event: AnActionEvent): Boolean = service<MotionSettingsService>().snapshot().enabled

    override fun setSelected(event: AnActionEvent, state: Boolean) {
        service<MotionSettingsService>().setEnabled(state)
        MotionFlowNotifications.stateChanged(event.project, state)
    }
}
