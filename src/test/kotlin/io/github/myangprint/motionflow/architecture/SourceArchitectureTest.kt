package io.github.myangprint.motionflow.architecture

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SourceArchitectureTest {
    private val root: Path = Path.of(System.getProperty("user.dir"))
    private val mainSource = root.resolve("src/main/kotlin")

    @Test
    fun `MF-S-004 through MF-S-006 stable source contains no forbidden mechanism`() {
        val forbidden = mapOf(
            "implementation package" to Regex("import\\s+com\\.intellij\\..*\\.impl(?:\\.|$)"),
            "internal glass pane" to Regex("IdeGlassPaneUtil"),
            "reflection" to Regex("(?:java\\.lang\\.reflect|kotlin\\.reflect)"),
            "JDK internals" to Regex("(?:sun\\.|jdk\\.internal)"),
            "network API" to Regex("(?:java\\.net|java\\.net\\.http|ktor|okhttp)"),
            "process-wide AWT hook" to Regex("AWTEventListener"),
            "instrumentation" to Regex("(?:javaagent|java\\.lang\\.instrument|ByteBuddy|ASM)"),
        )
        val source = kotlinSources().joinToString("\n") { it.readText() }
        forbidden.forEach { (name, pattern) -> assertFalse(pattern.containsMatchIn(source), name) }
    }

    @Test
    fun `MF-E-008 core has no surface settings UI VFS or PSI dependency`() {
        val coreSource = kotlinSources(mainSource.resolve("io/github/myangprint/motionflow/core"))
            .joinToString("\n") { it.readText() }
        listOf(".editor.", ".workbench.", ".settings.", ".ui.", ".diagnostics.", ".vfs.", ".psi.")
            .forEach { forbidden -> assertFalse(coreSource.contains(forbidden), forbidden) }
    }

    @Test
    fun `MF-E-002 and MF-P-001 exactly one Swing timer implementation exists`() {
        val source = kotlinSources().joinToString("\n") { it.readText() }
        assertEquals(1, Regex("javax\\.swing\\.Timer").findAll(source).count())
        assertEquals(1, Regex("Timer\\(FRAME_DELAY_MILLIS").findAll(source).count())
    }

    @Test
    fun `MF-F-001 startup marshals effect services to the EDT`() {
        val startup = mainSource.resolve(
            "io/github/myangprint/motionflow/startup/MotionFlowStartupActivity.kt",
        ).readText()
        assertTrue(startup.contains("ApplicationManager.getApplication().invokeLater"))
        assertTrue(startup.contains("ModalityState.nonModal()"))
        assertTrue(startup.contains("isExpired = { project.isDisposed }"))
    }

    @Test
    fun `MF-ED-013 split caret flow uses a shared root and local focus listeners`() {
        val flow = mainSource.resolve(
            "io/github/myangprint/motionflow/editor/CrossEditorCaretFlow.kt",
        ).readText()
        val session = mainSource.resolve(
            "io/github/myangprint/motionflow/editor/EditorEffectsSession.kt",
        ).readText()
        assertTrue(flow.contains("rootPane !== SwingUtilities.getRootPane(to.component)"))
        assertTrue(flow.contains("target = rootContent"))
        assertTrue(session.contains("addFocusListener(focusListener)"))
        assertTrue(session.contains("removeFocusListener(focusListener)"))
    }

    @Test
    fun `MF-ED-014 default speed line uses tapered gradient geometry`() {
        val geometry = mainSource.resolve(
            "io/github/myangprint/motionflow/editor/CaretFlowGeometry.kt",
        ).readText()
        val painter = mainSource.resolve(
            "io/github/myangprint/motionflow/editor/CaretTrailPainter.kt",
        ).readText()
        val state = mainSource.resolve(
            "io/github/myangprint/motionflow/settings/MotionFlowState.kt",
        ).readText()
        assertTrue(geometry.contains("fun speedLineRibbon"))
        assertTrue(painter.contains("LinearGradientPaint"))
        assertTrue(painter.contains("graphics.fill(path)"))
        assertTrue(state.contains("CaretTrailStyle.SPEED_LINES.name"))
    }

    @Test
    fun `MF-S-007 MVP has no third party runtime implementation dependency`() {
        val build = root.resolve("build.gradle.kts").readText()
        assertFalse(Regex("(?m)^\\s*(?:implementation|runtimeOnly)\\(").containsMatchIn(build))
        assertTrue(build.contains("testImplementation(kotlin(\"test-junit\"))"))
    }

    private fun kotlinSources(directory: Path = mainSource): List<Path> = Files.walk(directory).use { stream ->
        stream.filter { Files.isRegularFile(it) && it.extension == "kt" }.toList()
    }
}
