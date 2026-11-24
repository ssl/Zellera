package zel.protocol.generated

data class NavigatorSearchResultBlocksOut(
    val string1: String,
    val string2: String,
    val rest: ByteArray,
)

object NavigatorSearchResultBlocksOutCodec : OutgoingCodec<NavigatorSearchResultBlocksOut> {
    override val name = "NavigatorSearchResultBlocksOut"

    override fun read(r: PacketReader): NavigatorSearchResultBlocksOut = readNavigatorSearchResultBlocksOut(r)

    override fun write(w: PacketWriter, m: NavigatorSearchResultBlocksOut) = writeNavigatorSearchResultBlocksOut(w, m)

    private fun readNavigatorSearchResultBlocksOut(r: PacketReader): NavigatorSearchResultBlocksOut {
        val vString1 = r.string()
        val vString2 = r.string()
        val vRest = r.rest()
        return NavigatorSearchResultBlocksOut(vString1, vString2, vRest)
    }

    private fun writeNavigatorSearchResultBlocksOut(w: PacketWriter, m: NavigatorSearchResultBlocksOut) {
        w.string(m.string1)
        w.string(m.string2)
        w.bytes(m.rest)
    }
}
