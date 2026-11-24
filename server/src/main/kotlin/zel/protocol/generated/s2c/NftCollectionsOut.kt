package zel.protocol.generated

data class NftCollectionsOut(
    val rest: ByteArray,
)

object NftCollectionsOutCodec : OutgoingCodec<NftCollectionsOut> {
    override val name = "NftCollectionsOut"

    override fun read(r: PacketReader): NftCollectionsOut = readNftCollectionsOut(r)

    override fun write(w: PacketWriter, m: NftCollectionsOut) = writeNftCollectionsOut(w, m)

    private fun readNftCollectionsOut(r: PacketReader): NftCollectionsOut {
        val vRest = r.rest()
        return NftCollectionsOut(vRest)
    }

    private fun writeNftCollectionsOut(w: PacketWriter, m: NftCollectionsOut) {
        w.bytes(m.rest)
    }
}
