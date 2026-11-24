package zel.protocol.generated

data class NavigatorRemoveCollapsedCategoryIn(
    val category: String,
)

object NavigatorRemoveCollapsedCategoryInCodec : IncomingCodec<NavigatorRemoveCollapsedCategoryIn> {
    override val name = "NavigatorRemoveCollapsedCategoryIn"

    override fun read(r: PacketReader): NavigatorRemoveCollapsedCategoryIn = readNavigatorRemoveCollapsedCategoryIn(r)

    override fun write(w: PacketWriter, m: NavigatorRemoveCollapsedCategoryIn) = writeNavigatorRemoveCollapsedCategoryIn(w, m)

    private fun readNavigatorRemoveCollapsedCategoryIn(r: PacketReader): NavigatorRemoveCollapsedCategoryIn {
        val vCategory = r.string()
        return NavigatorRemoveCollapsedCategoryIn(vCategory)
    }

    private fun writeNavigatorRemoveCollapsedCategoryIn(w: PacketWriter, m: NavigatorRemoveCollapsedCategoryIn) {
        w.string(m.category)
    }
}
