package io.github.myangprint.motionflow.diagnostics

import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import io.github.myangprint.motionflow.MotionFlowPlugin
import io.github.myangprint.motionflow.capability.CapabilityRegistry
import io.github.myangprint.motionflow.core.DegradationLevel
import io.github.myangprint.motionflow.core.EffectType
import io.github.myangprint.motionflow.core.MotionEngine
import io.github.myangprint.motionflow.policy.MotionPolicyService
import java.util.Locale

@Service(Service.Level.APP)
class DiagnosticsService {
    private val recentErrors = ArrayDeque<String>(MAX_ERRORS)

    fun recordError(category: String) {
        val normalized = category.uppercase(Locale.ROOT).replace(Regex("[^A-Z0-9_.-]"), "_").take(48)
        if (normalized.isBlank() || normalized in recentErrors) return
        if (recentErrors.size == MAX_ERRORS) recentErrors.removeFirst()
        recentErrors.addLast(normalized)
    }

    fun snapshot(): String {
        val policy = service<MotionPolicyService>().current()
        val metrics = service<MotionEngine>().metricsSnapshot()
        val capabilities = service<CapabilityRegistry>().baseSnapshot()
        val text = buildString {
            appendLine("MotionFlow diagnostics")
            appendLine("pluginVersion=${MotionFlowPlugin.version()}")
            appendLine("ideBuild=${ApplicationInfo.getInstance().build.asString()}")
            appendLine("os=${safeOsName()} ${System.getProperty("os.arch", "unknown")}")
            appendLine("profile=${policy.profile.name}")
            appendLine("reducedMotion=${policy.reducedMotion}")
            appendLine("powerSave=${policy.powerSave}")
            appendLine("highContrast=${policy.highContrast}")
            appendLine("nativeSmoothCaretEra=${policy.nativeSmoothCaretEra}")
            appendLine("capabilities=${capabilities.entries.joinToString(",") { "${it.key.name}:${it.value.name}" }}")
            appendLine("timelines.active=${metrics.activeTimelines}")
            appendLine("timelines.peak=${metrics.peakTimelines}")
            appendLine("timelines.rejected=${metrics.rejected}")
            appendLine("frame.samples=${metrics.frameSamples}")
            appendLine("frame.p50Ms=${formatMillis(metrics.frameP50Nanos)}")
            appendLine("frame.p95Ms=${formatMillis(metrics.frameP95Nanos)}")
            appendLine("frame.p99Ms=${formatMillis(metrics.frameP99Nanos)}")
            appendLine("frames.dropped=${metrics.framesDropped}")
            appendLine("degradation=${service<MotionEngine>().degradationLevel().name}")
            appendLine("errors=${if (recentErrors.isEmpty()) "none" else recentErrors.joinToString(",")}")
        }.trimEnd()
        check(DiagnosticsSanitizer.isSafe(text)) { "Diagnostics schema emitted private data" }
        return text
    }

    fun degradation(): DegradationLevel = service<MotionEngine>().degradationLevel()

    private fun safeOsName(): String = System.getProperty("os.name", "unknown")
        .replace(Regex("[^A-Za-z0-9 ._-]"), "_")
        .take(40)

    private fun formatMillis(nanos: Long): String = String.format(Locale.ROOT, "%.3f", nanos / 1_000_000.0)

    private companion object {
        const val MAX_ERRORS = 5
    }
}
