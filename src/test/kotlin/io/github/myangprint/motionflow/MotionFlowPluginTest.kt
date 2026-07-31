package io.github.myangprint.motionflow

import kotlin.test.Test
import kotlin.test.assertEquals

class MotionFlowPluginTest {
    @Test
    fun `plugin identity remains stable`() {
        assertEquals("io.github.myangprint.motionflow", MotionFlowPlugin.ID)
        assertEquals("MotionFlow", MotionFlowPlugin.DISPLAY_NAME)
    }
}

