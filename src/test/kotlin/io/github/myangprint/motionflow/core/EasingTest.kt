package io.github.myangprint.motionflow.core

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class EasingTest {
    @Test
    fun `MF-E-007 every easing is clamped finite and preserves endpoints`() {
        EasingId.entries.forEach { easing ->
            assertEquals(0.0, easing.transform(-1.0), 1e-9)
            assertEquals(0.0, easing.transform(0.0), 1e-9)
            assertEquals(1.0, easing.transform(1.0), 1e-9)
            assertEquals(1.0, easing.transform(2.0), 1e-9)
            repeat(10_000) {
                val value = easing.transform(Random(it).nextDouble())
                assertTrue(value.isFinite())
                assertTrue(value in 0.0..1.0)
            }
        }
    }

    @Test
    fun `MF-E-007 invalid Bezier x control points are rejected`() {
        assertFailsWith<IllegalArgumentException> { CubicBezier(-0.1, 0.0, 1.0, 1.0) }
        assertFailsWith<IllegalArgumentException> { CubicBezier(0.0, 0.0, 1.1, 1.0) }
    }
}
