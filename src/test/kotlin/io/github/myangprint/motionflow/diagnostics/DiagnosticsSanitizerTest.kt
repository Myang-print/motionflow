package io.github.myangprint.motionflow.diagnostics

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DiagnosticsSanitizerTest {
    @Test
    fun `MF-S-003 sanitizer rejects paths identities and secrets`() {
        assertFalse(DiagnosticsSanitizer.isSafe("path=C:\\Users\\alice\\secret.kt"))
        assertFalse(DiagnosticsSanitizer.isSafe("home=/home/alice/project"))
        assertFalse(DiagnosticsSanitizer.isSafe("OPENAI_API_KEY=redacted"))
        assertFalse(DiagnosticsSanitizer.isSafe("hostname=developer-laptop"))
    }

    @Test
    fun `MF-F-006 schema-only diagnostics remain safe`() {
        assertTrue(
            DiagnosticsSanitizer.isSafe(
                "MotionFlow diagnostics\npluginVersion=0.1.0\nprofile=BALANCED\nerrors=none",
            ),
        )
    }
}
