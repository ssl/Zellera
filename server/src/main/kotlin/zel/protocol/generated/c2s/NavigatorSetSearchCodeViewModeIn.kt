package zel.protocol.generated

data class NavigatorSetSearchCodeViewModeIn(
    val category: String,
    val viewMode: Int,
)

object NavigatorSetSearchCodeViewModeInCodec : IncomingCodec<NavigatorSetSearchCodeViewModeIn> {
    override val name = "NavigatorSetSearchCodeViewModeIn"

    override fun read(r: PacketReader): NavigatorSetSearchCodeViewModeIn = readNavigatorSetSearchCodeViewModeIn(r)

    override fun write(w: PacketWriter, m: NavigatorSetSearchCodeViewModeIn) = writeNavigatorSetSearchCodeViewModeIn(w, m)

    private fun readNavigatorSetSearchCodeViewModeIn(r: PacketReader): NavigatorSetSearchCodeViewModeIn {
        val vCategory = r.string()
        val vViewMode = r.int()
        return NavigatorSetSearchCodeViewModeIn(vCategory, vViewMode)
    }

    private fun writeNavigatorSetSearchCodeViewModeIn(w: PacketWriter, m: NavigatorSetSearchCodeViewModeIn) {
        w.string(m.category)
        w.int(m.viewMode)
    }
}
