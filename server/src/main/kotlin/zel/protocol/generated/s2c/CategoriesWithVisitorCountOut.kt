package zel.protocol.generated

data class CategoriesWithVisitorCountOut(
    val categories: List<CategoriesItem>,
) {
    data class CategoriesItem(
        val categoryId: Int,
        val userCount: Int,
        val maxUserCount: Int,
    )
}

object CategoriesWithVisitorCountOutCodec : OutgoingCodec<CategoriesWithVisitorCountOut> {
    override val name = "CategoriesWithVisitorCountOut"

    override fun read(r: PacketReader): CategoriesWithVisitorCountOut = readCategoriesWithVisitorCountOut(r)

    override fun write(w: PacketWriter, m: CategoriesWithVisitorCountOut) = writeCategoriesWithVisitorCountOut(w, m)

    private fun readCategoriesWithVisitorCountOutCategoriesItem(r: PacketReader): CategoriesWithVisitorCountOut.CategoriesItem {
        val vCategoryId = r.int()
        val vUserCount = r.int()
        val vMaxUserCount = r.int()
        return CategoriesWithVisitorCountOut.CategoriesItem(vCategoryId, vUserCount, vMaxUserCount)
    }

    private fun readCategoriesWithVisitorCountOut(r: PacketReader): CategoriesWithVisitorCountOut {
        val vCategories = List(r.countInt(12)) { readCategoriesWithVisitorCountOutCategoriesItem(r) }
        return CategoriesWithVisitorCountOut(vCategories)
    }

    private fun writeCategoriesWithVisitorCountOutCategoriesItem(w: PacketWriter, m: CategoriesWithVisitorCountOut.CategoriesItem) {
        w.int(m.categoryId)
        w.int(m.userCount)
        w.int(m.maxUserCount)
    }

    private fun writeCategoriesWithVisitorCountOut(w: PacketWriter, m: CategoriesWithVisitorCountOut) {
        w.countInt(m.categories.size)
        m.categories.forEach { writeCategoriesWithVisitorCountOutCategoriesItem(w, it) }
    }
}
