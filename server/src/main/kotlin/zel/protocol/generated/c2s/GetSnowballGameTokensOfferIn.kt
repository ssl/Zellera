package zel.protocol.generated

data object GetSnowballGameTokensOfferIn

object GetSnowballGameTokensOfferInCodec : IncomingCodec<GetSnowballGameTokensOfferIn> {
    override val name = "GetSnowballGameTokensOfferIn"

    override fun read(r: PacketReader): GetSnowballGameTokensOfferIn = GetSnowballGameTokensOfferIn

    override fun write(w: PacketWriter, m: GetSnowballGameTokensOfferIn) = writeGetSnowballGameTokensOfferIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeGetSnowballGameTokensOfferIn(w: PacketWriter, m: GetSnowballGameTokensOfferIn) {
    }
}
