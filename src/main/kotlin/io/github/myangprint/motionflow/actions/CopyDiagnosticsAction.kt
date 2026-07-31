package io.github.myangprint.motionflow.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.ide.CopyPasteManager
import io.github.myangprint.motionflow.diagnostics.DiagnosticsService
import java.awt.datatransfer.StringSelection

class CopyDiagnosticsAction : AnAction() {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun actionPerformed(event: AnActionEvent) {
        CopyPasteManager.getInstance().setContents(StringSelection(service<DiagnosticsService>().snapshot()))
        MotionFlowNotifications.diagnosticsCopied(event.project)
    }
}
