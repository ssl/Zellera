package zel.protocol.generated

data class CollectibleMintingEnabledOut(
    val boolean1: Boolean,
)

object CollectibleMintingEnabledOutCodec : OutgoingCodec<CollectibleMintingEnabledOut> {
    override val name = "CollectibleMintingEnabledOut"

    override fun read(r: PacketReader): CollectibleMintingEnabledOut = readCollectibleMintingEnabledOut(r)

    override fun write(w: PacketWriter, m: CollectibleMintingEnabledOut) = writeCollectibleMintingEnabledOut(w, m)

    private fun readCollectibleMintingEnabledOut(r: PacketReader): CollectibleMintingEnabledOut {
        val vBoolean1 = r.boolean()
        return CollectibleMintingEnabledOut(vBoolean1)
    }

    private fun writeCollectibleMintingEnabledOut(w: PacketWriter, m: CollectibleMintingEnabledOut) {
        w.boolean(m.boolean1)
    }
}
