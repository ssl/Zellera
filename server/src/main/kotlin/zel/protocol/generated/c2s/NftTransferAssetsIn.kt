package zel.protocol.generated

data class NftTransferAssetsIn(
    val string1: String,
)

object NftTransferAssetsInCodec : IncomingCodec<NftTransferAssetsIn> {
    override val name = "NftTransferAssetsIn"

    override fun read(r: PacketReader): NftTransferAssetsIn = readNftTransferAssetsIn(r)

    override fun write(w: PacketWriter, m: NftTransferAssetsIn) = writeNftTransferAssetsIn(w, m)

    private fun readNftTransferAssetsIn(r: PacketReader): NftTransferAssetsIn {
        val vString1 = r.string()
        return NftTransferAssetsIn(vString1)
    }

    private fun writeNftTransferAssetsIn(w: PacketWriter, m: NftTransferAssetsIn) {
        w.string(m.string1)
    }
}
