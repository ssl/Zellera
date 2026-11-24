package zel.protocol.generated

data class GetCollectibleMintTokensIn(
    val string1: String,
)

object GetCollectibleMintTokensInCodec : IncomingCodec<GetCollectibleMintTokensIn> {
    override val name = "GetCollectibleMintTokensIn"

    override fun read(r: PacketReader): GetCollectibleMintTokensIn = readGetCollectibleMintTokensIn(r)

    override fun write(w: PacketWriter, m: GetCollectibleMintTokensIn) = writeGetCollectibleMintTokensIn(w, m)

    private fun readGetCollectibleMintTokensIn(r: PacketReader): GetCollectibleMintTokensIn {
        val vString1 = r.string()
        return GetCollectibleMintTokensIn(vString1)
    }

    private fun writeGetCollectibleMintTokensIn(w: PacketWriter, m: GetCollectibleMintTokensIn) {
        w.string(m.string1)
    }
}
