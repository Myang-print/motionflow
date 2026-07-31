package io.github.myangprint.motionflow.policy

import com.intellij.ide.PowerSaveMode
import com.intellij.ide.ui.LafManager
import com.intellij.ide.ui.LafManagerListener
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import io.github.myangprint.motionflow.settings.MotionFlowState
import io.github.myangprint.motionflow.settings.MotionSettingsListener
import io.github.myangprint.motionflow.settings.MotionSettingsService
import com.intellij.util.messages.Topic
import javax.swing.UIManager

fun interface MotionPolicyListener {
    fun policyChanged(policy: MotionPolicy)

    companion object {
        @JvmField
        val TOPIC: Topic<MotionPolicyListener> = Topic.create(
            "MotionFlow policy changed",
            MotionPolicyListener::class.java,
        )
    }
}

@Service(Service.Level.APP)
class MotionPolicyService : Disposable {
    private val application = ApplicationManager.getApplication()

    @Volatile
    private var current = resolve(application.service<MotionSettingsService>().snapshot())

    init {
        val connection = application.messageBus.connect(this)
        connection.subscribe(MotionSettingsListener.TOPIC, MotionSettingsListener { recompute(it) })
        connection.subscribe(PowerSaveMode.TOPIC, PowerSaveMode.Listener { recompute() })
        connection.subscribe(LafManagerListener.TOPIC, LafManagerListener { _: LafManager -> recompute(forcePublish = true) })
    }

    fun current(): MotionPolicy = current

    fun resolveFor(state: MotionFlowState): MotionPolicy = resolve(state)

    fun recompute(
        state: MotionFlowState = application.service<MotionSettingsService>().snapshot(),
        forcePublish: Boolean = false,
    ) {
        val next = resolve(state)
        if (!forcePublish && next == current) return
        current = next
        application.messageBus.syncPublisher(MotionPolicyListener.TOPIC).policyChanged(next)
    }

    override fun dispose() = Unit

    private fun resolve(state: MotionFlowState): MotionPolicy {
        val build = ApplicationInfo.getInstance().build.baselineVersion
        return MotionPolicyResolver.resolve(
            state,
            MotionEnvironment(
                headless = application.isHeadlessEnvironment,
                unitTest = application.isUnitTestMode,
                powerSave = PowerSaveMode.isEnabled(),
                highContrast = UIManager.getBoolean("Theme.highContrast") ||
                    UIManager.getBoolean("Component.highContrast"),
                nativeSmoothCaretEra = build >= 261,
            ),
        )
    }
}
