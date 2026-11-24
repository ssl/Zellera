package zel.protocol.generated

data object GetMintTokenOffersIn

object GetMintTokenOffersInCodec : IncomingCodec<GetMintTokenOffersIn> {
    override val name = "GetMintTokenOffersIn"

    override fun read(r: PacketReader): GetMintTokenOffersIn = GetMintTokenOffersIn

    override fun write(w: PacketWriter, m: GetMintTokenOffersIn) = writeGetMintTokenOffersIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeGetMintTokenOffersIn(w: PacketWriter, m: GetMintTokenOffersIn) {
    }
}
