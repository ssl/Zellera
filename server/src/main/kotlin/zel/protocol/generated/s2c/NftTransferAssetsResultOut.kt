package zel.protocol.generated

data class NftTransferAssetsResultOut(
    val short1: Short,
)

object NftTransferAssetsResultOutCodec : OutgoingCodec<NftTransferAssetsResultOut> {
    override val name = "NftTransferAssetsResultOut"

    override fun read(r: PacketReader): NftTransferAssetsResultOut = readNftTransferAssetsResultOut(r)

    override fun write(w: PacketWriter, m: NftTransferAssetsResultOut) = writeNftTransferAssetsResultOut(w, m)

    private fun readNftTransferAssetsResultOut(r: PacketReader): NftTransferAssetsResultOut {
        val vShort1 = r.short()
        return NftTransferAssetsResultOut(vShort1)
    }

    private fun writeNftTransferAssetsResultOut(w: PacketWriter, m: NftTransferAssetsResultOut) {
        w.short(m.short1)
    }
}
