package io.github.myangprint.motionflow.core

enum class CircuitState {
    CLOSED,
    RETRY_ALLOWED,
    OPEN_FOR_SESSION,
}

class EffectCircuitBreaker(private val onOpen: () -> Unit = {}) {
    var state: CircuitState = CircuitState.CLOSED
        private set

    fun <T> execute(operation: () -> T): Result<T> {
        if (state == CircuitState.OPEN_FOR_SESSION) {
            return Result.failure(CircuitOpenException())
        }
        return runCatching(operation).onSuccess {
            state = CircuitState.CLOSED
        }.onFailure {
            state = when (state) {
                CircuitState.CLOSED -> CircuitState.RETRY_ALLOWED
                CircuitState.RETRY_ALLOWED -> CircuitState.OPEN_FOR_SESSION
                CircuitState.OPEN_FOR_SESSION -> CircuitState.OPEN_FOR_SESSION
            }
            if (state == CircuitState.OPEN_FOR_SESSION) onOpen()
        }
    }

    fun reset() {
        state = CircuitState.CLOSED
    }
}

class CircuitOpenException : IllegalStateException("Effect circuit breaker is open")
