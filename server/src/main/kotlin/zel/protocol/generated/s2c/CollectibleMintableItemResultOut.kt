package zel.protocol.generated

data class CollectibleMintableItemResultOut(
    val short1: Short,
)

object CollectibleMintableItemResultOutCodec : OutgoingCodec<CollectibleMintableItemResultOut> {
    override val name = "CollectibleMintableItemResultOut"

    override fun read(r: PacketReader): CollectibleMintableItemResultOut = readCollectibleMintableItemResultOut(r)

    override fun write(w: PacketWriter, m: CollectibleMintableItemResultOut) = writeCollectibleMintableItemResultOut(w, m)

    private fun readCollectibleMintableItemResultOut(r: PacketReader): CollectibleMintableItemResultOut {
        val vShort1 = r.short()
        return CollectibleMintableItemResultOut(vShort1)
    }

    private fun writeCollectibleMintableItemResultOut(w: PacketWriter, m: CollectibleMintableItemResultOut) {
        w.short(m.short1)
    }
}
