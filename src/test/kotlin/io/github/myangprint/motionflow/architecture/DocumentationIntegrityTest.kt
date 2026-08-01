package io.github.myangprint.motionflow.architecture

import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.readBytes
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DocumentationIntegrityTest {
    private val root: Path = Path.of(System.getProperty("user.dir"))

    @Test
    fun `local Markdown links resolve`() {
        val broken = mutableListOf<String>()
        markdownFiles().forEach { document ->
            LINK.findAll(document.readText()).forEach { match ->
                val rawTarget = match.groupValues[1].trim().removeSurrounding("<", ">")
                if (rawTarget.startsWith("#") || "://" in rawTarget || rawTarget.startsWith("mailto:")) return@forEach
                val pathText = URLDecoder.decode(rawTarget.substringBefore('#'), StandardCharsets.UTF_8)
                if (pathText.isBlank()) return@forEach
                val resolved = document.parent.resolve(pathText).normalize()
                if (!Files.exists(resolved)) broken += "${root.relativize(document)} -> $rawTarget"
            }
        }
        assertTrue(broken.isEmpty(), "Broken local links:\n${broken.joinToString("\n")}")
    }

    @Test
    fun `PRD requirements have exactly one traceability row`() {
        val prdIds = REQUIREMENT.findAll(root.resolve("docs/PRD.md").readText()).map { it.value }.toSet()
        val matrixMatches = REQUIREMENT.findAll(root.resolve("docs/traceability-matrix.md").readText())
            .map { it.value }
            .toList()
        val matrixIds = matrixMatches.toSet()
        assertEquals(prdIds, matrixIds)
        assertEquals(matrixIds.size, matrixMatches.size, "Traceability requirement IDs must be unique")
    }

    @Test
    fun `release version is consistent across metadata`() {
        val properties = root.resolve("gradle.properties").readText()
        val version = Regex("(?m)^version\\s*=\\s*(.+)$").find(properties)?.groupValues?.get(1)?.trim()
            ?: error("Missing project version")
        assertTrue(root.resolve("CHANGELOG.md").readText().contains("## [$version]"))
        assertTrue(root.resolve("README.md").readText().contains("`$version`"))
    }

    @Test
    fun `brand assets are present and nonempty`() {
        val assets = root.resolve("docs/assets")
        listOf("motionflow-logo.svg", "architecture.svg", "effects.svg").forEach { name ->
            val text = assets.resolve(name).readText()
            assertTrue(text.startsWith("<svg"), "$name must be an SVG")
            assertTrue(text.length > 500, "$name is unexpectedly small")
        }
        val hero = assets.resolve("motionflow-hero.png").readBytes()
        assertTrue(hero.size > 100_000, "Hero image is unexpectedly small")
        assertTrue(hero.take(8).toByteArray().contentEquals(PNG_SIGNATURE))
    }

    private fun markdownFiles(): List<Path> = Files.walk(root).use { stream ->
        stream.filter {
            Files.isRegularFile(it) &&
                it.extension == "md" &&
                root.relativize(it).none { part -> part.toString() in EXCLUDED_DIRECTORIES }
        }.toList()
    }

    private companion object {
        val LINK = Regex("\\[[^]]*]\\(([^)]+)\\)")
        val REQUIREMENT = Regex("MF-[A-Z]+-\\d{3}")
        val EXCLUDED_DIRECTORIES = setOf("build", ".gradle", ".idea", ".intellijPlatform")
        val PNG_SIGNATURE = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
    }
}
