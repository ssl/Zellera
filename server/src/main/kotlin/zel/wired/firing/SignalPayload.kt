package zel.wired.firing

data class SignalPayload(
    val furniIds: List<Int>,
    val unitIndexes: List<Int>,
    val contextValues: Map<String, Long>,
    val through: Int = 0,
    val text: String = "",
    val placeholders: Map<String, String> = emptyMap(),
    val hops: Int = 0,
)
