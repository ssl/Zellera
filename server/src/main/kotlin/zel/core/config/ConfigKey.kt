package zel.core.config

data class ConfigKey(
    val name: String,
    val kind: Kind,
    val default: String,
    val description: String,
) {
    /** Blank always passes: set to nothing is as good as unset. */
    enum class Kind(
        val label: String,
        private val wanted: String,
        private val fits: (String) -> Boolean,
    ) {
        STRING("string", "text", { true }),
        INTEGER("integer", "an integer", { it.toIntOrNull() != null }),
        LONG("long", "a whole number", { it.toLongOrNull() != null }),
        BOOLEAN("boolean", "true, false, 1 or 0", { it.lowercase() in FLAG_WORDS }),
        ;

        fun problemWith(text: String): String? =
            text.trim().let { if (it.isEmpty() || fits(it)) null else "expected $wanted" }
    }
}

private val FLAG_WORDS = setOf("true", "false", "1", "0")
