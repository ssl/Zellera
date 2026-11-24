package zel.protocol.generated

data class NftCollectionsScoreOut(
    val int1: Int,
    val int2: Int,
    val int3: Int,
)

object NftCollectionsScoreOutCodec : OutgoingCodec<NftCollectionsScoreOut> {
    override val name = "NftCollectionsScoreOut"

    override fun read(r: PacketReader): NftCollectionsScoreOut = readNftCollectionsScoreOut(r)

    override fun write(w: PacketWriter, m: NftCollectionsScoreOut) = writeNftCollectionsScoreOut(w, m)

    private fun readNftCollectionsScoreOut(r: PacketReader): NftCollectionsScoreOut {
        val vInt1 = r.int()
        val vInt2 = r.int()
        val vInt3 = r.int()
        return NftCollectionsScoreOut(vInt1, vInt2, vInt3)
    }

    private fun writeNftCollectionsScoreOut(w: PacketWriter, m: NftCollectionsScoreOut) {
        w.int(m.int1)
        w.int(m.int2)
        w.int(m.int3)
    }
}
