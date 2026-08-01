package io.github.myangprint.motionflow.ui

import java.awt.Color
import kotlin.test.Test
import kotlin.test.assertTrue

class MotionPaletteTest {
    @Test
    fun `MF-A-005 accent meets target contrast on light and dark backgrounds`() {
        listOf(Color.WHITE, Color.BLACK, Color(0x2B2D30), Color(0xF7F8FA)).forEach { background ->
            assertTrue(MotionPalette.contrastRatio(MotionPalette.accent(background), background) >= 3.0)
        }
    }
}
