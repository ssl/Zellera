package zel.protocol.generated

data object GetRoomAdPurchaseInfoIn

object GetRoomAdPurchaseInfoInCodec : IncomingCodec<GetRoomAdPurchaseInfoIn> {
    override val name = "GetRoomAdPurchaseInfoIn"

    override fun read(r: PacketReader): GetRoomAdPurchaseInfoIn = GetRoomAdPurchaseInfoIn

    override fun write(w: PacketWriter, m: GetRoomAdPurchaseInfoIn) = writeGetRoomAdPurchaseInfoIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeGetRoomAdPurchaseInfoIn(w: PacketWriter, m: GetRoomAdPurchaseInfoIn) {
    }
}
