package zel.protocol.generated

data class NavigatorAddSavedSearchIn(
    val searchCode: String,
    val filter: String,
)

object NavigatorAddSavedSearchInCodec : IncomingCodec<NavigatorAddSavedSearchIn> {
    override val name = "NavigatorAddSavedSearchIn"

    override fun read(r: PacketReader): NavigatorAddSavedSearchIn = readNavigatorAddSavedSearchIn(r)

    override fun write(w: PacketWriter, m: NavigatorAddSavedSearchIn) = writeNavigatorAddSavedSearchIn(w, m)

    private fun readNavigatorAddSavedSearchIn(r: PacketReader): NavigatorAddSavedSearchIn {
        val vSearchCode = r.string()
        val vFilter = r.string()
        return NavigatorAddSavedSearchIn(vSearchCode, vFilter)
    }

    private fun writeNavigatorAddSavedSearchIn(w: PacketWriter, m: NavigatorAddSavedSearchIn) {
        w.string(m.searchCode)
        w.string(m.filter)
    }
}
