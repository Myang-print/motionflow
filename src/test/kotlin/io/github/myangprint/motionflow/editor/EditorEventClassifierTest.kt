package io.github.myangprint.motionflow.editor

import java.awt.Point
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EditorEventClassifierTest {
    @Test
    fun `MF-ED-007 primary release inside threshold is a click`() {
        assertTrue(EditorEventClassifier.isClick(Point(10, 10), Point(13, 12), 120, true, false))
    }

    @Test
    fun `MF-ED-008 drag right click popup and stale release are rejected`() {
        assertFalse(EditorEventClassifier.isClick(Point(0, 0), Point(20, 0), 100, true, false))
        assertFalse(EditorEventClassifier.isClick(Point(0, 0), Point(0, 0), 100, false, false))
        assertFalse(EditorEventClassifier.isClick(Point(0, 0), Point(0, 0), 100, true, true))
        assertFalse(EditorEventClassifier.isClick(Point(0, 0), Point(0, 0), 900, true, false))
    }

    @Test
    fun `MF-ED-009 and MF-ED-010 navigation threshold separates jumps from routine movement`() {
        assertFalse(EditorEventClassifier.isNavigationJump(10, 17, 8))
        assertTrue(EditorEventClassifier.isNavigationJump(10, 18, 8))
        assertTrue(EditorEventClassifier.isNavigationJump(18, 10, 8))
    }
}
