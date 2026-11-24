package zel.protocol.generated

data class CollectibleMintTokenCountOut(
    val int1: Int,
)

object CollectibleMintTokenCountOutCodec : OutgoingCodec<CollectibleMintTokenCountOut> {
    override val name = "CollectibleMintTokenCountOut"

    override fun read(r: PacketReader): CollectibleMintTokenCountOut = readCollectibleMintTokenCountOut(r)

    override fun write(w: PacketWriter, m: CollectibleMintTokenCountOut) = writeCollectibleMintTokenCountOut(w, m)

    private fun readCollectibleMintTokenCountOut(r: PacketReader): CollectibleMintTokenCountOut {
        val vInt1 = r.int()
        return CollectibleMintTokenCountOut(vInt1)
    }

    private fun writeCollectibleMintTokenCountOut(w: PacketWriter, m: CollectibleMintTokenCountOut) {
        w.int(m.int1)
    }
}
