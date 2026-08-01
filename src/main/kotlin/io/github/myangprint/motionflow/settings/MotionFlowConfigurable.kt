package io.github.myangprint.motionflow.settings

import com.intellij.openapi.components.service
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.util.Disposer
import io.github.myangprint.motionflow.MotionFlowBundle
import io.github.myangprint.motionflow.diagnostics.DiagnosticsService
import io.github.myangprint.motionflow.policy.MotionPolicyService
import io.github.myangprint.motionflow.ui.MotionPreview
import java.awt.BorderLayout
import java.awt.Component
import java.awt.FlowLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.awt.datatransfer.StringSelection
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.DefaultListCellRenderer
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JSpinner
import javax.swing.SpinnerNumberModel

class MotionFlowConfigurable : SearchableConfigurable {
    private var root: JComponent? = null
    private var updating = false
    private lateinit var enabled: JCheckBox
    private lateinit var reducedMotion: JCheckBox
    private lateinit var profile: JComboBox<MotionProfileId>
    private lateinit var durationScale: JSpinner
    private lateinit var intensityScale: JSpinner
    private lateinit var powerSaveBehavior: JComboBox<PowerSaveBehavior>
    private lateinit var caretTrail: JCheckBox
    private lateinit var caretTrailLength: JSpinner
    private lateinit var caretTrailStyle: JComboBox<CaretTrailStyle>
    private lateinit var caretTrailCoverage: JComboBox<CaretTrailCoverage>
    private lateinit var caretTrailVisibility: JComboBox<CaretTrailVisibility>
    private lateinit var clickRipple: JCheckBox
    private lateinit var navigationPulse: JCheckBox
    private lateinit var navigationThreshold: JSpinner
    private lateinit var editorContext: JCheckBox
    private lateinit var toolWindow: JCheckBox
    private lateinit var inactiveFrameDim: JCheckBox
    private lateinit var inactiveFrameDimAmount: JSpinner
    private lateinit var preview: MotionPreview

    override fun getId(): String = ID

    override fun getDisplayName(): String = MotionFlowBundle.message("settings.displayName")

    override fun createComponent(): JComponent {
        root?.let { return it }
        buildControls()
        val content = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = BorderFactory.createEmptyBorder(12, 12, 12, 12)
            add(topPanel())
            add(Box.createVerticalStrut(8))
            add(previewPanel())
            add(Box.createVerticalStrut(8))
            add(editorPanel())
            add(Box.createVerticalStrut(8))
            add(workbenchPanel())
            add(Box.createVerticalStrut(8))
            add(behaviorPanel())
            add(Box.createVerticalStrut(8))
            add(diagnosticsPanel())
            add(Box.createVerticalGlue())
        }
        root = JScrollPane(content).apply {
            border = BorderFactory.createEmptyBorder()
            verticalScrollBar.unitIncrement = 16
        }
        reset()
        return root!!
    }

    override fun isModified(): Boolean = root != null && readState() != service<MotionSettingsService>().snapshot()

    override fun apply() {
        service<MotionSettingsService>().replace(readState())
    }

    override fun reset() {
        if (root != null) writeState(service<MotionSettingsService>().snapshot())
    }

    override fun disposeUIResources() {
        if (::preview.isInitialized && !preview.motionDisposed) Disposer.dispose(preview)
        root = null
    }

    private fun buildControls() {
        enabled = checkBox("settings.enabled")
        reducedMotion = checkBox("settings.reducedMotion")
        profile = JComboBox(MotionProfileId.entries.toTypedArray()).apply {
            accessibleContext.accessibleName = MotionFlowBundle.message("settings.profile")
            renderer = enumRenderer { value -> MotionFlowBundle.message("settings.profile.${value.name.lowercase()}") }
            addActionListener {
                if (!updating) {
                    applyProfile(selectedItem as MotionProfileId)
                    updateEnabledState()
                }
            }
        }
        durationScale = doubleSpinner(0.25, 2.0, 0.05)
        intensityScale = doubleSpinner(0.25, 1.5, 0.05)
        powerSaveBehavior = JComboBox(PowerSaveBehavior.entries.toTypedArray()).apply {
            accessibleContext.accessibleName = MotionFlowBundle.message("settings.powerSave")
            renderer = enumRenderer { value -> MotionFlowBundle.message("settings.powerSave.${value.name.lowercase()}") }
        }
        caretTrail = checkBox("settings.caretTrail")
        caretTrailLength = intSpinner(2, 12, 1)
        caretTrailStyle = JComboBox(CaretTrailStyle.entries.toTypedArray()).apply {
            accessibleContext.accessibleName = MotionFlowBundle.message("settings.trailStyle")
            renderer = enumRenderer { value -> MotionFlowBundle.message("settings.trailStyle.${value.name.lowercase()}") }
            addActionListener(::markCustom)
        }
        caretTrailCoverage = JComboBox(CaretTrailCoverage.entries.toTypedArray()).apply {
            accessibleContext.accessibleName = MotionFlowBundle.message("settings.trailCoverage")
            renderer = enumRenderer { value -> MotionFlowBundle.message("settings.trailCoverage.${value.name.lowercase()}") }
            addActionListener(::markCustom)
        }
        caretTrailVisibility = JComboBox(CaretTrailVisibility.entries.toTypedArray()).apply {
            accessibleContext.accessibleName = MotionFlowBundle.message("settings.trailVisibility")
            renderer = enumRenderer { value -> MotionFlowBundle.message("settings.trailVisibility.${value.name.lowercase()}") }
            addActionListener(::markCustom)
        }
        clickRipple = checkBox("settings.clickRipple")
        navigationPulse = checkBox("settings.navigationPulse")
        navigationThreshold = intSpinner(3, 50, 1)
        editorContext = checkBox("settings.editorContext")
        toolWindow = checkBox("settings.toolWindow")
        inactiveFrameDim = checkBox("settings.inactiveFrameDim")
        inactiveFrameDimAmount = doubleSpinner(0.04, 0.25, 0.01)
        preview = MotionPreview()

        enabled.addActionListener { updateEnabledState() }
        reducedMotion.addActionListener { updateEnabledState() }
        inactiveFrameDim.addActionListener { updateEnabledState() }
        listOf(caretTrail, clickRipple, navigationPulse, editorContext, toolWindow, inactiveFrameDim).forEach {
            it.addActionListener(::markCustom)
        }
        listOf(durationScale, intensityScale, caretTrailLength, navigationThreshold, inactiveFrameDimAmount).forEach {
            it.addChangeListener { if (!updating) profile.selectedItem = MotionProfileId.CUSTOM }
        }
    }

    private fun topPanel(): JComponent = JPanel(FlowLayout(FlowLayout.LEFT, 12, 4)).apply {
        alignmentX = Component.LEFT_ALIGNMENT
        add(enabled)
        add(JLabel(MotionFlowBundle.message("settings.profile")))
        add(profile)
        add(reducedMotion)
    }

    private fun previewPanel(): JComponent = section("settings.preview") {
        gridRow(MotionFlowBundle.message("settings.preview.description"), preview)
        val actions = JPanel(FlowLayout(FlowLayout.LEFT, 6, 0)).apply {
            add(JButton(MotionFlowBundle.message("settings.preview.play")).apply {
                addActionListener { preview.play(service<MotionPolicyService>().resolveFor(readState())) }
            })
            add(JButton(MotionFlowBundle.message("settings.preview.pause")).apply {
                addActionListener { preview.pause() }
            })
        }
        gridRow("", actions)
    }

    private fun editorPanel(): JComponent = section("settings.editor") {
        gridRow("", caretTrail)
        gridRow(MotionFlowBundle.message("settings.trailLength"), caretTrailLength)
        gridRow(MotionFlowBundle.message("settings.trailStyle"), caretTrailStyle)
        gridRow(MotionFlowBundle.message("settings.trailCoverage"), caretTrailCoverage)
        gridRow(MotionFlowBundle.message("settings.trailVisibility"), caretTrailVisibility)
        gridRow("", clickRipple)
        gridRow("", navigationPulse)
        gridRow(MotionFlowBundle.message("settings.navigationThreshold"), navigationThreshold)
    }

    private fun workbenchPanel(): JComponent = section("settings.workbench") {
        gridRow("", editorContext)
        gridRow("", toolWindow)
        gridRow("", inactiveFrameDim)
        gridRow(MotionFlowBundle.message("settings.dimAmount"), inactiveFrameDimAmount)
    }

    private fun behaviorPanel(): JComponent = section("settings.behavior") {
        gridRow(MotionFlowBundle.message("settings.durationScale"), durationScale)
        gridRow(MotionFlowBundle.message("settings.intensityScale"), intensityScale)
        gridRow(MotionFlowBundle.message("settings.powerSave"), powerSaveBehavior)
    }

    private fun diagnosticsPanel(): JComponent = section("settings.diagnostics") {
        gridRow(MotionFlowBundle.message("settings.diagnostics.description"), JPanel(FlowLayout(FlowLayout.LEFT, 6, 0)).apply {
            add(JButton(MotionFlowBundle.message("settings.copyDiagnostics")).apply {
                addActionListener {
                    CopyPasteManager.getInstance().setContents(StringSelection(service<DiagnosticsService>().snapshot()))
                }
            })
            add(JButton(MotionFlowBundle.message("settings.resetDefaults")).apply {
                addActionListener { writeState(MotionFlowState()) }
            })
        })
    }

    private fun section(titleKey: String, content: SectionBuilder.() -> Unit): JComponent {
        val panel = JPanel(GridBagLayout()).apply {
            alignmentX = Component.LEFT_ALIGNMENT
            border = BorderFactory.createTitledBorder(MotionFlowBundle.message(titleKey))
        }
        SectionBuilder(panel).content()
        return panel
    }

    private inner class SectionBuilder(private val panel: JPanel) {
        private var row = 0

        fun gridRow(labelText: String, component: JComponent) {
            val label = JLabel(labelText).apply { labelFor = component }
            panel.add(label, GridBagConstraints().apply {
                gridx = 0
                gridy = row
                anchor = GridBagConstraints.WEST
                insets = Insets(4, 6, 4, 12)
            })
            panel.add(component, GridBagConstraints().apply {
                gridx = 1
                gridy = row++
                weightx = 1.0
                fill = if (component === preview) GridBagConstraints.HORIZONTAL else GridBagConstraints.NONE
                anchor = GridBagConstraints.WEST
                insets = Insets(4, 0, 4, 6)
            })
        }
    }

    private fun writeState(state: MotionFlowState) {
        updating = true
        try {
            enabled.isSelected = state.enabled
            reducedMotion.isSelected = state.reducedMotion
            profile.selectedItem = MotionProfileId.entries.firstOrNull { it.name == state.profile } ?: MotionProfileId.BALANCED
            durationScale.value = state.durationScale
            intensityScale.value = state.intensityScale
            powerSaveBehavior.selectedItem = PowerSaveBehavior.entries.firstOrNull { it.name == state.powerSaveBehavior }
                ?: PowerSaveBehavior.DISABLE_ALL
            caretTrail.isSelected = state.caretTrailEnabled
            caretTrailLength.value = state.caretTrailLength
            caretTrailStyle.selectedItem = CaretTrailStyle.entries.firstOrNull { it.name == state.caretTrailStyle }
                ?: CaretTrailStyle.SILKY_TRAIL
            caretTrailCoverage.selectedItem = CaretTrailCoverage.entries.firstOrNull { it.name == state.caretTrailCoverage }
                ?: CaretTrailCoverage.ALL_CARETS
            caretTrailVisibility.selectedItem = CaretTrailVisibility.entries.firstOrNull { it.name == state.caretTrailVisibility }
                ?: CaretTrailVisibility.CLEAR
            clickRipple.isSelected = state.clickRippleEnabled
            navigationPulse.isSelected = state.navigationPulseEnabled
            navigationThreshold.value = state.navigationJumpThreshold
            editorContext.isSelected = state.editorContextCueEnabled
            toolWindow.isSelected = state.toolWindowCueEnabled
            inactiveFrameDim.isSelected = state.inactiveFrameDimEnabled
            inactiveFrameDimAmount.value = state.inactiveFrameDimAmount
        } finally {
            updating = false
            updateEnabledState()
        }
    }

    private fun readState(): MotionFlowState = MotionStateNormalizer.normalize(
        MotionFlowState(
            enabled = enabled.isSelected,
            reducedMotion = reducedMotion.isSelected,
            profile = (profile.selectedItem as MotionProfileId).name,
            durationScale = (durationScale.value as Number).toDouble(),
            intensityScale = (intensityScale.value as Number).toDouble(),
            powerSaveBehavior = (powerSaveBehavior.selectedItem as PowerSaveBehavior).name,
            caretTrailEnabled = caretTrail.isSelected,
            caretTrailLength = (caretTrailLength.value as Number).toInt(),
            caretTrailStyle = (caretTrailStyle.selectedItem as CaretTrailStyle).name,
            caretTrailCoverage = (caretTrailCoverage.selectedItem as CaretTrailCoverage).name,
            caretTrailVisibility = (caretTrailVisibility.selectedItem as CaretTrailVisibility).name,
            clickRippleEnabled = clickRipple.isSelected,
            navigationPulseEnabled = navigationPulse.isSelected,
            navigationJumpThreshold = (navigationThreshold.value as Number).toInt(),
            editorContextCueEnabled = editorContext.isSelected,
            toolWindowCueEnabled = toolWindow.isSelected,
            inactiveFrameDimEnabled = inactiveFrameDim.isSelected,
            inactiveFrameDimAmount = (inactiveFrameDimAmount.value as Number).toDouble(),
            diagnosticsDebugMode = service<MotionSettingsService>().snapshot().diagnosticsDebugMode,
        ),
    ).state

    private fun applyProfile(selected: MotionProfileId) {
        if (selected == MotionProfileId.CUSTOM) return
        val replacement = MotionStateNormalizer.defaults(selected).copy(
            enabled = enabled.isSelected,
            reducedMotion = reducedMotion.isSelected,
        )
        writeState(replacement)
    }

    private fun updateEnabledState() {
        val active = enabled.isSelected
        val spatial = active && !reducedMotion.isSelected
        profile.isEnabled = active
        caretTrail.isEnabled = spatial
        caretTrailLength.isEnabled = spatial && caretTrail.isSelected
        caretTrailStyle.isEnabled = spatial && caretTrail.isSelected
        caretTrailCoverage.isEnabled = spatial && caretTrail.isSelected
        caretTrailVisibility.isEnabled = spatial && caretTrail.isSelected
        clickRipple.isEnabled = spatial
        navigationPulse.isEnabled = active
        navigationThreshold.isEnabled = active && navigationPulse.isSelected
        editorContext.isEnabled = active
        toolWindow.isEnabled = active
        inactiveFrameDim.isEnabled = spatial
        inactiveFrameDimAmount.isEnabled = spatial && inactiveFrameDim.isSelected
        durationScale.isEnabled = active
        intensityScale.isEnabled = active
        powerSaveBehavior.isEnabled = active
    }

    private fun markCustom(@Suppress("UNUSED_PARAMETER") event: java.awt.event.ActionEvent) {
        if (!updating) profile.selectedItem = MotionProfileId.CUSTOM
        updateEnabledState()
    }

    private fun checkBox(key: String) = JCheckBox(MotionFlowBundle.message(key)).apply {
        accessibleContext.accessibleDescription = text
    }

    private fun doubleSpinner(min: Double, max: Double, step: Double) =
        JSpinner(SpinnerNumberModel(min, min, max, step))

    private fun intSpinner(min: Int, max: Int, step: Int) =
        JSpinner(SpinnerNumberModel(min, min, max, step))

    private fun <T : Enum<T>> enumRenderer(label: (T) -> String) = object : DefaultListCellRenderer() {
        @Suppress("UNCHECKED_CAST")
        override fun getListCellRendererComponent(
            list: JList<*>?,
            value: Any?,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean,
        ): Component {
            val component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
            if (value != null) text = label(value as T)
            return component
        }
    }

    companion object {
        const val ID = "io.github.myangprint.motionflow.settings"
    }
}
