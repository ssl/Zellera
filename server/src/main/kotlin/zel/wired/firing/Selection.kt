package zel.wired.firing

class Selection {
    val furniIds = LinkedHashSet<Int>()
    val unitIndexes = LinkedHashSet<Int>()

    fun copy(): Selection =
        Selection().also {
            it.furniIds += furniIds
            it.unitIndexes += unitIndexes
        }
}
