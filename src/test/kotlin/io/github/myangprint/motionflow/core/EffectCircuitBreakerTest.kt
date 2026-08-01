package io.github.myangprint.motionflow.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EffectCircuitBreakerTest {
    @Test
    fun `MF-C-003 second consecutive failure opens only that breaker`() {
        var openings = 0
        val breaker = EffectCircuitBreaker { openings++ }
        assertTrue(breaker.execute<Unit> { error("first") }.isFailure)
        assertEquals(CircuitState.RETRY_ALLOWED, breaker.state)
        assertTrue(breaker.execute<Unit> { error("second") }.isFailure)
        assertEquals(CircuitState.OPEN_FOR_SESSION, breaker.state)
        assertEquals(1, openings)
    }

    @Test
    fun `MF-C-003 successful retry closes the breaker`() {
        val breaker = EffectCircuitBreaker()
        breaker.execute<Unit> { error("first") }
        assertTrue(breaker.execute { 42 }.isSuccess)
        assertEquals(CircuitState.CLOSED, breaker.state)
    }
}
