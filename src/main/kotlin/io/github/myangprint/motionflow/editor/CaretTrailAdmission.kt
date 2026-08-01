package io.github.myangprint.motionflow.editor

/** Selects a deterministic, identity-distinct, bounded set with the primary caret first. */
internal object CaretTrailAdmission {
    fun <T : Any> select(
        primary: T,
        allCarets: List<T>,
        includeSecondary: Boolean,
        limit: Int,
    ): List<T> {
        require(limit >= 1) { "Caret trail limit must be positive" }
        val selected = ArrayList<T>(limit)
        selected += primary
        if (!includeSecondary) return selected
        allCarets.forEach { candidate ->
            if (selected.size >= limit) return@forEach
            if (selected.none { it === candidate }) selected += candidate
        }
        return selected
    }
}
