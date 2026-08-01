package io.github.myangprint.motionflow.actions

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import io.github.myangprint.motionflow.MotionFlowBundle

internal object MotionFlowNotifications {
    fun stateChanged(project: Project?, enabled: Boolean) {
        val message = MotionFlowBundle.message(if (enabled) "notification.enabled" else "notification.disabled")
        NotificationGroupManager.getInstance()
            .getNotificationGroup("MotionFlow")
            .createNotification(message, NotificationType.INFORMATION)
            .notify(project)
    }

    fun diagnosticsCopied(project: Project?) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("MotionFlow")
            .createNotification(MotionFlowBundle.message("notification.diagnosticsCopied"), NotificationType.INFORMATION)
            .notify(project)
    }
}
