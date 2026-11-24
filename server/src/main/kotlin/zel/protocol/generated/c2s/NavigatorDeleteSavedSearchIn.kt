package zel.protocol.generated

data class NavigatorDeleteSavedSearchIn(
    val searchId: Int,
)

object NavigatorDeleteSavedSearchInCodec : IncomingCodec<NavigatorDeleteSavedSearchIn> {
    override val name = "NavigatorDeleteSavedSearchIn"

    override fun read(r: PacketReader): NavigatorDeleteSavedSearchIn = readNavigatorDeleteSavedSearchIn(r)

    override fun write(w: PacketWriter, m: NavigatorDeleteSavedSearchIn) = writeNavigatorDeleteSavedSearchIn(w, m)

    private fun readNavigatorDeleteSavedSearchIn(r: PacketReader): NavigatorDeleteSavedSearchIn {
        val vSearchId = r.int()
        return NavigatorDeleteSavedSearchIn(vSearchId)
    }

    private fun writeNavigatorDeleteSavedSearchIn(w: PacketWriter, m: NavigatorDeleteSavedSearchIn) {
        w.int(m.searchId)
    }
}
