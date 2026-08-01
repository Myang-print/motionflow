package io.github.myangprint.motionflow.diagnostics

object DiagnosticsSanitizer {
    private val forbiddenPatterns = listOf(
        Regex("(?i)(?:[a-z]:\\\\|/users/|/home/|\\\\\\\\)[^\\s]+"),
        Regex("(?i)\\b(?:user|home|project|file|path|host)(?:name)?\\b\\s*=[^\\s]+"),
        Regex("(?i)OPENAI_API_KEY|GITHUB_TOKEN|PASSWORD|SECRET"),
    )

    fun isSafe(text: String): Boolean = forbiddenPatterns.none { it.containsMatchIn(text) }
}
