package zel.wired.chest

object Details {
    /** The furni of one move, a line per kind. */
    fun lines(furni: List<StoredFurni>): List<DetailLine> =
        furni
            .groupingBy { it.isWall to it.spriteId }
            .eachCount()
            .map { (kind, count) -> DetailLine(kind.first, kind.second, count) }
}
