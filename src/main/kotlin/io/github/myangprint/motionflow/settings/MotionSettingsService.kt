package io.github.myangprint.motionflow.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.messages.Topic

fun interface MotionSettingsListener {
    fun settingsChanged(state: MotionFlowState)

    companion object {
        @JvmField
        val TOPIC: Topic<MotionSettingsListener> = Topic.create(
            "MotionFlow settings changed",
            MotionSettingsListener::class.java,
        )
    }
}

@Service(Service.Level.APP)
@State(name = "MotionFlowSettings", storages = [Storage("motionflow.xml")])
class MotionSettingsService : PersistentStateComponent<MotionFlowState> {
    @Volatile
    private var current = MotionFlowState()
    private val warnedFields = mutableSetOf<String>()

    override fun getState(): MotionFlowState = current.copy()

    override fun loadState(state: MotionFlowState) {
        val normalized = MotionStateNormalizer.normalize(state)
        current = normalized.state
        warnCorrections(normalized.correctedFields)
    }

    fun snapshot(): MotionFlowState = current.copy()

    fun replace(state: MotionFlowState) {
        val normalized = MotionStateNormalizer.normalize(state)
        current = normalized.state
        warnCorrections(normalized.correctedFields)
        ApplicationManager.getApplication().messageBus
            .syncPublisher(MotionSettingsListener.TOPIC)
            .settingsChanged(current.copy())
    }

    fun setEnabled(enabled: Boolean) {
        replace(snapshot().copy(enabled = enabled))
    }

    private fun warnCorrections(fields: Set<String>) {
        fields.filter(warnedFields::add).forEach { field ->
            LOG.warn("Normalized invalid MotionFlow setting: $field")
        }
    }

    private companion object {
        val LOG = Logger.getInstance(MotionSettingsService::class.java)
    }
}
