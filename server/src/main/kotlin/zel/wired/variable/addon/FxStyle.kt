package zel.wired.variable.addon

data class FxStyle(
    val renderer: Int,
    val extra: List<Pair<String, String>> = emptyList(),
)
