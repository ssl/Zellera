package zel.protocol.generated

data class UnseenItemsOut(
    val categories: List<CategoriesItem>,
) {
    data class CategoriesItem(
        val category: Int,
        val itemIds: List<Int>,
    )
}

object UnseenItemsOutCodec : OutgoingCodec<UnseenItemsOut> {
    override val name = "UnseenItemsOut"

    override fun read(r: PacketReader): UnseenItemsOut = readUnseenItemsOut(r)

    override fun write(w: PacketWriter, m: UnseenItemsOut) = writeUnseenItemsOut(w, m)

    private fun readUnseenItemsOutCategoriesItem(r: PacketReader): UnseenItemsOut.CategoriesItem {
        val vCategory = r.int()
        val vItemIds = List(r.countInt(4)) { r.int() }
        return UnseenItemsOut.CategoriesItem(vCategory, vItemIds)
    }

    private fun readUnseenItemsOut(r: PacketReader): UnseenItemsOut {
        val vCategories = List(r.countInt(8)) { readUnseenItemsOutCategoriesItem(r) }
        return UnseenItemsOut(vCategories)
    }

    private fun writeUnseenItemsOutCategoriesItem(w: PacketWriter, m: UnseenItemsOut.CategoriesItem) {
        w.int(m.category)
        w.countInt(m.itemIds.size)
        m.itemIds.forEach { w.int(it) }
    }

    private fun writeUnseenItemsOut(w: PacketWriter, m: UnseenItemsOut) {
        w.countInt(m.categories.size)
        m.categories.forEach { writeUnseenItemsOutCategoriesItem(w, it) }
    }
}
