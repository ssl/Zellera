package zel.protocol.generated

data object GetBasicMembershipExtendOfferIn

object GetBasicMembershipExtendOfferInCodec : IncomingCodec<GetBasicMembershipExtendOfferIn> {
    override val name = "GetBasicMembershipExtendOfferIn"

    override fun read(r: PacketReader): GetBasicMembershipExtendOfferIn = GetBasicMembershipExtendOfferIn

    override fun write(w: PacketWriter, m: GetBasicMembershipExtendOfferIn) = writeGetBasicMembershipExtendOfferIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeGetBasicMembershipExtendOfferIn(w: PacketWriter, m: GetBasicMembershipExtendOfferIn) {
    }
}
