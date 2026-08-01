package io.github.myangprint.motionflow.core

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.swing.JComponent
import javax.swing.SwingUtilities
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MotionEngineIntegrationTest {
    private val owner = TestOwner()
    private var engine: MotionEngine? = null

    @AfterTest
    fun tearDown() {
        SwingUtilities.invokeAndWait {
            owner.dispose()
            engine?.dispose()
        }
    }

    @Test
    fun `MF-E-003 zero-duration feedback completes without starting the scheduler`() {
        SwingUtilities.invokeAndWait {
            val created = MotionEngine().also { engine = it }
            var published = -1.0
            assertTrue(
                created.animate(
                    key = EffectKey(1, 1, EffectType.NAVIGATION_PULSE),
                    owner = owner,
                    spec = MotionSpec(0, essential = true),
                    onFrame = { value, _, _ -> published = value },
                ),
            )
            assertEquals(1.0, published)
            assertTrue(created.isIdle())
        }
    }

    @Test
    fun `MF-E-003 last completion stops the scheduler`() {
        val finished = CountDownLatch(1)
        SwingUtilities.invokeAndWait {
            val created = MotionEngine().also { engine = it }
            created.animate(
                key = EffectKey(1, 1, EffectType.NAVIGATION_PULSE),
                owner = owner,
                spec = MotionSpec(5, EasingId.LINEAR, essential = true),
                onFrame = { _, _, _ -> },
                onEnd = { finished.countDown() },
            )
        }
        assertTrue(finished.await(3, TimeUnit.SECONDS))
        SwingUtilities.invokeAndWait { assertTrue(engine!!.isIdle()) }
    }

    @Test
    fun `MF-P-007 global and per-surface timeline caps remain bounded`() {
        SwingUtilities.invokeAndWait {
            val created = MotionEngine().also { engine = it }
            repeat(80) { index ->
                created.animate(
                    key = EffectKey(index.toLong() + 1, (index / 8).toLong(), EffectType.CLICK_RIPPLE, index.toString()),
                    owner = owner,
                    spec = MotionSpec(60_000),
                    onFrame = { _, _, _ -> },
                )
            }
            assertEquals(64, created.activeTimelineCount())
            created.cancelAll()
            assertTrue(created.isIdle())
        }
    }

    @Test
    fun `MF-P-007 per-surface cap also bounds essential timelines`() {
        SwingUtilities.invokeAndWait {
            val created = MotionEngine().also { engine = it }
            repeat(8) { index ->
                assertTrue(
                    created.animate(
                        key = EffectKey(index.toLong() + 1, 42, EffectType.NAVIGATION_PULSE, index.toString()),
                        owner = owner,
                        spec = MotionSpec(60_000, essential = true),
                        onFrame = { _, _, _ -> },
                    ),
                )
            }
            assertFalse(
                created.animate(
                    key = EffectKey(99, 42, EffectType.EDITOR_CONTEXT),
                    owner = owner,
                    spec = MotionSpec(60_000, essential = true),
                    onFrame = { _, _, _ -> },
                ),
            )
            assertEquals(8, created.activeTimelineCount())
        }
    }

    @Test
    fun `MF-C-003 synchronous callback failure is isolated`() {
        SwingUtilities.invokeAndWait {
            val created = MotionEngine().also { engine = it }
            var reason: CancelReason? = null
            assertFalse(
                created.animate(
                    key = EffectKey(1, 1, EffectType.NAVIGATION_PULSE),
                    owner = owner,
                    spec = MotionSpec(0, essential = true),
                    onFrame = { _, _, _ -> error("synthetic callback failure") },
                    onEnd = { reason = it },
                ),
            )
            assertEquals(CancelReason.FAILED, reason)
            assertTrue(created.isIdle())
        }
    }

    @Test
    fun `MF-F-001 cancellation flushes cleanup repaint before the timer stops`() {
        SwingUtilities.invokeAndWait {
            val created = MotionEngine().also { engine = it }
            val component = RecordingComponent().apply { repaints = 0 }
            val key = EffectKey(1, 1, EffectType.CARET_TRAIL)
            created.animate(
                key = key,
                owner = owner,
                spec = MotionSpec(60_000),
                onFrame = { _, _, _ -> },
                onEnd = { created.requestRepaint(component) },
            )
            created.cancel(key)
            assertTrue(component.repaints > 0)
            assertTrue(created.isIdle())
        }
    }

    private class TestOwner : MotionOwner {
        override var motionDisposed: Boolean = false
            private set

        override fun dispose() {
            motionDisposed = true
        }
    }

    private class RecordingComponent : JComponent() {
        var repaints = 0

        override fun repaint() {
            repaints++
        }
    }
}
