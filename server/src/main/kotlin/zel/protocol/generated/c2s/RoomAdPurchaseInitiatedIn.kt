package zel.protocol.generated

data object RoomAdPurchaseInitiatedIn

object RoomAdPurchaseInitiatedInCodec : IncomingCodec<RoomAdPurchaseInitiatedIn> {
    override val name = "RoomAdPurchaseInitiatedIn"

    override fun read(r: PacketReader): RoomAdPurchaseInitiatedIn = RoomAdPurchaseInitiatedIn

    override fun write(w: PacketWriter, m: RoomAdPurchaseInitiatedIn) = writeRoomAdPurchaseInitiatedIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeRoomAdPurchaseInitiatedIn(w: PacketWriter, m: RoomAdPurchaseInitiatedIn) {
    }
}
