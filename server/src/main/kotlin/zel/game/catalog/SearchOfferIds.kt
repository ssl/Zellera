package zel.game.catalog

internal object SearchOfferIds {
    fun rowIdFree(
        taken: Set<Int>,
        rowId: Int,
    ): Boolean = rowId > 0 && rowId !in taken

    fun pageWins(
        current: CatalogPage?,
        candidate: CatalogPage,
    ): Boolean = current == null || candidate.isOpen
}
