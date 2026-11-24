package zel.protocol.generated

data object GetClubExtendOfferIn

object GetClubExtendOfferInCodec : IncomingCodec<GetClubExtendOfferIn> {
    override val name = "GetClubExtendOfferIn"

    override fun read(r: PacketReader): GetClubExtendOfferIn = GetClubExtendOfferIn

    override fun write(w: PacketWriter, m: GetClubExtendOfferIn) = writeGetClubExtendOfferIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeGetClubExtendOfferIn(w: PacketWriter, m: GetClubExtendOfferIn) {
    }
}
