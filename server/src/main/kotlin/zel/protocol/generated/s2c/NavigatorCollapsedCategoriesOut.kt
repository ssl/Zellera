package zel.protocol.generated

data class NavigatorCollapsedCategoriesOut(
    val categories: List<String>,
)

object NavigatorCollapsedCategoriesOutCodec : OutgoingCodec<NavigatorCollapsedCategoriesOut> {
    override val name = "NavigatorCollapsedCategoriesOut"

    override fun read(r: PacketReader): NavigatorCollapsedCategoriesOut = readNavigatorCollapsedCategoriesOut(r)

    override fun write(w: PacketWriter, m: NavigatorCollapsedCategoriesOut) = writeNavigatorCollapsedCategoriesOut(w, m)

    private fun readNavigatorCollapsedCategoriesOut(r: PacketReader): NavigatorCollapsedCategoriesOut {
        val vCategories = List(r.countInt(2)) { r.string() }
        return NavigatorCollapsedCategoriesOut(vCategories)
    }

    private fun writeNavigatorCollapsedCategoriesOut(w: PacketWriter, m: NavigatorCollapsedCategoriesOut) {
        w.countInt(m.categories.size)
        m.categories.forEach { w.string(it) }
    }
}
