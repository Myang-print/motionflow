package io.github.myangprint.motionflow.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import java.awt.Component
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Point
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.HierarchyBoundsAdapter
import java.awt.event.HierarchyEvent
import java.awt.event.HierarchyListener
import javax.swing.JComponent
import javax.swing.JLayeredPane
import javax.swing.SwingUtilities

/**
 * Input-transparent overlay implemented only with supported JDK/Swing APIs.
 * The target is never re-parented or assigned a replacement UI delegate.
 */
class SurfaceOverlay(
    private val target: JComponent,
    parentDisposable: Disposable,
    private val onFailure: (Exception) -> Unit = {},
    private val renderer: (Graphics2D, Dimension) -> Unit,
) : JComponent(), Disposable {
    private var disposed = false
    private var installedLayer: JLayeredPane? = null

    private val componentListener = object : ComponentAdapter() {
        override fun componentMoved(event: ComponentEvent) = syncBounds()
        override fun componentResized(event: ComponentEvent) = syncBounds()
        override fun componentShown(event: ComponentEvent) = syncBounds()
        override fun componentHidden(event: ComponentEvent) {
            isVisible = false
        }
    }
    private val hierarchyBoundsListener = object : HierarchyBoundsAdapter() {
        override fun ancestorMoved(event: HierarchyEvent) = syncBounds()
        override fun ancestorResized(event: HierarchyEvent) = syncBounds()
    }
    private val hierarchyListener = HierarchyListener { syncBounds() }

    init {
        isOpaque = false
        isFocusable = false
        isEnabled = false
        name = "MotionFlow overlay"
        target.addComponentListener(componentListener)
        target.addHierarchyBoundsListener(hierarchyBoundsListener)
        target.addHierarchyListener(hierarchyListener)
        Disposer.register(parentDisposable, this)
        syncBounds()
    }

    override fun contains(x: Int, y: Int): Boolean = false

    override fun paintComponent(graphics: Graphics) {
        if (disposed || !target.isShowing) return
        val copy = graphics.create() as Graphics2D
        try {
            try {
                renderer(copy, size)
            } catch (exception: Exception) {
                onFailure(exception)
            }
        } finally {
            copy.dispose()
        }
    }

    fun syncBounds() {
        if (disposed) return
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(::syncBounds)
            return
        }
        val parent = target.parent
        val rootPane = SwingUtilities.getRootPane(target)
        if (parent == null || rootPane == null) {
            uninstall()
            return
        }
        val layer = rootPane.layeredPane
        if (installedLayer !== layer) {
            uninstall()
            layer.add(this, Integer.valueOf(OVERLAY_LAYER))
            installedLayer = layer
        }
        val converted = SwingUtilities.convertRectangle(parent, target.bounds, layer)
        if (bounds != converted) bounds = converted
        isVisible = target.isShowing
    }

    fun targetPointToOverlay(point: Point): Point {
        syncBounds()
        return SwingUtilities.convertPoint(target, point, this)
    }

    override fun dispose() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(::dispose)
            return
        }
        if (disposed) return
        disposed = true
        target.removeComponentListener(componentListener)
        target.removeHierarchyBoundsListener(hierarchyBoundsListener)
        target.removeHierarchyListener(hierarchyListener)
        uninstall()
    }

    private fun uninstall() {
        val layer = installedLayer ?: return
        layer.remove(this)
        layer.repaint(bounds)
        installedLayer = null
    }

    private companion object {
        // Above normal content and below popup/drag layers.
        const val OVERLAY_LAYER = 200
    }
}
