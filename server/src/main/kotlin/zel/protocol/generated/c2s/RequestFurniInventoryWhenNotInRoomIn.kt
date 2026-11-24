package zel.protocol.generated

data object RequestFurniInventoryWhenNotInRoomIn

object RequestFurniInventoryWhenNotInRoomInCodec : IncomingCodec<RequestFurniInventoryWhenNotInRoomIn> {
    override val name = "RequestFurniInventoryWhenNotInRoomIn"

    override fun read(r: PacketReader): RequestFurniInventoryWhenNotInRoomIn = RequestFurniInventoryWhenNotInRoomIn

    override fun write(w: PacketWriter, m: RequestFurniInventoryWhenNotInRoomIn) = writeRequestFurniInventoryWhenNotInRoomIn(w, m)

    @Suppress("UNUSED_PARAMETER")
    private fun writeRequestFurniInventoryWhenNotInRoomIn(w: PacketWriter, m: RequestFurniInventoryWhenNotInRoomIn) {
    }
}
