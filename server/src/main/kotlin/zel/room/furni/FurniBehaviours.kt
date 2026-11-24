package zel.room.furni

class FurniBehaviours(
    entries: List<Pair<String, FurniBehaviour>>,
    private val fallback: (String) -> FurniBehaviour = { PlainFurni },
) {
    private val byType: Map<String, FurniBehaviour>

    init {
        val dup = entries.groupBy { it.first }.filterValues { it.size > 1 }.keys
        require(dup.isEmpty()) { "interaction types with two behaviours: $dup" }
        byType = entries.toMap()
    }

    fun forType(interactionType: String): FurniBehaviour = byType[interactionType] ?: fallback(interactionType)
}
