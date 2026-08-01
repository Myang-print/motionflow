package io.github.myangprint.motionflow.startup

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class MotionFlowStartupActivityTest {
    @Test
    fun `startup services are deferred to the scheduled EDT turn`() {
        var scheduled: Runnable? = null
        var starts = 0

        scheduleStartupOnEdt(
            dispatch = { scheduled = it },
            isExpired = { false },
            startServices = { starts++ },
        )

        assertEquals(0, starts)
        assertNotNull(scheduled).run()
        assertEquals(1, starts)
    }

    @Test
    fun `disposed project expires startup before the EDT turn`() {
        var scheduled: Runnable? = null
        var starts = 0

        scheduleStartupOnEdt(
            dispatch = { scheduled = it },
            isExpired = { true },
            startServices = { starts++ },
        )

        assertNotNull(scheduled).run()
        assertEquals(0, starts)
    }
}
