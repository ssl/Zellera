package zel.protocol.generated

data object GetNftStoreOffersIn

object GetNftStoreOffersInCodec : IncomingCodec<GetNftStoreOffersIn> {
    override val name = "GetNftStoreOffersIn"

    override fun read(r: PacketReader): GetNftStoreOffersIn = GetNftStoreOffersIn

    override fun write(w: PacketWriter, m: GetNftStoreOffersIn) = writeGetNftStoreOffersIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeGetNftStoreOffersIn(w: PacketWriter, m: GetNftStoreOffersIn) {
    }
}
