package zel.protocol.generated

data class NavigatorAddCollapsedCategoryIn(
    val category: String,
)

object NavigatorAddCollapsedCategoryInCodec : IncomingCodec<NavigatorAddCollapsedCategoryIn> {
    override val name = "NavigatorAddCollapsedCategoryIn"

    override fun read(r: PacketReader): NavigatorAddCollapsedCategoryIn = readNavigatorAddCollapsedCategoryIn(r)

    override fun write(w: PacketWriter, m: NavigatorAddCollapsedCategoryIn) = writeNavigatorAddCollapsedCategoryIn(w, m)

    private fun readNavigatorAddCollapsedCategoryIn(r: PacketReader): NavigatorAddCollapsedCategoryIn {
        val vCategory = r.string()
        return NavigatorAddCollapsedCategoryIn(vCategory)
    }

    private fun writeNavigatorAddCollapsedCategoryIn(w: PacketWriter, m: NavigatorAddCollapsedCategoryIn) {
        w.string(m.category)
    }
}
