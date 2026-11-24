package zel.wired.variable

data class ValueTexts(
    val entries: List<Pair<Int, String>>,
    val fallback: String? = null,
)
