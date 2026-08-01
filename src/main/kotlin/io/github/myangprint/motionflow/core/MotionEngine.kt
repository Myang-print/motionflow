package io.github.myangprint.motionflow.core

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import java.awt.Rectangle
import javax.swing.JComponent
import javax.swing.SwingUtilities
import javax.swing.Timer

@Service(Service.Level.APP)
class MotionEngine : Disposable {
    private data class Entry(
        val key: EffectKey,
        val owner: MotionOwner,
        var spec: MotionSpec,
        val timeline: ScalarTimeline,
        var repaintTarget: JComponent?,
        var dirtyRegion: (() -> Rectangle?)?,
        var onFrame: (value: Double, progress: Double, nowNanos: Long) -> Unit,
        var onEnd: (CancelReason?) -> Unit,
    )

    private val clock: MonotonicClock = SystemMonotonicClock
    private val entries = LinkedHashMap<EffectKey, Entry>()
    private val repaintRegions = LinkedHashMap<JComponent, Rectangle?>()
    private val metrics = MotionMetrics()
    private val degradation = AdaptiveDegradation()
    private var lastTickNanos = 0L
    private var disposed = false

    private val timer = Timer(FRAME_DELAY_MILLIS) { tick() }.apply {
        isCoalesce = true
        initialDelay = 0
    }

    fun animate(
        key: EffectKey,
        owner: MotionOwner,
        spec: MotionSpec,
        from: Double = 0.0,
        target: Double = 1.0,
        restart: Boolean = false,
        repaintTarget: JComponent? = null,
        dirtyRegion: (() -> Rectangle?)? = null,
        onFrame: (value: Double, progress: Double, nowNanos: Long) -> Unit,
        onEnd: (CancelReason?) -> Unit = {},
    ): Boolean {
        assertEdt()
        if (disposed || owner.motionDisposed) return false
        if (!spec.essential && degradation.level == DegradationLevel.NONESSENTIAL_PAUSED) {
            metrics.timelineRejected()
            return false
        }

        val now = clock.nowNanos()
        val existing = entries[key]
        if (existing != null && !restart) {
            existing.timeline.retarget(now, target, spec.durationMillis, spec.easing)
            existing.spec = spec
            existing.repaintTarget = repaintTarget
            existing.dirtyRegion = dirtyRegion
            existing.onFrame = onFrame
            existing.onEnd = onEnd
            metrics.timelineRetargeted()
            return true
        }
        if (existing != null) removeEntry(existing, CancelReason.REPLACED)

        if (!admit(key)) {
            metrics.timelineRejected()
            return false
        }

        if (spec.durationMillis == 0) {
            try {
                onFrame(target, 1.0, now)
            } catch (_: Exception) {
                invokeEnd(onEnd, CancelReason.FAILED)
                return false
            }
            repaintTarget?.let { requestRepaint(it, dirtyRegion?.invoke()) }
            flushRepaints()
            invokeEnd(onEnd, null)
            return true
        }

        val entry = Entry(
            key = key,
            owner = owner,
            spec = spec,
            timeline = ScalarTimeline(now, spec.durationMillis, from, target, spec.easing),
            repaintTarget = repaintTarget,
            dirtyRegion = dirtyRegion,
            onFrame = onFrame,
            onEnd = onEnd,
        )
        entries[key] = entry
        metrics.timelineSubmitted(entries.size)
        if (!timer.isRunning) {
            lastTickNanos = now
            timer.start()
        }
        return true
    }

    fun cancel(key: EffectKey, reason: CancelReason = CancelReason.EXPLICIT) {
        assertEdt()
        entries[key]?.let { removeEntry(it, reason) }
        flushRepaints()
        stopIfIdle()
    }

    fun cancelOwner(ownerToken: Long, reason: CancelReason = CancelReason.OWNER_DISPOSED) {
        assertEdt()
        entries.values.filter { it.key.ownerToken == ownerToken }.forEach { removeEntry(it, reason) }
        flushRepaints()
        stopIfIdle()
    }

    fun cancelAll(reason: CancelReason = CancelReason.POLICY_CHANGED) {
        assertEdt()
        entries.values.toList().forEach { removeEntry(it, reason) }
        flushRepaints()
        stopIfIdle()
    }

    fun requestRepaint(component: JComponent, region: Rectangle? = null) {
        assertEdt()
        metrics.repaintRequested()
        if (!repaintRegions.containsKey(component)) {
            repaintRegions[component] = region?.let(::Rectangle)
            return
        }
        val existing = repaintRegions[component]
        repaintRegions[component] = if (existing == null || region == null) null else existing.union(region)
    }

    fun metricsSnapshot(): MotionMetricsSnapshot = metrics.snapshot(entries.size)

    fun degradationLevel(): DegradationLevel = degradation.level

    fun resetDegradation() {
        assertEdt()
        degradation.reset()
    }

    fun isIdle(): Boolean = entries.isEmpty() && !timer.isRunning

    fun activeTimelineCount(): Int = entries.size

    override fun dispose() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater { dispose() }
            return
        }
        if (disposed) return
        disposed = true
        cancelAll(CancelReason.PLUGIN_DISPOSED)
        timer.stop()
        flushRepaints()
    }

    private fun tick() {
        assertEdt()
        if (disposed) return
        val tickStarted = clock.nowNanos()
        if (lastTickNanos != 0L && tickStarted - lastTickNanos > LATE_FRAME_NANOS) {
            metrics.frameDropped()
        }
        lastTickNanos = tickStarted

        val snapshot = entries.values.toList()
        for (entry in snapshot) {
            if (entries[entry.key] !== entry) continue
            if (entry.owner.motionDisposed) {
                removeEntry(entry, CancelReason.OWNER_DISPOSED)
                continue
            }
            val sample = entry.timeline.sample(tickStarted)
            try {
                entry.onFrame(sample.value, sample.progress, tickStarted)
                entry.repaintTarget?.let { requestRepaint(it, entry.dirtyRegion?.invoke()) }
            } catch (_: Exception) {
                removeEntry(entry, CancelReason.FAILED)
                continue
            }
            if (sample.finished && entries[entry.key] === entry) {
                entries.remove(entry.key)
                metrics.timelineCompleted(entries.size)
                invokeEnd(entry.onEnd, null)
            }
        }
        flushRepaints()
        val cost = (clock.nowNanos() - tickStarted).coerceAtLeast(0L)
        metrics.recordFrameCost(cost)
        degradation.record(cost)
        stopIfIdle()
    }

    private fun admit(key: EffectKey): Boolean {
        val surfaceEntries = entries.values.filter { it.key.surfaceToken == key.surfaceToken }
        if (surfaceEntries.size >= MAX_SURFACE_TIMELINES) {
            val victim = surfaceEntries.firstOrNull { !it.spec.essential }
            if (victim != null) removeEntry(victim, CancelReason.CAPACITY) else return false
        }
        if (entries.size >= MAX_GLOBAL_TIMELINES) {
            val victim = entries.values.firstOrNull { !it.spec.essential }
            if (victim != null) removeEntry(victim, CancelReason.CAPACITY) else return false
        }
        return true
    }

    private fun removeEntry(entry: Entry, reason: CancelReason) {
        if (entries.remove(entry.key) !== entry) return
        entry.timeline.cancel()
        metrics.timelineCancelled(entries.size)
        invokeEnd(entry.onEnd, reason)
    }

    private fun invokeEnd(callback: (CancelReason?) -> Unit, reason: CancelReason?) {
        try {
            callback(reason)
        } catch (_: Exception) {
            // Cleanup callbacks are isolated from the scheduler and other effects.
        }
    }

    private fun flushRepaints() {
        if (repaintRegions.isEmpty()) return
        val snapshot = repaintRegions.toMap()
        repaintRegions.clear()
        snapshot.forEach { (component, region) ->
            if (region == null) {
                component.repaint()
            } else if (!region.isEmpty) {
                component.repaint(region.x, region.y, region.width, region.height)
            }
            metrics.repaintIssued()
        }
    }

    private fun stopIfIdle() {
        if (entries.isEmpty() && timer.isRunning) timer.stop()
    }

    private fun assertEdt() {
        check(SwingUtilities.isEventDispatchThread()) { "MotionEngine state is EDT-confined" }
    }

    private companion object {
        const val FRAME_DELAY_MILLIS = 16
        const val LATE_FRAME_NANOS = 24_000_000L
        const val MAX_GLOBAL_TIMELINES = 64
        const val MAX_SURFACE_TIMELINES = 8
    }
}
